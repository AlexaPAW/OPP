package lab3.model.objects;

import lab3.model.core.Game;
import lab3.model.core.GameBalance;

public class ExitPortal extends GameObject
{
    public ExitPortal(int x, int y) {
        super(x, y,  GameBalance.EXIT_PORTAL_SIZE,  GameBalance.EXIT_PORTAL_SIZE, false);
    }

    @Override
    public void update(Game game) {
        if (!isAlive()) {
            return;
        }
        // Никакого перехода отсюда.
    }

    @Override
    public char getSymbol() {
        return '>';
    }
    
    @Override
    public int getRenderLayer() { return -10; }
}