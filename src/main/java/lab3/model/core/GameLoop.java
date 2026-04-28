package lab3.model.core;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Игровой цикл в отдельном потоке.
 *
 * Здесь считается delta time и передаётся в Game.update(deltaSeconds).
 * Это позволяет делать движение плавным и независимым от частоты нажатий клавиш.
 */
public final class GameLoop implements Runnable {
    /**
     * Защита от огромного delta после паузы, сворачивания окна и т.п.
     * Иначе объект может "телепортироваться" слишком далеко за один кадр.
     */
    private static final long MAX_FRAME_TIME_NANOS = TimeUnit.MILLISECONDS.toNanos(250);

    private final Game game;
    private final long tickMillis;

    private final Runnable beforeTick;
    private final Consumer<Game> afterTick;

    private volatile boolean running;
    private Thread thread;

    public GameLoop(Game game, long tickMillis, Runnable beforeTick, Consumer<Game> afterTick) {
        this.game = Objects.requireNonNull(game, "game must not be null");

        if (tickMillis <= 0) {
            throw new IllegalArgumentException("tickMillis must be > 0");
        }

        this.tickMillis = tickMillis;
        this.beforeTick = beforeTick;
        this.afterTick = afterTick;
        this.running = false;
    }

    public synchronized void start() {
        if (running) {
            return;
        }

        running = true;
        thread = new Thread(this, "game-loop");
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        game.stop();

        if (thread != null) {
            thread.interrupt();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public Thread getThread() {
        return thread;
    }

    @Override
    public void run() {
        long previousTickNanos = System.nanoTime();

        try {
            while (running && !game.isFinished()) {
                long frameStartNanos = System.nanoTime();

                long frameDeltaNanos = frameStartNanos - previousTickNanos;
                previousTickNanos = frameStartNanos;

                if (frameDeltaNanos < 0L) {
                    frameDeltaNanos = 0L;
                } else if (frameDeltaNanos > MAX_FRAME_TIME_NANOS) {
                    frameDeltaNanos = MAX_FRAME_TIME_NANOS;
                }

                double deltaSeconds = frameDeltaNanos / 1_000_000_000.0;

                if (beforeTick != null) {
                    beforeTick.run();
                }

                game.update(deltaSeconds);

                if (afterTick != null) {
                    afterTick.accept(game);
                }

                long spentNanos = System.nanoTime() - frameStartNanos;
                long targetFrameNanos = TimeUnit.MILLISECONDS.toNanos(tickMillis);
                long sleepNanos = targetFrameNanos - spentNanos;

                if (sleepNanos > 0L) {
                    long sleepMillis = sleepNanos / 1_000_000L;
                    int sleepExtraNanos = (int) (sleepNanos % 1_000_000L);

                    try {
                        Thread.sleep(sleepMillis, sleepExtraNanos);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    Thread.yield();
                }
            }
        } finally {
            running = false;
        }
    }
}