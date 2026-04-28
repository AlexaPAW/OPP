package lab3.gui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import lab3.model.core.Game;
import lab3.model.core.GameState;
import lab3.model.objects.Player;

public final class HudBar extends HBox {
    private final Label stateLabel = new Label();
    private final Label hpLabel = new Label();
    private final Label timeLabel = new Label();
    private final Label tickLabel = new Label();
    private final Label debugLabel = new Label();
    private final Label godModeLabel = new Label();

    public HudBar()
    {
        super(16);

        getStyleClass().add("hud-bar");
        setAlignment(Pos.CENTER_LEFT);

        configureLabel(stateLabel);
        configureLabel(hpLabel);
        configureLabel(timeLabel);
        configureLabel(tickLabel);
        configureLabel(debugLabel);
        configureLabel(godModeLabel);
        godModeLabel.getStyleClass().add("debug");
        debugLabel.getStyleClass().add("debug");


        getChildren().addAll(stateLabel, hpLabel, timeLabel, tickLabel, debugLabel, godModeLabel);
    }

    public void update(Game game, boolean debugMode) {
        if (game == null) {
            stateLabel.setText("State: -");
            hpLabel.setText("HP: -");
            timeLabel.setText("Time: -");
            tickLabel.setText("Tick: -");
            debugLabel.setText("Debug: " + (debugMode ? "ON" : "OFF"));
            godModeLabel.setText("GodMode: " + (debugMode ? "ON" : "OFF"));
            return;
        }

        stateLabel.setText("State: " + game.getState());
        timeLabel.setText("Time: " + game.getElapsedSeconds() + "s");
        tickLabel.setText("Tick: " + game.getTickCount());
        debugLabel.setText("Debug: " + (debugMode ? "ON" : "OFF"));
        godModeLabel.setText("GodMode: " + (debugMode ? "ON" : "OFF"));

        Player player = game.getPlayer().orElse(null);
        if (player == null) {
            hpLabel.setText("HP: -");
        } else {
            hpLabel.setText("HP: " + player.getHp() + "/" + player.getMaxHp() + " | DMG: " + player.getDamage());
        }

        if (game.getState() == GameState.WIN) {
            stateLabel.setText("State: WIN");
        } else if (game.getState() == GameState.LOSE) {
            stateLabel.setText("State: LOSE");
        }
    }

    private void configureLabel(Label label) {
        label.getStyleClass().add("hud-label");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(label, Priority.ALWAYS);
    }
}
