package lab3.model.core;

public final class GameBalance {
    private GameBalance() {}

    public static final int ROOM_WIDTH = 100;
    public static final int ROOM_HEIGHT = 60;

    public static final int PLAYER_SIZE = 5;
    public static final int ENEMY_SIZE = 5;
    public static final int BOSS_SIZE = PLAYER_SIZE + 1;
    public static final int CHEST_SIZE = 5;
    public static final int WALL_SIZE = 5;
    public static final int BULLET_SIZE = 1;
    public static final int EXIT_PORTAL_SIZE = 15;

    public static final double PORTAL_STAY_SECONDS = 2.0;

    public static final int PLAYER_FIRE_COOLDOWN_TICKS = 10;
    public static final int ENEMY_MOVE_COOLDOWN_TICKS = 8;
    public static final int ENEMY_SHOT_COOLDOWN_TICKS = 20;
    public static final int ENEMY_SIGHT_RANGE = 30;

    public static final int BULLET_STEP_PER_TICK = 1;
    
}