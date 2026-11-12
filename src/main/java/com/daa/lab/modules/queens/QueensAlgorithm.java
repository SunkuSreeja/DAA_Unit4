package com.daa.lab.modules.queens;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pure algorithm class (no JavaFX) implementing N-Queens backtracking.
 *
 * <p>Models a factory-floor scenario where N robots (queens) must be placed
 * on an N×N grid so that no two robots threaten each other — i.e., no two
 * share a row, column, or diagonal.</p>
 *
 * <p>Usage:
 * <pre>
 *   QueensAlgorithm qa = new QueensAlgorithm();
 *   List&lt;int[]&gt; solutions = qa.solve(8);
 *   // solutions.get(i)[row] == column position of queen in that row
 * </pre>
 * </p>
 */
public class QueensAlgorithm {

    // ── Fields ────────────────────────────────────────────────────────────────

    /** Board size (number of queens). Default: 8. */
    private int n = 8;

    /** Current board state: board[row] = column where the queen is placed. */
    private int[] board;

    /** All complete, valid placements found during the last {@link #solve} call. */
    private List<int[]> solutions;

    /**
     * Counts every individual step taken during backtracking
     * (a "step" is any attempt to place a queen in a cell).
     */
    private int stepsCount;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Solves the N-Queens problem for the given board size.
     *
     * <p>Resets internal state, then explores all column choices for every row
     * through recursive backtracking. Every complete solution (all N rows filled)
     * is added to {@link #solutions}.</p>
     *
     * @param n board dimension (number of queens)
     * @return an unmodifiable view of all solutions found
     */
    public List<int[]> solve(int n) {
        this.n = n;
        this.board = new int[n];
        Arrays.fill(this.board, -1);   // -1 = no queen placed yet
        this.solutions = new ArrayList<>();
        this.stepsCount = 0;

        // Start placing from row 0
        backtrack(board, 0, solutions);

        return List.copyOf(solutions);
    }

    /**
     * Checks whether placing a queen at {@code (row, col)} is safe given the
     * queens already placed in rows {@code 0 .. row-1}.
     *
     * <p>A placement is unsafe if:</p>
     * <ul>
     *   <li>another queen occupies the same column, or</li>
     *   <li>another queen is on the same diagonal (|rowDiff| == |colDiff|).</li>
     * </ul>
     * Row conflicts are impossible because we place exactly one queen per row.
     *
     * @param board current board state
     * @param row   row index to test
     * @param col   column index to test
     * @return {@code true} if the placement is safe
     */
    public boolean isSafe(int[] board, int row, int col) {
        for (int prevRow = 0; prevRow < row; prevRow++) {
            int prevCol = board[prevRow];

            // Check same column conflict
            if (prevCol == col) {
                return false;
            }

            // Check diagonal conflict (both directions)
            int rowDiff = row - prevRow;
            int colDiff = Math.abs(col - prevCol);
            if (rowDiff == colDiff) {
                return false;
            }
        }
        return true;
    }

    /** Returns all solutions found by the last {@link #solve} call. */
    public List<int[]> getSolutions() {
        return solutions == null ? List.of() : List.copyOf(solutions);
    }

    /** Returns the total number of placement attempts made during backtracking. */
    public int getStepCount() {
        return stepsCount;
    }

    /** Returns the board size used in the last {@link #solve} call. */
    public int getN() {
        return n;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Recursive backtracking engine.
     *
     * <p>For each {@code row}, try every column from 0 to N-1:</p>
     * <ol>
     *   <li><b>Check safety</b> – skip the column if it conflicts with an
     *       already-placed queen.</li>
     *   <li><b>Place</b> – record the column choice for this row.</li>
     *   <li><b>Recurse</b> – move to the next row; if all rows are filled, a
     *       solution has been found.</li>
     *   <li><b>Backtrack</b> – remove the queen (reset cell to -1) so the
     *       parent call can try the next column.</li>
     * </ol>
     *
     * @param board     current board state (modified in-place)
     * @param row       the row being processed in this call frame
     * @param solutions accumulates complete solutions
     */
    private void backtrack(int[] board, int row, List<int[]> solutions) {
        // ── Base case: all N rows have a queen → record this solution ──────────
        if (row == n) {
            solutions.add(Arrays.copyOf(board, n));
            return;
        }

        // ── Try every column in the current row ───────────────────────────────
        for (int col = 0; col < n; col++) {
            stepsCount++; // count every attempt

            // Check whether placing a queen at (row, col) is safe
            if (!isSafe(board, row, col)) {
                // Conflict detected – skip this column, backtracking implicitly
                continue;
            }

            // Safe to place: assign the queen's column position
            board[row] = col;

            // Recurse to place queens in subsequent rows
            backtrack(board, row + 1, solutions);

            // Backtrack: remove the queen so the next column can be tried
            board[row] = -1;
        }
    }
}
