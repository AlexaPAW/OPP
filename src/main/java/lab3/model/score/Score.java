package lab3.model.score;

import java.time.LocalDateTime;
import java.util.Objects;

public class Score implements Comparable<Score>
{
    private final String playerName;
    private final int points;
    private final int survivedSeconds;
    private final LocalDateTime dateTime;

    public Score(String playerName, int points, int survivedSeconds)
    {
        this(playerName, points, survivedSeconds, LocalDateTime.now());
    }

    public Score(String playerName, int points, int survivedSeconds, LocalDateTime dateTime)
    {
        this.playerName = normalizeName(playerName);
        this.points = Math.max(0, points);
        this.survivedSeconds = Math.max(0, survivedSeconds);
        this.dateTime = Objects.requireNonNull(dateTime, "dateTime must not be null");
    }

    public String getPlayerName()
    {
        return playerName;
    }

    public int getPoints()
    {
        return points;
    }

    public int getSurvivedSeconds()
    {
        return survivedSeconds;
    }

    public LocalDateTime getDateTime()
    {
        return dateTime;
    }

    @Override
    public int compareTo(Score other)
    {
        int byPoints = Integer.compare(other.points, this.points);
        if (byPoints != 0)
            {
            return byPoints;
        }

        int byTime = Integer.compare(other.survivedSeconds, this.survivedSeconds);
        if (byTime != 0)
        {
            return byTime;
        }

        return this.dateTime.compareTo(other.dateTime);
    }

    public String toCsv()
    {
        return escape(playerName) + "," + points + "," + survivedSeconds + "," + dateTime;
    }

    public static Score fromCsv(String line)
    {
        if (line == null || line.isBlank())
        {
            throw new IllegalArgumentException("CSV line is empty");
        }

        String[] parts = line.split(",", 4);
        if (parts.length != 4)
        {
            throw new IllegalArgumentException("Invalid CSV line: " + line);
        }

        String name = unescape(parts[0].trim());
        int points = Integer.parseInt(parts[1].trim());
        int seconds = Integer.parseInt(parts[2].trim());
        LocalDateTime dateTime = LocalDateTime.parse(parts[3].trim());

        return new Score(name, points, seconds, dateTime);
    }

    private static String normalizeName(String playerName)
    {
        if (playerName == null || playerName.isBlank())
        {
            return "Player";
        }
        return playerName.trim();
    }

    private static String escape(String value)
    {
        return value.replace("\\", "\\\\").replace(",", "\\,");
    }

    private static String unescape(String value)
    {
        return value.replace("\\,", ",").replace("\\\\", "\\");
    }

    @Override
    public String toString()
    {
        return "Score{" +
                "playerName='" + playerName + '\'' +
                ", points=" + points +
                ", survivedSeconds=" + survivedSeconds +
                ", dateTime=" + dateTime +
                '}';
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (!(obj instanceof Score other)) return false;
        return points == other.points
                && survivedSeconds == other.survivedSeconds
                && playerName.equals(other.playerName)
                && dateTime.equals(other.dateTime);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(playerName, points, survivedSeconds, dateTime);
    }
}