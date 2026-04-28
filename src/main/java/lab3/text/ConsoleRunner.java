package lab3.text;

import lab3.controller.CommandType;
import lab3.controller.GameController;
import lab3.controller.InputCommand;
import lab3.model.core.Game;
import lab3.model.core.GameLoop;
import lab3.model.core.GameState;
import lab3.model.objects.Player;
import lab3.model.score.HighScoreTable;
import lab3.model.score.Score;
import lab3.model.world.Room;
import lab3.model.world.RoomFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ConsoleRunner {
    private final TextUI ui;
    private final CommandParser parser;
    private final HighScoreTable highScoreTable;

    private final RoomFactory roomFactory;
    private final Game game;
    private final GameController controller;
    private GameLoop loop;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Path scoreFile;

    private volatile boolean finalScoreRecorded = false;

    public ConsoleRunner() {
        this.ui = new TextUI();
        this.parser = new CommandParser();
        this.highScoreTable = new HighScoreTable(10);
        this.scoreFile = Path.of(System.getProperty("user.home"), ".lab3", "highscores.csv");

        loadScores();

        this.roomFactory = new RoomFactory(RoomFactory.defaultTemplates());
        Room initialRoom = roomFactory.createRandomRoom();
        this.game = new Game(initialRoom);
        this.controller = new GameController(game, roomFactory::createRandomRoom);

        Consumer<InputCommand> commandSink = this::handleUiCommand;
        this.loop = new GameLoop(game, 120, controller::processQueuedInputs, g -> ui.render(g, controller.isDebugMode()));
    }

    public void run() {
        if (running.getAndSet(true)) {
            return;
        }

        controller.newGame();
        loop.start();
        ui.render(game, controller.isDebugMode());

        Thread inputThread = new Thread(this::readInputLoop, "console-input");
        inputThread.setDaemon(true);
        inputThread.start();

        while (running.get()) {
            if (controller.consumeExitRequested()) {
                shutdown();
                break;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void readInputLoop() {
        InputStream in = new BufferedInputStream(System.in);

        try {
            while (running.get()) {
                int ch = in.read();
                if (ch == -1) {
                    break;
                }

                InputCommand command = parser.parse(ch);
                if (command.getType() == CommandType.NONE) {
                    continue;
                }

                handleUiCommand(command);
                ui.render(game, controller.isDebugMode());

                if (!running.get()) {
                    break;
                }
            }
        } catch (IOException e) {
            ui.showMessage("INPUT ERROR", "Failed to read keyboard input: " + e.getMessage());
            shutdown();
        }
    }

    private void handleUiCommand(InputCommand command) {
        if (command == null || command.getType() == CommandType.NONE) {
            return;
        }

        switch (command.getType()) {
            case NEW_GAME -> restartGame();
            case EXIT -> shutdown();
            case ABOUT -> ui.showAbout();
            case HIGH_SCORES -> ui.showHighScores(highScoreTable);
            default -> controller.handleInput(command);
        }

        if (command.getType() != CommandType.EXIT)
        {
            ui.render(game, controller.isDebugMode());
        }
    }

    private void restartGame()
    {
        finalScoreRecorded = false;

        controller.newGame();
        game.start();

        if (loop == null || !loop.isRunning())
        {
            loop = new GameLoop( game, 120, controller::processQueuedInputs, g -> ui.render(g, controller.isDebugMode()) );
            loop.start();
        }

        ui.render(game, controller.isDebugMode());
    }

    private void loadScores() {
        try {
            highScoreTable.load(scoreFile);
        } catch (Exception ignored) {
            // стартуем с пустой таблицы
        }
    }

    private void saveScores() {
        try {
            highScoreTable.save(scoreFile);
        } catch (Exception ignored) {
            // не мешаем игре
        }
    }

    private void handleGameFinished() {
        if (finalScoreRecorded) {
            return;
        }
        finalScoreRecorded = true;

        Player player = game.getPlayer().orElse(null);
        int hp = player != null ? player.getHp() : 0;
        int points = game.getElapsedSeconds() * 100 + hp * 10;

        if (game.getState() == GameState.WIN) {
            points += 1000;
        }

        ui.showMessage(
                "RESULT",
                game.getState() == GameState.WIN
                        ? "You win! Score: " + points
                        : "Game over. Score: " + points
        );

        highScoreTable.addScore(new Score("Player", points, game.getElapsedSeconds()));
        saveScores();

        ui.render(game, controller.isDebugMode());
    }

    private void shutdown() {
        if (!running.getAndSet(false)) {
            return;
        }

        saveScores();

        if (loop != null) {
            loop.stop();
        }

        ui.showMessage("EXIT", "Game closed.");
    }

    public Game getGame() {
        return game;
    }

    public GameController getController() {
        return controller;
    }

    public TextUI getUi() {
        return ui;
    }

    public HighScoreTable getHighScoreTable() {
        return highScoreTable;
    }
}