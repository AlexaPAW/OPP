package lab3.model.score;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class HighScoreTable
{
    private final List<Score> scores = new ArrayList<>();
    private final int maxEntries;

    public HighScoreTable()
    {
        this(10);
    }

    public HighScoreTable(int maxEntries)
    {
        this.maxEntries = Math.max(1, maxEntries);
    }

    public void addScore(Score score)
    {
        scores.add(Objects.requireNonNull(score, "score must not be null"));
        Collections.sort(scores);

        if (scores.size() > maxEntries)
        {
            scores.subList(maxEntries, scores.size()).clear();
        }
    }

    public List<Score> getScores()
    {
        return List.copyOf(scores);
    }

    public Score getBestScore()
    {
        return scores.isEmpty() ? null : scores.get(0);
    }

    public void clear()
    {
        scores.clear();
    }

    public int size()
    {
        return scores.size();
    }

    public boolean isEmpty()
    {
        return scores.isEmpty();
    }

    public void load(Path file) throws IOException
    {
        scores.clear();

        if (file == null || !Files.exists(file))
        {
            return;
        }

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (String line : lines)
        {
            if (line == null || line.isBlank())
            {
                continue;
            }
            try
            {
                scores.add(Score.fromCsv(line));
            }
            catch (RuntimeException ignored)
            {
                // Пропускаем битые строки
            }
        }

        Collections.sort(scores);
        trimToMax();
    }

    public void save(Path file) throws IOException
    {
        if (file == null)
        {
            throw new IllegalArgumentException("file must not be null");
        }

        Path parent = file.getParent();
        if (parent != null)
        {
            Files.createDirectories(parent);
        }

        List<String> lines = new ArrayList<>();
        for (Score score : scores)
        {
            lines.add(score.toCsv());
        }

        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    public List<Score> top(int count)
    {
        int n = Math.max(0, Math.min(count, scores.size()));
        return List.copyOf(scores.subList(0, n));
    }

    private void trimToMax()
    {
        if (scores.size() > maxEntries)
        {
            scores.subList(maxEntries, scores.size()).clear();
        }
    }

    @Override
    public String toString()
    {
        return "HighScoreTable{" +
                "maxEntries=" + maxEntries +
                ", scores=" + scores +
                '}';
    }
}