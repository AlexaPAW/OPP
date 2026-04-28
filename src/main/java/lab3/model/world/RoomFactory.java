package lab3.model.world;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import lab3.model.objects.Chest;
import lab3.model.objects.EnemyKind;
import lab3.model.objects.GameObject;
import lab3.model.objects.Player;

public class RoomFactory {
    private static final int ROOM_WIDTH = 100;
    private static final int ROOM_HEIGHT = 60;

    private static final int PLAYER_SIZE = 5;
    private static final int WALL_SIZE = 5;

    private final Random random = new Random();
    private final List<RoomTemplate> templates;

    private final List<RoomTemplate> earlyCombatRooms;
    private final List<RoomTemplate> midCombatRooms;
    private final List<RoomTemplate> mixedCombatRooms;
    private final List<RoomTemplate> treasureRooms;
    private final List<RoomTemplate> bossRooms;

    private int scriptedLevel = 0;

    public RoomFactory(List<RoomTemplate> templates)
    {
        this.templates = new ArrayList<>(Objects.requireNonNull(templates, "templates must not be null"));
        if (this.templates.isEmpty()) {
            throw new IllegalArgumentException("templates must not be empty");
        }
        this.earlyCombatRooms = buildEarlyCombatRooms();
        this.midCombatRooms = buildMidCombatRooms();
        this.mixedCombatRooms = buildMixedCombatRooms();
        this.treasureRooms = buildTreasureRooms();
        this.bossRooms = buildBossRooms();
    }

    public Room createNextRoom() {
        scriptedLevel++;
        RoomTemplate template = switch (scriptedLevel) {
            case 1, 2 -> pickRandom(earlyCombatRooms);
            case 3 -> pickRandom(treasureRooms);
            case 4, 5 -> pickRandom(midCombatRooms);
            case 6 -> pickRandom(treasureRooms);
            case 7, 8 -> pickRandom(mixedCombatRooms);
            case 9 -> pickRandom(treasureRooms);
            case 10 -> pickRandom(bossRooms);
            default -> pickRandom(bossRooms);
        };

        return createFromTemplate(template, null);
    }

    private RoomTemplate pickRandom(List<RoomTemplate> list) {
        if (list == null || list.isEmpty()) {
            return templates.get(random.nextInt(templates.size()));
        }
        return list.get(random.nextInt(list.size()));
    }

    private List<RoomTemplate> buildEarlyCombatRooms() {
        List<RoomTemplate> list = new ArrayList<>();

        RoomTemplate r1 = new RoomTemplate(ROOM_WIDTH, ROOM_HEIGHT, EnumSet.of(Direction.LEFT, Direction.RIGHT));
        addBorderWalls(r1, ROOM_WIDTH, ROOM_HEIGHT);
        r1.addSpawn(SpawnData.player((ROOM_WIDTH - PLAYER_SIZE) / 2, (ROOM_HEIGHT - PLAYER_SIZE) / 2));
        r1.addSpawn(SpawnData.enemy(25, 40, EnemyKind.SLIME, 1));
        r1.addSpawn(SpawnData.enemy(70, 40, EnemyKind.SLIME, 2));
        r1.addSpawn(SpawnData.enemy(25, 20, EnemyKind.SLIME, 2));
        r1.addSpawn(SpawnData.enemy(70, 20, EnemyKind.SLIME, 1));
        list.add(r1);

        RoomTemplate r2 = new RoomTemplate(ROOM_WIDTH, ROOM_HEIGHT, EnumSet.of(Direction.LEFT, Direction.UP));
        addBorderWalls(r2, ROOM_WIDTH, ROOM_HEIGHT);
        r2.addSpawn(SpawnData.player((ROOM_WIDTH - PLAYER_SIZE) / 2, (ROOM_HEIGHT - PLAYER_SIZE) / 2));
        r2.addSpawn(SpawnData.enemy(20, 30, EnemyKind.SLIME, 2));
        r2.addSpawn(SpawnData.enemy(80, 30, EnemyKind.SLIME, 1));
        r2.addSpawn(SpawnData.enemy(50, 10, EnemyKind.SLIME, 2));
        list.add(r2);

        RoomTemplate r3 = new RoomTemplate(ROOM_WIDTH, ROOM_HEIGHT, EnumSet.of(Direction.RIGHT, Direction.DOWN));
        addBorderWalls(r3, ROOM_WIDTH, ROOM_HEIGHT);
        r3.addSpawn(SpawnData.player((ROOM_WIDTH - PLAYER_SIZE) / 2, (ROOM_HEIGHT - PLAYER_SIZE) / 2));
        r3.addSpawn(SpawnData.enemy(45, 10, EnemyKind.SLIME, 2));
        r3.addSpawn(SpawnData.enemy(65, 10, EnemyKind.SLIME, 1));
        r3.addSpawn(SpawnData.enemy(25, 10, EnemyKind.SLIME, 2));
        list.add(r3);

        RoomTemplate r4 = new RoomTemplate(ROOM_WIDTH, ROOM_HEIGHT, EnumSet.of(Direction.UP, Direction.DOWN));
        addBorderWalls(r4, ROOM_WIDTH, ROOM_HEIGHT);
        r4.addSpawn(SpawnData.player((ROOM_WIDTH - PLAYER_SIZE) / 2, (ROOM_HEIGHT - PLAYER_SIZE) / 2));
        r4.addSpawn(SpawnData.enemy(5, 5, EnemyKind.SLIME, 2));
        r4.addSpawn(SpawnData.enemy(5, 50, EnemyKind.SLIME, 2));
        r4.addSpawn(SpawnData.enemy(90, 5, EnemyKind.SLIME, 2));
        r4.addSpawn(SpawnData.enemy(90, 50, EnemyKind.SLIME, 2));
        list.add(r4);

        return list;
    }

    private List<RoomTemplate> buildMidCombatRooms() {
        List<RoomTemplate> list = new ArrayList<>();

        RoomTemplate r1 = new RoomTemplate(ROOM_WIDTH, ROOM_HEIGHT, EnumSet.of(Direction.LEFT, Direction.RIGHT));
        addBorderWalls(r1, ROOM_WIDTH, ROOM_HEIGHT);
        r1.addSpawn(SpawnData.player((ROOM_WIDTH - PLAYER_SIZE) / 2, (ROOM_HEIGHT - PLAYER_SIZE) / 2));
        r1.addSpawn(SpawnData.enemy(5, 15, EnemyKind.SHOOTER, 4));
        r1.addSpawn(SpawnData.enemy(25, 15, EnemyKind.SHOOTER, 4));
        r1.addSpawn(SpawnData.enemy(45, 15, EnemyKind.SHOOTER, 4));
        r1.addSpawn(SpawnData.enemy(65, 15, EnemyKind.SHOOTER, 4));
        r1.addSpawn(SpawnData.enemy(85, 15, EnemyKind.SHOOTER, 4));
        list.add(r1);

        RoomTemplate r2 = new RoomTemplate(ROOM_WIDTH, ROOM_HEIGHT, EnumSet.of(Direction.LEFT, Direction.UP));
        addBorderWalls(r2, ROOM_WIDTH, ROOM_HEIGHT);
        r2.addSpawn(SpawnData.player((ROOM_WIDTH - PLAYER_SIZE) / 2, (ROOM_HEIGHT - PLAYER_SIZE) / 2));
        r2.addSpawn(SpawnData.enemy(50, 20, EnemyKind.SHOOTER, 2));
        r2.addSpawn(SpawnData.enemy(5, 50, EnemyKind.SHOOTER, 5));
        r2.addSpawn(SpawnData.enemy(90, 50, EnemyKind.SHOOTER, 5));
        r2.addSpawn(SpawnData.enemy(40, 30, EnemyKind.SLIME, 4));
        r2.addSpawn(SpawnData.enemy(60, 30, EnemyKind.SLIME, 4));
        list.add(r2);

        RoomTemplate r3 = new RoomTemplate(ROOM_WIDTH, ROOM_HEIGHT, EnumSet.of(Direction.RIGHT, Direction.DOWN));
        addBorderWalls(r3, ROOM_WIDTH, ROOM_HEIGHT);
        r3.addSpawn(SpawnData.player((ROOM_WIDTH - PLAYER_SIZE) / 2, (ROOM_HEIGHT - PLAYER_SIZE) / 2));
        r3.addSpawn(SpawnData.enemy(85, 5, EnemyKind.SHOOTER, 7));
        r3.addSpawn(SpawnData.enemy(15, 50, EnemyKind.SHOOTER, 7));
        list.add(r3);

        RoomTemplate r4 = new RoomTemplate(ROOM_WIDTH, ROOM_HEIGHT, EnumSet.of(Direction.UP, Direction.DOWN));
        addBorderWalls(r4, ROOM_WIDTH, ROOM_HEIGHT);
        r4.addSpawn(SpawnData.player((ROOM_WIDTH - PLAYER_SIZE) / 2, (ROOM_HEIGHT - PLAYER_SIZE) / 2));
        r4.addSpawn(SpawnData.enemy(90, 10, EnemyKind.SHOOTER, 5));
        r4.addSpawn(SpawnData.enemy(20, 15, EnemyKind.TANK, 5));
        r4.addSpawn(SpawnData.enemy(75, 30, EnemyKind.TANK, 5));
        r4.addSpawn(SpawnData.enemy(5, 10, EnemyKind.SHOOTER, 5));
        list.add(r4);

        return list;
    }

    private List<RoomTemplate> buildMixedCombatRooms() {
        List<RoomTemplate> list = new ArrayList<>();

        RoomTemplate r1 = new RoomTemplate(ROOM_WIDTH, ROOM_HEIGHT, EnumSet.of(Direction.LEFT, Direction.RIGHT));
        addBorderWalls(r1, ROOM_WIDTH, ROOM_HEIGHT);
        r1.addSpawn(SpawnData.player((ROOM_WIDTH - PLAYER_SIZE) / 2, (ROOM_HEIGHT - PLAYER_SIZE) / 2));
        r1.addSpawn(SpawnData.enemy(5, 10, EnemyKind.SLIME, 7));
        r1.addSpawn(SpawnData.enemy(25, 10, EnemyKind.SLIME, 7));
        r1.addSpawn(SpawnData.enemy(45, 10, EnemyKind.SHOOTER, 5));
        r1.addSpawn(SpawnData.enemy(65, 10, EnemyKind.SLIME, 7));
        r1.addSpawn(SpawnData.enemy(85, 10, EnemyKind.SLIME, 7));
        r1.addSpawn(SpawnData.enemy(5, 10, EnemyKind.TANK, 4));
        r1.addSpawn(SpawnData.enemy(45, 10, EnemyKind.TANK, 4));
        r1.addSpawn(SpawnData.enemy(85, 10, EnemyKind.TANK, 4));
        list.add(r1);

        RoomTemplate r2 = new RoomTemplate(ROOM_WIDTH, ROOM_HEIGHT, EnumSet.of(Direction.LEFT, Direction.UP));
        addBorderWalls(r2, ROOM_WIDTH, ROOM_HEIGHT);
        r2.addSpawn(SpawnData.player((ROOM_WIDTH - PLAYER_SIZE) / 2, (ROOM_HEIGHT - PLAYER_SIZE) / 2));
        r2.addSpawn(SpawnData.enemy(18, 14, EnemyKind.TANK, 7));
        r2.addSpawn(SpawnData.enemy(62, 30, EnemyKind.SHOOTER, 7));
        r2.addSpawn(SpawnData.enemy(30, 34, EnemyKind.SLIME, 7));
        list.add(r2);

        RoomTemplate r3 = new RoomTemplate(ROOM_WIDTH, ROOM_HEIGHT, EnumSet.of(Direction.RIGHT, Direction.DOWN));
        addBorderWalls(r3, ROOM_WIDTH, ROOM_HEIGHT);
        r3.addSpawn(SpawnData.player((ROOM_WIDTH - PLAYER_SIZE) / 2, (ROOM_HEIGHT - PLAYER_SIZE) / 2));
        r3.addSpawn(SpawnData.enemy(16, 18, EnemyKind.SHOOTER, 8));
        r3.addSpawn(SpawnData.enemy(64, 18, EnemyKind.TANK, 8));
        r3.addSpawn(SpawnData.enemy(40, 42, EnemyKind.SLIME, 8));
        list.add(r3);

        RoomTemplate r4 = new RoomTemplate(ROOM_WIDTH, ROOM_HEIGHT, EnumSet.of(Direction.UP, Direction.DOWN));
        addBorderWalls(r4, ROOM_WIDTH, ROOM_HEIGHT);
        r4.addSpawn(SpawnData.player((ROOM_WIDTH - PLAYER_SIZE) / 2, (ROOM_HEIGHT - PLAYER_SIZE) / 2));
        r4.addSpawn(SpawnData.enemy(18, 16, EnemyKind.SLIME, 8));
        r4.addSpawn(SpawnData.enemy(50, 26, EnemyKind.SHOOTER, 8));
        r4.addSpawn(SpawnData.enemy(70, 36, EnemyKind.TANK, 8));
        list.add(r4);

        return list;
    }

    private List<RoomTemplate> buildTreasureRooms() {
        List<RoomTemplate> list = new ArrayList<>();

        RoomTemplate r1 = new RoomTemplate(ROOM_WIDTH, ROOM_HEIGHT, EnumSet.of(Direction.LEFT, Direction.RIGHT));
        addBorderWalls(r1, ROOM_WIDTH, ROOM_HEIGHT);
        r1.addSpawn(SpawnData.player((ROOM_WIDTH - PLAYER_SIZE) / 2, (ROOM_HEIGHT - PLAYER_SIZE) / 2));
        r1.addSpawn(SpawnData.chest(18, 14, Chest.RewardType.HEAL, 2));
        r1.addSpawn(SpawnData.chest(60, 28, Chest.RewardType.DAMAGE_BUFF, 1));
        list.add(r1);

        RoomTemplate r2 = new RoomTemplate(ROOM_WIDTH, ROOM_HEIGHT, EnumSet.of(Direction.LEFT, Direction.UP));
        addBorderWalls(r2, ROOM_WIDTH, ROOM_HEIGHT);
        r2.addSpawn(SpawnData.player((ROOM_WIDTH - PLAYER_SIZE) / 2, (ROOM_HEIGHT - PLAYER_SIZE) / 2));
        r2.addSpawn(SpawnData.chest(20, 12, Chest.RewardType.HEAL, 3));
        r2.addSpawn(SpawnData.chest(58, 22, Chest.RewardType.DAMAGE_BUFF, 1));
        list.add(r2);

        RoomTemplate r3 = new RoomTemplate(ROOM_WIDTH, ROOM_HEIGHT, EnumSet.of(Direction.RIGHT, Direction.DOWN));
        addBorderWalls(r3, ROOM_WIDTH, ROOM_HEIGHT);
        r3.addSpawn(SpawnData.player((ROOM_WIDTH - PLAYER_SIZE) / 2, (ROOM_HEIGHT - PLAYER_SIZE) / 2));
        r3.addSpawn(SpawnData.chest(24, 16, Chest.RewardType.HEAL, 2));
        r3.addSpawn(SpawnData.chest(64, 30, Chest.RewardType.DAMAGE_BUFF, 2));
        list.add(r3);

        RoomTemplate r4 = new RoomTemplate(ROOM_WIDTH, ROOM_HEIGHT, EnumSet.of(Direction.UP, Direction.DOWN));
        addBorderWalls(r4, ROOM_WIDTH, ROOM_HEIGHT);
        r4.addSpawn(SpawnData.player((ROOM_WIDTH - PLAYER_SIZE) / 2, (ROOM_HEIGHT - PLAYER_SIZE) / 2));
        r4.addSpawn(SpawnData.chest(16, 18, Chest.RewardType.DAMAGE_BUFF, 1));
        r4.addSpawn(SpawnData.chest(68, 24, Chest.RewardType.HEAL, 2));
        list.add(r4);

        return list;
    }

    private List<RoomTemplate> buildBossRooms() {
        List<RoomTemplate> list = new ArrayList<>();

        RoomTemplate r1 = new RoomTemplate(ROOM_WIDTH, ROOM_HEIGHT, EnumSet.of(Direction.LEFT, Direction.RIGHT));
        addBorderWalls(r1, ROOM_WIDTH, ROOM_HEIGHT);
        r1.addSpawn(SpawnData.player((ROOM_WIDTH - PLAYER_SIZE) / 2, (ROOM_HEIGHT - PLAYER_SIZE) / 2));
        r1.addSpawn(SpawnData.enemy(42, 22, EnemyKind.BOSS, 10));
        list.add(r1);

        RoomTemplate r2 = new RoomTemplate(ROOM_WIDTH, ROOM_HEIGHT, EnumSet.of(Direction.UP, Direction.DOWN));
        addBorderWalls(r2, ROOM_WIDTH, ROOM_HEIGHT);
        r2.addSpawn(SpawnData.player((ROOM_WIDTH - PLAYER_SIZE) / 2, (ROOM_HEIGHT - PLAYER_SIZE) / 2));
        r2.addSpawn(SpawnData.enemy(34, 18, EnemyKind.BOSS, 10));
        list.add(r2);

        return list;
    }

    public Room createRandomRoom() {
        RoomTemplate template = templates.get(random.nextInt(templates.size()));
        return createFromTemplate(template, null);
    }

    public Room createConnectedRoom(Direction enterFrom) {
        List<RoomTemplate> filtered = templates.stream()
                .filter(template -> template.hasExit(enterFrom.opposite()) || template.getExits().isEmpty())
                .toList();

        RoomTemplate chosen;
        if (filtered.isEmpty()) {
            chosen = templates.get(random.nextInt(templates.size()));
        } else {
            chosen = filtered.get(random.nextInt(filtered.size()));
        }

        return createFromTemplate(chosen, enterFrom);
    }

    public Room createFromTemplate(RoomTemplate template) {
        return createFromTemplate(template, null);
    }

    public Room createFromTemplate(RoomTemplate template, Direction enterFrom) {
        Objects.requireNonNull(template, "template must not be null");

        Room room = new Room(template.getWidth(), template.getHeight(), enterFrom, 0, 0);

        for (SpawnData spawnData : template.getSpawns()) {
            GameObject object = spawnData.createObject();
            room.addObject(object);
        }

        if (enterFrom != null) {
            applyEntryPosition(room, enterFrom);
        } else {
            ensurePlayerSpawn(room);
        }

        return room;
    }

    private void applyEntryPosition(Room room, Direction enterFrom) {
        int spawnX;
        int spawnY;

        switch (enterFrom) {
            case LEFT -> {
                spawnX = WALL_SIZE;
                spawnY = (room.getHeight() - PLAYER_SIZE) / 2;
            }
            case RIGHT -> {
                spawnX = room.getWidth() - WALL_SIZE - PLAYER_SIZE;
                spawnY = (room.getHeight() - PLAYER_SIZE) / 2;
            }
            case UP -> {
                spawnX = (room.getWidth() - PLAYER_SIZE) / 2;
                spawnY = WALL_SIZE;
            }
            case DOWN -> {
                spawnX = (room.getWidth() - PLAYER_SIZE) / 2;
                spawnY = room.getHeight() - WALL_SIZE - PLAYER_SIZE;
            }
            default -> throw new IllegalStateException("Unexpected direction: " + enterFrom);
        }

        spawnX = clampSpawn(spawnX, room.getWidth(), PLAYER_SIZE);
        spawnY = clampSpawn(spawnY, room.getHeight(), PLAYER_SIZE);

        room.setPlayerSpawn(spawnX, spawnY);

        final int fx = spawnX;
        final int fy = spawnY;

        room.getObjects().stream()
                .filter(obj -> obj instanceof Player)
                .map(obj -> (Player) obj)
                .findFirst()
                .ifPresent(player -> player.teleportTo(fx, fy));
    }

    private void ensurePlayerSpawn(Room room) {
        boolean hasPlayer = room.getObjects().stream().anyMatch(obj -> obj instanceof Player);
        if (!hasPlayer) {
            int x = clampSpawn((room.getWidth() - PLAYER_SIZE) / 2, room.getWidth(), PLAYER_SIZE);
            int y = clampSpawn((room.getHeight() - PLAYER_SIZE) / 2, room.getHeight(), PLAYER_SIZE);
            room.setPlayerSpawn(x, y);
        } else {
            room.getObjects().stream()
                    .filter(obj -> obj instanceof Player)
                    .map(obj -> (Player) obj)
                    .findFirst()
                    .ifPresent(player -> room.setPlayerSpawn(player.getX(), player.getY()));
        }
    }

    private int clampSpawn(int value, int roomSize, int objectSize) {
        int max = Math.max(0, roomSize - objectSize);
        if (value < 0) {
            return 0;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static void addBorderWalls(RoomTemplate template, int width, int height) {
        // Верх и низ
        for (int x = 0; x < width; x += WALL_SIZE) {
            int w = Math.min(WALL_SIZE, width - x);
            template.addSpawn(SpawnData.wall(x, 0, w, WALL_SIZE));
            template.addSpawn(SpawnData.wall(x, height - WALL_SIZE, w, WALL_SIZE));
        }

        // Лево и право, без углов (они уже покрыты верх/низ полосами)
        for (int y = WALL_SIZE; y < height - WALL_SIZE; y += WALL_SIZE) {
            int h = Math.min(WALL_SIZE, height - y - WALL_SIZE);
            if (h <= 0) {
                break;
            }
            template.addSpawn(SpawnData.wall(0, y, WALL_SIZE, h));
            template.addSpawn(SpawnData.wall(width - WALL_SIZE, y, WALL_SIZE, h));
        }
    }

    public static RoomTemplate basicRoomTemplate(int width, int height) {
        RoomTemplate template = new RoomTemplate(width, height);

        addBorderWalls(template, width, height);

        template.addSpawn(SpawnData.player((width - PLAYER_SIZE) / 2, (height - PLAYER_SIZE) / 2));
        template.addSpawn(SpawnData.enemy(12, 12, EnemyKind.SLIME, 1));
        template.addSpawn(SpawnData.chest(width - 16, height - 16, Chest.RewardType.HEAL, 2));

        return template;
    }

    public static List<RoomTemplate> defaultTemplates()
    {
        List<RoomTemplate> list = new ArrayList<>();

        RoomTemplate empty = new RoomTemplate(ROOM_WIDTH, ROOM_HEIGHT, EnumSet.of(Direction.LEFT, Direction.RIGHT));
        addBorderWalls(empty, ROOM_WIDTH, ROOM_HEIGHT);
        empty.addSpawn(SpawnData.player((ROOM_WIDTH - PLAYER_SIZE) / 2, (ROOM_HEIGHT - PLAYER_SIZE) / 2));
        list.add(empty);

        RoomTemplate combat = new RoomTemplate(ROOM_WIDTH, ROOM_HEIGHT, EnumSet.of(Direction.LEFT, Direction.RIGHT, Direction.UP));
        addBorderWalls(combat, ROOM_WIDTH, ROOM_HEIGHT);
        combat.addSpawn(SpawnData.player((ROOM_WIDTH - PLAYER_SIZE) / 2, (ROOM_HEIGHT - PLAYER_SIZE) / 2));
        combat.addSpawn(SpawnData.enemy(12, 12, EnemyKind.SLIME, 1));
        combat.addSpawn(SpawnData.enemy(56, 24, EnemyKind.SLIME, 1));
        list.add(combat);

        RoomTemplate treasure = new RoomTemplate(ROOM_WIDTH, ROOM_HEIGHT, EnumSet.of(Direction.LEFT, Direction.DOWN));
        addBorderWalls(treasure, ROOM_WIDTH, ROOM_HEIGHT);
        treasure.addSpawn(SpawnData.player((ROOM_WIDTH - PLAYER_SIZE) / 2, (ROOM_HEIGHT - PLAYER_SIZE) / 2));
        treasure.addSpawn(SpawnData.chest(24, 12, Chest.RewardType.HEAL, 3));
        treasure.addSpawn(SpawnData.chest(56, 28, Chest.RewardType.DAMAGE_BUFF, 1));
        list.add(treasure);

        RoomTemplate arena = new RoomTemplate(ROOM_WIDTH, ROOM_HEIGHT, EnumSet.of(Direction.RIGHT, Direction.UP, Direction.DOWN));
        addBorderWalls(arena, ROOM_WIDTH, ROOM_HEIGHT);
        arena.addSpawn(SpawnData.player((ROOM_WIDTH - PLAYER_SIZE) / 2, (ROOM_HEIGHT - PLAYER_SIZE) / 2));
        arena.addSpawn(SpawnData.enemy(16, 16, EnemyKind.SHOOTER, 2));
        arena.addSpawn(SpawnData.enemy(60, 20, EnemyKind.TANK, 2));
        list.add(arena);

        return list;
    }

    public void resetScript()
    {
        scriptedLevel = 0;
    }
}
