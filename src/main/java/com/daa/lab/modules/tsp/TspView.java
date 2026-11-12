package com.daa.lab.modules.tsp;

import com.daa.lab.ui.BaseModuleView;
import com.daa.lab.ui.StyleConstants;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * TspView – JavaFX UI for the TSP Branch-and-Bound module ("Air Cargo Routing").
 *
 * <p>Layout:
 * <pre>
 *   ┌─────────────────────────────────────────────────────┐
 *   │  Top bar (from BaseModuleView)                      │
 *   ├──────────────────────┬──────────────────────────────┤
 *   │  LEFT PANEL          │  RIGHT PANEL                 │
 *   │  · Info box          │  · India map canvas          │
 *   │  · Dataset selector  │  · B&B tree canvas (tab)     │
 *   │  · Distance matrix   │                              │
 *   │  · Action buttons    │                              │
 *   │  · Results           │                              │
 *   │  · Step controls     │                              │
 *   └──────────────────────┴──────────────────────────────┘
 * </pre>
 */
public class TspView extends BaseModuleView {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    private static final int    MAP_W  = 480;
    private static final int    MAP_H  = 420;
    private static final int    TREE_W = 480;
    private static final int    TREE_H = 220;
    private static final int    APT_R  = 20;    // airport circle radius
    private static final double DEFAULT_SPEED_MS = 600;

    // Airport positions on the canvas (scaled from approximate lat/lon for India).
    // Order: MUM(0), DEL(1), CHE(2), KOL(3), BAN(4), HYD(5)
    private static final double[] APT_X = { 100, 220, 260, 390, 230, 230 };
    private static final double[] APT_Y = { 230,  60, 340, 160, 310, 240 };

    // Colors (from StyleConstants)
    private static final Color C_BLUE    = Color.web(StyleConstants.ACCENT_BLUE);
    private static final Color C_GREEN   = Color.web(StyleConstants.ACCENT_GREEN);
    private static final Color C_RED     = Color.web(StyleConstants.ACCENT_RED);
    private static final Color C_AMBER   = Color.web(StyleConstants.ACCENT_AMBER);
    private static final Color C_GREY    = Color.web("#9CA3AF");
    private static final Color C_GOLD    = Color.web("#F59E0B");
    private static final Color C_BG      = Color.web(StyleConstants.BG_PRIMARY);
    private static final Color C_CARD    = Color.web(StyleConstants.BG_CARD);
    private static final Color C_TXT_PRI = Color.web(StyleConstants.TEXT_PRIMARY);
    private static final Color C_TXT_SEC = Color.web(StyleConstants.TEXT_SECONDARY);

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private final TspAlgorithm algorithm = new TspAlgorithm();
    private int[][] currentDist;
    private String[] currentNames;
    private String[] currentCodes;

    // Animation
    private List<TspAlgorithm.TspNode> animNodes = new ArrayList<>();
    private int animStep   = 0;
    private boolean playing = false;
    private Timeline timeline;

    // Results
    private int[]  optimalPath;
    private int    optimalCost;
    private int[]  nnPath;
    private int    nnCost;
    private boolean solved = false;

    // -----------------------------------------------------------------------
    // UI controls (kept as fields to update from callbacks)
    // -----------------------------------------------------------------------

    // Distance matrix input grid
    private TextField[][] matrixFields;
    private int matrixN = 6;

    // Right panel canvases
    private Canvas mapCanvas;
    private Canvas treeCanvas;

    // Result labels
    private Label lblBestRoute;
    private Label lblOptCost;
    private Label lblNodesExp;
    private Label lblNodesPruned;
    private Label lblNNCost;
    private Label lblSavings;
    private VBox  resultsBox;

    // Step controls
    private Button btnPlayPause;
    private Button btnNext;
    private Button btnPrev;
    private Slider speedSlider;
    private Label  lblStepInfo;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    @SuppressWarnings("this-escape")
    public TspView() {
        // All fields are initialized. Safe to build UI now.
        initializeView("✈️ TSP – Air Cargo Route Optimization");
    }

    // -----------------------------------------------------------------------
    // BaseModuleView contract
    // -----------------------------------------------------------------------

    @Override
    protected Node buildContent() {
        // Load default airport data
        currentDist  = algorithm.defaultAirports();
        currentNames = TspAlgorithm.AIRPORT_NAMES.clone();
        currentCodes = TspAlgorithm.AIRPORT_CODES.clone();
        matrixN      = currentDist.length;

        HBox root = new HBox(StyleConstants.GAP);
        root.setPadding(new Insets(StyleConstants.PADDING));
        root.setStyle("-fx-background-color: " + StyleConstants.BG_PRIMARY + ";");

        ScrollPane leftScroll = new ScrollPane(buildLeftPanel());
        leftScroll.setFitToWidth(true);
        leftScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        leftScroll.setPrefWidth(430);
        leftScroll.setMinWidth(380);

        Node rightPanel = buildRightPanel();
        ScrollPane rightScroll = new ScrollPane(rightPanel);
        rightScroll.setFitToWidth(true);
        rightScroll.setFitToHeight(false);
        rightScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rightScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        HBox.setHgrow(rightScroll, Priority.ALWAYS);

        root.getChildren().addAll(leftScroll, rightScroll);
        return root;
    }

    @Override
    protected void reset() {
        solved      = false;
        animStep    = 0;
        playing     = false;
        animNodes.clear();
        if (timeline != null) timeline.stop();

        currentDist  = algorithm.defaultAirports();
        currentNames = TspAlgorithm.AIRPORT_NAMES.clone();
        currentCodes = TspAlgorithm.AIRPORT_CODES.clone();
        populateMatrixFields(currentDist);
        clearResults();
        drawMap(null, null, null);
        drawTree(null, -1);
    }

    // -----------------------------------------------------------------------
    // LEFT PANEL
    // -----------------------------------------------------------------------

    private VBox buildLeftPanel() {
        VBox panel = new VBox(StyleConstants.GAP);
        panel.setPadding(new Insets(0, 8, 0, 0));

        // 1. Algorithm info
        panel.getChildren().add(algorithmInfoBox(
            "Air cargo companies must find the cheapest route visiting every airport once " +
            "and returning to base, saving millions in fuel costs.",
            "1. Start at airport 0\n2. Compute lower bound for partial route\n" +
            "3. If bound ≥ best known cost, prune this branch\n" +
            "4. Otherwise expand: try each unvisited airport\n" +
            "5. When all airports visited, update best if cheaper\n" +
            "6. Continue until all branches explored or pruned",
            "Time: O(N² × 2^N) worst case | Space: O(N²)"
        ));

        // 2. Dataset selector
        panel.getChildren().add(buildDatasetSelector());

        // 3. Distance matrix
        panel.getChildren().add(buildMatrixSection());

        // 4. Action buttons
        panel.getChildren().add(buildActionButtons());

        // 5. Results
        resultsBox = buildResultsPanel();
        panel.getChildren().add(resultsBox);

        // 6. Step controls
        panel.getChildren().add(buildStepControls());

        return panel;
    }

    private HBox buildDatasetSelector() {
        Label lbl = new Label("Dataset:");
        lbl.setFont(Font.font("System", FontWeight.BOLD, StyleConstants.FONT_BODY));
        lbl.setStyle("-fx-text-fill: " + StyleConstants.TEXT_PRIMARY + ";");

        ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(
            "Default – 6 Indian Airports", "Custom"
        ));
        combo.setValue("Default – 6 Indian Airports");
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setOnAction(e -> {
            if ("Default – 6 Indian Airports".equals(combo.getValue())) {
                currentDist  = algorithm.defaultAirports();
                currentNames = TspAlgorithm.AIRPORT_NAMES.clone();
                currentCodes = TspAlgorithm.AIRPORT_CODES.clone();
                populateMatrixFields(currentDist);
                clearResults();
                drawMap(null, null, null);
                drawTree(null, -1);
            }
            // "Custom" → user edits fields directly
        });

        HBox row = new HBox(8, lbl, combo);
        HBox.setHgrow(combo, Priority.ALWAYS);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox buildMatrixSection() {
        VBox box = new VBox(6);
        box.getChildren().add(sectionLabel("Distance Matrix (km)"));

        // Airport name row headers
        HBox nameRow = new HBox(4);
        nameRow.getChildren().add(spacer(50));
        for (int j = 0; j < matrixN; j++) {
            Label code = new Label(currentCodes[j]);
            code.setPrefWidth(58);
            code.setAlignment(Pos.CENTER);
            code.setStyle("-fx-font-weight: bold; -fx-font-size: 11; -fx-text-fill: "
                + StyleConstants.ACCENT_BLUE + ";");
            nameRow.getChildren().add(code);
        }
        box.getChildren().add(nameRow);

        matrixFields = new TextField[matrixN][matrixN];
        for (int i = 0; i < matrixN; i++) {
            HBox row = new HBox(4);
            Label rowCode = new Label(currentCodes[i]);
            rowCode.setPrefWidth(50);
            rowCode.setAlignment(Pos.CENTER_RIGHT);
            rowCode.setStyle("-fx-font-weight: bold; -fx-font-size: 11; -fx-text-fill: "
                + StyleConstants.ACCENT_BLUE + ";");
            row.getChildren().add(rowCode);

            for (int j = 0; j < matrixN; j++) {
                TextField tf = new TextField();
                tf.setPrefWidth(58);
                tf.setAlignment(Pos.CENTER);
                tf.setStyle("-fx-font-size: 11;");
                if (i == j) {
                    tf.setText("0");
                    tf.setEditable(false);
                    tf.setStyle("-fx-background-color: #E5E7EB; -fx-font-size: 11;");
                } else {
                    tf.setText(String.valueOf(currentDist[i][j]));
                    // Mirror on edit
                    final int fi = i, fj = j;
                    tf.textProperty().addListener((obs, oldVal, newVal) -> {
                        if (matrixFields != null && matrixFields[fj][fi] != null
                                && !matrixFields[fj][fi].isFocused()) {
                            matrixFields[fj][fi].setText(newVal);
                        }
                    });
                }
                matrixFields[i][j] = tf;
                row.getChildren().add(tf);
            }
            box.getChildren().add(row);
        }

        // Airport full names
        VBox nameBox = new VBox(2);
        nameBox.getChildren().add(sectionLabel("Airports"));
        for (int i = 0; i < matrixN; i++) {
            Label nl = new Label(i + ": " + currentCodes[i] + " – " + currentNames[i]);
            nl.setStyle("-fx-font-size: 11; -fx-text-fill: " + StyleConstants.TEXT_SECONDARY + ";");
            nameBox.getChildren().add(nl);
        }
        box.getChildren().add(nameBox);

        return box;
    }

    private HBox buildActionButtons() {
        Button btnSolve   = styledButton("Find Optimal Route", StyleConstants.ACCENT_BLUE);
        Button btnNN      = styledButton("Compare with Nearest Neighbour", StyleConstants.ACCENT_AMBER);
        Button btnStep    = styledButton("Step Mode", StyleConstants.ACCENT_GREEN);
        Button btnReset   = styledButton("Reset", "#6B7280");

        btnSolve.setOnAction(e -> runSolver(false));
        btnNN.setOnAction(e   -> runNearestNeighbour());
        btnStep.setOnAction(e -> startStepMode());
        btnReset.setOnAction(e -> reset());

        FlowPane fp = new FlowPane(8, 8);
        fp.getChildren().addAll(btnSolve, btnNN, btnStep, btnReset);
        return new HBox(fp);
    }

    private VBox buildResultsPanel() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: " + StyleConstants.BG_CARD + "; "
            + "-fx-background-radius: " + StyleConstants.RADIUS + "; "
            + "-fx-border-color: #E5E7EB; -fx-border-radius: " + StyleConstants.RADIUS + ";");

        box.getChildren().add(sectionLabel("Results"));

        lblBestRoute  = resultLabel("Best Route", "—");
        lblOptCost    = resultLabel("Optimal Cost", "—");
        lblNodesExp   = resultLabel("Nodes Explored", "—");
        lblNodesPruned = resultLabel("Nodes Pruned", "—");
        lblNNCost     = resultLabel("Nearest Neighbour Cost", "—");
        lblSavings    = new Label();
        lblSavings.setStyle("-fx-text-fill: " + StyleConstants.ACCENT_GREEN
            + "; -fx-font-weight: bold; -fx-font-size: 12;");

        box.getChildren().addAll(lblBestRoute, lblOptCost, lblNodesExp,
                                  lblNodesPruned, lblNNCost, lblSavings);
        return box;
    }

    private VBox buildStepControls() {
        VBox box = new VBox(6);
        box.getChildren().add(sectionLabel("Step Controls"));

        btnPlayPause = styledButton("▶ Play", StyleConstants.ACCENT_BLUE);
        btnNext      = styledButton("Next ▶", "#6B7280");
        btnPrev      = styledButton("◀ Prev", "#6B7280");

        btnPlayPause.setOnAction(e -> togglePlay());
        btnNext.setOnAction(e      -> stepForward());
        btnPrev.setOnAction(e      -> stepBack());

        HBox btns = new HBox(8, btnPlayPause, btnNext, btnPrev);
        btns.setAlignment(Pos.CENTER_LEFT);

        speedSlider = new Slider(100, 2000, DEFAULT_SPEED_MS);
        speedSlider.setShowTickLabels(true);
        speedSlider.setMajorTickUnit(500);
        speedSlider.setBlockIncrement(100);
        speedSlider.setMaxWidth(Double.MAX_VALUE);

        lblStepInfo = new Label("Run solver or enable Step Mode first.");
        lblStepInfo.setStyle("-fx-font-size: 11; -fx-text-fill: " + StyleConstants.TEXT_SECONDARY + ";");
        lblStepInfo.setWrapText(true);

        box.getChildren().addAll(btns, new Label("Speed (ms/step):"), speedSlider, lblStepInfo);
        return box;
    }

    // -----------------------------------------------------------------------
    // RIGHT PANEL – map + B&B tree
    // -----------------------------------------------------------------------

    private Node buildRightPanel() {
        mapCanvas  = new Canvas(MAP_W,  MAP_H);
        treeCanvas = new Canvas(TREE_W, TREE_H);

        drawMap(null, null, null);
        drawTree(null, -1);

        // Map card
        VBox mapCard = card("India Airport Map", mapCanvas);

        // Tree card in a tab
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tab treeTab = new Tab("B&B Tree", card("Branch & Bound Tree", treeCanvas));
        tabs.getTabs().add(treeTab);
        tabs.setStyle("-fx-font-size: 12;");

        VBox right = new VBox(StyleConstants.GAP, mapCard, tabs);
        right.setPadding(new Insets(0, 0, 0, 0));
        return right;
    }

    // -----------------------------------------------------------------------
    // DRAWING – MAP CANVAS
    // -----------------------------------------------------------------------

    /**
     * Render the airport map.
     *
     * @param optPath   optimal route indices (null = not solved yet)
     * @param animPath  current animation partial path (null = show full opt route)
     * @param nodeState per-node state for coloring during animation ("explored"/"pruned"/"best")
     */
    private void drawMap(int[] optPath, int[] animPath, String[] nodeState) {
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, MAP_W, MAP_H);

        // Background
        gc.setFill(C_BG);
        gc.fillRoundRect(0, 0, MAP_W, MAP_H, 12, 12);

        // ---- Draw all edges (grey, thin) ----
        gc.setStroke(C_GREY);
        gc.setLineWidth(1);
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        for (int i = 0; i < matrixN; i++) {
            for (int j = i + 1; j < matrixN; j++) {
                double x1 = APT_X[i], y1 = APT_Y[i];
                double x2 = APT_X[j], y2 = APT_Y[j];
                gc.strokeLine(x1, y1, x2, y2);
                // Cost label at midpoint
                double mx = (x1 + x2) / 2, my = (y1 + y2) / 2;
                gc.setFill(C_TXT_SEC);
                gc.setFont(Font.font("System", 9));
                gc.fillText(nf.format(currentDist[i][j]), mx + 2, my - 2);
            }
        }

        // ---- Draw optimal or animation route ----
        if (animPath != null && animPath.length > 1) {
            drawRouteEdges(gc, animPath, C_AMBER, 2.5, false);
        } else if (optPath != null && optPath.length > 1) {
            drawRouteEdges(gc, optPath, C_BLUE, 3.5, true);
        }

        // ---- Draw airport circles ----
        for (int i = 0; i < matrixN; i++) {
            double cx = APT_X[i], cy = APT_Y[i];

            Color fill = C_CARD;
            Color stroke = C_BLUE;
            if (nodeState != null && i < nodeState.length) {
                if ("current".equals(nodeState[i]))  { fill = C_AMBER;  stroke = C_AMBER; }
                if ("pruned".equals(nodeState[i]))   { fill = C_RED;    stroke = C_RED; }
                if ("best".equals(nodeState[i]))     { fill = C_GREEN;  stroke = C_GREEN; }
            }
            if (optPath != null && i == 0)           { fill = C_GOLD;   stroke = C_GOLD; }

            gc.setFill(fill);
            gc.setStroke(stroke);
            gc.setLineWidth(2.5);
            gc.fillOval(cx - APT_R, cy - APT_R, APT_R * 2, APT_R * 2);
            gc.strokeOval(cx - APT_R, cy - APT_R, APT_R * 2, APT_R * 2);

            // Code label inside circle
            gc.setFill(C_TXT_PRI);
            gc.setFont(Font.font("System", FontWeight.BOLD, 10));
            gc.fillText(currentCodes[i < currentCodes.length ? i : 0],
                        cx - 12, cy + 4);

            // Full name below circle
            gc.setFill(C_TXT_SEC);
            gc.setFont(Font.font("System", 10));
            gc.fillText(currentNames[i < currentNames.length ? i : 0],
                        cx - 24, cy + APT_R + 13);
        }
    }

    private void drawRouteEdges(GraphicsContext gc, int[] path, Color color, double width,
                                 boolean drawArrows) {
        gc.setStroke(color);
        gc.setLineWidth(width);
        int len = path.length;
        for (int k = 0; k < len - 1; k++) {
            int a = path[k], b = path[k + 1];
            if (a < 0 || a >= matrixN || b < 0 || b >= matrixN) continue;
            double x1 = APT_X[a], y1 = APT_Y[a];
            double x2 = APT_X[b], y2 = APT_Y[b];
            gc.strokeLine(x1, y1, x2, y2);
            if (drawArrows) drawArrowHead(gc, x1, y1, x2, y2, color);
        }
    }

    private void drawArrowHead(GraphicsContext gc, double x1, double y1,
                                double x2, double y2, Color color) {
        double angle  = Math.atan2(y2 - y1, x2 - x1);
        double arrowLen = 12;
        double arrowAngle = Math.toRadians(25);
        // Pull tip back to edge of circle
        double dist = Math.hypot(x2 - x1, y2 - y1);
        double tx   = x2 - APT_R * (x2 - x1) / dist;
        double ty   = y2 - APT_R * (y2 - y1) / dist;

        double ax1 = tx - arrowLen * Math.cos(angle - arrowAngle);
        double ay1 = ty - arrowLen * Math.sin(angle - arrowAngle);
        double ax2 = tx - arrowLen * Math.cos(angle + arrowAngle);
        double ay2 = ty - arrowLen * Math.sin(angle + arrowAngle);

        gc.setFill(color);
        gc.fillPolygon(new double[]{tx, ax1, ax2}, new double[]{ty, ay1, ay2}, 3);
    }

    // -----------------------------------------------------------------------
    // DRAWING – B&B TREE CANVAS
    // -----------------------------------------------------------------------

    /**
     * Render the B&B search tree.
     *
     * @param nodes    list of explored/pruned nodes; null → empty tree
     * @param highlight index of the node currently highlighted (animation step)
     */
    private void drawTree(List<TspAlgorithm.TspNode> nodes, int highlight) {
        GraphicsContext gc = treeCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, TREE_W, TREE_H);
        gc.setFill(C_BG);
        gc.fillRect(0, 0, TREE_W, TREE_H);

        if (nodes == null || nodes.isEmpty()) {
            gc.setFill(C_TXT_SEC);
            gc.setFont(Font.font("System", 12));
            gc.fillText("Run solver to see the B&B tree.", 10, TREE_H / 2.0);
            return;
        }

        // Group nodes by level
        int maxLevel = 0;
        for (TspAlgorithm.TspNode n : nodes) maxLevel = Math.max(maxLevel, n.level);

        List<List<Integer>> byLevel = new ArrayList<>();
        for (int l = 0; l <= maxLevel; l++) byLevel.add(new ArrayList<>());
        for (int idx = 0; idx < nodes.size(); idx++) byLevel.get(nodes.get(idx).level).add(idx);

        double levelH  = (TREE_H - 20) / (double) Math.max(1, maxLevel + 1);
        double nodeR   = 7;
        double[] nodeX = new double[nodes.size()];
        double[] nodeY = new double[nodes.size()];

        // Assign positions
        for (int l = 0; l <= maxLevel; l++) {
            List<Integer> ids = byLevel.get(l);
            int cnt = ids.size();
            for (int k = 0; k < cnt; k++) {
                int idx = ids.get(k);
                nodeX[idx] = (k + 1) * TREE_W / (double)(cnt + 1);
                nodeY[idx] = 15 + l * levelH;
            }
        }

        // Draw edges (parent = previous node with level-1 and an overlapping path prefix)
        gc.setStroke(C_GREY);
        gc.setLineWidth(1);
        for (int idx = 1; idx < nodes.size(); idx++) {
            TspAlgorithm.TspNode child = nodes.get(idx);
            // Find best parent: last node at level-1 whose path is a prefix of child's
            for (int pi = idx - 1; pi >= 0; pi--) {
                TspAlgorithm.TspNode parent = nodes.get(pi);
                if (parent.level == child.level - 1 && isPrefixPath(parent, child)) {
                    gc.strokeLine(nodeX[pi], nodeY[pi], nodeX[idx], nodeY[idx]);
                    break;
                }
            }
        }

        // Draw nodes
        for (int idx = 0; idx < nodes.size(); idx++) {
            TspAlgorithm.TspNode node = nodes.get(idx);
            Color fill = node.pruned ? C_RED : C_GREEN;
            if (idx == highlight) fill = C_GOLD;

            gc.setFill(fill);
            gc.fillOval(nodeX[idx] - nodeR, nodeY[idx] - nodeR, nodeR * 2, nodeR * 2);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1);
            gc.strokeOval(nodeX[idx] - nodeR, nodeY[idx] - nodeR, nodeR * 2, nodeR * 2);

            // Level label
            gc.setFill(C_TXT_SEC);
            gc.setFont(Font.font("System", 8));
            gc.fillText(String.valueOf(node.bound), nodeX[idx] - 10, nodeY[idx] + nodeR + 10);
        }

        // Legend
        double ly = TREE_H - 10;
        gc.setFont(Font.font("System", 9));
        gc.setFill(C_GREEN); gc.fillOval(5,  ly - 7, 8, 8); gc.setFill(C_TXT_SEC); gc.fillText("Explored", 15,  ly);
        gc.setFill(C_RED);   gc.fillOval(70, ly - 7, 8, 8); gc.setFill(C_TXT_SEC); gc.fillText("Pruned",   80,  ly);
        gc.setFill(C_GOLD);  gc.fillOval(120,ly - 7, 8, 8); gc.setFill(C_TXT_SEC); gc.fillText("Current",  130, ly);
    }

    private boolean isPrefixPath(TspAlgorithm.TspNode parent, TspAlgorithm.TspNode child) {
        for (int k = 0; k <= parent.level; k++) {
            if (parent.path[k] != child.path[k]) return false;
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // SOLVER ACTIONS
    // -----------------------------------------------------------------------

    private void runSolver(boolean stepMode) {
        readMatrixFromFields();
        int[] result = algorithm.solve(currentDist);
        optimalPath  = result;
        optimalCost  = algorithm.getBestCost();
        animNodes    = new ArrayList<>(algorithm.getExploredNodes());

        if (!stepMode) {
            updateResults();
            drawMap(optimalPath, null, null);
            drawTree(animNodes, -1);
        }
        solved = true;
    }

    private void runNearestNeighbour() {
        readMatrixFromFields();
        if (!solved) runSolver(false);
        nnPath = algorithm.nearestNeighbour(currentDist);
        nnCost = algorithm.nearestNeighbourCost(currentDist);
        updateResults();
    }

    private void startStepMode() {
        if (!solved) runSolver(true);
        animStep = 0;
        playing  = false;
        renderStep(0);
        lblStepInfo.setText("Step 0 / " + animNodes.size() + " – use ▶ Play or Next ▶");
    }

    private void togglePlay() {
        if (!solved) {
            startStepMode();
            return;
        }
        playing = !playing;
        btnPlayPause.setText(playing ? "⏸ Pause" : "▶ Play");
        if (playing) {
            timeline = new Timeline(new KeyFrame(
                Duration.millis(speedSlider.getValue()),
                e -> {
                    if (animStep < animNodes.size() - 1) {
                        stepForward();
                    } else {
                        playing = false;
                        btnPlayPause.setText("▶ Play");
                        timeline.stop();
                    }
                }
            ));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();
        } else {
            if (timeline != null) timeline.stop();
        }
    }

    private void stepForward() {
        if (!solved || animNodes.isEmpty()) return;
        if (animStep < animNodes.size() - 1) animStep++;
        renderStep(animStep);
    }

    private void stepBack() {
        if (!solved || animNodes.isEmpty()) return;
        if (animStep > 0) animStep--;
        renderStep(animStep);
    }

    private void renderStep(int step) {
        if (animNodes.isEmpty()) return;
        TspAlgorithm.TspNode node = animNodes.get(step);

        // Build a partial path to draw on the map
        int[] partial = new int[node.level + 1];
        System.arraycopy(node.path, 0, partial, 0, node.level + 1);

        // Show best path if this is a complete tour
        int[] showPath = (node.level == matrixN - 1 && !node.pruned) ? optimalPath : null;
        drawMap(showPath, partial, null);
        drawTree(animNodes.subList(0, step + 1), step);

        lblStepInfo.setText("Step " + (step + 1) + " / " + animNodes.size()
            + " | Level " + node.level
            + " | Bound: " + node.bound
            + " | " + (node.pruned ? "PRUNED" : "Explored"));
    }

    // -----------------------------------------------------------------------
    // RESULTS
    // -----------------------------------------------------------------------

    private void updateResults() {
        if (optimalPath == null) return;
        NumberFormat nf = NumberFormat.getInstance(Locale.US);

        StringBuilder route = new StringBuilder();
        for (int i = 0; i <= matrixN; i++) {
            int idx = optimalPath[i < matrixN ? i : matrixN];
            if (idx >= 0 && idx < currentCodes.length)
                route.append(currentCodes[idx]);
            if (i < matrixN) route.append(" → ");
        }
        lblBestRoute.setText("Best Route: " + route);
        lblOptCost.setText("Optimal Cost: " + nf.format(optimalCost) + " km");
        lblNodesExp.setText("Nodes Explored: " + algorithm.getExploredCount());
        lblNodesPruned.setText("Nodes Pruned:   " + algorithm.getPrunedCount());

        if (nnCost > 0) {
            lblNNCost.setText("Nearest Neighbour Cost: " + nf.format(nnCost) + " km");
            int savings = nnCost - optimalCost;
            if (savings > 0) {
                double pct = 100.0 * savings / nnCost;
                lblSavings.setText(String.format("✔ B&B saves %s km (%.1f%%) over NN",
                    nf.format(savings), pct));
            } else {
                lblSavings.setText("");
            }
        }
    }

    private void clearResults() {
        lblBestRoute.setText("Best Route: —");
        lblOptCost.setText("Optimal Cost: —");
        lblNodesExp.setText("Nodes Explored: —");
        lblNodesPruned.setText("Nodes Pruned: —");
        lblNNCost.setText("Nearest Neighbour Cost: —");
        lblSavings.setText("");
    }

    // -----------------------------------------------------------------------
    // HELPERS
    // -----------------------------------------------------------------------

    private void readMatrixFromFields() {
        if (matrixFields == null) return;
        for (int i = 0; i < matrixN; i++) {
            for (int j = 0; j < matrixN; j++) {
                try {
                    currentDist[i][j] = Integer.parseInt(matrixFields[i][j].getText().trim());
                } catch (NumberFormatException ignored) {
                    currentDist[i][j] = 0;
                }
            }
        }
    }

    private void populateMatrixFields(int[][] dist) {
        if (matrixFields == null) return;
        for (int i = 0; i < matrixN; i++)
            for (int j = 0; j < matrixN; j++)
                matrixFields[i][j].setText(String.valueOf(dist[i][j]));
    }

    // ---- Label factory for result rows ----
    private Label resultLabel(String key, String value) {
        Label lbl = new Label(key + ": " + value);
        lbl.setStyle("-fx-font-size: 12; -fx-text-fill: " + StyleConstants.TEXT_PRIMARY + ";");
        lbl.setWrapText(true);
        return lbl;
    }

    // ---- Generic card wrapper ----
    private VBox card(String title, Node content) {
        Label hdr = new Label(title);
        hdr.setFont(Font.font("System", FontWeight.BOLD, StyleConstants.FONT_BODY));
        hdr.setStyle("-fx-text-fill: " + StyleConstants.TEXT_PRIMARY + ";");
        VBox box = new VBox(6, hdr, content);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: " + StyleConstants.BG_CARD + "; "
            + "-fx-background-radius: " + StyleConstants.RADIUS + "; "
            + "-fx-border-color: #E5E7EB; -fx-border-radius: " + StyleConstants.RADIUS + ";");
        return box;
    }

    // ---- Styled button ----
    private Button styledButton(String text, String colorHex) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; "
            + "-fx-background-radius: 6; -fx-font-size: 12; -fx-cursor: hand; "
            + "-fx-padding: 6 14 6 14;");
        return btn;
    }

    // ---- Spacer region ----
    private Region spacer(double width) {
        Region r = new Region();
        r.setPrefWidth(width);
        return r;
    }
}
