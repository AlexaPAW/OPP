package lab3.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import lab3.model.core.Game;
import lab3.model.core.GameState;
import lab3.model.objects.Chest;
import lab3.model.objects.Enemy;
import lab3.model.objects.EnemyKind;
import lab3.model.objects.GameObject;
import lab3.model.objects.Player;
import lab3.model.world.Room;

/**
 * Контроллер принимает абстрактные команды от UI:
 * - GUI: key bindings / buttons / menu items
 * - text UI: одиночные клавиши
 *
 * Контроллер НЕ читает строки из консоли и НЕ знает про KeyEvent.
 * Он работает только с InputCommand.
 */
public class GameController {
    private static final int BIG_OBJECT_SIZE = 4;

    private final Game game;
    private final Supplier<Room> newGameRoomSupplier;
    private final Random random = new Random();

    private boolean debugMode;
    private boolean godMode;

    private boolean exitRequested;
    private boolean aboutRequested;
    private boolean highScoresRequested;

    public GameController(Game game, Supplier<Room> newGameRoomSupplier) {
        this.game = Objects.requireNonNull(game, "game must not be null");
        this.newGameRoomSupplier = Objects.requireNonNull(newGameRoomSupplier, "newGameRoomSupplier must not be null");
    }

    private final ConcurrentLinkedQueue<InputCommand> inputQueue = new ConcurrentLinkedQueue<>();

    public void enqueueInput(InputCommand command) {
        if (command != null && command.getType() != CommandType.NONE) {
            inputQueue.add(command);
        }
    }

    public void processQueuedInputs() {
        InputCommand command;
        while ((command = inputQueue.poll()) != null) {
            applyInput(command);
        }
    }

    public void handleInput(InputCommand command) {
        applyInput(command);
    }

    private void applyInput(InputCommand command) {
        switch (command.getType()) {
            case MOVE_UP -> movePlayer(0, -1);
            case MOVE_DOWN -> movePlayer(0, 1);
            case MOVE_LEFT -> movePlayer(-1, 0);
            case MOVE_RIGHT -> movePlayer(1, 0);

            case SHOOT_UP -> shootPlayer(0, -1);
            case SHOOT_DOWN -> shootPlayer(0, 1);
            case SHOOT_LEFT -> shootPlayer(-1, 0);
            case SHOOT_RIGHT -> shootPlayer(1, 0);

            case NEW_GAME -> newGame();
            case EXIT -> requestExit();
            case ABOUT -> requestAbout();
            case HIGH_SCORES -> requestHighScores();

            case PAUSE -> pause();
            case RESUME -> resume();

            case DEBUG_TOGGLE_MODE -> toggleDebugMode();
            case DEBUG_GOD_TOGGLE -> toggleGodMode();
            case DEBUG_HEAL_PLAYER -> { if (debugMode) debugHealPlayer(); }
            case DEBUG_DAMAGE_PLAYER -> { if (debugMode) debugDamagePlayer(1); }
            case DEBUG_BUFF_DAMAGE -> { if (debugMode) debugBuffDamage(1); }
            case DEBUG_KILL_ENEMIES -> { if (debugMode) debugKillEnemies(); }
            case DEBUG_SPAWN_CHEST -> { if (debugMode) debugSpawnChest(); }
            case DEBUG_SPAWN_SLIME -> { if (debugMode) debugSpawnEnemy(EnemyKind.SLIME); }
            case DEBUG_SPAWN_SHOOTER -> { if (debugMode) debugSpawnEnemy(EnemyKind.SHOOTER); }
            case DEBUG_SPAWN_TANK -> { if (debugMode) debugSpawnEnemy(EnemyKind.TANK); }
            case DEBUG_SPAWN_BOSS -> { if (debugMode) debugSpawnEnemy(EnemyKind.BOSS); }
            case DEBUG_WIN -> { if (debugMode) game.win(); }
            case DEBUG_LOSE -> { if (debugMode) game.lose(); }

            case NONE -> { }
        }
    }

    public synchronized void newGame() {
        Room room = newGameRoomSupplier.get();
        game.reset(room);
        game.start();
        clearRequests();
    }

    public synchronized void pause() {
        game.pause();
    }

    public synchronized void resume() {
        game.resume();
    }

    public synchronized void togglePause() {
        if (game.getState() == GameState.RUNNING) {
            game.pause();
        } else if (game.getState() == GameState.PAUSED) {
            game.resume();
        }
    }

    public synchronized void requestExit() {
        exitRequested = true;
        game.stop();
    }

    public synchronized void requestAbout() {
        aboutRequested = true;
    }

    public synchronized void requestHighScores() {
        highScoresRequested = true;
    }

    public synchronized boolean consumeExitRequested() {
        boolean value = exitRequested;
        exitRequested = false;
        return value;
    }

    public synchronized boolean consumeAboutRequested() {
        boolean value = aboutRequested;
        aboutRequested = false;
        return value;
    }

    public synchronized boolean consumeHighScoresRequested() {
        boolean value = highScoresRequested;
        highScoresRequested = false;
        return value;
    }

    public synchronized boolean isDebugMode() {
        return debugMode;
    }

    private void applyGodMode(boolean enabled) {
        Player player = getPlayer();
        if (player != null) {
            player.setInvulnerable(enabled);
        }
    }

    public synchronized void toggleGodMode() {
        godMode = !godMode;
        applyGodMode(godMode);
    }

    public synchronized void toggleDebugMode() {
        debugMode = !debugMode;
        if (debugMode) {
            godMode = true;
        }
        else
        {
            godMode = false;
        }
        applyGodMode(godMode);
    }

    private void debugSpawnChest()
    {
        Room room = game.getCurrentRoom();
        if (room == null) {
            return;
        }

        // Отступ от внешних стен комнаты.
        int minX = 2;
        int minY = 2;

        int maxX = room.getWidth() - BIG_OBJECT_SIZE;
        int maxY = room.getHeight() - BIG_OBJECT_SIZE;
        if (maxX < 0 || maxY < 0) {
            return;
        }

        for (int attempts = 0; attempts < 200; attempts++) {
            int x = random.nextInt(maxX - minX + 1);
            int y = random.nextInt(maxY - minY + 1);

            if (room.isAreaFree(x, y, BIG_OBJECT_SIZE, BIG_OBJECT_SIZE))
            {
                room.addObject(new Chest(x, y));
                return;
            }
        }
    }

    public synchronized Game getGame() {
        return game;
    }

    public synchronized void beginFrame() {
        Player player = getPlayer();
        if (player != null) {
            player.setMoving(false);
        }
    }

    private void movePlayer(int dx, int dy) {
        if (game.getState() == GameState.READY) {
            game.start();
        }
        if (game.getState() != GameState.RUNNING) {
            return;
        }

        Player player = getPlayer();
        if (player == null) {
            return;
        }

        if (dx < 0) {
            player.setFacing(Player.Direction.LEFT);
        } else if (dx > 0) {
            player.setFacing(Player.Direction.RIGHT);
        }

        boolean moved = player.tryMove(game, dx, dy);
        player.setMoving(moved);
    }

    private void shootPlayer(int dx, int dy) {
        if (game.getState() == GameState.READY) {
            game.start();
        }
        if (game.getState() != GameState.RUNNING) {
            return;
        }

        Player player = getPlayer();
        if (player != null) {
            player.shoot(game, dx, dy);
        }
    }

    private void debugHealPlayer() {
        Player player = getPlayer();
        if (player != null) {
            player.heal(player.getMaxHp());
        }
    }

    private void debugDamagePlayer(int amount) {
        Player player = getPlayer();
        if (player != null) {
            player.takeDamage(game, Math.max(1, amount));
        }
    }

    private void debugBuffDamage(int amount) {
        Player player = getPlayer();
        if (player != null) {
            player.addDamage(Math.max(1, amount));
        }
    }

    private void debugKillEnemies() {
        List<GameObject> snapshot = new ArrayList<>(game.getCurrentRoom().getObjects());
        for (GameObject object : snapshot) {
            if (object instanceof Enemy) {
                game.removeObject(object);
            }
        }
    }

    private void debugSpawnEnemy(EnemyKind kind) {
        Room room = game.getCurrentRoom();
        if (room == null) {
            return;
        }

        // Отступ от внешних стен комнаты.
        int minX = 2;
        int minY = 2;

        int maxX = room.getWidth() - BIG_OBJECT_SIZE;
        int maxY = room.getHeight() - BIG_OBJECT_SIZE;
        if (maxX < 0 || maxY < 0) {
            return;
        }

        for (int attempts = 0; attempts < 200; attempts++) {
            int x = random.nextInt(maxX - minX + 1);
            int y = random.nextInt(maxY - minY + 1);

            if (room.isAreaFree(x, y, BIG_OBJECT_SIZE, BIG_OBJECT_SIZE)) {
                room.addObject(new Enemy(x, y, kind, 1));
                return;
            }
        }
    }

    private Player getPlayer() {
        Optional<Player> player = game.getPlayer();
        return player.orElse(null);
    }

    private void clearRequests() {
        exitRequested = false;
        aboutRequested = false;
        highScoresRequested = false;
    }
}
