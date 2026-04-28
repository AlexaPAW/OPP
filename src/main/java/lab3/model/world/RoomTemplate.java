package lab3.model.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class RoomTemplate
{
    private final int width;
    private final int height;
    private final EnumSet<Direction> exits;
    private final List<SpawnData> spawns;

    public RoomTemplate(int width, int height)
    {
        this(width, height, EnumSet.noneOf(Direction.class), new ArrayList<>());
    }

    public RoomTemplate(int width, int height, EnumSet<Direction> exits, List<SpawnData> spawns)
    {
        if (width <= 0 || height <= 0)
        {
            throw new IllegalArgumentException("RoomTemplate dimensions must be positive");
        }
        this.width = width;
        this.height = height;
        this.exits = exits == null ? EnumSet.noneOf(Direction.class) : EnumSet.copyOf(exits);
        this.spawns = new ArrayList<>();
        if (spawns != null)
        {
            this.spawns.addAll(spawns);
        }
    }

    public RoomTemplate(int width, int height, EnumSet<Direction> exits)
    {
        this(width, height, exits, new ArrayList<>());
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }

    public EnumSet<Direction> getExits()
    {
        return EnumSet.copyOf(exits);
    }

    public boolean hasExit(Direction direction)
    {
        return direction != null && exits.contains(direction);
    }

    public List<SpawnData> getSpawns()
    {
        return Collections.unmodifiableList(spawns);
    }

    public RoomTemplate addExit(Direction direction)
    {
        exits.add(Objects.requireNonNull(direction, "direction must not be null"));
        return this;
    }

    public RoomTemplate addSpawn(SpawnData spawnData)
    {
        spawns.add(Objects.requireNonNull(spawnData, "spawnData must not be null"));
        return this;
    }

    public Room instantiate()
    {
        Room room = new Room(width, height);
        for (SpawnData spawn : spawns)
        {
            room.addObject(spawn.createObject());
        }
        return room;
    }
}