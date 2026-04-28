package lab3.gui;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lab3.controller.CommandType;
import lab3.controller.InputCommand;

/**
 * Ввод через состояние клавиш.
 *
 * Главное отличие от Swing-версии:
 * - удерживаемые клавиши не превращаются в "однократное событие";
 * - движение и стрельба могут работать одновременно;
 * - команда на меню не повторяется из-за auto-repeat.
 */
public final class InputHandler {
    private final Consumer<InputCommand> commandSink;

    private final Map<KeyCode, CommandType> heldBindings = new EnumMap<>(KeyCode.class);
    private final Map<KeyCode, CommandType> oneShotBindings = new EnumMap<>(KeyCode.class);
    private final Set<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);

    public InputHandler(Consumer<InputCommand> commandSink) {
        this.commandSink = Objects.requireNonNull(commandSink, "commandSink must not be null");
        bindDefaults();
    }

    public void attach(Scene scene) {
        Objects.requireNonNull(scene, "scene must not be null");

        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, this::onKeyReleased);
    }

    public void clear() {
        pressedKeys.clear();
    }

    public void emitHeldCommands(Consumer<InputCommand> sink) {
        Objects.requireNonNull(sink, "sink must not be null");

        EnumSet<CommandType> uniqueCommands = EnumSet.noneOf(CommandType.class);

        for (KeyCode code : pressedKeys) {
            CommandType type = heldBindings.get(code);
            if (type != null) {
                uniqueCommands.add(type);
            }
        }

        // Движение
        boolean up = uniqueCommands.contains(CommandType.MOVE_UP);
        boolean down = uniqueCommands.contains(CommandType.MOVE_DOWN);
        boolean left = uniqueCommands.contains(CommandType.MOVE_LEFT);
        boolean right = uniqueCommands.contains(CommandType.MOVE_RIGHT);

        if (up ^ down) {
            sink.accept(InputCommand.of(up ? CommandType.MOVE_UP : CommandType.MOVE_DOWN));
        }

        if (left ^ right) {
            sink.accept(InputCommand.of(left ? CommandType.MOVE_LEFT : CommandType.MOVE_RIGHT));
        }

        // Стрельба
        boolean shootUp = uniqueCommands.contains(CommandType.SHOOT_UP);
        boolean shootDown = uniqueCommands.contains(CommandType.SHOOT_DOWN);
        boolean shootLeft = uniqueCommands.contains(CommandType.SHOOT_LEFT);
        boolean shootRight = uniqueCommands.contains(CommandType.SHOOT_RIGHT);

        if (shootUp ^ shootDown) {
            sink.accept(InputCommand.of(shootUp ? CommandType.SHOOT_UP : CommandType.SHOOT_DOWN));
        }

        if (shootLeft ^ shootRight) {
            sink.accept(InputCommand.of(shootLeft ? CommandType.SHOOT_LEFT : CommandType.SHOOT_RIGHT));
        }
    }

    private void onKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();

        // Удерживаемые команды просто запоминаем.
        if (heldBindings.containsKey(code)) {
            pressedKeys.add(code);
            event.consume();
            return;
        }

        // Одноразовые команды срабатывают только при первом нажатии,
        // повторные key repeat от клавиатуры игнорируются.
        CommandType oneShot = oneShotBindings.get(code);
        if (oneShot != null) {
            if (pressedKeys.add(code)) {
                commandSink.accept(InputCommand.of(oneShot));
            }
            event.consume();
        }
    }

    private void onKeyReleased(KeyEvent event) {
        KeyCode code = event.getCode();
        pressedKeys.remove(code);
    }

    private void bindDefaults() {
        // Движение.
        bindHeld(KeyCode.W, CommandType.MOVE_UP);
        bindHeld(KeyCode.S, CommandType.MOVE_DOWN);
        bindHeld(KeyCode.A, CommandType.MOVE_LEFT);
        bindHeld(KeyCode.D, CommandType.MOVE_RIGHT);

        bindHeld(KeyCode.UP, CommandType.MOVE_UP);
        bindHeld(KeyCode.DOWN, CommandType.MOVE_DOWN);
        bindHeld(KeyCode.LEFT, CommandType.MOVE_LEFT);
        bindHeld(KeyCode.RIGHT, CommandType.MOVE_RIGHT);

        // Стрельба.
        bindHeld(KeyCode.I, CommandType.SHOOT_UP);
        bindHeld(KeyCode.K, CommandType.SHOOT_DOWN);
        bindHeld(KeyCode.J, CommandType.SHOOT_LEFT);
        bindHeld(KeyCode.L, CommandType.SHOOT_RIGHT);

        // Меню и служебные команды.
        bindOneShot(KeyCode.N, CommandType.NEW_GAME);
        bindOneShot(KeyCode.Q, CommandType.EXIT);
        bindOneShot(KeyCode.B, CommandType.ABOUT);
        bindOneShot(KeyCode.H, CommandType.HIGH_SCORES);
        bindOneShot(KeyCode.P, CommandType.PAUSE);
        bindOneShot(KeyCode.R, CommandType.RESUME);
        bindOneShot(KeyCode.ESCAPE, CommandType.EXIT);

        // Debug
        bindOneShot(KeyCode.BACK_QUOTE, CommandType.DEBUG_TOGGLE_MODE); // ` = ё
        bindOneShot(KeyCode.DIGIT1, CommandType.DEBUG_HEAL_PLAYER);
        bindOneShot(KeyCode.DIGIT2, CommandType.DEBUG_DAMAGE_PLAYER);
        bindOneShot(KeyCode.DIGIT3, CommandType.DEBUG_BUFF_DAMAGE);
        bindOneShot(KeyCode.DIGIT4, CommandType.DEBUG_KILL_ENEMIES);
        bindOneShot(KeyCode.DIGIT5, CommandType.DEBUG_SPAWN_CHEST);
        bindOneShot(KeyCode.DIGIT6, CommandType.DEBUG_SPAWN_SLIME);
        bindOneShot(KeyCode.DIGIT7, CommandType.DEBUG_SPAWN_SHOOTER);
        bindOneShot(KeyCode.DIGIT8, CommandType.DEBUG_SPAWN_TANK);
        bindOneShot(KeyCode.DIGIT9, CommandType.DEBUG_SPAWN_BOSS);
        bindOneShot(KeyCode.DIGIT0, CommandType.DEBUG_WIN);
        bindOneShot(KeyCode.MINUS, CommandType.DEBUG_LOSE);
    }

    private void bindHeld(KeyCode code, CommandType type) {
        heldBindings.put(code, type);
    }

    private void bindOneShot(KeyCode code, CommandType type) {
        oneShotBindings.put(code, type);
    }
}