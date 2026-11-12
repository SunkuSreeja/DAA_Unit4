package com.daa.lab.modules.queens;

import com.daa.lab.ui.BaseModuleView;
import com.daa.lab.ui.StyleConstants;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Full UI view for Module 1: 8-Queens – Backtracking (Robot Collision-Free
 * Motion Planning).
 *
 * <p>
 * Layout: left controls/info panel + right canvas visualisations (chessboard
 * &amp; robot grid).
 * </p>
 */
public class QueensView extends BaseModuleView {

    // ── Algorithm instances ───────────────────────────────────────────────────
    private final QueensAlgorithm queensAlgo = new QueensAlgorithm();
    private final RobotPathAlgorithm robotAlgo = new RobotPathAlgorithm(8, 8);

    // ── Queens state ──────────────────────────────────────────────────────────
    private List<int[]> queensSolutions = new ArrayList<>();
    private int currentSolutionIndex = 0;
    private int currentN = 8;

    /**
     * Represents a single step in the backtracking search tree for visualization.
     * cellStatus: 1 = TRYING (yellow), 2 = INVALID (red), 3 = PLACED (green), 4 =
     * BACKTRACKED (light red)
     */
    private static class AnimStep {
        final int[] boardSnapshot;
        final int row;
        final int col;
        final int cellStatus;

        AnimStep(int[] board, int row, int col, int cellStatus) {
            this.boardSnapshot = Arrays.copyOf(board, board.length);
            this.row = row;
            this.col = col;
            this.cellStatus = cellStatus;
        }
    }

    private List<AnimStep> animationSteps = new ArrayList<>();
    private int animStepIndex = 0;
    private Timeline queensTimeline;

    // ── Queens UI controls ────────────────────────────────────────────────────
    private Canvas queensCanvas;
    private Label solutionCountLabel;
    private Label queensStatusLabel;
    private Slider nSlider;
    private Label nLabel;
    private Slider speedSlider;
    private Button playPauseBtn;

    // ── Robot path state ──────────────────────────────────────────────────────
    private boolean[][] robotObstacles = new boolean[8][8];
    private List<int[]> robotPath = new ArrayList<>();
    private List<int[]> robotAnimPath = new ArrayList<>();
    private int robotAnimStep = 0;
    private Timeline robotTimeline;

    // ── Robot UI controls ─────────────────────────────────────────────────────
    private Canvas robotCanvas;
    private Label robotStatusLabel;

    // ── Hover state ───────────────────────────────────────────────────────────
    private int hoverRow = -1;
    private int hoverCol = -1;

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final int QUEENS_CANVAS_SIZE = 400;
    private static final int ROBOT_CANVAS_SIZE = 320;

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final Color LIGHT_SQUARE = Color.web("#F0D9B5");
    private static final Color DARK_SQUARE = Color.web("#B58863");
    private static final Color QUEEN_LIGHT = Color.web(StyleConstants.ACCENT_BLUE);
    private static final Color QUEEN_DARK = Color.web(StyleConstants.ACCENT_GREEN);
    private static final Color ATTACKED_COLOR = Color.web("#DC262640");
    private static final Color TRY_COLOR = Color.web("#F59E0BAA"); // yellow / amber
    private static final Color INVALID_COLOR = Color.web("#EF4444AA"); // red
    private static final Color PLACED_COLOR = Color.web("#22C55EAA"); // green
    private static final Color BACKTRACK_COLOR = Color.web("#F8717188"); // light red

    // ── Constructor ───────────────────────────────────────────────────────────

    @SuppressWarnings("this-escape")
    public QueensView() {
        // 1. All fields are now initialized (instance initializers have run).
        // 2. Initialize default obstacles before buildContent() draws the grid.
        robotObstacles = robotAlgo.generateDefaultObstacles(8, 8);
        // 3. Now safe to build the UI – all fields are ready.
        initializeView("8-Queens – Robot Collision-Free Motion Planning");
    }

    // ── BaseModuleView contract ───────────────────────────────────────────────

    @Override
    protected Node buildContent() {
        HBox root = new HBox(StyleConstants.GAP);
        root.setPadding(new Insets(StyleConstants.PADDING));
        root.setStyle("-fx-background-color: " + StyleConstants.BG_PRIMARY + ";");

        VBox leftPanel = buildLeftPanel();
        VBox rightPanel = buildRightPanel();

        ScrollPane leftScroll = new ScrollPane(leftPanel);
        leftScroll.setFitToWidth(true);
        leftScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScroll.setMinWidth(300);
        leftScroll.setPrefWidth(360);
        leftScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");

        ScrollPane rightScroll = new ScrollPane(rightPanel);
        rightScroll.setFitToWidth(true);
        rightScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rightScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");

        HBox.setHgrow(leftScroll, Priority.NEVER);
        HBox.setHgrow(rightScroll, Priority.ALWAYS);

        root.getChildren().addAll(leftScroll, rightScroll);
        return root;
    }

    @Override
    protected void reset() {
        // Stop any running animations
        stopQueensAnimation();
        stopRobotAnimation();

        // Reset queens state
        queensSolutions.clear();
        animationSteps.clear();
        animStepIndex = 0;
        currentSolutionIndex = 0;

        // Reset robot state
        robotPath.clear();
        robotAnimPath.clear();
        robotAnimStep = 0;
        robotObstacles = robotAlgo.generateDefaultObstacles(8, 8);

        // Reset UI labels
        if (solutionCountLabel != null)
            solutionCountLabel.setText("Solutions found: 0");
        if (queensStatusLabel != null) {
            queensStatusLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + ";");
            queensStatusLabel.setText("Press 'Find All Solutions' to start");
        }
        if (robotStatusLabel != null) {
            robotStatusLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + ";");
            robotStatusLabel.setText("Ready – obstacles generated");
        }
        if (playPauseBtn != null)
            playPauseBtn.setText("▶ Play");

        // Redraw canvases
        if (queensCanvas != null)
            drawEmptyBoard();
        if (robotCanvas != null)
            drawRobotGrid();
    }

    // ── Left panel ────────────────────────────────────────────────────

    private VBox buildLeftPanel() {
        VBox panel = new VBox(StyleConstants.GAP);
        panel.setPrefWidth(350);
        panel.setMaxWidth(350);
        panel.setPadding(new Insets(0, 4, 0, 0));

        // Algorithm info box
        VBox infoBox = algorithmInfoBox(
                "Robots on a factory floor must move without colliding. " +
                        "The 8-Queens problem models this: place 8 queens (robots) " +
                        "so none threatens another.",
                "1. Place queen in row 0\n" +
                        "2. For each row, try columns left→right\n" +
                        "3. Check column & diagonal safety\n" +
                        "4. If safe, recurse to next row\n" +
                        "5. If conflict, backtrack and try next column\n" +
                        "6. Record solution when all 8 rows placed",
                "Time: O(N!) | Space: O(N)");

        // N-Queens control section
        VBox queensControls = buildQueensControls();

        // Robot path section
        VBox robotControls = buildRobotControls();

        panel.getChildren().addAll(infoBox, queensControls, robotControls);
        return panel;
    }

    private VBox buildQueensControls() {
        VBox box = cardBox();
        box.getChildren().add(sectionLabel("N-Queens Backtracking"));

        // N slider
        nLabel = new Label("N = 8");
        nLabel.setFont(Font.font("System", FontWeight.BOLD, StyleConstants.FONT_BODY));
        nLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_PRIMARY + ";");

        nSlider = new Slider(4, 12, 8);
        nSlider.setShowTickMarks(true);
        nSlider.setShowTickLabels(true);
        nSlider.setMajorTickUnit(2);
        nSlider.setSnapToTicks(true);
        nSlider.setBlockIncrement(1);
        nSlider.setMaxWidth(Double.MAX_VALUE);
        nSlider.valueProperty().addListener((obs, oldV, newV) -> {
            currentN = newV.intValue();
            nLabel.setText("N = " + currentN);
            reset();
        });

        // Find solutions button
        Button findBtn = styledButton("Find All Solutions", StyleConstants.ACCENT_BLUE);
        findBtn.setOnAction(e -> doFindSolutions());

        // Previous / Next solution navigation
        Button prevBtn = styledButton("◀ Previous", StyleConstants.TEXT_SECONDARY);
        Button nextBtn = styledButton("Next ▶", StyleConstants.ACCENT_GREEN);
        prevBtn.setOnAction(e -> showSolution(currentSolutionIndex - 1));
        nextBtn.setOnAction(e -> showSolution(currentSolutionIndex + 1));
        HBox navBox = new HBox(8, prevBtn, nextBtn);
        HBox.setHgrow(prevBtn, Priority.ALWAYS);
        HBox.setHgrow(nextBtn, Priority.ALWAYS);

        solutionCountLabel = new Label("Solutions found: 0");
        solutionCountLabel.setFont(Font.font("System", FontWeight.BOLD, StyleConstants.FONT_BODY));
        solutionCountLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_PRIMARY + ";");

        // Step animation controls
        Label animLabel = sectionLabel("Step-by-Step Backtracking Animation");
        animLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

        playPauseBtn = styledButton("▶ Play", StyleConstants.ACCENT_BLUE);
        Button nextStepBtn = styledButton("Step →", StyleConstants.ACCENT_GREEN);
        Button prevStepBtn = styledButton("← Step", StyleConstants.TEXT_SECONDARY);
        Button resetAnimBtn = styledButton("↺ Reset", StyleConstants.ACCENT_AMBER);

        playPauseBtn.setOnAction(e -> toggleQueensPlay());
        nextStepBtn.setOnAction(e -> stepQueens(1));
        prevStepBtn.setOnAction(e -> stepQueens(-1));
        resetAnimBtn.setOnAction(e -> resetQueensAnimation());

        HBox stepBox = new HBox(6, prevStepBtn, playPauseBtn, nextStepBtn, resetAnimBtn);
        stepBox.setAlignment(Pos.CENTER_LEFT);

        Label speedLabel = new Label("Speed:");
        speedLabel.setFont(Font.font("System", StyleConstants.FONT_BODY));
        speedLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + ";");
        speedSlider = new Slider(100, 1000, 400);
        speedSlider.setShowTickMarks(false);
        speedSlider.setMaxWidth(Double.MAX_VALUE);
        HBox speedBox = new HBox(6, speedLabel, speedSlider);
        speedBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(speedSlider, Priority.ALWAYS);

        queensStatusLabel = new Label("Press 'Find All Solutions' to start");
        queensStatusLabel.setFont(Font.font("System", StyleConstants.FONT_BODY));
        queensStatusLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + ";");
        queensStatusLabel.setWrapText(true);

        box.getChildren().addAll(
                nLabel, nSlider,
                findBtn, navBox, solutionCountLabel,
                animLabel, stepBox, speedBox,
                queensStatusLabel);
        return box;
    }

    private VBox buildRobotControls() {
        VBox box = cardBox();
        box.getChildren().add(sectionLabel("Robot Path Navigation"));

        Label tip = new Label("Click cells to toggle obstacles manually.");
        tip.setFont(Font.font("System", 11));
        tip.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + ";");

        Button genBtn = styledButton("Generate Obstacles", StyleConstants.ACCENT_AMBER);
        Button findBtn = styledButton("Find Path", StyleConstants.ACCENT_BLUE);
        Button animBtn = styledButton("Animate Path", StyleConstants.ACCENT_GREEN);
        Button resetBtn = styledButton("Reset", StyleConstants.ACCENT_RED);

        genBtn.setOnAction(e -> doGenerateObstacles());
        findBtn.setOnAction(e -> doFindRobotPath());
        animBtn.setOnAction(e -> doAnimateRobotPath());
        resetBtn.setOnAction(e -> {
            stopRobotAnimation();
            robotPath.clear();
            robotAnimPath.clear();
            robotAnimStep = 0;
            robotObstacles = new boolean[8][8];
            robotStatusLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + ";");
            robotStatusLabel.setText("Grid cleared – click cells to add obstacles");
            drawRobotGrid();
        });

        HBox row1 = new HBox(6, genBtn, findBtn);
        HBox row2 = new HBox(6, animBtn, resetBtn);
        HBox.setHgrow(genBtn, Priority.ALWAYS);
        HBox.setHgrow(findBtn, Priority.ALWAYS);
        HBox.setHgrow(animBtn, Priority.ALWAYS);
        HBox.setHgrow(resetBtn, Priority.ALWAYS);

        robotStatusLabel = new Label("Ready – obstacles generated");
        robotStatusLabel.setFont(Font.font("System", StyleConstants.FONT_BODY));
        robotStatusLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + ";");
        robotStatusLabel.setWrapText(true);

        box.getChildren().addAll(tip, row1, row2, robotStatusLabel);
        return box;
    }

    // ── Right panel ───────────────────────────────────────────────────────────

    private VBox buildRightPanel() {
        VBox panel = new VBox(StyleConstants.GAP);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(0, 0, 10, 0));

        // Queens chessboard
        Label queensTitle = sectionLabel("Chessboard Visualisation");
        queensCanvas = new Canvas(QUEENS_CANVAS_SIZE, QUEENS_CANVAS_SIZE);
        drawEmptyBoard();

        // Hover to highlight attacked cells
        queensCanvas.setOnMouseMoved(e -> {
            int cellSize = QUEENS_CANVAS_SIZE / currentN;
            int r = (int) (e.getY() / cellSize);
            int c = (int) (e.getX() / cellSize);
            if (r != hoverRow || c != hoverCol) {
                hoverRow = r;
                hoverCol = c;
                redrawQueensBoard();
            }
        });
        queensCanvas.setOnMouseExited(e -> {
            hoverRow = -1;
            hoverCol = -1;
            redrawQueensBoard();
        });

        VBox queensBox = centeredCanvas(queensCanvas);

        // Robot grid
        Label robotTitle = sectionLabel("Robot Grid Visualisation (8×8)");
        robotCanvas = new Canvas(ROBOT_CANVAS_SIZE, ROBOT_CANVAS_SIZE);

        // Click to toggle obstacles
        robotCanvas.setOnMouseClicked(e -> {
            double cellSize = (double) ROBOT_CANVAS_SIZE / 8;
            int r = (int) (e.getY() / cellSize);
            int c = (int) (e.getX() / cellSize);
            if (r >= 0 && r < 8 && c >= 0 && c < 8) {
                // Don't toggle start (0,0) or goal (7,7)
                if ((r == 0 && c == 0) || (r == 7 && c == 7))
                    return;
                robotObstacles[r][c] = !robotObstacles[r][c];
                stopRobotAnimation();
                robotPath.clear();
                robotAnimPath.clear();
                robotAnimStep = 0;
                robotStatusLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + ";");
                robotStatusLabel.setText("Cell (" + r + "," + c + ") " +
                        (robotObstacles[r][c] ? "blocked" : "freed") + ". Press 'Find Path'.");
                drawRobotGrid();
            }
        });

        drawRobotGrid();
        VBox robotBox = centeredCanvas(robotCanvas);

        panel.getChildren().addAll(queensTitle, queensBox, robotTitle, robotBox);
        return panel;
    }

    // ── Queens algorithm actions ───────────────────────────────────────────────

    private void doFindSolutions() {
        stopQueensAnimation();
        currentSolutionIndex = 0;

        // Build animation step list via instrumented backtracking
        animationSteps = buildAnimationSteps(currentN);
        animStepIndex = 0;

        // Run the pure algorithm
        queensSolutions = new ArrayList<>(queensAlgo.solve(currentN));
        solutionCountLabel.setText("Solutions found: " + queensSolutions.size());
        queensStatusLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_PRIMARY + ";");
        queensStatusLabel.setText("Found " + queensSolutions.size() +
                " solutions in " + queensAlgo.getStepCount() + " steps.");

        if (!queensSolutions.isEmpty()) {
            showSolution(0);
        } else {
            drawEmptyBoard();
        }
    }

    private void showSolution(int index) {
        if (queensSolutions.isEmpty())
            return;
        currentSolutionIndex = Math.max(0, Math.min(index, queensSolutions.size() - 1));
        int[] sol = queensSolutions.get(currentSolutionIndex);
        queensStatusLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_PRIMARY + ";");
        queensStatusLabel.setText("Showing solution " + (currentSolutionIndex + 1) +
                " of " + queensSolutions.size());
        drawSolution(sol);
    }

    // ── Queens animation ──────────────────────────────────────────────────────

    private void toggleQueensPlay() {
        if (queensTimeline != null && queensTimeline.getStatus() == Timeline.Status.RUNNING) {
            queensTimeline.pause();
            playPauseBtn.setText("▶ Play");
        } else {
            if (animationSteps.isEmpty()) {
                doFindSolutions();
            }
            playQueensAnimation();
        }
    }

    private void playQueensAnimation() {
        stopQueensAnimation();
        playPauseBtn.setText("⏸ Pause");

        queensTimeline = new Timeline(new KeyFrame(
                Duration.millis(speedSlider.getValue()),
                e -> {
                    if (animStepIndex >= animationSteps.size()) {
                        stopQueensAnimation();
                        playPauseBtn.setText("▶ Play");
                        queensStatusLabel.setText("Animation complete.");
                        return;
                    }
                    applyAnimStep(animStepIndex++);
                }));
        queensTimeline.setCycleCount(Timeline.INDEFINITE);
        queensTimeline.play();
    }

    private void stopQueensAnimation() {
        if (queensTimeline != null) {
            queensTimeline.stop();
        }
    }

    private void resetQueensAnimation() {
        stopQueensAnimation();
        playPauseBtn.setText("▶ Play");
        animStepIndex = 0;
        if (!animationSteps.isEmpty()) {
            applyAnimStep(0);
        } else {
            drawEmptyBoard();
        }
    }

    private void stepQueens(int direction) {
        if (animationSteps.isEmpty())
            doFindSolutions();
        stopQueensAnimation();
        playPauseBtn.setText("▶ Play");
        animStepIndex = Math.max(0, Math.min(animStepIndex + direction, animationSteps.size() - 1));
        applyAnimStep(animStepIndex);
    }

    private void applyAnimStep(int step) {
        if (step >= animationSteps.size())
            return;
        AnimStep snap = animationSteps.get(step);
        drawStepSnapshot(snap);

        String stateDesc;
        switch (snap.cellStatus) {
            case 1:
                stateDesc = "Trying (" + snap.row + "," + snap.col + ") [Yellow]";
                break;
            case 2:
                stateDesc = "Conflict at (" + snap.row + "," + snap.col + ") [Red]";
                break;
            case 3:
                stateDesc = "Safe! Placed at (" + snap.row + "," + snap.col + ") [Green]";
                break;
            case 4:
                stateDesc = "Backtracking from (" + snap.row + "," + snap.col + ")";
                break;
            default:
                stateDesc = "";
        }
        queensStatusLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_PRIMARY + ";");
        queensStatusLabel.setText("Step " + (step + 1) + "/" + animationSteps.size() +
                " – Row " + snap.row + " – " + stateDesc);
    }

    /**
     * Builds an instrumented step trace of backtracking:
     * yellow = trying, red = invalid, green = placed, light red = backtracked.
     */
    private List<AnimStep> buildAnimationSteps(int n) {
        List<AnimStep> steps = new ArrayList<>();
        int[] board = new int[n];
        Arrays.fill(board, -1);
        recordSteps(board, 0, n, steps);
        return steps;
    }

    private void recordSteps(int[] board, int row, int n, List<AnimStep> steps) {
        // Cap step count to 2000 so animation stays snappy
        if (steps.size() > 2000)
            return;

        if (row == n) {
            // Found a full solution
            steps.add(new AnimStep(board, row - 1, board[row - 1], 3));
            return;
        }

        for (int col = 0; col < n; col++) {
            if (steps.size() > 2000)
                return;

            // 1. Trying: yellow
            steps.add(new AnimStep(board, row, col, 1));

            if (!queensAlgo.isSafe(board, row, col)) {
                // 2. Conflict: red
                steps.add(new AnimStep(board, row, col, 2));
                continue;
            }

            // 3. Safe & Placed: green
            board[row] = col;
            steps.add(new AnimStep(board, row, col, 3));

            // Recurse to next row
            recordSteps(board, row + 1, n, steps);

            // 4. Backtrack: unplace
            board[row] = -1;
            steps.add(new AnimStep(board, row, col, 4));
        }
    }

    // ── Robot path actions ────────────────────────────────────────────────────

    private void doGenerateObstacles() {
        stopRobotAnimation();
        robotPath.clear();
        robotAnimPath.clear();
        robotAnimStep = 0;
        robotObstacles = robotAlgo.generateDefaultObstacles(8, 8);
        robotStatusLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_SECONDARY + ";");
        robotStatusLabel.setText("Obstacles generated – press 'Find Path'");
        drawRobotGrid();
    }

    private void doFindRobotPath() {
        stopRobotAnimation();
        robotPath = new ArrayList<>(robotAlgo.findPath(robotObstacles));
        robotAnimPath.clear();
        robotAnimStep = 0;
        if (robotPath.isEmpty()) {
            robotStatusLabel.setStyle("-fx-text-fill: " + StyleConstants.ACCENT_RED + ";");
            robotStatusLabel.setText("No solution found");
        } else {
            robotStatusLabel.setStyle("-fx-text-fill: " + StyleConstants.ACCENT_GREEN + ";");
            robotStatusLabel.setText("Path found! " + robotPath.size() + " steps. Press 'Animate Path'.");
        }
        drawRobotGrid();
    }

    private void doAnimateRobotPath() {
        if (robotPath.isEmpty()) {
            doFindRobotPath();
            if (robotPath.isEmpty())
                return;
        }
        stopRobotAnimation();
        robotAnimPath.clear();
        robotAnimStep = 0;
        robotStatusLabel.setStyle("-fx-text-fill: " + StyleConstants.TEXT_PRIMARY + ";");
        robotStatusLabel.setText("Animating robot motion…");

        robotTimeline = new Timeline(new KeyFrame(
                Duration.millis(speedSlider.getValue()),
                e -> {
                    if (robotAnimStep >= robotPath.size()) {
                        stopRobotAnimation();
                        robotStatusLabel.setStyle("-fx-text-fill: " + StyleConstants.ACCENT_GREEN + ";");
                        robotStatusLabel.setText("Destination reached! Path length: " + robotPath.size());
                        return;
                    }
                    robotAnimPath.add(robotPath.get(robotAnimStep++));
                    drawRobotGrid();
                }));
        robotTimeline.setCycleCount(Timeline.INDEFINITE);
        robotTimeline.play();
    }

    private void stopRobotAnimation() {
        if (robotTimeline != null)
            robotTimeline.stop();
    }

    // ── Queens drawing ────────────────────────────────────────────────────────

    private void drawEmptyBoard() {
        int[] emptyBoard = new int[currentN];
        Arrays.fill(emptyBoard, -1);
        drawBoardState(emptyBoard, true, -1, -1, 0);
    }

    private void drawSolution(int[] board) {
        drawBoardState(board, true, -1, -1, 0);
    }

    private void drawStepSnapshot(AnimStep snap) {
        drawBoardState(snap.boardSnapshot, true, snap.row, snap.col, snap.cellStatus);
    }

    private void redrawQueensBoard() {
        if (!animationSteps.isEmpty() && animStepIndex > 0 && animStepIndex <= animationSteps.size()) {
            drawStepSnapshot(animationSteps.get(animStepIndex - 1));
        } else if (!queensSolutions.isEmpty() && currentSolutionIndex < queensSolutions.size()) {
            drawBoardState(queensSolutions.get(currentSolutionIndex), true, -1, -1, 0);
        } else {
            int[] emptyBoard = new int[currentN];
            Arrays.fill(emptyBoard, -1);
            drawBoardState(emptyBoard, true, -1, -1, 0);
        }
    }

    /**
     * Core chessboard render routine.
     *
     * @param board        board[row] = col, -1 = empty
     * @param drawQueens   whether to render queen glyphs
     * @param activeRow    row currently active in animation (-1 if none)
     * @param activeCol    col currently active in animation (-1 if none)
     * @param activeStatus status of active cell: 1=trying (yellow), 2=invalid
     *                     (red), 3=placed (green), 4=backtrack
     */
    private void drawBoardState(int[] board, boolean drawQueens,
            int activeRow, int activeCol, int activeStatus) {
        int n = board.length;
        double cellSize = (double) QUEENS_CANVAS_SIZE / n;
        GraphicsContext gc = queensCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, QUEENS_CANVAS_SIZE, QUEENS_CANVAS_SIZE);

        // 1. Draw alternating chessboard squares
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                boolean lightSquare = (r + c) % 2 == 0;
                gc.setFill(lightSquare ? LIGHT_SQUARE : DARK_SQUARE);
                gc.fillRect(c * cellSize, r * cellSize, cellSize, cellSize);
            }
        }

        // 2. Highlight attacked cells if hovering over a queen
        if (hoverRow >= 0 && hoverRow < n && hoverCol >= 0 && hoverCol < n
                && drawQueens && board[hoverRow] == hoverCol) {
            gc.setFill(ATTACKED_COLOR);
            for (int r = 0; r < n; r++) {
                // Column & Row
                gc.fillRect(hoverCol * cellSize, r * cellSize, cellSize, cellSize);
                gc.fillRect(0, hoverRow * cellSize, QUEENS_CANVAS_SIZE, cellSize);
                // Diagonals
                for (int c = 0; c < n; c++) {
                    if (Math.abs(r - hoverRow) == Math.abs(c - hoverCol)) {
                        gc.fillRect(c * cellSize, r * cellSize, cellSize, cellSize);
                    }
                }
            }
        }

        // 3. Highlight currently placed queens cells (green background)
        for (int r = 0; r < n; r++) {
            if (board[r] >= 0) {
                gc.setFill(PLACED_COLOR);
                gc.fillRect(board[r] * cellSize, r * cellSize, cellSize, cellSize);
            }
        }

        // 4. Highlight active animated cell (trying, invalid, placed, backtrack)
        if (activeRow >= 0 && activeCol >= 0) {
            Color statusColor;
            switch (activeStatus) {
                case 1:
                    statusColor = TRY_COLOR;
                    break; // yellow: trying
                case 2:
                    statusColor = INVALID_COLOR;
                    break; // red: invalid conflict
                case 3:
                    statusColor = PLACED_COLOR;
                    break; // green: placed
                case 4:
                    statusColor = BACKTRACK_COLOR;
                    break; // light red: backtracked
                default:
                    statusColor = null;
            }
            if (statusColor != null) {
                gc.setFill(statusColor);
                gc.fillRect(activeCol * cellSize, activeRow * cellSize, cellSize, cellSize);
            }
        }

        // 5. Draw queens
        if (drawQueens) {
            for (int r = 0; r < n; r++) {
                int col = board[r];
                if (col < 0)
                    continue;
                boolean lightSq = (r + col) % 2 == 0;
                gc.setFill(lightSq ? QUEEN_LIGHT : QUEEN_DARK);

                // Draw filled circle
                double margin = cellSize * 0.12;
                double diameter = cellSize - 2 * margin;
                gc.fillOval(col * cellSize + margin, r * cellSize + margin, diameter, diameter);

                // Draw ♛ symbol centered
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("System", FontWeight.BOLD, cellSize * 0.48));
                double textX = col * cellSize + cellSize * 0.28;
                double textY = r * cellSize + cellSize * 0.68;
                gc.fillText("♛", textX, textY);
            }
        }

        // 6. Draw grid lines
        gc.setStroke(Color.web("#00000028"));
        gc.setLineWidth(0.75);
        for (int i = 0; i <= n; i++) {
            gc.strokeLine(i * cellSize, 0, i * cellSize, QUEENS_CANVAS_SIZE);
            gc.strokeLine(0, i * cellSize, QUEENS_CANVAS_SIZE, i * cellSize);
        }
    }

    // ── Robot grid drawing ────────────────────────────────────────────────────

    private void drawRobotGrid() {
        int rows = 8, cols = 8;
        double cellSize = (double) ROBOT_CANVAS_SIZE / cols;
        GraphicsContext gc = robotCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, ROBOT_CANVAS_SIZE, ROBOT_CANVAS_SIZE);

        // Build path & visited lookup
        boolean[][] onPath = new boolean[rows][cols];
        boolean[][] onVisited = new boolean[rows][cols];
        for (int[] step : robotAnimPath) {
            onPath[step[0]][step[1]] = true;
        }
        boolean[][] vis = robotAlgo.getVisited();
        if (vis != null) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    onVisited[r][c] = vis[r][c];
                }
            }
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double x = c * cellSize;
                double y = r * cellSize;

                Color fill;
                if (r == 0 && c == 0) {
                    fill = Color.web(StyleConstants.ACCENT_RED); // start: red
                } else if (r == rows - 1 && c == cols - 1) {
                    fill = Color.CYAN; // end: cyan
                } else if (robotObstacles[r][c]) {
                    fill = Color.web("#374151"); // obstacle: dark grey
                } else if (onPath[r][c]) {
                    fill = Color.web(StyleConstants.ACCENT_GREEN); // path: green
                } else if (onVisited[r][c] && !robotPath.isEmpty()) {
                    fill = Color.web("#BFDBFE"); // visited: light blue
                } else {
                    fill = Color.WHITE; // free: white
                }

                gc.setFill(fill);
                gc.fillRect(x + 1, y + 1, cellSize - 2, cellSize - 2);
            }
        }

        // Draw robot icon on last animated position
        if (!robotAnimPath.isEmpty()) {
            int[] pos = robotAnimPath.get(robotAnimPath.size() - 1);
            gc.setFont(Font.font("System", cellSize * 0.55));
            gc.fillText("🤖", pos[1] * cellSize + cellSize * 0.12,
                    pos[0] * cellSize + cellSize * 0.72);
        }

        // Grid lines
        gc.setStroke(Color.web("#9CA3AF"));
        gc.setLineWidth(0.8);
        for (int i = 0; i <= cols; i++) {
            gc.strokeLine(i * cellSize, 0, i * cellSize, ROBOT_CANVAS_SIZE);
            gc.strokeLine(0, i * cellSize, ROBOT_CANVAS_SIZE, i * cellSize);
        }

        // Legend
        gc.setFill(Color.web(StyleConstants.TEXT_SECONDARY));
        gc.setFont(Font.font("System", 10));
        gc.fillText("Red=Start  Cyan=End  DarkGrey=Obstacle  Green=Path  LightBlue=Visited", 4, ROBOT_CANVAS_SIZE - 4);
    }

    // ── Helper builders ───────────────────────────────────────────────────────

    /** Styled card-style VBox. */
    private VBox cardBox() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(StyleConstants.PADDING));
        box.setStyle(
                "-fx-background-color: " + StyleConstants.BG_CARD + ";" +
                        "-fx-background-radius: " + StyleConstants.RADIUS + ";" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 6, 0, 0, 1);");
        return box;
    }

    /** Wraps a canvas in a centred VBox card. */
    private VBox centeredCanvas(Canvas canvas) {
        VBox box = new VBox(canvas);
        box.setAlignment(Pos.CENTER);
        box.setStyle(
                "-fx-background-color: " + StyleConstants.BG_CARD + ";" +
                        "-fx-background-radius: " + StyleConstants.RADIUS + ";" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 6, 0, 0, 1);");
        box.setPadding(new Insets(10));
        return box;
    }

    /** Creates a styled button. */
    private Button styledButton(String text, String hexColor) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: " + hexColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 7 14 7 14;");
        return btn;
    }
}
