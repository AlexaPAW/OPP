package lab3.model.objects;

import lab3.model.core.Game;
import lab3.model.core.GameBalance;

import java.util.Random;

public class Chest extends GameObject
{
    public enum RewardType
    {
        HEAL,
        DAMAGE_BUFF
    }

    private final RewardType rewardType;
    private final int rewardAmount;
    private boolean opened;

    public Chest(int x, int y)
    {
        this(x, y, randomRewardType(), 2);
    }

    public Chest(int x, int y, RewardType rewardType, int rewardAmount)
    {
        super(x, y, GameBalance.CHEST_SIZE, GameBalance.CHEST_SIZE, false);
        this.rewardType = rewardType == null ? RewardType.HEAL : rewardType;
        this.rewardAmount = Math.max(1, rewardAmount);
        this.opened = false;
    }

    @Override
    public void update(Game game)
    {
        if (opened || !isAlive())
        {
            return;
        }

        Player player = game.getPlayer().orElse(null);
        if (player == null || !player.isAlive())
        {
            return;
        }

        if (intersects(player))
        {
            open(game, player);
        }
    }

    public boolean open(Game game, Player player)
    {
        if (opened || player == null)
        {
            return false;
        }

        opened = true;

        if (rewardType == RewardType.HEAL)
        {
            player.heal(rewardAmount);
        }
        else
        {
            player.addDamage(rewardAmount);
        }

        setAlive(false);
        if (game != null)
        {
            game.removeObject(this);
        }

        return true;
    }

    public boolean isOpened()
    {
        return opened;
    }

    public RewardType getRewardType()
    {
        return rewardType;
    }

    public int getRewardAmount()
    {
        return rewardAmount;
    }

    private static RewardType randomRewardType()
    {
        return new Random().nextBoolean() ? RewardType.HEAL : RewardType.DAMAGE_BUFF;
    }

    @Override
    public char getSymbol()
    {
        return 'C';
    }

    @Override
    public int getRenderLayer() { return 20; }
}