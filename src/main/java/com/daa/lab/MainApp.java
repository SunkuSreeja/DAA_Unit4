package com.daa.lab;

import com.daa.lab.ui.DashboardView;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main entry point for the Robotics Path Planning – Algorithmic Optimization Lab.
 * Provides a static {@link #navigateTo(Parent)} helper so any view can swap
 * the scene root without holding a reference to the Stage.
 */
public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        stage.setTitle("Robotics Path Planning \u2013 Algorithmic Optimization Lab");
        stage.setWidth(1280);
        stage.setHeight(800);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);

        DashboardView dashboard = new DashboardView();
        Scene scene = new Scene(dashboard, 1280, 800);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Replaces the current scene root with {@code view}.
     * Safe to call from any JavaFX thread.
     *
     * @param view the new root {@link Parent} to display
     */
    public static void navigateTo(Parent view) {
        if (primaryStage != null && primaryStage.getScene() != null) {
            primaryStage.getScene().setRoot(view);
        }
    }

    /** Returns the application's primary stage. */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
