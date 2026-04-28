package lab3.model.core;

public enum GameState
{
    READY,
    RUNNING,
    PAUSED,
    WIN,
    LOSE,
    STOPPED;

    public boolean isTerminal()
    {
        return this == WIN || this == LOSE || this == STOPPED;
    }
}