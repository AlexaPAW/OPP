package lab3.model.world;

import java.util.Objects;

import lab3.model.objects.Chest;
import lab3.model.objects.Enemy;
import lab3.model.objects.EnemyKind;
import lab3.model.objects.GameObject;
import lab3.model.objects.Player;
import lab3.model.objects.Wall;

public final class SpawnData
{
    public enum Type
    {
        PLAYER,
        ENEMY,
        CHEST,
        WALL
    }

    private final Type type;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private final int rewardAmount;
    private final Chest.RewardType rewardType;

    private final EnemyKind enemyKind;
    private final int enemyLevel;

    private SpawnData(
            Type type,
            int x,
            int y,
            int width,
            int height,
            int rewardAmount,
            Chest.RewardType rewardType,
            EnemyKind enemyKind,
            int enemyLevel)
    {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.x = x;
        this.y = y;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.rewardAmount = rewardAmount;
        this.rewardType = rewardType;
        this.enemyKind = enemyKind;
        this.enemyLevel = enemyLevel;
    }

    public static SpawnData player(int x, int y)
    {
        return new SpawnData(Type.PLAYER, x, y, 1, 1, 0, null, null, 0);
    }

    public static SpawnData enemy(int x, int y, EnemyKind kind, int enemyLevel)
    {
        return new SpawnData(Type.ENEMY, x, y, 1, 1, 0, null, kind, enemyLevel);
    }

    public static SpawnData chest(int x, int y, Chest.RewardType rewardType, int rewardAmount)
    {
        return new SpawnData(Type.CHEST, x, y, 1, 1, rewardAmount, rewardType, null, 0);
    }

    public static SpawnData wall(int x, int y, int width, int height)
    {
        return new SpawnData(Type.WALL, x, y, width, height, 0, null, null, 0);
    }

    public Type getType()
    {
        return type;
    }

    public int getX()
    {
        return x;
    }

    public int getY()
    {
        return y;
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }

    public int getRewardAmount()
    {
        return rewardAmount;
    }

    public EnemyKind getEnemyKind()
    {
        return enemyKind;
    }

    public int getEnemyLevel()
    {
        return enemyLevel;
    }

    public Chest.RewardType getRewardType()
    {
        return rewardType;
    }

    public GameObject createObject()
    {
        return switch (type)
        {
            case PLAYER -> new Player(x, y);
            case ENEMY -> new Enemy(x, y, enemyKind, enemyLevel);
            case CHEST -> new Chest(x, y, rewardType, rewardAmount);
            case WALL -> new Wall(x, y, width, height);
        };
    }
}