package lab3.model.world;

import lab3.model.objects.GameObject;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Room {
    private final int width;
    private final int height;
    private final List<GameObject> objects;

    private Direction entryDirection;
    private int playerSpawnX;
    private int playerSpawnY;

    public Room(int width, int height) {
        this(width, height, null, 0, 0);
    }

    public Room(int width, int height, Direction entryDirection, int playerSpawnX, int playerSpawnY) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Room dimensions must be positive");
        }
        this.width = width;
        this.height = height;
        this.objects = new ArrayList<>();
        this.entryDirection = entryDirection;
        this.playerSpawnX = playerSpawnX;
        this.playerSpawnY = playerSpawnY;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public List<GameObject> getObjects() {
        return objects;
    }

    public List<GameObject> getObjectsView() {
        return Collections.unmodifiableList(objects);
    }

    public void addObject(GameObject object) {
        objects.add(Objects.requireNonNull(object, "object must not be null"));
    }

    public void addObjects(List<? extends GameObject> newObjects) {
        if (newObjects == null) {
            return;
        }
        for (GameObject object : newObjects) {
            addObject(object);
        }
    }

    public void removeObject(GameObject object) {
        objects.remove(object);
    }

    public void clearObjects() {
        objects.clear();
    }

    public boolean isInside(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    /**
     * Проверка, помещается ли прямоугольник целиком в комнату.
     */
    public boolean isInside(int x, int y, int w, int h) {
        return x >= 0 && y >= 0 && w > 0 && h > 0 && x + w <= width && y + h <= height;
    }

    /**
     * Проверка, свободна ли область для размещения объекта.
     */
    public boolean isAreaFree(int x, int y, int w, int h) {
        return isAreaFree(x, y, w, h, null);
    }

    public boolean isAreaFree(int x, int y, int w, int h, GameObject ignore) {
        if (!isInside(x, y, w, h)) {
            return false;
        }

        Rectangle area = new Rectangle(x, y, w, h);

        for (GameObject object : objects) {
            if (object == null || object == ignore || !object.isAlive()) {
                continue;
            }
            if (area.intersects(object.getBounds())) {
                return false;
            }
        }

        return true;
    }

    public Direction getEntryDirection() {
        return entryDirection;
    }

    public void setEntryDirection(Direction entryDirection) {
        this.entryDirection = entryDirection;
    }

    public int getPlayerSpawnX() {
        return playerSpawnX;
    }

    public int getPlayerSpawnY() {
        return playerSpawnY;
    }

    public void setPlayerSpawn(int x, int y) {
        this.playerSpawnX = x;
        this.playerSpawnY = y;
    }

    public long countAliveObjects() {
        return objects.stream().filter(GameObject::isAlive).count();
    }
}