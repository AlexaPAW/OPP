package lab3.model.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import lab3.model.objects.Chest;
import lab3.model.objects.Enemy;
import lab3.model.objects.ExitPortal;
import lab3.model.objects.GameObject;
import lab3.model.objects.Player;
import lab3.model.world.Room;

/**
 * Игровая модель.
 *
 * Логика остаётся клеточной.
 * Плавность в GUI достигается интерполяцией между previousX/previousY и x/y.
 */
public class Game {
    private static final double DEFAULT_DELTA_SECONDS = 1.0 / 60.0;
    private volatile List<GameObject> objectsSnapshot = List.of();
    private double portalContactSeconds = 0.0;

    private Room currentRoom;
    private GameState state;

    private long tickCount;
    private int elapsedSeconds;

    private double deltaSeconds = DEFAULT_DELTA_SECONDS;

    private Room pendingRoom;
    private boolean roomTransitionRequested;
    private Supplier<Room> nextRoomSupplier;

    // для обновления выстрелов/объектов, которые появляются "одновременно"
    private final List<GameObject> pendingAdditions = new ArrayList<>();
    private final List<GameObject> pendingRemovals = new ArrayList<>();
    private final List<ScheduledSpawn> scheduledSpawns = new ArrayList<>();

    private static final class ScheduledSpawn {
        private final long dueTick;
        private final GameObject object;

        private ScheduledSpawn(long dueTick, GameObject object) {
            this.dueTick = dueTick;
            this.object = object;
        }
    }

    public Game(Room initialRoom) {
        this.currentRoom = Objects.requireNonNull(initialRoom, "initialRoom must not be null");
        this.state = GameState.READY;
        this.tickCount = 0L;
        this.elapsedSeconds = 0;
        this.roomTransitionRequested = false;
    }

    public synchronized void setNextRoomSupplier(Supplier<Room> nextRoomSupplier) {
        this.nextRoomSupplier = nextRoomSupplier;
    }

    public synchronized void goToNextRoom() {
        if (nextRoomSupplier == null) {
            return;
        }
        requestRoomTransition(nextRoomSupplier.get());
    }

    public synchronized void start() {
        if (state == GameState.READY || state == GameState.PAUSED) {
            state = GameState.RUNNING;
        }
    }

    public synchronized void pause() {
        if (state == GameState.RUNNING) {
            state = GameState.PAUSED;
        }
    }

    public synchronized void resume() {
        if (state == GameState.PAUSED) {
            state = GameState.RUNNING;
        }
    }

    public synchronized void stop() {
        state = GameState.STOPPED;
    }

    public synchronized void win() {
        state = GameState.WIN;
    }

    public synchronized void lose() {
        state = GameState.LOSE;
    }

    public synchronized void reset(Room initialRoom) {
        this.currentRoom = Objects.requireNonNull(initialRoom, "initialRoom must not be null");
        this.state = GameState.READY;
        this.tickCount = 0L;
        this.elapsedSeconds = 0;
        this.deltaSeconds = DEFAULT_DELTA_SECONDS;
        this.pendingRoom = null;
        this.roomTransitionRequested = false;
        this.portalContactSeconds = 0.0;
        this.pendingAdditions.clear();
        this.pendingRemovals.clear();
        this.scheduledSpawns.clear();
        refreshObjectsSnapshot();
    }

    public synchronized void addObjectLater(GameObject object, int delayTicks) {
        Objects.requireNonNull(object, "object must not be null");

        if (delayTicks <= 0) {
            pendingAdditions.add(object);
            return;
        }

        scheduledSpawns.add(new ScheduledSpawn(tickCount + delayTicks, object));
    }

    private void updatePortalContact(double deltaSeconds) {
        Player player = getPlayer().orElse(null);
        ExitPortal portal = getAliveExitPortal();

        if (player == null || portal == null) {
            portalContactSeconds = 0.0;
            return;
        }

        boolean standingOnPortal = portal.getBounds().contains(player.getCenterX(), player.getCenterY());

        if (standingOnPortal) {
            portalContactSeconds += deltaSeconds;

            if (portalContactSeconds >= GameBalance.PORTAL_STAY_SECONDS) {
                portalContactSeconds = 0.0;
                goToNextRoom();
            }
        } else {
            portalContactSeconds = 0.0;
        }
    }

    private ExitPortal getAliveExitPortal() {
        for (GameObject object : currentRoom.getObjects()) {
            if (object instanceof ExitPortal portal && portal.isAlive()) {
                return portal;
            }
        }
        return null;
    }

    /**
     * Сохраняет текущие координаты как "предыдущие" перед новым кадром.
     * Нужно вызывать ДО обработки ввода и ДО update(), иначе игрок теряет плавность.
     */
    public synchronized void beginFrame() {
        for (GameObject object : currentRoom.getObjects()) {
            if (object != null && object.isAlive()) {
                object.beginTick();
            }
        }
    }

    public synchronized void update() {
        update(deltaSeconds);
    }

    private void resolveContactDamage() {
        Player player = getPlayer().orElse(null);
        if (player == null || !player.isAlive()) {
            return;
        }

        long tick = tickCount;
        List<GameObject> snapshot = new ArrayList<>(currentRoom.getObjects());
        for (GameObject object : snapshot) {
            if (object instanceof Enemy enemy) {
                enemy.tryDealContactDamage(this, player, tick);
            }
        }
    }

    public synchronized void update(double deltaSeconds) {
        if (state != GameState.RUNNING) {
            return;
        }

        if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0.0) {
            throw new IllegalArgumentException("deltaSeconds must be finite and >= 0");
        }

        this.deltaSeconds = deltaSeconds;
        tickCount++;

        flushQueuedObjectChanges();

        List<GameObject> snapshot = new ArrayList<>(currentRoom.getObjects());
        for (GameObject object : snapshot) {
            object.update(this);
            if (state.isTerminal()) {
                break;
            }
        }

        resolveContactDamage();

        currentRoom.getObjects().removeIf(object -> !object.isAlive());

        flushQueuedObjectChanges();

        updatePortalContact(deltaSeconds);

        if (roomTransitionRequested && pendingRoom != null) {
            applyRoomTransition();
            refreshObjectsSnapshot();
            return;
        }

        spawnExitIfRoomCleared();
        refreshObjectsSnapshot();
    }

    /**
     * Портал всегда появляется в центре комнаты.
     */
    private void spawnExitIfRoomCleared() {
        if (nextRoomSupplier == null) {
            return;
        }

        if (hasExitPortal()) {
            return;
        }

        if (hasAliveEnemies() || hasAliveChests()) {
            return;
        }

        int x = (currentRoom.getWidth() - GameBalance.EXIT_PORTAL_SIZE) / 2;
        int y = (currentRoom.getHeight() - GameBalance.EXIT_PORTAL_SIZE) / 2;

        if (x < 0 || y < 0) {
            return;
        }

        currentRoom.getObjects().removeIf(object -> object instanceof ExitPortal);
        currentRoom.addObject(new ExitPortal(x, y));
    }

    private boolean hasAliveEnemies() {
        for (GameObject object : currentRoom.getObjects()) {
            if (object instanceof Enemy && object.isAlive()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAliveChests() {
        for (GameObject object : currentRoom.getObjects()) {
            if (object instanceof Chest && object.isAlive()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasExitPortal() {
        for (GameObject object : currentRoom.getObjects()) {
            if (object instanceof ExitPortal && object.isAlive()) {
                return true;
            }
        }
        return false;
    }

    public synchronized void requestRoomTransition(Room nextRoom) {
        this.pendingRoom = Objects.requireNonNull(nextRoom, "nextRoom must not be null");
        this.roomTransitionRequested = true;
    }

    private void applyRoomTransition() {
        Player player = getPlayer().orElse(null);

        if (player != null) {
            currentRoom.getObjects().remove(player);
        }

        currentRoom = pendingRoom;
        currentRoom.getObjects().removeIf(object -> object instanceof Player);

        if (player != null) {
            int spawnX = currentRoom.getPlayerSpawnX();
            int spawnY = currentRoom.getPlayerSpawnY();

            player.teleportTo(spawnX, spawnY);
            currentRoom.getObjects().add(player);
        }

        pendingRoom = null;
        roomTransitionRequested = false;
        pendingAdditions.clear();
        pendingRemovals.clear();
        scheduledSpawns.clear();
    }

    public synchronized Room getCurrentRoom() {
        return currentRoom;
    }

    public synchronized GameState getState() {
        return state;
    }

    public synchronized long getTickCount() {
        return tickCount;
    }

    public synchronized int getElapsedSeconds() {
        return elapsedSeconds;
    }

    public synchronized void incrementElapsedSeconds() {
        if (!state.isTerminal() && state != GameState.STOPPED) {
            elapsedSeconds++;
        }
    }

    public synchronized double getDeltaSeconds() {
        return deltaSeconds;
    }

    public synchronized boolean isRunning() {
        return state == GameState.RUNNING;
    }

    public synchronized boolean isPaused() {
        return state == GameState.PAUSED;
    }

    public synchronized boolean isFinished() {
        return state.isTerminal();
    }

    public List<GameObject> getObjectsSnapshot() {
        return objectsSnapshot;
    }

    public synchronized void addObject(GameObject object) {
        pendingAdditions.add(Objects.requireNonNull(object, "object must not be null"));
    }

    public synchronized void removeObject(GameObject object) {
        if (object != null) {
            pendingRemovals.add(object);
        }
    }

    private void refreshObjectsSnapshot() {
        objectsSnapshot = Collections.unmodifiableList(new ArrayList<>(currentRoom.getObjects()));
    }

    // метод применения очередей
    private void flushQueuedObjectChanges() {
        if (!scheduledSpawns.isEmpty()) {
            List<GameObject> ready = new ArrayList<>();
            scheduledSpawns.removeIf(spawn -> {
                if (spawn.dueTick <= tickCount) {
                    ready.add(spawn.object);
                    return true;
                }
                return false;
            });
            pendingAdditions.addAll(ready);
        }

        if (!pendingRemovals.isEmpty()) {
            java.util.Set<GameObject> removals = new java.util.HashSet<>(pendingRemovals);
            currentRoom.getObjects().removeIf(removals::contains);
            pendingRemovals.clear();
        }

        if (!pendingAdditions.isEmpty()) {
            currentRoom.getObjects().addAll(pendingAdditions);
            pendingAdditions.clear();
        }
        refreshObjectsSnapshot();
    }

    public synchronized Optional<Player> getPlayer() {
        for (GameObject object : currentRoom.getObjects()) {
            if (object instanceof Player player) {
                return Optional.of(player);
            }
        }
        return Optional.empty();
    }
}
