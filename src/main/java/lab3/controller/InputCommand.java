package lab3.controller;

import java.util.Objects;

public final class InputCommand
{
    private final CommandType type;
    private final long timestampNanos;

    public InputCommand(CommandType type)
    {
        this(type, System.nanoTime());
    }

    public InputCommand(CommandType type, long timestampNanos)
    {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.timestampNanos = timestampNanos;
    }

    public CommandType getType()
    {
        return type;
    }

    public long getTimestampNanos()
    {
        return timestampNanos;
    }

    public static InputCommand of(CommandType type)
    {
        return new InputCommand(type);
    }
}