package com.daa.lab.modules.hamiltonian;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure algorithm class for finding a Hamiltonian Cycle via backtracking.
 *
 * <p>A Hamiltonian Cycle is a closed path in a graph that visits every vertex
 * exactly once and returns to the starting vertex. This is fundamental in
 * PCB circuit routing where every connection point must be visited once.
 *
 * <p>Algorithm Overview (Backtracking):
 * <ol>
 *   <li>Start at vertex 0 (always fixed as the starting point).</li>
 *   <li>At each position in the path, try every vertex not yet visited.</li>
 *   <li>A vertex is "safe" if it is adjacent to the previous vertex AND not
 *       already in the path ("visiting each vertex exactly once" constraint).</li>
 *   <li>When all n vertices are placed, check that the last vertex has an edge
 *       back to vertex 0 — this closes the cycle.</li>
 *   <li>If no vertex can be placed at the current position, backtrack.</li>
 * </ol>
 *
 * <p>Time complexity: O(N!) in the worst case (all permutations tried).
 * Space complexity: O(N) for the path and visited arrays.
 */
public class HamiltonianAlgorithm {

    /** Number of vertices in the graph. */
    private int n;

    /** Adjacency matrix: graph[u][v] == 1 means there is an edge u–v. */
    private int[][] graph;

    /**
     * The current path being built. path[i] holds the vertex at position i.
     * path[0] is always 0 (the start vertex).
     */
    private int[] path;

    /**
     * Boolean flags: inPath[v] == true means vertex v has already been placed
     * in the path and must not be visited again (the "exactly once" constraint).
     */
    private boolean[] inPath;

    // ── Step-recording for animated playback ─────────────────────────────────

    /**
     * Each AnimationStep records one decision point during the backtracking
     * search, allowing the UI to replay the algorithm step by step.
     */
    public static class AnimationStep {
        /** The partial path at this moment in the search. */
        public final int[] pathSnapshot;
        /** The vertex being tested at this step (-1 if none). */
        public final int currentVertex;
        /** Whether this step was a successful placement (true) or a backtrack (false). */
        public final boolean placed;
        /** Human-readable description of what happened. */
        public final String description;

        public AnimationStep(int[] pathSnapshot, int currentVertex, boolean placed, String description) {
            this.pathSnapshot = pathSnapshot.clone();
            this.currentVertex = currentVertex;
            this.placed = placed;
            this.description = description;
        }
    }

    /** All recorded steps from the last {@link #solve(int[][])} call. */
    private List<AnimationStep> steps;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Runs the Hamiltonian Cycle backtracking algorithm on the given adjacency matrix.
     *
     * @param adjacencyMatrix square 0/1 matrix; adjacencyMatrix[u][v]==1 iff edge u–v exists
     * @return the Hamiltonian cycle path as an int[] (length n+1, first==last==0)
     *         if one exists, or {@code null} if the graph has no Hamiltonian cycle
     */
    public int[] solve(int[][] adjacencyMatrix) {
        this.graph = adjacencyMatrix;
        this.n     = adjacencyMatrix.length;
        this.path  = new int[n];
        this.inPath = new boolean[n];
        this.steps  = new ArrayList<>();

        // Fill path with -1 to indicate "empty" slots
        for (int i = 0; i < n; i++) path[i] = -1;

        // Always start the Hamiltonian cycle at vertex 0
        path[0] = 0;
        inPath[0] = true;

        // Record the initial placement of vertex 0
        steps.add(new AnimationStep(path, 0, true, "Start at vertex 0"));

        if (backtrack(1)) {
            // Build result array: path[0..n-1] + closing vertex 0
            int[] result = new int[n + 1];
            System.arraycopy(path, 0, result, 0, n);
            result[n] = 0; // close the cycle back to vertex 0
            return result;
        }
        return null; // no Hamiltonian cycle found
    }

    /**
     * Recursive backtracking: tries to fill position {@code pos} in the path.
     *
     * <p>When {@code pos == n} all vertices have been placed and we perform the
     * <em>cycle closure check</em>: the last vertex in the path must have an edge
     * back to vertex 0, completing the circuit. Without this check we would only
     * have a Hamiltonian <em>path</em>, not a <em>cycle</em>.
     *
     * @param pos the current position (1-based; position 0 is always vertex 0)
     * @return {@code true} if a complete Hamiltonian cycle was found from this state
     */
    private boolean backtrack(int pos) {
        // --- Cycle closure check ---
        // All n vertices have been placed. Check whether the last vertex (path[n-1])
        // has an edge back to vertex 0 to complete the cycle.
        if (pos == n) {
            boolean closed = graph[path[n - 1]][0] == 1;
            if (closed) {
                steps.add(new AnimationStep(path, 0, true,
                        "Cycle closed! Edge " + path[n - 1] + "→0 exists."));
            }
            return closed;
        }

        // Try every vertex as a candidate for position pos
        for (int v = 1; v < n; v++) { // vertex 0 is already at position 0
            if (isSafe(v, pos)) {
                // Place vertex v at position pos
                path[pos]  = v;
                inPath[v]  = true;
                steps.add(new AnimationStep(path, v, true,
                        "Place vertex " + v + " at position " + pos));

                // Recurse for the next position
                if (backtrack(pos + 1)) {
                    return true;
                }

                // --- Backtrack ---
                // Placing v here did not lead to a solution; remove it and try next.
                steps.add(new AnimationStep(path, v, false,
                        "Backtrack from vertex " + v + " at position " + pos));
                path[pos]  = -1;
                inPath[v]  = false;
            }
        }
        return false; // no vertex worked at this position
    }

    /**
     * Checks whether {@code vertex} can be safely placed at position {@code pos}.
     *
     * <p>Two conditions must both be satisfied:
     * <ol>
     *   <li><b>Adjacency:</b> there must be an edge from the previously placed
     *       vertex ({@code path[pos-1]}) to {@code vertex} in the graph. Without
     *       an edge the routing path would be invalid on the PCB.</li>
     *   <li><b>Not already in path:</b> {@code vertex} must not have been visited
     *       yet ({@code inPath[vertex] == false}). This enforces the "each vertex
     *       exactly once" constraint that defines a Hamiltonian cycle.</li>
     * </ol>
     *
     * @param vertex the candidate vertex to add next
     * @param pos    the position we are trying to fill (≥ 1)
     * @return {@code true} iff placing {@code vertex} at {@code pos} is legal
     */
    private boolean isSafe(int vertex, int pos) {
        // Adjacency check: must have an edge from the last placed vertex
        if (graph[path[pos - 1]][vertex] == 0) {
            return false;
        }
        // "Exactly once" check: vertex must not already be in the path
        return !inPath[vertex];
    }

    // ── Graph generators ─────────────────────────────────────────────────────

    /**
     * Returns an adjacency matrix for a 6-vertex graph that is guaranteed to
     * contain a Hamiltonian cycle.
     *
     * <p>Structure: a cycle 0–1–2–3–4–5–0, plus extra chords so the graph is
     * not trivially sparse. The Hamiltonian cycle is 0→1→2→3→4→5→0.
     *
     * @param n ignored; always returns a 6×6 matrix for the demo
     * @return 6×6 adjacency matrix
     */
    public int[][] generateDefaultGraph(int n) {
        // 6-vertex graph with Hamiltonian cycle 0-1-2-3-4-5-0
        // Extra edges added for visual richness
        return new int[][] {
            {0, 1, 0, 0, 0, 1}, // 0 connects to 1, 5
            {1, 0, 1, 0, 0, 1}, // 1 connects to 0, 2, 5
            {0, 1, 0, 1, 1, 0}, // 2 connects to 1, 3, 4
            {0, 0, 1, 0, 1, 0}, // 3 connects to 2, 4
            {0, 0, 1, 1, 0, 1}, // 4 connects to 2, 3, 5
            {1, 1, 0, 0, 1, 0}  // 5 connects to 0, 1, 4
        };
    }

    /**
     * Returns an adjacency matrix for an {@code n}-vertex graph that has
     * <em>no</em> Hamiltonian cycle, demonstrated by disconnecting the graph.
     *
     * <p>Implementation: creates two disjoint cliques. With vertices partitioned
     * into two disconnected groups, it is impossible to visit every vertex and
     * return to the start through a single closed path.
     *
     * @param n number of vertices (minimum 4 for two non-trivial cliques)
     * @return n×n adjacency matrix with no Hamiltonian cycle
     */
    public int[][] generateNoHamiltonianGraph(int n) {
        int[][] mat = new int[n][n];
        int half = n / 2;
        // First clique: vertices 0 .. half-1 (fully connected among themselves)
        for (int i = 0; i < half; i++) {
            for (int j = 0; j < half; j++) {
                if (i != j) mat[i][j] = 1;
            }
        }
        // Second clique: vertices half .. n-1 (fully connected among themselves)
        for (int i = half; i < n; i++) {
            for (int j = half; j < n; j++) {
                if (i != j) mat[i][j] = 1;
            }
        }
        // No edges between the two cliques → no Hamiltonian cycle possible
        return mat;
    }

    /**
     * Builds an adjacency matrix from a comma-separated edge list string.
     * Format: "0-1,1-2,2-3" → edges 0–1, 1–2, 2–3 (undirected).
     *
     * @param edgeList edge list string
     * @param numVertices total number of vertices
     * @return adjacency matrix, or a default graph on parse error
     */
    public int[][] parseEdgeList(String edgeList, int numVertices) {
        int[][] mat = new int[numVertices][numVertices];
        try {
            String[] edges = edgeList.trim().split(",");
            for (String edge : edges) {
                edge = edge.trim();
                if (edge.isEmpty()) continue;
                String[] parts = edge.split("-");
                int u = Integer.parseInt(parts[0].trim());
                int v = Integer.parseInt(parts[1].trim());
                if (u >= 0 && u < numVertices && v >= 0 && v < numVertices) {
                    mat[u][v] = 1;
                    mat[v][u] = 1;
                }
            }
        } catch (Exception e) {
            // On any parse error, fall back to the default graph
            return generateDefaultGraph(numVertices);
        }
        return mat;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Returns all recorded animation steps from the last {@link #solve} call. */
    public List<AnimationStep> getSteps() {
        return steps;
    }

    /** Returns the number of vertices set by the last {@link #solve} call. */
    public int getN() {
        return n;
    }
}
