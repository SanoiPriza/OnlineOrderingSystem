package org.example.adminService.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AdminDashboardApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        String adminUrl = System.getProperty("adminServiceUrl", "http://localhost:8090");
        AdminApiClient client = new AdminApiClient(adminUrl);

        DashboardView view = new DashboardView(client, primaryStage);

        Scene scene = new Scene(view.buildRoot(), 1100, 720);
        scene.getStylesheets().add(getClass().getResource("/dashboard.css").toExternalForm());

        primaryStage.setTitle("Online Ordering System — Admin Dashboard");
        primaryStage.setScene(scene);
        primaryStage.show();

        view.startAutoRefresh();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
