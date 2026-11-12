package com.daa.lab.modules.tsp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

/**
 * TspAlgorithm – Branch and Bound solution for the Travelling Salesman Problem.
 *
 * <p><b>Domain context:</b> Air cargo routing – find the cheapest route that visits every
 * airport exactly once and returns to the starting hub, minimising total fuel cost.
 *
 * <p><b>Branch and Bound overview:</b>
 * <ol>
 *   <li>Start at airport 0 (the cargo hub).</li>
 *   <li>At each step, compute a lower-bound on the remaining cost of completing the route
 *       ({@link #computeLowerBound}). This bound is always ≤ the actual remaining cost.</li>
 *   <li><b>Prune:</b> If {@code bound + currentCost ≥ bestCost}, this branch cannot improve the
 *       known best solution, so we discard it immediately — saving exponential work.</li>
 *   <li>Otherwise, <b>branch</b>: for each unvisited airport, extend the partial route and push
 *       the new node onto the min-heap ordered by its bound.</li>
 *   <li>When a complete route (all airports visited + return edge) is found, update
 *       {@code bestCost}/{@code bestPath} if it is cheaper.</li>
 *   <li>Repeat until the priority queue is empty (all promising branches exhausted).</li>
 * </ol>
 */
public class TspAlgorithm {

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    /** Number of airports in the problem. */
    private int n;

    /** Cost/distance matrix; dist[i][j] = dist[j][i] = km between airports i and j. */
    private int[][] dist;

    /** Cost of the best complete route found so far; initialised to MAX_VALUE. */
    private int bestCost;

    /** City sequence of the best complete route (0-indexed, length n+1, starts and ends at 0). */
    private int[] bestPath;

    /**
     * Every B&B tree node that was <em>expanded</em> (not pruned on creation), stored in
     * expansion order. Used by the visualisation layer to replay the search tree.
     */
    private List<TspNode> exploredNodes;

    /** Number of branches discarded via the pruning condition. */
    private int prunedCount;

    /** Number of B&B nodes actually expanded (dequeued and processed). */
    private int exploredCount;

    // -----------------------------------------------------------------------
    // Inner class – B&B tree node
    // -----------------------------------------------------------------------

    /**
     * A single node in the Branch-and-Bound search tree.
     *
     * <p><b>level</b>: depth in the tree = number of airports fixed in {@code path} so far.
     * Level 0 → only the start city is in the path; level n-1 → all cities are fixed and we
     * only need to add the return edge to close the tour.
     */
    public static class TspNode implements Comparable<TspNode> {
        /** Depth in the B&B tree (= cities committed to the route). */
        public int level;
        /** Actual travel cost accumulated so far on this partial route. */
        public int cost;
        /**
         * Lower bound on the total tour cost if we complete this partial route optimally.
         * The bound is what the priority queue sorts by: expanding cheapest-bound nodes
         * first (best-first search) finds good solutions early and prunes more aggressively.
         */
        public int bound;
        /** Partial route: path[0..level] are the airports visited in order. */
        public int[] path;
        /** visited[i] is true when airport i has already been added to the partial route. */
        public boolean[] visited;
        /** True if this node was pruned (bound ≥ bestCost at time of creation). */
        public boolean pruned;

        public TspNode(int n) {
            path    = new int[n + 1]; // extra slot for the closing return edge
            visited = new boolean[n];
        }

        /** Min-heap ordering by lower bound. */
        @Override
        public int compareTo(TspNode other) {
            return Integer.compare(this.bound, other.bound);
        }
    }

    // -----------------------------------------------------------------------
    // Core solver
    // -----------------------------------------------------------------------

    /**
     * Solve the TSP using Branch and Bound.
     *
     * @param distMatrix symmetric n×n cost matrix (0 on diagonal)
     * @return the best complete tour found (array of city indices, length n+1, starts/ends at 0)
     */
    public int[] solve(int[][] distMatrix) {
        this.dist          = distMatrix;
        this.n             = distMatrix.length;
        this.bestCost      = Integer.MAX_VALUE;
        this.bestPath      = new int[n + 1];
        this.exploredNodes = new ArrayList<>();
        this.prunedCount   = 0;
        this.exploredCount = 0;

        // ---- Create the root node (start at airport 0, level 0) ----
        TspNode root   = new TspNode(n);
        root.level     = 0;
        root.cost      = 0;
        root.path[0]   = 0;        // start at hub (airport 0)
        root.visited[0] = true;
        root.bound     = computeLowerBound(root.path, root.visited, 0);

        // Priority queue: expand the node with the lowest bound first (best-first).
        PriorityQueue<TspNode> pq = new PriorityQueue<>();
        pq.add(root);

        while (!pq.isEmpty()) {
            TspNode current = pq.poll();

            // Pruning check on dequeue: if the bound can no longer improve bestCost, skip.
            if (current.bound >= bestCost) {
                prunedCount++;
                current.pruned = true;
                exploredNodes.add(current);
                continue;
            }

            exploredCount++;
            exploredNodes.add(current);

            // All cities committed → try to close the tour.
            if (current.level == n - 1) {
                int lastCity    = current.path[current.level];
                int returnEdge  = dist[lastCity][0];
                int totalCost   = current.cost + returnEdge;

                if (totalCost < bestCost) {
                    bestCost = totalCost;
                    // Copy path and append the return to hub.
                    System.arraycopy(current.path, 0, bestPath, 0, n);
                    bestPath[n] = 0;
                }
                continue;
            }

            // Branch: extend the partial route to each unvisited airport.
            for (int city = 1; city < n; city++) {        // city 0 is always the start/hub
                if (current.visited[city]) continue;

                TspNode child       = new TspNode(n);
                child.level         = current.level + 1;
                child.visited       = Arrays.copyOf(current.visited, n);
                child.visited[city] = true;
                System.arraycopy(current.path, 0, child.path, 0, current.level + 1);
                child.path[child.level] = city;

                int edgeCost  = dist[current.path[current.level]][city];
                child.cost    = current.cost + edgeCost;
                child.bound   = child.cost + computeLowerBound(child.path, child.visited, child.cost);

                // --- PRUNE: if bound cannot beat current best, discard. ---
                // Rationale: the bound is a lower estimate of the cheapest possible completion.
                // If even that lower estimate is ≥ bestCost, no completion of this branch
                // can produce a better tour — prune immediately.
                if (child.bound < bestCost) {
                    pq.add(child);
                } else {
                    prunedCount++;
                    child.pruned = true;
                    exploredNodes.add(child);
                }
            }
        }

        return bestPath;
    }

    // -----------------------------------------------------------------------
    // Lower-bound computation (row-minimum heuristic)
    // -----------------------------------------------------------------------

    /**
     * Compute a lower bound on the remaining travel cost needed to complete the tour.
     *
     * <p><b>Formula (row-minimum bound):</b><br>
     * For every city that has NOT yet been committed to the route, we look at the cheapest
     * edge that leaves that city (the row minimum in the cost matrix, excluding the diagonal).
     * Summing these minima gives a lower bound because any complete tour must use at least
     * one outgoing edge from each unvisited city.
     *
     * <pre>
     *   bound = Σ  min{ dist[city][j] : j ≠ city, j not yet visited before city in path }
     *           city ∈ unvisited ∪ {last committed city}
     * </pre>
     *
     * <p>This is admissible (never over-estimates) because we take the global row minimum,
     * which may not even be reachable given the already-fixed partial route.
     *
     * @param path        partial route array (only indices 0..level are meaningful)
     * @param visited     which cities are already in the partial route
     * @param currentCost accumulated cost so far (not used in computation but kept for context)
     * @return a non-negative lower bound on the additional cost to close the tour
     */
    public int computeLowerBound(int[] path, boolean[] visited, int currentCost) {
        int bound = 0;

        for (int city = 0; city < n; city++) {
            // We include the last committed city (needs an outgoing edge to the next hop)
            // and all completely unvisited cities.
            if (!visited[city] || city == path[0]) {
                // For visited cities other than the last one, they already have both
                // incoming and outgoing edges fixed — skip them.
                // (Here we use the simpler "unvisited + start" variant for clarity.)
            }
            if (!visited[city]) {
                // Find the cheapest edge leaving this unvisited city.
                int minEdge = Integer.MAX_VALUE;
                for (int j = 0; j < n; j++) {
                    if (j != city && dist[city][j] > 0) {
                        minEdge = Math.min(minEdge, dist[city][j]);
                    }
                }
                if (minEdge != Integer.MAX_VALUE) {
                    bound += minEdge;
                }
            }
        }

        // Also account for the cheapest edge from the last committed city to any unvisited one,
        // and the cheapest return-to-hub edge from any unvisited city.
        // (The simple row-min above already covers this conservatively.)

        return bound;
    }

    // -----------------------------------------------------------------------
    // Nearest-Neighbour heuristic (for comparison)
    // -----------------------------------------------------------------------

    /**
     * Nearest-Neighbour greedy heuristic: always fly to the closest unvisited airport.
     *
     * <p>This runs in O(N²) and typically produces a route 20-25 % worse than optimal,
     * giving a useful upper bound to seed the B&amp;B solver and to show students the
     * advantage of exact methods.
     *
     * @param distMatrix cost matrix
     * @return tour array (length n+1, starts and ends at 0); index n+1 holds the total cost
     *         (access via {@code path[n+1]} after casting — actually returned separately via
     *         {@link #nearestNeighbourCost(int[][])})
     */
    public int[] nearestNeighbour(int[][] distMatrix) {
        int size     = distMatrix.length;
        int[] tour   = new int[size + 1];
        boolean[] vis = new boolean[size];
        tour[0]      = 0;
        vis[0]       = true;
        int current  = 0;

        for (int step = 1; step < size; step++) {
            int nearest  = -1;
            int minDist  = Integer.MAX_VALUE;
            for (int j = 0; j < size; j++) {
                if (!vis[j] && distMatrix[current][j] > 0 && distMatrix[current][j] < minDist) {
                    minDist = distMatrix[current][j];
                    nearest = j;
                }
            }
            tour[step] = nearest;
            vis[nearest] = true;
            current = nearest;
        }
        tour[size] = 0; // return to hub
        return tour;
    }

    /** Total cost of a nearest-neighbour tour. */
    public int nearestNeighbourCost(int[][] distMatrix) {
        int[] tour = nearestNeighbour(distMatrix);
        int cost   = 0;
        for (int i = 0; i < distMatrix.length; i++) {
            cost += distMatrix[tour[i]][tour[i + 1]];
        }
        return cost;
    }

    // -----------------------------------------------------------------------
    // Default airport data
    // -----------------------------------------------------------------------

    /**
     * Default 6×6 symmetric distance matrix for major Indian airports (km, rounded).
     *
     * <p>Index mapping:
     * <pre>
     *   0 = MUM (Mumbai)
     *   1 = DEL (Delhi)
     *   2 = CHE (Chennai)
     *   3 = KOL (Kolkata)
     *   4 = BAN (Bangalore)
     *   5 = HYD (Hyderabad)
     * </pre>
     */
    public int[][] defaultAirports() {
        return new int[][] {
            //       MUM    DEL    CHE    KOL    BAN    HYD
            /* MUM */ { 0, 1400, 1330, 1930,  980,  710 },
            /* DEL */ { 1400,  0, 2180, 1340, 2150, 1570 },
            /* CHE */ { 1330, 2180,  0, 1670,  350,  630 },
            /* KOL */ { 1930, 1340, 1670,    0, 1870, 1490 },
            /* BAN */ {  980, 2150,  350, 1870,    0,  570 },
            /* HYD */ {  710, 1570,  630, 1490,  570,    0 }
        };
    }

    /** Display names matching the index order in {@link #defaultAirports()}. */
    public static final String[] AIRPORT_NAMES  = { "Mumbai", "Delhi", "Chennai", "Kolkata", "Bangalore", "Hyderabad" };
    /** IATA-style 3-letter codes matching the index order in {@link #defaultAirports()}. */
    public static final String[] AIRPORT_CODES  = { "MUM", "DEL", "CHE", "KOL", "BAN", "HYD" };

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /** @return explored (non-pruned) B&B node list, in expansion order. */
    public List<TspNode> getExploredNodes() { return exploredNodes; }

    /** @return all paths recorded (both explored and pruned nodes). */
    public List<int[]> getAllPaths() {
        List<int[]> paths = new ArrayList<>();
        for (TspNode node : exploredNodes) {
            int[] p = Arrays.copyOf(node.path, node.level + 1);
            paths.add(p);
        }
        return paths;
    }

    /** @return cost of the best complete tour found. */
    public int getBestCost()     { return bestCost; }

    /** @return number of branches discarded by the pruning condition. */
    public int getPrunedCount()  { return prunedCount; }

    /** @return number of B&B nodes actually expanded. */
    public int getExploredCount() { return exploredCount; }

    /** @return the best-tour path array (length n+1, 0-indexed). */
    public int[] getBestPath()   { return bestPath; }

    /** @return the number of airports (n). */
    public int getN()            { return n; }
}
