package lab3.text;

import lab3.model.core.Game;
import lab3.model.core.GameState;
import lab3.model.objects.Bullet;
import lab3.model.objects.Chest;
import lab3.model.objects.Enemy;
import lab3.model.objects.GameObject;
import lab3.model.objects.Player;
import lab3.model.objects.Wall;
import lab3.model.score.HighScoreTable;
import lab3.model.score.Score;
import lab3.model.world.Room;

import java.util.List;

public class TextUI {

    public synchronized void render(Game game, boolean debugMode) {
        clearScreen();

        if (game == null || game.getCurrentRoom() == null) {
            System.out.println("Game is not initialized.");
            return;
        }

        Room room = game.getCurrentRoom();

        System.out.println("=== ROGUELIKE ===");
        System.out.println("State: " + game.getState());
        System.out.println("Tick: " + game.getTickCount() + "   Time: " + game.getElapsedSeconds() + "s");
        System.out.println("Debug: " + (debugMode ? "ON" : "OFF"));

        Player player = game.getPlayer().orElse(null);
        if (player != null) {
            System.out.println("HP: " + player.getHp() + "/" + player.getMaxHp()
                    + "   Damage: " + player.getDamage());
        }

        System.out.println();
        printRoom(room, game.getObjectsSnapshot());

        System.out.println();
        System.out.println("Legend: P=player  E=enemy  *=bullet  C=chest  #=wall  .=empty");
        System.out.println("Controls: WASD move | IJKL shoot | N new game | Q exit | B about | H highscores | P pause | R resume");
        System.out.println("Debug: ` or ~ toggle | 1 heal | 2 dmg | 3 buff | 4 kill enemies | 5 spawn enemy | 6 win | 7 lose");

        if (game.getState() == GameState.WIN) {
            System.out.println();
            System.out.println("YOU WIN! Press N for new game or Q to exit.");
        } else if (game.getState() == GameState.LOSE) {
            System.out.println();
            System.out.println("GAME OVER! Press N for new game or Q to exit.");
        }
    }

    public synchronized void showAbout() {
        clearScreen();
        System.out.println("=== ABOUT ===");
        System.out.println("Tick-based MVC roguelike.");
        System.out.println("Move with WASD or arrows.");
        System.out.println("Shoot with IJKL.");
        System.out.println("Menu: New Game, High Scores, About, Exit.");
        System.out.println("Debug keys are available when debug mode is toggled.");
        System.out.println();
        System.out.println("Press any key to continue...");
    }

    public synchronized void showHighScores(HighScoreTable table) {
        clearScreen();
        System.out.println("=== HIGH SCORES ===");

        if (table == null || table.isEmpty()) {
            System.out.println("No scores yet.");
        } else {
            int index = 1;
            for (Score score : table.getScores()) {
                System.out.println(index + ". " + score.getPlayerName()
                        + " | points=" + score.getPoints()
                        + " | time=" + score.getSurvivedSeconds() + "s");
                index++;
            }
        }

        System.out.println();
        System.out.println("Press any key to continue...");
    }

    public synchronized void showMessage(String title, String message) {
        clearScreen();
        System.out.println("=== " + title + " ===");
        System.out.println(message);
        System.out.println();
        System.out.println("Press any key to continue...");
    }

    private void printRoom(Room room, List<GameObject> objects) {
        for (int y = 0; y < room.getHeight(); y++) {
            StringBuilder line = new StringBuilder(room.getWidth());
            for (int x = 0; x < room.getWidth(); x++) {
                line.append(symbolAt(objects, x, y));
            }
            System.out.println(line);
        }
    }

    private char symbolAt(List<GameObject> objects, int x, int y) {
        GameObject best = null;
        int bestPriority = Integer.MIN_VALUE;

        for (GameObject object : objects) {
            if (object == null || !object.isAlive()) {
                continue;
            }

            if (object.getX() == x && object.getY() == y) {
                int p = priority(object);
                if (p > bestPriority) {
                    bestPriority = p;
                    best = object;
                }
            }
        }

        return best == null ? '.' : best.getSymbol();
    }

    private int priority(GameObject object) {
        if (object instanceof Player) return 500;
        if (object instanceof Enemy) return 400;
        if (object instanceof Chest) return 300;
        if (object instanceof Bullet) return 200;
        if (object instanceof Wall) return 100;
        return 0;
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}