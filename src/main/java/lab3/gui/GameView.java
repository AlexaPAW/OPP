package lab3.gui;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javafx.util.Duration;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import lab3.controller.CommandType;
import lab3.controller.GameController;
import lab3.controller.InputCommand;
import lab3.model.core.Game;
import lab3.model.core.GameState;
import lab3.model.objects.Player;
import lab3.model.score.HighScoreTable;
import lab3.model.score.Score;
import lab3.model.world.Room;
import lab3.model.world.RoomFactory;

public final class GameView extends BorderPane {
    private final RoomFactory roomFactory;
    private final HighScoreTable highScoreTable;
    private final Path scoreFile;

    private final Game game;
    private final GameController controller;

    private final InputHandler inputHandler;
    private final Renderer renderer;
    private final HudBar hudBar;
    private final GameLoop gameLoop;

    private final Canvas canvas;
    private final StackPane canvasLayer;

    private final VBox debugPanel;
    private final HBox playArea;
    private boolean debugPanelVisible = false;

    private static final double DEBUG_PANEL_WIDTH = 240.0;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile boolean finalScoreRecorded = false;

    private Thread timerThread;

    public GameView() {
        this.roomFactory = new RoomFactory(RoomFactory.defaultTemplates());
        this.scoreFile = Path.of(System.getProperty("user.home"), ".lab3", "highscores.csv");
        this.highScoreTable = new HighScoreTable(10);
        loadScores();

        Room initialRoom = roomFactory.createNextRoom();
        int roomPixelW = initialRoom.getWidth() * 15;
        int roomPixelH = initialRoom.getHeight() * 15;
        this.game = new Game(initialRoom);
        this.game.setNextRoomSupplier(roomFactory::createNextRoom);

        this.controller = new GameController(game, roomFactory::createNextRoom);

        this.renderer = new Renderer(15);
        this.hudBar = new HudBar();

        this.canvas = new Canvas(roomPixelW, roomPixelH);
        this.canvas.setFocusTraversable(false);

        this.canvasLayer = new StackPane(canvas);
        this.canvasLayer.getStyleClass().add("canvas-frame");
        this.canvasLayer.setMinSize(0, 0);
        this.canvasLayer.setPrefSize(800, 600);
        this.canvasLayer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        this.canvasLayer.setPadding(Insets.EMPTY);

        Consumer<InputCommand> commandSink = this::handleUiCommand;
        this.inputHandler = new InputHandler(commandSink);

        this.gameLoop = new GameLoop(
                80,
                this::updateLogicStep,
                this::renderFrame
        );

        this.debugPanel = buildDebugPanel();
        this.debugPanel.setMinWidth(0);
        this.debugPanel.setPrefWidth(0);
        this.debugPanel.setMaxWidth(0);
        this.debugPanel.setOpacity(0);
        this.debugPanel.setVisible(false);
        this.debugPanel.setManaged(false);
        this.debugPanel.setMouseTransparent(true);

        HBox.setHgrow(canvasLayer, Priority.ALWAYS);

        this.playArea = new HBox(canvasLayer, debugPanel);
        setTop(buildMenuBar());
        setCenter(playArea);
        setBottom(hudBar);

        getStyleClass().add("root-view");
        setFocusTraversable(true);

        Platform.runLater(() -> {
            renderFrame(1.0);
            refreshHud();
        });
    }

    public void attach(Scene scene) {
        inputHandler.attach(scene);
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        finalScoreRecorded = false;
        controller.newGame();
        game.start();
        inputHandler.clear();

        refreshHud();
        renderFrame(1.0);
        startTimerThread();

        if (!gameLoop.isRunning()) {
            gameLoop.startLoop();
        }
    }

    public void shutdown() {
        if (running.getAndSet(false)) {
            gameLoop.stopLoop();
            if (timerThread != null) {
                timerThread.interrupt();
            }
        }
        saveScores();
    }

    private MenuBar buildMenuBar() {
        MenuItem newGame = new MenuItem("New Game");
        newGame.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
        newGame.setOnAction(e -> handleUiCommand(InputCommand.of(CommandType.NEW_GAME)));

        MenuItem highScores = new MenuItem("High Scores");
        highScores.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN));
        highScores.setOnAction(e -> handleUiCommand(InputCommand.of(CommandType.HIGH_SCORES)));

        MenuItem about = new MenuItem("About");
        about.setAccelerator(new KeyCodeCombination(KeyCode.F1));
        about.setOnAction(e -> handleUiCommand(InputCommand.of(CommandType.ABOUT)));

        MenuItem exit = new MenuItem("Exit");
        exit.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN));
        exit.setOnAction(e -> handleUiCommand(InputCommand.of(CommandType.EXIT)));

        Menu gameMenu = new Menu("Game");
        gameMenu.getItems().addAll(newGame, highScores, exit);

        Menu helpMenu = new Menu("Help");
        helpMenu.getItems().add(about);

        MenuBar bar = new MenuBar(gameMenu, helpMenu);
        bar.getStyleClass().add("app-menu-bar");
        return bar;
    }

    private VBox buildDebugPanel() {
        Label title = new Label("Debug");
        title.getStyleClass().add("debug-title");

        Button toggleDebug = new Button("Toggle Debug");
        toggleDebug.setMaxWidth(Double.MAX_VALUE);
        toggleDebug.setOnAction(e -> {
            controller.toggleDebugMode();
            syncDebugPanelState();
            refreshHud();
        });

        Button toggleGod = new Button("Toggle God Mode");
        toggleGod.setMaxWidth(Double.MAX_VALUE);
        toggleGod.setOnAction(e -> handleUiCommand(InputCommand.of(CommandType.DEBUG_GOD_TOGGLE)));

        Button heal = new Button("Heal Player");
        heal.setMaxWidth(Double.MAX_VALUE);
        heal.setOnAction(e -> handleUiCommand(InputCommand.of(CommandType.DEBUG_HEAL_PLAYER)));

        Button buff = new Button("Buff Damage");
        buff.setMaxWidth(Double.MAX_VALUE);
        buff.setOnAction(e -> handleUiCommand(InputCommand.of(CommandType.DEBUG_BUFF_DAMAGE)));

        Button spawnChest = new Button("Spawn Chest");
        spawnChest.setMaxWidth(Double.MAX_VALUE);
        spawnChest.setOnAction(e -> handleUiCommand(InputCommand.of(CommandType.DEBUG_SPAWN_CHEST)));

        Button spawnSlime = new Button("Spawn Slime");
        spawnSlime.setMaxWidth(Double.MAX_VALUE);
        spawnSlime.setOnAction(e -> handleUiCommand(InputCommand.of(CommandType.DEBUG_SPAWN_SLIME)));

        Button spawnShooter = new Button("Spawn Shooter");
        spawnShooter.setMaxWidth(Double.MAX_VALUE);
        spawnShooter.setOnAction(e -> handleUiCommand(InputCommand.of(CommandType.DEBUG_SPAWN_SHOOTER)));

        Button spawnTank = new Button("Spawn Tank");
        spawnTank.setMaxWidth(Double.MAX_VALUE);
        spawnTank.setOnAction(e -> handleUiCommand(InputCommand.of(CommandType.DEBUG_SPAWN_TANK)));

        Button spawnBoss = new Button("Spawn Boss");
        spawnBoss.setMaxWidth(Double.MAX_VALUE);
        spawnBoss.setOnAction(e -> handleUiCommand(InputCommand.of(CommandType.DEBUG_SPAWN_BOSS)));

        Button killEnemies = new Button("Kill Enemies");
        killEnemies.setMaxWidth(Double.MAX_VALUE);
        killEnemies.setOnAction(e -> handleUiCommand(InputCommand.of(CommandType.DEBUG_KILL_ENEMIES)));

        Button win = new Button("Win");
        win.setMaxWidth(Double.MAX_VALUE);
        win.setOnAction(e -> handleUiCommand(InputCommand.of(CommandType.DEBUG_WIN)));

        Button lose = new Button("Lose");
        lose.setMaxWidth(Double.MAX_VALUE);
        lose.setOnAction(e -> handleUiCommand(InputCommand.of(CommandType.DEBUG_LOSE)));

        Label hint = new Label("Debug key: `/ ~");
        hint.getStyleClass().add("debug-hint");

        Label helpTitle = new Label("Shortcut help");
        helpTitle.getStyleClass().add("debug-help-title");

        Label helpText = new Label(
                "`  — toggle debug mode\n" +
                "G  — toggle god mode\n" +
                "1  — heal player\n" +
                "2  — damage player\n" +
                "3  — buff damage\n" +
                "4  — kill enemies\n" +
                "5  — spawn chest\n" +
                "6  — spawn slime\n" +
                "7  — spawn shooter\n" +
                "8  — spawn tank\n" +
                "9  — spawn boss\n" +
                "0  — win\n" +
                "-  — lose"
        );
        helpText.getStyleClass().add("debug-help-text");
        helpText.setWrapText(true);

        VBox box = new VBox(8,
                title,
                toggleDebug,
                toggleGod,
                heal,
                buff,
                spawnChest,
                spawnSlime,
                spawnShooter,
                spawnTank,
                spawnBoss,
                killEnemies,
                win,
                lose,
                hint,
                helpTitle,
                helpText
        );

        box.getStyleClass().add("debug-panel");
        box.setPrefWidth(DEBUG_PANEL_WIDTH);
        box.setMinWidth(0);
        box.setMaxWidth(DEBUG_PANEL_WIDTH);
        box.setPadding(new Insets(10));

        for (javafx.scene.Node node : box.getChildren()) {
            if (node instanceof Button button) {
                button.setMaxWidth(Double.MAX_VALUE);
            }
        }

        return box;
    }

    private void syncDebugPanelState() {
        boolean shouldShow = controller.isDebugMode();
        if (shouldShow == debugPanelVisible) {
            return;
        }

        debugPanelVisible = shouldShow;
        animateDebugPanel(shouldShow);
    }

    private void animateDebugPanel(boolean show) {
        double fromWidth = debugPanel.getPrefWidth();
        double toWidth = show ? DEBUG_PANEL_WIDTH : 0.0;

        double fromOpacity = debugPanel.getOpacity();
        double toOpacity = show ? 1.0 : 0.0;

        if (show) {
            debugPanel.setManaged(true);
            debugPanel.setVisible(true);
            debugPanel.setMouseTransparent(false);
        }

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(debugPanel.prefWidthProperty(), fromWidth),
                        new KeyValue(debugPanel.maxWidthProperty(), fromWidth),
                        new KeyValue(debugPanel.opacityProperty(), fromOpacity)
                ),
                new KeyFrame(Duration.millis(180),
                        new KeyValue(debugPanel.prefWidthProperty(), toWidth, Interpolator.EASE_BOTH),
                        new KeyValue(debugPanel.maxWidthProperty(), toWidth, Interpolator.EASE_BOTH),
                        new KeyValue(debugPanel.opacityProperty(), toOpacity, Interpolator.EASE_BOTH)
                )
        );

        timeline.setOnFinished(e -> {
            if (!show) {
                debugPanel.setVisible(false);
                debugPanel.setManaged(false);
                debugPanel.setMouseTransparent(true);
                debugPanel.setPrefWidth(0);
                debugPanel.setMaxWidth(0);
            } else {
                debugPanel.setPrefWidth(DEBUG_PANEL_WIDTH);
                debugPanel.setMaxWidth(DEBUG_PANEL_WIDTH);
            }
        });

        timeline.playFromStart();
    }

    private void handleUiCommand(InputCommand command) {
        if (command == null || command.getType() == null || command.getType() == CommandType.NONE) {
            return;
        }

        switch (command.getType()) {
            case NEW_GAME -> restartGame();
            case EXIT -> {
                shutdown();
                Platform.exit();
            }
            case ABOUT -> showAboutDialog();
            case HIGH_SCORES -> showHighScoresDialog();
            default -> controller.enqueueInput(command);
        }
    }

    private void restartGame() {
        finalScoreRecorded = false;
        inputHandler.clear();

        roomFactory.resetScript();
        controller.newGame();
        game.start();

        if (!running.get()) {
            running.set(true);
        }

        refreshHud();
        renderFrame(1.0);

        if (!gameLoop.isRunning()) {
            gameLoop.startLoop();
        }
    }

    /**
     * Одна фиксированная логическая итерация.
     * Плавность не здесь, а в renderFrame(alpha).
     */
    private void updateLogicStep(double deltaSeconds) {
        if (!running.get()) {
            return;
        }

        game.beginFrame();
        controller.beginFrame();
        inputHandler.emitHeldCommands(controller::enqueueInput);
        controller.processQueuedInputs();
        game.update(deltaSeconds);

        refreshHud();

        if (game.isFinished() && !finalScoreRecorded) {
            finalScoreRecorded = true;
            gameLoop.stopLoop();
            Platform.runLater(this::handleGameFinished);
        }
    }

    /**
     * Рендер вызывается каждый кадр, alpha приходит от fixed-step loop.
     */
    private void renderFrame(double alpha) {
        if (!running.get()) {
            return;
        }

        double width = canvas.getWidth();
        double height = canvas.getHeight();

        if (width <= 1 || height <= 1) {
            return;
        }

        renderer.render(canvas.getGraphicsContext2D(), game, width, height, alpha);
    }

    private void refreshHud() {
        hudBar.update(game, controller.isDebugMode());
        syncDebugPanelState();
    }

    private void startTimerThread() {
        if (timerThread != null && timerThread.isAlive()) {
            return;
        }

        timerThread = new Thread(() -> {
            while (running.get()) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (!running.get()) {
                    break;
                }

                if (game.getState() == GameState.RUNNING) {
                    game.incrementElapsedSeconds();
                    Platform.runLater(this::refreshHud);
                }
            }
        }, "fx-game-timer");

        timerThread.setDaemon(true);
        timerThread.start();
    }

    private void handleGameFinished() {
        if (game.getState() == GameState.STOPPED) {
            return;
        }

        Player player = game.getPlayer().orElse(null);
        int hp = player != null ? player.getHp() : 0;
        int points = game.getElapsedSeconds() * 100 + hp * 10;

        if (game.getState() == GameState.WIN) {
            points += 1000;
        }

        TextInputDialog input = new TextInputDialog("Player");
        input.setTitle("High Scores");
        input.setHeaderText("Game finished. Enter your name:");
        input.setContentText("Name:");
        ownerWindowIfPresent(input::initOwner);

        String name = input.showAndWait().orElse("Player");
        if (name.isBlank()) {
            name = "Player";
        }

        highScoreTable.addScore(new Score(name.trim(), points, game.getElapsedSeconds()));
        saveScores();

        Alert alert = new Alert(
                Alert.AlertType.INFORMATION,
                game.getState() == GameState.WIN
                        ? "You win! Score saved."
                        : "Game over. Score saved.",
                ButtonType.OK
        );
        alert.setTitle("Result");
        alert.setHeaderText(null);
        ownerWindowIfPresent(alert::initOwner);
        alert.showAndWait();
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Tick-based MVC roguelike");
        alert.setContentText(
                "Move: WASD or arrows\n" +
                "Shoot: IJKL\n" +
                "Menu: New Game, High Scores, About, Exit\n" +
                "Debug keys are available through the controller"
        );
        ownerWindowIfPresent(alert::initOwner);
        alert.showAndWait();
    }

    private void showHighScoresDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("High Scores");
        dialog.setHeaderText("Top results");

        TextArea area = new TextArea(buildHighScoresText());
        area.setEditable(false);
        area.setWrapText(false);
        area.setPrefColumnCount(42);
        area.setPrefRowCount(14);
        area.setStyle("-fx-font-family: 'Consolas', 'Monospaced'; -fx-font-size: 13px;");

        dialog.getDialogPane().setContent(area);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        ownerWindowIfPresent(dialog::initOwner);
        dialog.showAndWait();
    }

    private String buildHighScoresText() {
        StringBuilder sb = new StringBuilder();
        if (highScoreTable.isEmpty()) {
            sb.append("No scores yet.");
            return sb.toString();
        }

        int index = 1;
        for (Score score : highScoreTable.getScores()) {
            sb.append(index++)
                    .append(". ")
                    .append(score.getPlayerName())
                    .append(" | points=")
                    .append(score.getPoints())
                    .append(" | time=")
                    .append(score.getSurvivedSeconds())
                    .append("s\n");
        }
        return sb.toString();
    }

    private void loadScores() {
        try {
            highScoreTable.load(scoreFile);
        } catch (Exception ignored) {
            // Стартуем с пустой таблицы, если файл недоступен.
        }
    }

    private void saveScores() {
        try {
            highScoreTable.save(scoreFile);
        } catch (Exception ignored) {
            // Не мешаем игре, если сохранение не удалось.
        }
    }

    private void ownerWindowIfPresent(Consumer<Window> consumer) {
        if (getScene() != null && getScene().getWindow() != null) {
            consumer.accept(getScene().getWindow());
        }
    }
}
