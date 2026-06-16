package org.example.adminService.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import org.example.adminService.dto.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DashboardView {

    private static final int AUTO_REFRESH_SECONDS = 15;

    private final AdminApiClient client;
    private final Stage stage;

    private Label lastRefreshLabel;
    private Label statusBarLabel;

    private final ObservableList<ServiceStatus> serviceList = FXCollections.observableArrayList();

    private final ObservableList<QueueStats> queueList = FXCollections.observableArrayList();

    private Label outboxPending;
    private Label outboxProcessing;
    private Label outboxCompleted;
    private Label outboxFailed;

    private final java.util.Map<String, Label> dlqStatusLabels = new java.util.LinkedHashMap<>();
    private FlowPane dlqButtonsPane;

    private ScheduledExecutorService scheduler;

    public DashboardView(AdminApiClient client, Stage stage) {
        this.client = client;
        this.stage = stage;
    }

    public VBox buildRoot() {
        VBox root = new VBox();
        root.getStyleClass().add("root");

        root.getChildren().addAll(
                buildHeader(),
                buildMiddleRow(),
                buildQueueSection(),
                buildDlqRetryPanel(),
                buildStatusBar());

        VBox.setVgrow(buildQueueSection(), Priority.ALWAYS);
        return root;
    }

    private HBox buildHeader() {
        HBox header = new HBox();
        header.getStyleClass().add("header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 20, 14, 20));
        header.setSpacing(12);

        Label title = new Label("🛒  Online Ordering System — Admin Dashboard");
        title.getStyleClass().add("header-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        lastRefreshLabel = new Label("Last refresh: —");
        lastRefreshLabel.getStyleClass().add("header-meta");

        Button refreshBtn = new Button("⟳  Refresh Now");
        refreshBtn.getStyleClass().add("btn-primary");
        refreshBtn.setOnAction(e -> refreshAsync());

        header.getChildren().addAll(title, spacer, lastRefreshLabel, refreshBtn);
        return header;
    }

    private HBox buildMiddleRow() {
        HBox row = new HBox(16);
        row.setPadding(new Insets(14, 20, 0, 20));
        HBox.setHgrow(buildServicePanel(), Priority.ALWAYS);
        row.getChildren().addAll(buildServicePanel(), buildOutboxPanel());
        return row;
    }

    @SuppressWarnings("unchecked")
    private VBox buildServicePanel() {
        VBox panel = new VBox(8);
        panel.getStyleClass().add("card");
        HBox.setHgrow(panel, Priority.ALWAYS);

        Label heading = new Label("Service Health");
        heading.getStyleClass().add("card-title");

        TableView<ServiceStatus> table = new TableView<>(serviceList);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getStyleClass().add("health-table");

        TableColumn<ServiceStatus, String> nameCol = new TableColumn<>("Service");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        nameCol.setPrefWidth(150);

        TableColumn<ServiceStatus, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        statusCol.setPrefWidth(80);
        statusCol.setCellFactory(col -> new TableCell<ServiceStatus, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                setStyle(switch (item) {
                    case "UP" -> "-fx-text-fill: #27ae60; -fx-font-weight: bold;";
                    case "DOWN" -> "-fx-text-fill: #e74c3c; -fx-font-weight: bold;";
                    default -> "-fx-text-fill: #f39c12; -fx-font-weight: bold;";
                });
            }
        });

        TableColumn<ServiceStatus, String> urlCol = new TableColumn<>("URL");
        urlCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUrl()));

        table.getColumns().addAll(nameCol, statusCol, urlCol);
        panel.getChildren().addAll(heading, table);
        return panel;
    }

    private VBox buildOutboxPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("card");
        panel.setPrefWidth(280);
        panel.setMinWidth(240);

        Label heading = new Label("Outbox Events");
        heading.getStyleClass().add("card-title");

        outboxPending = statLabel("—");
        outboxProcessing = statLabel("—");
        outboxCompleted = statLabel("—");
        outboxFailed = statLabel("—");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);

        addStatRow(grid, 0, "Pending", outboxPending, "#f39c12");
        addStatRow(grid, 1, "Processing", outboxProcessing, "#3498db");
        addStatRow(grid, 2, "Completed", outboxCompleted, "#27ae60");
        addStatRow(grid, 3, "Failed", outboxFailed, "#e74c3c");

        panel.getChildren().addAll(heading, grid);
        return panel;
    }

    private void addStatRow(GridPane grid, int row, String labelText, Label valueLabel, String colour) {
        Label key = new Label(labelText + ":");
        key.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 13px;");
        valueLabel.setStyle("-fx-text-fill: " + colour + "; -fx-font-size: 18px; -fx-font-weight: bold;");
        grid.add(key, 0, row);
        grid.add(valueLabel, 1, row);
    }

    private Label statLabel(String text) {
        Label l = new Label(text);
        l.setMinWidth(60);
        return l;
    }

    @SuppressWarnings("unchecked")
    private VBox buildQueueSection() {
        VBox section = new VBox(8);
        section.getStyleClass().add("card");
        section.setPadding(new Insets(14, 20, 14, 20));
        VBox.setVgrow(section, Priority.ALWAYS);
        VBox.setMargin(section, new Insets(14, 20, 0, 20));

        Label heading = new Label("Queue Depths");
        heading.getStyleClass().add("card-title");

        TableView<QueueStats> table = new TableView<>(queueList);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<QueueStats, String> nameCol = new TableColumn<>("Queue");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        nameCol.setPrefWidth(300);
        nameCol.setCellFactory(col -> new TableCell<QueueStats, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                boolean isDlq = item.endsWith(".dlq");
                setStyle(isDlq ? "-fx-text-fill: #e67e22; -fx-font-weight: bold;" : "");
            }
        });

        TableColumn<QueueStats, String> totalCol = numericCol("Total", q -> String.valueOf(q.getMessages()));
        totalCol.setCellFactory(col -> new TableCell<QueueStats, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                long val = Long.parseLong(item);
                TableRow<QueueStats> row = getTableRow();
                if (row != null && row.getItem() != null && row.getItem().isDlq() && val > 0) {
                    setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                } else {
                    setStyle("");
                }
            }
        });

        TableColumn<QueueStats, String> readyCol = numericCol("Ready", q -> String.valueOf(q.getMessagesReady()));
        TableColumn<QueueStats, String> unackedCol = numericCol("Unacked",
                q -> String.valueOf(q.getMessagesUnacknowledged()));
        TableColumn<QueueStats, String> stateCol = numericCol("State", q -> q.getState() != null ? q.getState() : "?");
        TableColumn<QueueStats, String> typeCol = numericCol("Type",
                q -> q.isDlq() ? "⚠ DLQ" : "Main");

        table.getColumns().addAll(nameCol, totalCol, readyCol, unackedCol, stateCol, typeCol);
        section.getChildren().addAll(heading, table);
        return section;
    }

    private TableColumn<QueueStats, String> numericCol(String title,
            java.util.function.Function<QueueStats, String> fn) {
        TableColumn<QueueStats, String> col = new TableColumn<>(title);
        col.setCellValueFactory(c -> new SimpleStringProperty(fn.apply(c.getValue())));
        col.setPrefWidth(90);
        return col;
    }

    private VBox buildDlqRetryPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("card");
        panel.setPadding(new Insets(14, 20, 14, 20));
        VBox.setMargin(panel, new Insets(14, 20, 0, 20));

        Label heading = new Label("DLQ Retry Controls");
        heading.getStyleClass().add("card-title");

        Label sub = new Label(
                "Drains all messages from the selected DLQ and republishes them to the main exchange for reprocessing.");
        sub.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 12px;");
        sub.setWrapText(true);

        dlqButtonsPane = new FlowPane();
        dlqButtonsPane.setHgap(14);
        dlqButtonsPane.setVgap(10);

        panel.getChildren().addAll(heading, sub, dlqButtonsPane);
        return panel;
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox();
        bar.getStyleClass().add("status-bar");
        bar.setPadding(new Insets(6, 20, 6, 20));

        statusBarLabel = new Label("Ready");
        statusBarLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px;");
        bar.getChildren().add(statusBarLabel);
        return bar;
    }

    public void startAutoRefresh() {
        refreshAsync();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "dashboard-refresh");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::refreshAsync,
                AUTO_REFRESH_SECONDS, AUTO_REFRESH_SECONDS, TimeUnit.SECONDS);

        stage.setOnCloseRequest(e -> scheduler.shutdownNow());
    }

    private void refreshAsync() {
        Thread.ofVirtual().start(() -> {
            try {
                DashboardSnapshot snap = client.fetchSnapshot();
                Platform.runLater(() -> applySnapshot(snap));
            } catch (Exception ex) {
                Platform.runLater(() -> setStatus("⚠  Refresh failed: " + ex.getMessage(), true));
            }
        });
    }

    private void applySnapshot(DashboardSnapshot snap) {
        if (snap.getServices() != null) {
            serviceList.setAll(snap.getServices());
        }

        if (snap.getKnownDlqNames() != null && dlqButtonsPane.getChildren().size() != snap.getKnownDlqNames().size()) {
            dlqButtonsPane.getChildren().clear();
            dlqStatusLabels.clear();
            for (String dlqName : snap.getKnownDlqNames()) {
                VBox card = new VBox(6);
                card.getStyleClass().add("dlq-card");
                card.setPadding(new Insets(10, 14, 10, 14));
                card.setMinWidth(230);

                Label nameLabel = new Label(dlqName);
                nameLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-font-size: 12px;");
                nameLabel.setWrapText(true);

                Label statusLabel = new Label("—");
                statusLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px;");
                statusLabel.setWrapText(true);
                dlqStatusLabels.put(dlqName, statusLabel);

                Button retryBtn = new Button("⟳  Retry All");
                retryBtn.getStyleClass().add("btn-warning");
                retryBtn.setMaxWidth(Double.MAX_VALUE);
                retryBtn.setOnAction(e -> retryDlqAsync(dlqName, statusLabel, retryBtn));

                card.getChildren().addAll(nameLabel, retryBtn, statusLabel);
                dlqButtonsPane.getChildren().add(card);
            }
        }

        if (snap.getQueues() != null) {
            List<QueueStats> sorted = snap.getQueues().stream()
                    .sorted((a, b) -> {
                        if (a.isDlq() != b.isDlq())
                            return a.isDlq() ? -1 : 1;
                        return a.getName().compareTo(b.getName());
                    }).toList();
            queueList.setAll(sorted);
        }

        OutboxStats ob = snap.getOutbox();
        if (ob != null) {
            outboxPending.setText(ob.getPending() < 0 ? "N/A" : String.valueOf(ob.getPending()));
            outboxProcessing.setText(ob.getProcessing() < 0 ? "N/A" : String.valueOf(ob.getProcessing()));
            outboxCompleted.setText(ob.getCompleted() < 0 ? "N/A" : String.valueOf(ob.getCompleted()));
            outboxFailed.setText(ob.getFailed() < 0 ? "N/A" : String.valueOf(ob.getFailed()));

            outboxFailed.setStyle(ob.getFailed() > 0
                    ? "-fx-text-fill: #e74c3c; -fx-font-size: 18px; -fx-font-weight: bold;"
                    : "-fx-text-fill: #27ae60; -fx-font-size: 18px; -fx-font-weight: bold;");
        }

        lastRefreshLabel.setText("Last refresh: " + snap.getSnapshotTime());
        setStatus("✓  Data refreshed at " + snap.getSnapshotTime(), false);
    }

    private void retryDlqAsync(String dlqName, Label statusLabel, Button btn) {
        btn.setDisable(true);
        statusLabel.setText("Retrying…");
        statusLabel.setStyle("-fx-text-fill: #3498db; -fx-font-size: 11px;");

        Thread.ofVirtual().start(() -> {
            try {
                var result = client.retryDlq(dlqName);
                Platform.runLater(() -> {
                    statusLabel.setText("✓  " + result.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 11px;");
                    btn.setDisable(false);
                    refreshAsync();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    statusLabel.setText("⚠  " + ex.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
                    btn.setDisable(false);
                });
            }
        });
    }

    private void setStatus(String text, boolean isError) {
        statusBarLabel.setText(text);
        statusBarLabel.setStyle(isError
                ? "-fx-text-fill: #e74c3c; -fx-font-size: 11px;"
                : "-fx-text-fill: #95a5a6; -fx-font-size: 11px;");
    }
}
