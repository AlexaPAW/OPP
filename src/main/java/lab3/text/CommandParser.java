package lab3.text;

import lab3.controller.CommandType;
import lab3.controller.InputCommand;

public class CommandParser {

    public InputCommand parse(int rawKeyCode) {
        if (rawKeyCode == -1) {
            return InputCommand.of(CommandType.NONE);
        }

        char ch = (char) rawKeyCode;

        if (ch == '\n' || ch == '\r' || ch == '\t') {
            return InputCommand.of(CommandType.NONE);
        }

        if (rawKeyCode == 27) {
            return InputCommand.of(CommandType.EXIT);
        }

        ch = Character.toLowerCase(ch);

        return switch (ch) {
            case 'w' -> InputCommand.of(CommandType.MOVE_UP);
            case 's' -> InputCommand.of(CommandType.MOVE_DOWN);
            case 'a' -> InputCommand.of(CommandType.MOVE_LEFT);
            case 'd' -> InputCommand.of(CommandType.MOVE_RIGHT);

            case 'i' -> InputCommand.of(CommandType.SHOOT_UP);
            case 'k' -> InputCommand.of(CommandType.SHOOT_DOWN);
            case 'j' -> InputCommand.of(CommandType.SHOOT_LEFT);
            case 'l' -> InputCommand.of(CommandType.SHOOT_RIGHT);

            case 'n' -> InputCommand.of(CommandType.NEW_GAME);
            case 'q' -> InputCommand.of(CommandType.EXIT);
            case 'b' -> InputCommand.of(CommandType.ABOUT);
            case 'h' -> InputCommand.of(CommandType.HIGH_SCORES);
            case 'p' -> InputCommand.of(CommandType.PAUSE);
            case 'r' -> InputCommand.of(CommandType.RESUME);

            case '`', '~' -> InputCommand.of(CommandType.DEBUG_TOGGLE_MODE);
            case '1' -> InputCommand.of(CommandType.DEBUG_HEAL_PLAYER);
            case '2' -> InputCommand.of(CommandType.DEBUG_DAMAGE_PLAYER);
            case '3' -> InputCommand.of(CommandType.DEBUG_BUFF_DAMAGE);
            case '4' -> InputCommand.of(CommandType.DEBUG_KILL_ENEMIES);
            case '5' -> InputCommand.of(CommandType.DEBUG_SPAWN_CHEST);
            case '6' -> InputCommand.of(CommandType.DEBUG_SPAWN_SLIME);
            case '7' -> InputCommand.of(CommandType.DEBUG_SPAWN_SHOOTER);
            case '8' -> InputCommand.of(CommandType.DEBUG_SPAWN_TANK);
            case '9' -> InputCommand.of(CommandType.DEBUG_SPAWN_BOSS);
            case '0' -> InputCommand.of(CommandType.DEBUG_WIN);
            case '-' -> InputCommand.of(CommandType.DEBUG_LOSE);

            default -> InputCommand.of(CommandType.NONE);
        };
    }

    public String helpText() {
        return """
                Controls:
                  W/A/S/D  - move
                  I/J/K/L  - shoot
                  N        - new game
                  Q / Esc  - exit
                  B        - about
                  H        - high scores
                  P        - pause
                  R        - resume

                Debug mode:
                  ` / ~    - toggle debug mode
                  1        - heal player
                  2        - damage player
                  3        - buff player damage
                  4        - kill enemies
                  5        - spawn chest
                  6        - spawn slime
                  7        - spawn shooter
                  8        - spawn tank
                  9        - spawn boss
                  0        - win
                  -        - lose
                """;
    }
}