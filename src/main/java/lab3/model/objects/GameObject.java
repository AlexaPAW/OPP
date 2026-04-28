package lab3.model.objects;

import lab3.model.core.Game;

import java.awt.Rectangle;
import java.util.List;

public abstract class GameObject {
    private int x;
    private int y;

    /**
     * Предыдущая логическая позиция.
     * Нужна только для GUI-интерполяции между тиками.
     */
    private int previousX;
    private int previousY;

    private int width;
    private int height;
    private boolean solid;
    private boolean alive = true;

    protected GameObject(int x, int y, int width, int height, boolean solid) {
        this.x = x;
        this.y = y;
        this.previousX = x;
        this.previousY = y;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.solid = solid;
    }

    public abstract void update(Game game);

    public abstract char getSymbol();

    public final Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public final boolean intersects(GameObject other) {
        return other != null && getBounds().intersects(other.getBounds());
    }

    protected final boolean canOccupy(Game game, int nx, int ny, int w, int h) {
        if (game == null || game.getCurrentRoom() == null) {
            return false;
        }

        int roomWidth = game.getCurrentRoom().getWidth();
        int roomHeight = game.getCurrentRoom().getHeight();

        if (nx < 0 || ny < 0 || nx + w > roomWidth || ny + h > roomHeight) {
            return false;
        }

        Rectangle nextBounds = new Rectangle(nx, ny, w, h);

        List<GameObject> objects = game.getCurrentRoom().getObjects();
        for (GameObject object : objects) {
            if (object == null || object == this || !object.isAlive() || !object.isSolid()) {
                continue;
            }
            if (object instanceof Player) //для контактного урона с игроком от врагов
            {
                continue;
            }
            if (nextBounds.intersects(object.getBounds())) {
                return false;
            }
        }

        return true;
    }

    protected final boolean isInsideRoom(Game game, int nx, int ny, int w, int h) {
        if (game == null || game.getCurrentRoom() == null) {
            return false;
        }

        int roomWidth = game.getCurrentRoom().getWidth();
        int roomHeight = game.getCurrentRoom().getHeight();

        return nx >= 0 && ny >= 0 && nx + w <= roomWidth && ny + h <= roomHeight;
    }

    protected final List<GameObject> objects(Game game) {
        return game.getCurrentRoom().getObjects();
    }

    public final int getX() {
        return x;
    }

    public final int getY() {
        return y;
    }

    public final int getPreviousX() {
        return previousX;
    }

    public final int getPreviousY() {
        return previousY;
    }

    /**
     * Вызывается в начале каждого тика.
     * Нужен, чтобы если объект в этот тик не двигается,
     * он не продолжал интерполироваться из старой клетки.
     */
    public final void beginTick()
    {
        this.previousX = this.x;
        this.previousY = this.y;
    }


    /**
     * Логическое перемещение по клеткам.
     * GUI будет плавно интерполировать между previous* и текущими x/y.
     */
    public final void setPosition(int x, int y) {
        if (this.x == x && this.y == y) {
            return;
        }

        this.previousX = this.x;
        this.previousY = this.y;
        this.x = x;
        this.y = y;
    }

    /**
     * Мгновенный телепорт без анимации.
     * Полезно для перехода между комнатами, спавна и reset.
     */
    public final void teleportTo(int x, int y) {
        this.x = x;
        this.y = y;
        this.previousX = x;
        this.previousY = y;
    }

    public final void moveBy(int dx, int dy) {
        setPosition(this.x + dx, this.y + dy);
    }

    public final int getWidth() {
        return width;
    }

    public final int getHeight() {
        return height;
    }

    public final boolean isSolid() {
        return solid;
    }

    protected final void setSolid(boolean solid) {
        this.solid = solid;
    }

    public final boolean isAlive() {
        return alive;
    }

    public final void setAlive(boolean alive) {
        this.alive = alive;
    }

    /**
     * Интерполяция для GUI.
     * alpha = 0.0 -> previous position
     * alpha = 1.0 -> current position
     */
    public final double getRenderX(double alpha) {
        return lerp(previousX, x, alpha);
    }

    public final double getRenderY(double alpha) {
        return lerp(previousY, y, alpha);
    }

    /**
     * Если нужно принудительно синхронизировать анимацию после телепорта/загрузки.
     */
    public final void resetInterpolation() {
        this.previousX = this.x;
        this.previousY = this.y;
    }

    private static double lerp(int from, int to, double alpha) {
        double a = Math.max(0.0, Math.min(1.0, alpha));
        return from + (to - from) * a;
    }

    public final int getCenterX() {
        return x + width / 2;
    }

    public final int getCenterY() {
        return y + height / 2;
    }

    /**
     * Точка появления снаряда по X:
     * dx > 0 — выстрел вправо
     * dx < 0 — выстрел влево
     * dx == 0 — по центру по X
     */
    public final int getProjectileSpawnX(int dx) {
        if (dx > 0) {
            return x + width;
        }
        if (dx < 0) {
            return x - 1;
        }
        return getCenterX();
    }

    /**
     * Точка появления снаряда по Y:
     * dy > 0 — выстрел вниз
     * dy < 0 — выстрел вверх
     * dy == 0 — по центру по Y
     */
    public final int getProjectileSpawnY(int dy) {
        if (dy > 0) {
            return y + height;
        }
        if (dy < 0) {
            return y - 1;
        }
        return getCenterY();
    }

    public int getRenderLayer() {
        return 0;
    }

}
