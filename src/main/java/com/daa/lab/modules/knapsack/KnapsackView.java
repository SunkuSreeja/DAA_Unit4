package com.daa.lab.modules.knapsack;

import com.daa.lab.ui.BaseModuleView;
import com.daa.lab.ui.StyleConstants;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import javafx.util.converter.IntegerStringConverter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JavaFX view for the Knapsack Branch-and-Bound module.
 *
 * <p>
 * Displays an editable project table on the left, an "Optimize Portfolio"
 * button, step-through controls, and on the right a portfolio result panel
 * plus an animated B&amp;B tree canvas.
 * </p>
 */
public class KnapsackView extends BaseModuleView {

    // ── Algorithm ──────────────────────────────────────────────────────────────
    private final KnapsackAlgorithm algorithm = new KnapsackAlgorithm();

    // ── Observable table data ──────────────────────────────────────────────────
    private final ObservableList<KnapsackAlgorithm.Item> tableItems = FXCollections.observableArrayList();

    // ── UI refs kept for reset / update ───────────────────────────────────────
    private Slider budgetSlider;
    private Label budgetLabel;
    private TableView<KnapsackAlgorithm.Item> projectTable;

    // Result panel
    private VBox selectedBox;
    private VBox rejectedBox;
    private Label lblTotalInvestment;
    private Label lblExpectedProfit;
    private Label lblRemainingBudget;
    private Label lblROI;
    private ProgressBar budgetBar;

    // Tree canvas
    private Canvas treeCanvas;

    // Step-through state
    private List<KnapsackAlgorithm.KnapsackNode> allNodes = new ArrayList<>();
    private int currentStep = -1; // -1 = not started
    private Timeline playTimeline;
    private Button btnPlay;
    private Button btnPause;
    private Button btnNext;
    private Button btnPrev;
    private Slider speedSlider;

    // Stats labels
    private Label lblNodesExplored;
    private Label lblNodesPruned;

    // ── Constructor ────────────────────────────────────────────────────────────

    @SuppressWarnings("this-escape")
    public KnapsackView() {
        // Populate table data before buildContent() reads tableItems.
        tableItems.addAll(algorithm.defaultItems());
        // All fields ready – build the UI now.
        initializeView("💼 Knapsack – Investment Portfolio Optimization");
    }

    // ── BaseModuleView contract ────────────────────────────────────────────────

    @Override
    protected Node buildContent() {
        // Two-column split: left=controls, right=results+tree
        HBox root = new HBox(StyleConstants.GAP);
        root.setPadding(new Insets(StyleConstants.PADDING));
        root.setStyle("-fx-background-color: " + StyleConstants.BG_PRIMARY + ";");

        VBox leftPanel = buildLeftPanel();
        VBox rightPanel = buildRightPanel();

        HBox.setHgrow(leftPanel, Priority.NEVER);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        leftPanel.setMinWidth(360);
        leftPanel.setMaxWidth(400);

        root.getChildren().addAll(leftPanel, rightPanel);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    @Override
    protected void reset() {
        // Clear results
        clearResults();

        // Reset table to defaults
        tableItems.setAll(algorithm.defaultItems());

        // Reset budget slider
        budgetSlider.setValue(algorithm.getDefaultBudget());

        // Reset step state
        allNodes.clear();
        currentStep = -1;
        stopPlayback();
        updateStepButtons();

        // Clear canvas
        clearCanvas();
    }

    // ── Left panel ─────────────────────────────────────────────────────────────

    private VBox buildLeftPanel() {
        VBox panel = new VBox(StyleConstants.GAP);
        panel.setPadding(new Insets(0));

        // 1. Algorithm info box
        VBox infoBox = algorithmInfoBox(
                "A venture capital firm has ₹2000 crores to invest. Each company " +
                        "project has a cost and expected profit. Find the best portfolio " +
                        "that maximizes profit without exceeding the budget.",
                "1. Sort projects by profit/cost ratio (best value first)\n" +
                        "2. Compute upper bound: take items greedily (fractional)\n" +
                        "3. If upper bound ≤ best profit so far, prune\n" +
                        "4. Otherwise branch: include this item OR exclude it\n" +
                        "5. Recurse for next item\n" +
                        "6. Update best when all items considered",
                "Time: O(2^N) worst case | Space: O(N×2^N)");

        // 2. Budget slider
        VBox budgetSection = buildBudgetSection();

        // 3. Project table with CRUD buttons
        VBox tableSection = buildTableSection();

        // 4. Optimize button
        Button btnOptimize = new Button("⚡  Optimize Portfolio");
        btnOptimize.setMaxWidth(Double.MAX_VALUE);
        btnOptimize.setStyle(
                "-fx-background-color: " + StyleConstants.ACCENT_BLUE + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 15px;" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 12 20 12 20;");
        btnOptimize.setOnAction(e -> runOptimization());

        // 5. Step controls
        VBox stepSection = buildStepSection();

        // Stats row
        lblNodesExplored = new Label("Nodes explored: –");
        lblNodesPruned = new Label("Nodes pruned: –");
        lblNodesExplored.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + "; -fx-font-size: 12px;");
        lblNodesPruned.setStyle("-fx-text-fill: " + StyleConstants.ACCENT_RED + "; -fx-font-size: 12px;");
        HBox statsRow = new HBox(16, lblNodesExplored, lblNodesPruned);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        panel.getChildren().addAll(
                infoBox, budgetSection, tableSection,
                btnOptimize, stepSection, statsRow);
        return panel;
    }

    private VBox buildBudgetSection() {
        budgetLabel = new Label("Budget: ₹2000 Cr");
        budgetLabel.setFont(Font.font("System", FontWeight.BOLD, StyleConstants.FONT_BODY));
        budgetLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_PRIMARY + ";");

        budgetSlider = new Slider(500, 5000, algorithm.getDefaultBudget());
        budgetSlider.setShowTickLabels(true);
        budgetSlider.setShowTickMarks(true);
        budgetSlider.setMajorTickUnit(1000);
        budgetSlider.setMinorTickCount(3);
        budgetSlider.setBlockIncrement(100);
        budgetSlider.valueProperty()
                .addListener((obs, o, n) -> budgetLabel.setText("Budget: ₹" + n.intValue() + " Cr"));
        budgetSlider.setMaxWidth(Double.MAX_VALUE);

        VBox box = card();
        box.getChildren().addAll(sectionLabel("💰 Investment Budget"), budgetLabel, budgetSlider);
        return box;
    }

    @SuppressWarnings("unchecked")
    private VBox buildTableSection() {
        // Build editable TableView
        projectTable = new TableView<>(tableItems);
        projectTable.setEditable(true);
        projectTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        projectTable.setPrefHeight(220);

        TableColumn<KnapsackAlgorithm.Item, String> colName = new TableColumn<>("Project Name");
        TableColumn<KnapsackAlgorithm.Item, Integer> colCost = new TableColumn<>("Cost (₹Cr)");
        TableColumn<KnapsackAlgorithm.Item, Integer> colProfit = new TableColumn<>("Profit (₹Cr)");
        TableColumn<KnapsackAlgorithm.Item, Double> colRatio = new TableColumn<>("P/C Ratio");

        colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name));
        colCost.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().cost).asObject());
        colProfit.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().profit).asObject());
        colRatio.setCellValueFactory(cd -> new SimpleDoubleProperty(
                Math.round(cd.getValue().ratio * 100.0) / 100.0).asObject());

        // Make Name editable
        colName.setCellFactory(TextFieldTableCell.forTableColumn());
        colName.setOnEditCommit(e -> replaceItem(e.getRowValue(),
                e.getNewValue(), e.getRowValue().cost, e.getRowValue().profit));

        // Make Cost editable
        colCost.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colCost.setOnEditCommit(e -> replaceItem(e.getRowValue(),
                e.getRowValue().name, e.getNewValue(), e.getRowValue().profit));

        // Make Profit editable
        colProfit.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colProfit.setOnEditCommit(e -> replaceItem(e.getRowValue(),
                e.getRowValue().name, e.getRowValue().cost, e.getNewValue()));

        // Ratio column – read-only, formatted
        colRatio.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : String.format("%.2f", item));
            }
        });

        projectTable.getColumns().addAll(colName, colCost, colProfit, colRatio);

        // CRUD buttons
        Button btnAdd = tableButton("+ Add Row", StyleConstants.ACCENT_GREEN);
        Button btnRemove = tableButton("− Remove Row", StyleConstants.ACCENT_RED);
        Button btnReset = tableButton("↺ Reset to Default", StyleConstants.ACCENT_AMBER);

        btnAdd.setOnAction(e -> tableItems.add(new KnapsackAlgorithm.Item("New Project", 100, 150)));
        btnRemove.setOnAction(e -> {
            int idx = projectTable.getSelectionModel().getSelectedIndex();
            if (idx >= 0)
                tableItems.remove(idx);
        });
        btnReset.setOnAction(e -> tableItems.setAll(algorithm.defaultItems()));

        HBox crudRow = new HBox(8, btnAdd, btnRemove, btnReset);
        crudRow.setAlignment(Pos.CENTER_LEFT);

        VBox box = card();
        box.getChildren().addAll(sectionLabel("📋 Project Investments"), projectTable, crudRow);
        return box;
    }

    private VBox buildStepSection() {
        btnPrev = iconButton("⏮ Prev");
        btnNext = iconButton("⏭ Next");
        btnPlay = iconButton("▶ Play");
        btnPause = iconButton("⏸ Pause");
        btnPause.setDisable(true);

        speedSlider = new Slider(1, 10, 5);
        speedSlider.setShowTickLabels(true);
        speedSlider.setMajorTickUnit(3);
        speedSlider.setPrefWidth(100);
        Label speedLbl = new Label("Speed");
        speedLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + StyleConstants.TEXT_SECONDARY + ";");

        btnPrev.setOnAction(e -> stepBackward());
        btnNext.setOnAction(e -> stepForward());
        btnPlay.setOnAction(e -> startPlayback());
        btnPause.setOnAction(e -> stopPlayback());

        HBox row = new HBox(8, btnPrev, btnNext, btnPlay, btnPause, speedLbl, speedSlider);
        row.setAlignment(Pos.CENTER_LEFT);

        updateStepButtons();

        VBox box = card();
        box.getChildren().addAll(sectionLabel("🎮 Step Through B&B Tree"), row);
        return box;
    }

    // ── Right panel ────────────────────────────────────────────────────────────

    private VBox buildRightPanel() {
        VBox panel = new VBox(StyleConstants.GAP);
        VBox.setVgrow(panel, Priority.ALWAYS);

        // Portfolio results card
        VBox resultsCard = buildResultsCard();

        // B&B Tree visualisation card
        VBox treeCard = buildTreeCard();

        VBox.setVgrow(resultsCard, Priority.SOMETIMES);
        VBox.setVgrow(treeCard, Priority.ALWAYS);

        panel.getChildren().addAll(resultsCard, treeCard);
        return panel;
    }

    private VBox buildResultsCard() {
        // Selected projects (green ✓)
        selectedBox = new VBox(4);
        selectedBox.setPadding(new Insets(8));
        selectedBox.setStyle("-fx-background-color: #F0FDF4; -fx-background-radius: 8;");

        // Rejected projects (red ✗)
        rejectedBox = new VBox(4);
        rejectedBox.setPadding(new Insets(8));
        rejectedBox.setStyle("-fx-background-color: #FEF2F2; -fx-background-radius: 8;");

        // Summary labels
        lblTotalInvestment = summaryLabel("Total Investment:  –");
        lblExpectedProfit = summaryLabel("Expected Profit:   –");
        lblRemainingBudget = summaryLabel("Remaining Budget: –");
        lblROI = summaryLabel("ROI:              –");

        budgetBar = new ProgressBar(0);
        budgetBar.setMaxWidth(Double.MAX_VALUE);
        budgetBar.setStyle("-fx-accent: " + StyleConstants.ACCENT_BLUE + ";");

        GridPane summaryGrid = new GridPane();
        summaryGrid.setHgap(16);
        summaryGrid.setVgap(4);
        summaryGrid.addRow(0, lblTotalInvestment, lblExpectedProfit);
        summaryGrid.addRow(1, lblRemainingBudget, lblROI);

        Label selHeader = boldLabel("✅ Selected Projects");
        selHeader.setStyle("-fx-text-fill: " + StyleConstants.ACCENT_GREEN + "; -fx-font-weight: bold;");
        Label rejHeader = boldLabel("❌ Rejected Projects");
        rejHeader.setStyle("-fx-text-fill: " + StyleConstants.ACCENT_RED + "; -fx-font-weight: bold;");

        Label barLbl = new Label("Budget Utilisation");
        barLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + StyleConstants.TEXT_SECONDARY + ";");

        VBox card = card();
        card.getChildren().addAll(
                sectionLabel("📊 Portfolio Results"),
                selHeader, selectedBox,
                rejHeader, rejectedBox,
                summaryGrid,
                barLbl, budgetBar);
        return card;
    }

    private VBox buildTreeCard() {
        treeCanvas = new Canvas(420, 300);
        clearCanvas();

        ScrollPane canvasScroll = new ScrollPane(treeCanvas);
        canvasScroll.setStyle("-fx-background-color: #1E1E2E; -fx-background: #1E1E2E;");
        canvasScroll.setFitToWidth(false);
        canvasScroll.setMinHeight(310);
        VBox.setVgrow(canvasScroll, Priority.ALWAYS);

        VBox card = card();
        VBox.setVgrow(card, Priority.ALWAYS);
        card.getChildren().addAll(sectionLabel("🌲 Branch & Bound Tree"), canvasScroll);
        return card;
    }

    // ── Optimization logic ─────────────────────────────────────────────────────

    private void runOptimization() {
        stopPlayback();
        clearResults();

        List<KnapsackAlgorithm.Item> items = new ArrayList<>(tableItems);
        int budget = (int) budgetSlider.getValue();

        algorithm.solve(items, budget);

        allNodes = new ArrayList<>(algorithm.getAllNodes());
        currentStep = -1;

        // Show full results immediately
        populateResults(budget);

        // Update stats
        lblNodesExplored.setText("Nodes explored: " + algorithm.getNodesExplored());
        lblNodesPruned.setText("Nodes pruned: " + algorithm.getNodesPruned());

        // Draw the complete tree
        redrawTree(allNodes.size());
        updateStepButtons();
    }

    private void populateResults(int budget) {
        List<KnapsackAlgorithm.Item> selected = algorithm.getBestSelection();
        Set<String> selectedNames = new HashSet<>();
        for (KnapsackAlgorithm.Item it : selected)
            selectedNames.add(it.name);

        selectedBox.getChildren().clear();
        rejectedBox.getChildren().clear();

        for (KnapsackAlgorithm.Item it : tableItems) {
            if (selectedNames.contains(it.name)) {
                selectedBox.getChildren().add(projectRow(
                        "✔ " + it.name, it.cost, it.profit, StyleConstants.ACCENT_GREEN));
            } else {
                rejectedBox.getChildren().add(projectRow(
                        "✘ " + it.name, it.cost, it.profit, StyleConstants.ACCENT_RED));
            }
        }

        int totalCost = algorithm.getTotalCost();
        int totalProfit = algorithm.getBestProfit();
        int remaining = budget - totalCost;
        double roi = totalCost > 0 ? (double) (totalProfit - totalCost) / totalCost * 100 : 0;

        lblTotalInvestment.setText(String.format("Total Investment: ₹%,d Cr", totalCost));
        lblExpectedProfit.setText(String.format("Expected Profit:  ₹%,d Cr", totalProfit));
        lblRemainingBudget.setText(String.format("Remaining Budget: ₹%,d Cr", remaining));
        lblROI.setText(String.format("ROI: %.1f%%", roi));

        budgetBar.setProgress(budget > 0 ? (double) totalCost / budget : 0);
    }

    private HBox projectRow(String name, int cost, int profit, String color) {
        Label nameLbl = new Label(name);
        nameLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        nameLbl.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nameLbl, Priority.ALWAYS);

        Label detailLbl = new Label(String.format("₹%d Cr / +₹%d Cr", cost, profit));
        detailLbl.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + "; -fx-font-size: 11px;");

        HBox row = new HBox(8, nameLbl, detailLbl);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ── Step-through controls ──────────────────────────────────────────────────

    private void stepForward() {
        if (allNodes.isEmpty())
            return;
        currentStep = Math.min(currentStep + 1, allNodes.size() - 1);
        redrawTree(currentStep + 1);
        updateStepButtons();
    }

    private void stepBackward() {
        if (allNodes.isEmpty())
            return;
        currentStep = Math.max(currentStep - 1, 0);
        redrawTree(currentStep + 1);
        updateStepButtons();
    }

    private void startPlayback() {
        if (allNodes.isEmpty())
            return;
        btnPlay.setDisable(true);
        btnPause.setDisable(false);

        double intervalMs = 1100 - speedSlider.getValue() * 100; // 100ms–1000ms
        playTimeline = new Timeline(new KeyFrame(Duration.millis(intervalMs), e -> {
            if (currentStep < allNodes.size() - 1) {
                stepForward();
            } else {
                stopPlayback();
            }
        }));
        playTimeline.setCycleCount(Animation.INDEFINITE);
        playTimeline.play();
    }

    private void stopPlayback() {
        if (playTimeline != null) {
            playTimeline.stop();
            playTimeline = null;
        }
        if (btnPlay != null)
            btnPlay.setDisable(false);
        if (btnPause != null)
            btnPause.setDisable(true);
    }

    private void updateStepButtons() {
        boolean hasNodes = !allNodes.isEmpty();
        if (btnPrev != null)
            btnPrev.setDisable(!hasNodes || currentStep <= 0);
        if (btnNext != null)
            btnNext.setDisable(!hasNodes || currentStep >= allNodes.size() - 1);
        if (btnPlay != null)
            btnPlay.setDisable(!hasNodes || currentStep >= allNodes.size() - 1);
    }

    // ── Tree canvas drawing ────────────────────────────────────────────────────

    /**
     * Draws the first {@code count} nodes of the B&amp;B tree onto the canvas.
     *
     * <p>
     * Layout: nodes are arranged in rows by level (BFS-like). Each node is
     * drawn as a small rounded rectangle coloured by its state:
     * </p>
     * <ul>
     * <li><b>Gold</b> – the best-solution leaf</li>
     * <li><b>Red</b> – pruned node</li>
     * <li><b>Green</b> – explored and kept</li>
     * </ul>
     * Lines connect each child to its parent.
     */
    private void redrawTree(int count) {
        if (allNodes.isEmpty()) {
            clearCanvas();
            return;
        }

        GraphicsContext gc = treeCanvas.getGraphicsContext2D();
        double W = treeCanvas.getWidth();
        double H = treeCanvas.getHeight();

        gc.clearRect(0, 0, W, H);
        gc.setFill(Color.web("#1E1E2E"));
        gc.fillRect(0, 0, W, H);

        // We layout nodes level by level.
        // Collect nodes-per-level to compute horizontal spacing.
        int maxLevel = 0;
        for (int i = 0; i < count; i++) {
            maxLevel = Math.max(maxLevel, allNodes.get(i).level);
        }
        int levels = maxLevel + 1;

        // Count nodes per level
        int[] perLevel = new int[levels + 1];
        for (int i = 0; i < count; i++) {
            int lv = allNodes.get(i).level;
            if (lv < perLevel.length)
                perLevel[lv]++;
        }

        // Assign x,y positions
        double rowH = Math.min(60, (H - 20) / Math.max(levels, 1));
        double nodeW = 72;
        double nodeH = 40;
        double[] nodeXArr = new double[count];
        double[] nodeYArr = new double[count];
        int[] levelIdx = new int[levels + 1];

        for (int i = 0; i < count; i++) {
            int lv = allNodes.get(i).level;
            int inLevel = levelIdx[lv];
            int total = perLevel[lv];
            double slotW = W / Math.max(total, 1);
            nodeXArr[i] = slotW * inLevel + slotW / 2 - nodeW / 2;
            nodeYArr[i] = 10 + lv * rowH;
            levelIdx[lv]++;
        }

        // Resize canvas height if needed
        double neededH = 10 + levels * rowH + nodeH + 10;
        if (neededH > treeCanvas.getHeight()) {
            treeCanvas.setHeight(neededH);
            gc = treeCanvas.getGraphicsContext2D();
            gc.setFill(Color.web("#1E1E2E"));
            gc.fillRect(0, 0, treeCanvas.getWidth(), treeCanvas.getHeight());
        }

        // Draw edges first (parent = previous node at level-1 nearest x)
        gc.setStroke(Color.web("#4B5563"));
        gc.setLineWidth(1);
        for (int i = 1; i < count; i++) {
            int lv = allNodes.get(i).level;
            if (lv == 0)
                continue;
            // Find parent: last node drawn at level lv-1 whose index < i
            int parentIdx = -1;
            for (int j = i - 1; j >= 0; j--) {
                if (allNodes.get(j).level == lv - 1) {
                    parentIdx = j;
                    break;
                }
            }
            if (parentIdx >= 0) {
                gc.strokeLine(
                        nodeXArr[parentIdx] + nodeW / 2,
                        nodeYArr[parentIdx] + nodeH,
                        nodeXArr[i] + nodeW / 2,
                        nodeYArr[i]);
            }
        }

        // Draw nodes
        for (int i = 0; i < count; i++) {
            KnapsackAlgorithm.KnapsackNode node = allNodes.get(i);
            double x = nodeXArr[i];
            double y = nodeYArr[i];

            // Fill colour
            Color fillColor;
            if (node.best)
                fillColor = Color.web("#D97706"); // gold
            else if (node.pruned)
                fillColor = Color.web("#991B1B"); // dark red
            else
                fillColor = Color.web("#166534"); // dark green

            gc.setFill(fillColor);
            gc.fillRoundRect(x, y, nodeW, nodeH, 8, 8);

            // Border: highlight current step
            if (i == currentStep) {
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(2);
            } else {
                gc.setStroke(fillColor.darker());
                gc.setLineWidth(1);
            }
            gc.strokeRoundRect(x, y, nodeW, nodeH, 8, 8);

            // Text (white)
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("System", 9));
            String line1 = "L" + node.level + " W:" + node.weight;
            String line2 = "P:" + node.profit;
            String line3 = String.format("UB:%.0f", node.upperBound);
            gc.fillText(line1, x + 4, y + 13);
            gc.fillText(line2, x + 4, y + 25);
            gc.fillText(line3, x + 4, y + 37);
        }

        // Legend
        drawLegend(gc, W);
    }

    private void drawLegend(GraphicsContext gc, double W) {
        gc.setFont(Font.font("System", 10));
        double lx = W - 150, ly = 8;
        // Green
        gc.setFill(Color.web("#166534"));
        gc.fillRoundRect(lx, ly, 12, 10, 3, 3);
        gc.setFill(Color.WHITE);
        gc.fillText("Explored", lx + 15, ly + 10);
        ly += 16;
        // Red
        gc.setFill(Color.web("#991B1B"));
        gc.fillRoundRect(lx, ly, 12, 10, 3, 3);
        gc.setFill(Color.WHITE);
        gc.fillText("Pruned", lx + 15, ly + 10);
        ly += 16;
        // Gold
        gc.setFill(Color.web("#D97706"));
        gc.fillRoundRect(lx, ly, 12, 10, 3, 3);
        gc.setFill(Color.WHITE);
        gc.fillText("Best", lx + 15, ly + 10);
    }

    private void clearCanvas() {
        GraphicsContext gc = treeCanvas.getGraphicsContext2D();
        gc.setFill(Color.web("#1E1E2E"));
        gc.fillRect(0, 0, treeCanvas.getWidth(), treeCanvas.getHeight());
        gc.setFill(Color.web("#4B5563"));
        gc.setFont(Font.font("System", 13));
        gc.fillText("Run optimization to see the B&B tree", 60, 155);
    }

    // ── Helper: replace immutable Item in list ─────────────────────────────────

    private void replaceItem(KnapsackAlgorithm.Item old, String name, int cost, int profit) {
        int idx = tableItems.indexOf(old);
        if (idx >= 0) {
            tableItems.set(idx, new KnapsackAlgorithm.Item(name, cost, profit));
        }
    }

    private void clearResults() {
        if (selectedBox != null)
            selectedBox.getChildren().clear();
        if (rejectedBox != null)
            rejectedBox.getChildren().clear();
        if (lblTotalInvestment != null)
            lblTotalInvestment.setText("Total Investment: –");
        if (lblExpectedProfit != null)
            lblExpectedProfit.setText("Expected Profit: –");
        if (lblRemainingBudget != null)
            lblRemainingBudget.setText("Remaining Budget: –");
        if (lblROI != null)
            lblROI.setText("ROI: –");
        if (budgetBar != null)
            budgetBar.setProgress(0);
        if (lblNodesExplored != null)
            lblNodesExplored.setText("Nodes explored: –");
        if (lblNodesPruned != null)
            lblNodesPruned.setText("Nodes pruned: –");
    }

    // ── Style helpers ──────────────────────────────────────────────────────────

    private VBox card() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(StyleConstants.PADDING));
        box.setStyle(
                "-fx-background-color: " + StyleConstants.BG_CARD + ";" +
                        "-fx-background-radius: " + StyleConstants.RADIUS + ";" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 6, 0, 0, 1);");
        return box;
    }

    private Button tableButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 11px;" +
                        "-fx-background-radius: 6;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 4 10 4 10;");
        return btn;
    }

    private Button iconButton(String text) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: " + StyleConstants.ACCENT_BLUE + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 11px;" +
                        "-fx-background-radius: 6;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 5 10 5 10;");
        return btn;
    }

    private Label summaryLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + StyleConstants.TEXT_PRIMARY + ";");
        return lbl;
    }

    private Label boldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("System", FontWeight.BOLD, StyleConstants.FONT_BODY));
        return lbl;
    }
}
