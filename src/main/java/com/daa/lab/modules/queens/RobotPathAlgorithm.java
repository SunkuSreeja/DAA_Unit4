package com.daa.lab.modules.queens;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pure algorithm class (no JavaFX) that finds a path for a robot navigating
 * an N×M grid from the top-left corner (0,0) to the bottom-right corner
 * (rows-1, cols-1), avoiding obstacle cells.
 *
 * <p>Uses recursive depth-first search with backtracking. If multiple paths
 * exist, the first one found (in the direction order: right, down, left, up)
 * is returned.</p>
 *
 * <p>Usage:
 * <pre>
 *   RobotPathAlgorithm rpa = new RobotPathAlgorithm(8, 8);
 *   boolean[][] obstacles = rpa.generateDefaultObstacles(8, 8);
 *   List&lt;int[]&gt; path = rpa.findPath(obstacles);
 *   // path.get(i) == int[]{row, col}
 * </pre>
 * </p>
 */
public class RobotPathAlgorithm {

    // ── Fields ────────────────────────────────────────────────────────────────

    /** Number of grid rows. */
    private final int rows;

    /** Number of grid columns. */
    private final int cols;

    /**
     * Obstacle map: {@code obstacles[r][c] == true} means cell (r,c) is blocked.
     * Set during {@link #findPath}.
     */
    private boolean[][] obstacles;

    /**
     * The discovered path, as a list of {row, col} pairs from (0,0) to
     * (rows-1, cols-1). Empty if no path exists.
     */
    private List<int[]> path;

    /**
     * Visited flags used during DFS to avoid revisiting cells.
     * Reset at the start of each {@link #findPath} call.
     */
    private boolean[][] visited;

    /**
     * Movement directions: right, down, left, up.
     * Stored as {rowDelta, colDelta} pairs.
     */
    private static final int[][] DIRECTIONS = {
        { 0,  1},   // right
        { 1,  0},   // down
        { 0, -1},   // left
        {-1,  0}    // up
    };

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Creates a new robot-path solver for a grid of the given dimensions.
     *
     * @param rows number of rows
     * @param cols number of columns
     */
    public RobotPathAlgorithm(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
    }

    /** Convenience constructor for the default 8×8 grid. */
    public RobotPathAlgorithm() {
        this(8, 8);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Finds a path from (0,0) to (rows-1, cols-1) avoiding the given obstacles.
     *
     * <p>Resets visited flags and the path list, then starts DFS from the
     * top-left corner. Returns the path if one exists, or an empty list.</p>
     *
     * @param obstacles obstacle map ({@code true} = blocked); must be rows×cols
     * @return list of {row, col} steps, or an empty list if no path exists
     */
    public List<int[]> findPath(boolean[][] obstacles) {
        this.obstacles = obstacles;
        this.path      = new ArrayList<>();
        this.visited   = new boolean[rows][cols];

        // Start DFS from the top-left cell
        boolean found = explore(0, 0);
        return found ? List.copyOf(path) : List.of();
    }

    /**
     * Recursive DFS step.
     *
     * <p>Algorithm:</p>
     * <ol>
     *   <li><b>Boundary / obstacle / visited check</b> – prune invalid cells.</li>
     *   <li><b>Mark visited</b> – prevent cycles.</li>
     *   <li><b>Record step</b> – add cell to path.</li>
     *   <li><b>Goal check</b> – return {@code true} if destination reached.</li>
     *   <li><b>Recurse</b> – try each direction in order.</li>
     *   <li><b>Backtrack</b> – if no direction succeeded, remove cell from
     *       path and unmark visited so the caller can try another direction.</li>
     * </ol>
     *
     * @param row current row
     * @param col current column
     * @return {@code true} if the goal was reached through (row, col)
     */
    public boolean explore(int row, int col) {
        // ── Visited check: skip if out-of-bounds, obstacle, or already visited ─
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return false;
        }
        if (obstacles[row][col]) {
            return false;   // blocked cell – do not enter
        }
        if (visited[row][col]) {
            return false;   // already explored via another path branch
        }

        // Mark this cell as visited to avoid revisiting it in this DFS branch
        visited[row][col] = true;

        // Add cell to current path candidate
        path.add(new int[]{row, col});

        // Goal check: reached bottom-right corner
        if (row == rows - 1 && col == cols - 1) {
            return true;    // path is complete
        }

        // Recursive step: try each direction (right, down, left, up)
        for (int[] dir : DIRECTIONS) {
            int nextRow = row + dir[0];
            int nextCol = col + dir[1];

            if (explore(nextRow, nextCol)) {
                return true;   // found a valid path through this direction
            }
        }

        // Backtrack: this cell led to a dead end → unmark visited and remove from path
        visited[row][col] = false;
        path.remove(path.size() - 1);
        return false;
    }

    /**
     * Generates a reproducible obstacle layout for the default grid size.
     *
     * <p>Approximately 20% of interior cells are randomly blocked. A fixed
     * random seed ensures the same layout appears on every run. The start
     * (0,0) and goal (rows-1, cols-1) cells are always kept clear. The
     * generator retries until a solvable layout is found, so callers are
     * guaranteed a non-empty path.</p>
     *
     * @param rows grid height
     * @param cols grid width
     * @return a boolean[][] obstacle map guaranteed to have at least one path
     */
    public boolean[][] generateDefaultObstacles(int rows, int cols) {
        // Seed the PRNG for reproducibility across runs
        Random rng = new Random(42L);

        boolean[][] obs;
        int attempts = 0;

        do {
            obs = new boolean[rows][cols];

            // Place obstacles with ~20% probability on interior cells
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    // Always keep start and goal clear
                    if ((r == 0 && c == 0) || (r == rows - 1 && c == cols - 1)) {
                        obs[r][c] = false;
                    } else {
                        obs[r][c] = (rng.nextInt(10) < 2); // ~20%
                    }
                }
            }

            attempts++;
            // Safety valve: after too many attempts, return a clear grid
            if (attempts > 100) {
                return new boolean[rows][cols];
            }

        } while (findPath(obs).isEmpty()); // retry until a path exists

        return obs;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** Returns the last path found (empty if {@link #findPath} was not called or failed). */
    public List<int[]> getPath() {
        return path == null ? List.of() : List.copyOf(path);
    }

    /** Returns the visited map from the last {@link #findPath} call. */
    public boolean[][] getVisited() {
        return visited;
    }

    /** Grid row count. */
    public int getRows() { return rows; }

    /** Grid column count. */
    public int getCols() { return cols; }
}
