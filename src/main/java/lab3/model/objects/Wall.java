package lab3.model.objects;

import lab3.model.core.Game;
import lab3.model.core.GameBalance;

public class Wall extends GameObject
{
    public Wall(int x, int y)
    {
        this(x, y, GameBalance.WALL_SIZE, GameBalance.WALL_SIZE);
    }

    public Wall(int x, int y, int width, int height)
    {
        super(x, y, width, height, true);
    }

    @Override
    public void update(Game game)
    {
        // Стена статична
    }

    @Override
    public char getSymbol()
    {
        return '#';
    }

    @Override
    public int getRenderLayer() { return 10; }
}