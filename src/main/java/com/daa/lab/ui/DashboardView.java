package com.daa.lab.ui;

import com.daa.lab.MainApp;
import com.daa.lab.modules.queens.QueensView;
import com.daa.lab.modules.hamiltonian.HamiltonianView;
import com.daa.lab.modules.tsp.TspView;
import com.daa.lab.modules.knapsack.KnapsackView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Font;

/**
 * Dashboard / home view.
 *
 * <p>Displays four module cards arranged in a 2×2 grid. Navigation to individual
 * modules is wired up by the respective module workers; for now each "Explore →"
 * button simply prints a placeholder message.</p>
 *
 * <p>This class intentionally does <em>not</em> import any module class.</p>
 */
public class DashboardView extends VBox {

    @SuppressWarnings("this-escape")
    public DashboardView() {
        super(0);
        setStyle("-fx-background-color: " + StyleConstants.BG_PRIMARY + ";");
        setPadding(new Insets(StyleConstants.PADDING));

        GridPane grid = buildGrid();
        getChildren().addAll(buildTopBar(), grid, buildFooter());
        VBox.setVgrow(grid, Priority.ALWAYS);
    }

    // ── Top bar ───────────────────────────────────────────────────────────────

    private VBox buildTopBar() {
        Label title = new Label("Robotics Path Planning");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setStyle("-fx-text-fill: " + StyleConstants.TEXT_PRIMARY + ";");

        Label subtitle = new Label(
                "Algorithmic Optimization Lab \u2013 Unit IV: Backtracking & Branch and Bound");
        subtitle.setFont(Font.font("System", StyleConstants.FONT_BODY));
        subtitle.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + ";");

        VBox bar = new VBox(4, title, subtitle);
        bar.setPadding(new Insets(0, 0, StyleConstants.GAP, 0));
        return bar;
    }

    // ── 2×2 card grid ─────────────────────────────────────────────────────────

    private GridPane buildGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(StyleConstants.GAP);
        grid.setVgap(StyleConstants.GAP);
        grid.setPadding(new Insets(StyleConstants.GAP / 2, 0, StyleConstants.GAP, 0));

        // Equal-width columns
        for (int i = 0; i < 2; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }
        // Equal-height rows
        for (int i = 0; i < 2; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(50);
            rc.setVgrow(Priority.ALWAYS);
            grid.getRowConstraints().add(rc);
        }

        grid.add(makeCard("\uD83D\uDC51", "8-Queens",
                "Robot Collision-Free Motion Planning", "Backtracking",
                StyleConstants.ACCENT_BLUE, "8-Queens"), 0, 0);

        grid.add(makeCard("\uD83D\uDD0C", "Hamiltonian Cycle",
                "PCB Circuit Routing", "Backtracking",
                StyleConstants.ACCENT_GREEN, "Hamiltonian Cycle"), 1, 0);

        grid.add(makeCard("\u2708\uFE0F", "TSP \u2013 Air Cargo",
                "Optimal Route Planning", "Branch & Bound",
                StyleConstants.ACCENT_AMBER, "TSP"), 0, 1);

        grid.add(makeCard("\uD83D\uDCBC", "Knapsack",
                "Investment Portfolio Optimization", "Branch & Bound",
                StyleConstants.ACCENT_RED, "Knapsack"), 1, 1);

        VBox.setVgrow(grid, Priority.ALWAYS);
        return grid;
    }

    /**
     * Builds a single module card.
     *
     * @param icon        emoji icon character
     * @param title       module name
     * @param description short real-world description
     * @param tag         algorithm category label
     * @param accentColor hex colour for the tag badge
     * @param moduleName  used only in the placeholder println
     */
    private VBox makeCard(String icon, String title, String description,
                          String tag, String accentColor, String moduleName) {

        // Icon
        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font(36));

        // Title
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, StyleConstants.FONT_SUBHEADING));
        titleLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_PRIMARY + ";");

        // Description
        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("System", StyleConstants.FONT_BODY));
        descLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + ";");
        descLabel.setWrapText(true);

        // Tag badge
        Label tagLabel = new Label(tag);
        tagLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        tagLabel.setStyle(
                "-fx-text-fill: white;"
                + "-fx-background-color: " + accentColor + ";"
                + "-fx-background-radius: 6;"
                + "-fx-padding: 2 8 2 8;");

        // Explore button
        Button exploreBtn = new Button("Explore \u2192");
        exploreBtn.setStyle(
                "-fx-background-color: " + accentColor + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 8;"
                + "-fx-cursor: hand;"
                + "-fx-padding: 8 20 8 20;");
        exploreBtn.setMaxWidth(Double.MAX_VALUE);
        exploreBtn.setOnAction(e -> {
            switch (moduleName) {
                case "8-Queens"       -> MainApp.navigateTo(new QueensView());
                case "Hamiltonian Cycle" -> MainApp.navigateTo(new HamiltonianView());
                case "TSP"            -> MainApp.navigateTo(new TspView());
                case "Knapsack"       -> MainApp.navigateTo(new KnapsackView());
                default               -> System.out.println("navigate to " + moduleName);
            }
        });

        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox card = new VBox(10, iconLabel, titleLabel, descLabel, tagLabel, spacer, exploreBtn);
        card.setPadding(new Insets(StyleConstants.PADDING));
        card.setMinSize(240, 200);
        card.setStyle(
                "-fx-background-color: " + StyleConstants.BG_CARD + ";"
                + "-fx-background-radius: " + StyleConstants.RADIUS + ";"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 8, 0, 0, 2);");

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: " + StyleConstants.BG_CARD + ";"
                + "-fx-background-radius: " + StyleConstants.RADIUS + ";"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.20), 14, 0, 0, 4);"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: " + StyleConstants.BG_CARD + ";"
                + "-fx-background-radius: " + StyleConstants.RADIUS + ";"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 8, 0, 0, 2);"));

        return card;
    }

    // ── Footer ────────────────────────────────────────────────────────────────

    private HBox buildFooter() {
        Label footer = new Label("Unit IV  |  Backtracking & Branch and Bound  |  Academic Demo");
        footer.setFont(Font.font("System", StyleConstants.FONT_BODY));
        footer.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + ";");

        HBox box = new HBox(footer);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(StyleConstants.GAP, 0, 4, 0));
        return box;
    }
}
