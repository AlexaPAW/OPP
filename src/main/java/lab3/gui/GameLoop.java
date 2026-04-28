package lab3.gui;

import javafx.animation.AnimationTimer;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleConsumer;

/**
 * GUI-цикл:
 * - updateAction вызывается фиксированными шагами (логика);
 * - renderAction вызывается каждый кадр с alpha для интерполяции.
 */
public final class GameLoop extends AnimationTimer {
    private static final long MAX_FRAME_TIME_NANOS = TimeUnit.MILLISECONDS.toNanos(250);

    private final long fixedStepNanos;
    private final DoubleConsumer updateAction;
    private final DoubleConsumer renderAction;

    private long lastTime = 0L;
    private long accumulator = 0L;

    private volatile boolean running = false;

    public GameLoop(long fixedStepMillis, DoubleConsumer updateAction, DoubleConsumer renderAction) {
        if (fixedStepMillis <= 0) {
            throw new IllegalArgumentException("fixedStepMillis must be > 0");
        }

        this.fixedStepNanos = TimeUnit.MILLISECONDS.toNanos(fixedStepMillis);
        this.updateAction = Objects.requireNonNull(updateAction, "updateAction must not be null");
        this.renderAction = Objects.requireNonNull(renderAction, "renderAction must not be null");
    }

    public synchronized void startLoop() {
        if (running) {
            return;
        }

        running = true;
        lastTime = 0L;
        accumulator = 0L;
        start();
    }

    public synchronized void stopLoop() {
        if (!running) {
            return;
        }

        running = false;
        stop();
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void handle(long now) {
        if (!running) {
            return;
        }

        if (lastTime == 0L) {
            lastTime = now;
            renderAction.accept(1.0);
            return;
        }

        long delta = now - lastTime;
        lastTime = now;

        if (delta < 0L) {
            delta = 0L;
        } else if (delta > MAX_FRAME_TIME_NANOS) {
            delta = MAX_FRAME_TIME_NANOS;
        }

        accumulator += delta;

        while (accumulator >= fixedStepNanos) {
            updateAction.accept(fixedStepNanos / 1_000_000_000.0);
            accumulator -= fixedStepNanos;
        }

        double alpha = fixedStepNanos == 0L ? 1.0 : (double) accumulator / (double) fixedStepNanos;
        alpha = Math.max(0.0, Math.min(1.0, alpha));

        renderAction.accept(alpha);
    }
}