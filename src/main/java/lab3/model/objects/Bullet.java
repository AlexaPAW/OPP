package lab3.model.objects;

import lab3.model.core.Game;
import lab3.model.core.GameBalance;

import java.util.ArrayList;
import java.util.List;

public class Bullet extends GameObject
{
    private int dx;
    private int dy;
    private boolean fromPlayer;
    private final int damage;
    private final int maxLifeTicks;
    private int ageTicks;

    public Bullet(int x, int y, int dx, int dy, boolean fromPlayer, int damage)
    {
        this(x, y, dx, dy, fromPlayer, damage, 60);
    }

    public Bullet(int x, int y, int dx, int dy, boolean fromPlayer, int damage, int maxLifeTicks)
    {
        super(x, y, GameBalance.BULLET_SIZE, GameBalance.BULLET_SIZE, false);
        if (Math.abs(dx) + Math.abs(dy) != 1)
        {
            throw new IllegalArgumentException("Bullet direction must be one of 4 cardinal directions");
        }
        this.dx = dx;
        this.dy = dy;
        this.fromPlayer = fromPlayer;
        this.damage = Math.max(1, damage);
        this.maxLifeTicks = Math.max(1, maxLifeTicks);
        this.ageTicks = 0;
    }

    @Override
    public void update(Game game)
    {
        if (!isAlive())
        {
            return;
        }

        ageTicks++;
        if (ageTicks > maxLifeTicks)
        {
            destroy(game);
            return;
        }

        int nx = getX() + dx;
        int ny = getY() + dy;

        if (!isInsideRoom(game, nx, ny, getWidth(), getHeight()))
        {
            destroy(game);
            return;
        }

        setPosition(nx, ny);

        List<GameObject> snapshot = new ArrayList<>(game.getCurrentRoom().getObjects());
        for (GameObject object : snapshot)
        {
            if (object == null || object == this || !object.isAlive())
            {
                continue;
            }

            if (!intersects(object))
            {
                continue;
            }

            if (fromPlayer && object instanceof Enemy enemy)
            {
                if (enemy.getKind() == EnemyKind.TANK && enemy.isTankShielded())
                {
                    reflect();
                    return;
                }
                enemy.takeDamage(game, damage);
                destroy(game);
                return;
            }

            if (!fromPlayer && object instanceof Player)
            {
                ((Player) object).takeDamage(game, damage);
                destroy(game);
                return;
            }

            if (object instanceof Wall)
            {
                destroy(game);
                return;
            }

            if (object.isSolid())
            {
                destroy(game);
                return;
            }
        }
    }

    private void reflect()
    {
        dx = -dx;
        dy = -dy;
        fromPlayer = false;

        setPosition(getX() + dx, getY() + dy);
    }

    private void destroy(Game game)
    {
        setAlive(false);
        if (game != null)
        {
            game.removeObject(this);
        }
    }

    public boolean isFromPlayer()
    {
        return fromPlayer;
    }

    public int getDamage()
    {
        return damage;
    }

    @Override
    public char getSymbol()
    {
        return '*';
    }

    @Override
    public int getRenderLayer() { return 40; }
}
