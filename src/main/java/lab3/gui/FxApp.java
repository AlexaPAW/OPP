package lab3.gui;

import java.util.Objects;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Точка входа для JavaFX-версии игры.
 * Вся графика и ввод живут здесь, модель остаётся общей.
 */
public final class FxApp extends Application {
    private GameView root;

    @Override
    public void start(Stage stage) {
        root = new GameView();

        Scene scene = new Scene(root, 1024, 768);
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        FxApp.class.getResource("/fx/app.css"),
                        "Missing /fx/app.css"
                ).toExternalForm()
        );

        root.attach(scene);

        stage.setTitle("Lab3 - Roguelike");
        stage.setScene(scene);
        stage.setMinWidth(820);
        stage.setMinHeight(620);

        stage.setOnCloseRequest(e -> {
            e.consume();
            root.shutdown();
            Platform.exit();
        });

        stage.show();

        // Фокус нужен для обработки клавиш.
        Platform.runLater(root::requestFocus);

        root.start();
    }

    @Override
    public void stop() {
        if (root != null) {
            root.shutdown();
        }
    }
    
}