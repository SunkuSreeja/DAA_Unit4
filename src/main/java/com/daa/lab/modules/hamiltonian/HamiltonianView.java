package com.daa.lab.modules.hamiltonian;

import com.daa.lab.ui.BaseModuleView;
import com.daa.lab.ui.StyleConstants;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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

import java.util.List;

/**
 * Module 2 – Hamiltonian Cycle (PCB Circuit Routing).
 *
 * <p>Provides an interactive visualization of the backtracking algorithm that
 * finds a Hamiltonian cycle in an undirected graph. The left panel contains
 * algorithm information and controls; the right panel renders the graph on a
 * Canvas with animated step playback.
 */
public class HamiltonianView extends BaseModuleView {

    // ── Algorithm state ───────────────────────────────────────────────────────
    private final HamiltonianAlgorithm algorithm = new HamiltonianAlgorithm();
    private int[][] currentGraph;
    private int     currentN = 6;
    private int[]   foundCycle; // null when no cycle found

    private List<HamiltonianAlgorithm.AnimationStep> animSteps;
    private int stepIndex = 0;

    // ── Canvas ────────────────────────────────────────────────────────────────
    private static final double CANVAS_W = 450;
    private static final double CANVAS_H = 450;
    private Canvas canvas;
    private GraphicsContext gc;

    // ── Animation timeline ────────────────────────────────────────────────────
    private Timeline timeline;
    private double   animSpeedMs = 600;
    private boolean  playing     = false;

    // ── Controls (need refs for enable/disable) ───────────────────────────────
    private ComboBox<String> graphSelector;
    private Spinner<Integer> verticesSpinner;
    private TextArea         edgesArea;
    private Button           btnFind;
    private Button           btnPlay;
    private Button           btnNext;
    private Button           btnPrev;
    private Button           btnStepReset;
    private Slider           speedSlider;
    private Label            statusLabel;
    private Label            stepLabel;

    // ── Constructor ───────────────────────────────────────────────────────────

    @SuppressWarnings("this-escape")
    public HamiltonianView() {
        // Initialize graph data before buildContent() tries to draw the graph.
        currentGraph = algorithm.generateDefaultGraph(6);
        // Build the full UI now that all fields are ready.
        initializeView("🔌 Hamiltonian Cycle – PCB Circuit Routing");
        // Initial graph render (canvas was created by buildContent() above).
        drawGraph(currentGraph, null, -1, null);
    }

    // ── BaseModuleView contract ───────────────────────────────────────────────

    @Override
    protected Node buildContent() {
        HBox root = new HBox(StyleConstants.GAP);
        root.setPadding(new Insets(StyleConstants.PADDING));
        root.setStyle("-fx-background-color: " + StyleConstants.BG_PRIMARY + ";");

        VBox left  = buildLeftPanel();
        VBox right = buildRightPanel();

        ScrollPane leftScroll = new ScrollPane(left);
        leftScroll.setFitToWidth(true);
        leftScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScroll.setMinWidth(300);
        leftScroll.setPrefWidth(355);
        leftScroll.setMaxWidth(360);
        leftScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");

        HBox.setHgrow(leftScroll, Priority.NEVER);
        HBox.setHgrow(right, Priority.ALWAYS);

        root.getChildren().addAll(leftScroll, right);
        return root;
    }

    @Override
    protected void reset() {
        stopAnimation();
        stepIndex   = 0;
        foundCycle  = null;
        animSteps   = null;
        playing     = false;

        // Reload the currently selected graph preset
        loadSelectedGraph();
        updateStepLabel();
        statusLabel.setText("Ready – select a graph and click 'Find Hamiltonian Cycle'.");
        statusLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + "; -fx-font-size: 12px;");
        setAnimControlsDisabled(true);
        drawGraph(currentGraph, null, -1, null);
    }

    // ── Left panel ────────────────────────────────────────────────────────────

    private VBox buildLeftPanel() {
        VBox panel = new VBox(StyleConstants.GAP);
        panel.setPadding(new Insets(0, 4, 0, 0));
        // --- Algorithm info box ---
        VBox infoBox = algorithmInfoBox(
                "PCB manufacturers must route a circuit that visits every connection point " +
                "exactly once and returns to the start, minimizing wire length and avoiding re-routing.",
                "1. Start at vertex 0\n" +
                "2. Try adding each unvisited adjacent vertex\n" +
                "3. If vertex is safe (adjacent + unvisited), add to path\n" +
                "4. Recurse for next position\n" +
                "5. Backtrack if stuck\n" +
                "6. Check closure: last vertex must connect to vertex 0",
                "Time: O(N!) | Space: O(N)"
        );

        // --- Graph selection ---
        VBox graphCard = card();

        graphSelector = new ComboBox<>();
        graphSelector.getItems().addAll(
                "Default Graph (6 vertices)",
                "No Hamiltonian Graph",
                "Custom (edit edges)"
        );
        graphSelector.setValue("Default Graph (6 vertices)");
        graphSelector.setMaxWidth(Double.MAX_VALUE);
        graphSelector.setStyle("-fx-font-size: 12px;");
        graphSelector.setOnAction(e -> onGraphSelected());

        // Vertices spinner
        HBox spinnerRow = new HBox(8);
        spinnerRow.setAlignment(Pos.CENTER_LEFT);
        Label vLabel = new Label("Vertices:");
        vLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + "; -fx-font-size: 12px;");
        verticesSpinner = new Spinner<>(4, 10, 6);
        verticesSpinner.setEditable(true);
        verticesSpinner.setPrefWidth(80);
        verticesSpinner.setDisable(true);
        verticesSpinner.valueProperty().addListener((obs, o, nv) -> {
            currentN = nv;
            edgesArea.setPromptText("e.g. 0-1,1-2,2-3,...," + (nv - 1) + "-0");
        });
        spinnerRow.getChildren().addAll(vLabel, verticesSpinner);

        // Edges text area
        Label eLabel = new Label("Edges (u-v,...):");
        eLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + "; -fx-font-size: 12px;");
        edgesArea = new TextArea();
        edgesArea.setPrefRowCount(3);
        edgesArea.setPromptText("e.g. 0-1,1-2,2-3,3-4,4-5,5-0");
        edgesArea.setDisable(true);
        edgesArea.setWrapText(true);
        edgesArea.setStyle("-fx-font-size: 12px;");

        graphCard.getChildren().addAll(
                sectionLabel("Graph Setup"),
                graphSelector, spinnerRow, eLabel, edgesArea
        );

        // --- Action buttons ---
        btnFind = styledButton("Find Hamiltonian Cycle", StyleConstants.ACCENT_BLUE);
        btnFind.setMaxWidth(Double.MAX_VALUE);
        btnFind.setOnAction(e -> runAlgorithm());

        // --- Status label ---
        statusLabel = new Label("Ready – select a graph and click 'Find Hamiltonian Cycle'.");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + "; -fx-font-size: 12px;");

        // --- Step animation controls ---
        VBox stepCard = card();
        stepCard.getChildren().add(sectionLabel("Step Animation"));

        // Speed slider
        HBox speedRow = new HBox(8);
        speedRow.setAlignment(Pos.CENTER_LEFT);
        Label speedLbl = new Label("Speed:");
        speedLbl.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + "; -fx-font-size: 12px;");
        speedSlider = new Slider(100, 2000, 600);
        speedSlider.setShowTickLabels(false);
        speedSlider.setMajorTickUnit(500);
        HBox.setHgrow(speedSlider, Priority.ALWAYS);
        speedSlider.valueProperty().addListener((obs, o, nv) -> {
            animSpeedMs = nv.doubleValue();
            if (playing) restartTimeline();
        });
        Label speedVal = new Label("600ms");
        speedVal.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + "; -fx-font-size: 11px;");
        speedSlider.valueProperty().addListener((obs, o, nv) ->
                speedVal.setText(String.format("%.0fms", nv.doubleValue())));
        speedRow.getChildren().addAll(speedLbl, speedSlider, speedVal);

        // Play/Pause/Next/Prev/Reset buttons
        btnPlay      = styledButton("▶ Play",  StyleConstants.ACCENT_GREEN);
        btnNext      = styledButton("Next ▶",  StyleConstants.ACCENT_BLUE);
        btnPrev      = styledButton("◀ Prev",  StyleConstants.TEXT_SECONDARY);
        btnStepReset = styledButton("↺ Reset", StyleConstants.ACCENT_AMBER);

        HBox stepBtns = new HBox(6, btnPlay, btnPrev, btnNext, btnStepReset);
        stepBtns.setAlignment(Pos.CENTER_LEFT);

        stepLabel = new Label("Step: 0 / 0");
        stepLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + "; -fx-font-size: 12px;");

        stepCard.getChildren().addAll(speedRow, stepBtns, stepLabel);

        btnPlay.setOnAction(e -> togglePlay());
        btnNext.setOnAction(e -> stepForward());
        btnPrev.setOnAction(e -> stepBack());
        btnStepReset.setOnAction(e -> resetSteps());

        setAnimControlsDisabled(true);

        panel.getChildren().addAll(
                infoBox, graphCard, btnFind, statusLabel, stepCard
        );
        return panel;
    }

    // ── Right panel (canvas) ──────────────────────────────────────────────────

    private VBox buildRightPanel() {
        VBox panel = new VBox(StyleConstants.GAP);
        panel.setPadding(new Insets(0, 0, 0, 4));

        Label heading = sectionLabel("PCB Graph Visualization");

        canvas = new Canvas(CANVAS_W, CANVAS_H);
        gc     = canvas.getGraphicsContext2D();

        // Wrap canvas in a card-styled pane
        StackPane canvasHolder = new StackPane(canvas);
        canvasHolder.setStyle(
                "-fx-background-color: " + StyleConstants.BG_CARD + ";" +
                "-fx-background-radius: " + StyleConstants.RADIUS + ";" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        canvasHolder.setPadding(new Insets(12));
        VBox.setVgrow(canvasHolder, Priority.ALWAYS);

        panel.getChildren().addAll(heading, canvasHolder);
        return panel;
    }

    // ── Graph drawing ─────────────────────────────────────────────────────────

    /**
     * Draws the graph on the canvas.
     *
     * @param adj          adjacency matrix
     * @param cyclePath    if non-null, the Hamiltonian cycle path to highlight
     * @param currentTrying the vertex currently being tried (-1 for none)
     * @param backtrackedSet array of booleans marking backtracked vertices
     */
    private void drawGraph(int[][] adj, int[] cyclePath, int currentTrying, boolean[] backtrackedSet) {
        if (gc == null) return;
        int n = adj.length;
        gc.clearRect(0, 0, CANVAS_W, CANVAS_H);
        gc.setFill(Color.web(StyleConstants.BG_CARD));
        gc.fillRect(0, 0, CANVAS_W, CANVAS_H);

        double[] cx = new double[n];
        double[] cy = new double[n];
        double   radius = 22;
        double   cx0    = CANVAS_W / 2;
        double   cy0    = CANVAS_H / 2;
        double   r      = Math.min(CANVAS_W, CANVAS_H) / 2.0 - 50;

        // Arrange vertices in a regular polygon
        for (int i = 0; i < n; i++) {
            double angle = -Math.PI / 2 + 2 * Math.PI * i / n;
            cx[i] = cx0 + r * Math.cos(angle);
            cy[i] = cy0 + r * Math.sin(angle);
        }

        // --- Draw all edges (grey) ---
        gc.setStroke(Color.web("#9CA3AF"));
        gc.setLineWidth(1.5);
        for (int u = 0; u < n; u++) {
            for (int v = u + 1; v < n; v++) {
                if (adj[u][v] == 1) {
                    gc.strokeLine(cx[u], cy[u], cx[v], cy[v]);
                }
            }
        }

        // --- Highlight Hamiltonian cycle edges (blue, weight 3) ---
        if (cyclePath != null && cyclePath.length > 1) {
            gc.setStroke(Color.web(StyleConstants.ACCENT_BLUE));
            gc.setLineWidth(3.5);
            for (int i = 0; i < cyclePath.length - 1; i++) {
                int u = cyclePath[i];
                int v = cyclePath[i + 1];
                gc.strokeLine(cx[u], cy[u], cx[v], cy[v]);
            }
        }

        // --- Draw vertices ---
        for (int i = 0; i < n; i++) {
            Color fillColor;

            if (cyclePath != null) {
                // Cycle found: start=gold, rest=green
                if (i == 0) {
                    fillColor = Color.web("#F59E0B"); // gold
                } else {
                    // Check if vertex is in cycle
                    boolean inCycle = false;
                    for (int p : cyclePath) { if (p == i) { inCycle = true; break; } }
                    fillColor = inCycle ? Color.web(StyleConstants.ACCENT_GREEN) : Color.web("#D1D5DB");
                }
            } else if (backtrackedSet != null && i < backtrackedSet.length && backtrackedSet[i]) {
                fillColor = Color.web(StyleConstants.ACCENT_RED); // backtracked = red
            } else if (i == currentTrying) {
                fillColor = Color.web(StyleConstants.ACCENT_AMBER); // currently trying = amber
            } else {
                fillColor = Color.web("#BFDBFE"); // default: light blue
            }

            // Shadow
            gc.setFill(Color.rgb(0, 0, 0, 0.12));
            gc.fillOval(cx[i] - radius + 2, cy[i] - radius + 2, radius * 2, radius * 2);

            // Fill
            gc.setFill(fillColor);
            gc.fillOval(cx[i] - radius, cy[i] - radius, radius * 2, radius * 2);

            // Border
            gc.setStroke(Color.web(StyleConstants.TEXT_PRIMARY));
            gc.setLineWidth(cyclePath != null && i == 0 ? 2.5 : 1.5);
            gc.strokeOval(cx[i] - radius, cy[i] - radius, radius * 2, radius * 2);

            // Label
            gc.setFill(Color.web(StyleConstants.TEXT_PRIMARY));
            gc.setFont(Font.font("System", FontWeight.BOLD, 13));
            String label = String.valueOf(i);
            gc.fillText(label, cx[i] - (label.length() > 1 ? 7 : 4), cy[i] + 5);
        }

        // --- Legend ---
        drawLegend(cyclePath, currentTrying, backtrackedSet);
    }

    private void drawLegend(int[] cyclePath, int currentTrying, boolean[] backtrackedSet) {
        double x = 10, y = CANVAS_H - 90;
        gc.setFont(Font.font("System", 11));

        if (cyclePath != null) {
            drawLegendItem(x, y, "#F59E0B", "Start vertex"); y += 20;
            drawLegendItem(x, y, StyleConstants.ACCENT_GREEN, "Cycle vertex"); y += 20;
            drawLegendItem(x, y, StyleConstants.ACCENT_BLUE, "Cycle edge (stroke)");
        } else if (currentTrying >= 0 || backtrackedSet != null) {
            drawLegendItem(x, y, StyleConstants.ACCENT_AMBER, "Trying"); y += 20;
            drawLegendItem(x, y, StyleConstants.ACCENT_RED, "Backtracked"); y += 20;
            drawLegendItem(x, y, "#BFDBFE", "Unvisited");
        }
    }

    private void drawLegendItem(double x, double y, String color, String text) {
        gc.setFill(Color.web(color));
        gc.fillOval(x, y, 12, 12);
        gc.setStroke(Color.web(StyleConstants.TEXT_PRIMARY));
        gc.setLineWidth(1);
        gc.strokeOval(x, y, 12, 12);
        gc.setFill(Color.web(StyleConstants.TEXT_SECONDARY));
        gc.fillText(text, x + 18, y + 10);
    }

    // ── Algorithm execution ───────────────────────────────────────────────────

    private void runAlgorithm() {
        stopAnimation();
        loadSelectedGraph();

        foundCycle = algorithm.solve(currentGraph);
        animSteps  = algorithm.getSteps();
        stepIndex  = 0;
        playing    = false;

        if (foundCycle != null) {
            // Build display string: 0 → 2 → 4 → ... → 0
            StringBuilder sb = new StringBuilder("Cycle found: ");
            for (int i = 0; i < foundCycle.length; i++) {
                sb.append(foundCycle[i]);
                if (i < foundCycle.length - 1) sb.append(" → ");
            }
            statusLabel.setText(sb.toString());
            statusLabel.setStyle("-fx-text-fill: " + StyleConstants.ACCENT_GREEN + "; -fx-font-size: 12px; -fx-font-weight: bold;");
            drawGraph(currentGraph, foundCycle, -1, null);
        } else {
            statusLabel.setText("No Hamiltonian Cycle exists for this graph.");
            statusLabel.setStyle("-fx-text-fill: " + StyleConstants.ACCENT_RED + "; -fx-font-size: 12px; -fx-font-weight: bold;");
            drawGraph(currentGraph, null, -1, null);
        }

        updateStepLabel();
        setAnimControlsDisabled(animSteps == null || animSteps.isEmpty());
    }

    private void loadSelectedGraph() {
        String sel = graphSelector.getValue();
        if (sel == null) return;
        switch (sel) {
            case "Default Graph (6 vertices)":
                currentN     = 6;
                currentGraph = algorithm.generateDefaultGraph(6);
                break;
            case "No Hamiltonian Graph":
                currentN     = 6;
                currentGraph = algorithm.generateNoHamiltonianGraph(6);
                break;
            case "Custom (edit edges)":
                currentGraph = algorithm.parseEdgeList(edgesArea.getText(), currentN);
                break;
        }
    }

    private void onGraphSelected() {
        String sel = graphSelector.getValue();
        boolean custom = "Custom (edit edges)".equals(sel);
        verticesSpinner.setDisable(!custom);
        edgesArea.setDisable(!custom);
        stopAnimation();
        foundCycle = null;
        animSteps  = null;
        stepIndex  = 0;
        loadSelectedGraph();
        setAnimControlsDisabled(true);
        statusLabel.setText("Graph changed. Click 'Find Hamiltonian Cycle'.");
        statusLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + "; -fx-font-size: 12px;");
        drawGraph(currentGraph, null, -1, null);
        updateStepLabel();
    }

    // ── Step animation ────────────────────────────────────────────────────────

    private void togglePlay() {
        if (playing) {
            pauseAnimation();
        } else {
            startAnimation();
        }
    }

    private void startAnimation() {
        if (animSteps == null || animSteps.isEmpty()) return;
        playing = true;
        btnPlay.setText("⏸ Pause");
        restartTimeline();
    }

    private void pauseAnimation() {
        playing = false;
        btnPlay.setText("▶ Play");
        if (timeline != null) timeline.pause();
    }

    private void stopAnimation() {
        playing = false;
        if (btnPlay != null) btnPlay.setText("▶ Play");
        if (timeline != null) { timeline.stop(); timeline = null; }
    }

    private void restartTimeline() {
        if (timeline != null) timeline.stop();
        timeline = new Timeline(new KeyFrame(Duration.millis(animSpeedMs), e -> {
            if (stepIndex < animSteps.size()) {
                renderStep(stepIndex);
                stepIndex++;
                updateStepLabel();
            } else {
                pauseAnimation();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void stepForward() {
        if (animSteps == null) return;
        if (stepIndex < animSteps.size()) {
            renderStep(stepIndex);
            stepIndex++;
            updateStepLabel();
        }
    }

    private void stepBack() {
        if (animSteps == null) return;
        if (stepIndex > 0) {
            stepIndex--;
            renderStep(stepIndex);
            updateStepLabel();
        }
    }

    private void resetSteps() {
        stopAnimation();
        stepIndex = 0;
        updateStepLabel();
        drawGraph(currentGraph, null, -1, null);
        statusLabel.setText("Step animation reset. Press ▶ Play or Next to replay.");
        statusLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + "; -fx-font-size: 12px;");
    }

    private void renderStep(int idx) {
        if (animSteps == null || idx >= animSteps.size()) return;
        HamiltonianAlgorithm.AnimationStep step = animSteps.get(idx);
        int n = currentGraph.length;

        // Build backtracked set: any vertex in a previous non-placed step
        boolean[] backtracked = new boolean[n];
        for (int i = 0; i <= idx; i++) {
            HamiltonianAlgorithm.AnimationStep s = animSteps.get(i);
            if (!s.placed && s.currentVertex >= 0 && s.currentVertex < n) {
                backtracked[s.currentVertex] = true;
            }
        }
        // Unmark currently placed vertices (they might have been backtracked earlier)
        for (int v : step.pathSnapshot) {
            if (v >= 0 && v < n) backtracked[v] = false;
        }

        // Check if this is the final cycle-closed step
        boolean cycleComplete = step.description.startsWith("Cycle closed");

        if (cycleComplete && foundCycle != null) {
            drawGraph(currentGraph, foundCycle, -1, null);
        } else {
            drawGraph(currentGraph, null, step.placed ? step.currentVertex : -1, backtracked);
        }

        stepLabel.setText("Step: " + (idx + 1) + " / " + animSteps.size() +
                "  |  " + step.description);
    }

    private void updateStepLabel() {
        int total = (animSteps != null) ? animSteps.size() : 0;
        stepLabel.setText("Step: " + stepIndex + " / " + total);
    }

    private void setAnimControlsDisabled(boolean disabled) {
        btnPlay.setDisable(disabled);
        btnNext.setDisable(disabled);
        btnPrev.setDisable(disabled);
        btnStepReset.setDisable(disabled);
        speedSlider.setDisable(disabled);
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private Button styledButton(String text, String colorHex) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: " + colorHex + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 7 14 7 14;" +
                "-fx-font-size: 12px;");
        return btn;
    }

    private VBox card() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(14));
        box.setStyle(
                "-fx-background-color: " + StyleConstants.BG_CARD + ";" +
                "-fx-background-radius: " + StyleConstants.RADIUS + ";" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 6, 0, 0, 1);");
        return box;
    }
}
