package lab3.gui;

import java.util.Objects;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class MainApp extends Application
{
    private GameView root;

    @Override
    public void start(Stage stage) {
        root = new GameView();

        Scene scene = new Scene(root, 1024, 768);
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/app.css"),
                        "Missing CSS: src/main/resources/app.css"
                ).toExternalForm()
        );

        root.attach(scene);

        stage.setTitle("Lab3 - Roguelike");
        stage.setScene(scene);
        // stage.setMinWidth(820);
        // stage.setMinHeight(620);
        //stage.setMaximized(true);   // или stage.setFullScreen(true);
        stage.setFullScreen(true);
        stage.setOnCloseRequest(e -> {
            e.consume();
            shutdown();
            Platform.exit();
        });

        stage.show();
        Platform.runLater(root::requestFocus);

        root.start();
    }

    @Override
    public void stop() {
        shutdown();
    }

    private void shutdown() {
        if (root != null) {
            root.shutdown();
        }
    }
}