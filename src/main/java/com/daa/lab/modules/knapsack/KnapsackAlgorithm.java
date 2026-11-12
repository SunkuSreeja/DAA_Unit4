package com.daa.lab.modules.knapsack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Pure algorithm class (no JavaFX) implementing 0/1 Knapsack via Branch and Bound.
 *
 * <p><b>Investment Portfolio Optimization context:</b>
 * A venture capital firm has a limited budget (₹ crores).  Each company
 * project has a one-time cost and an expected profit.  We want to choose a
 * subset of projects that maximises total profit without exceeding the budget.
 * This is exactly the 0/1 Knapsack problem.</p>
 *
 * <h3>Branch-and-Bound overview</h3>
 * <ol>
 *   <li>Sort items by profit/cost ratio descending – the greedy order gives
 *       the tightest fractional upper bounds.</li>
 *   <li>Use a max-heap (priority queue) keyed on upper bound so we always
 *       expand the most promising node first (best-first search).</li>
 *   <li>At each node, compute an upper bound using the fractional-knapsack
 *       relaxation.  If the bound cannot beat the current best, prune.</li>
 *   <li>Branch by either <em>including</em> or <em>excluding</em> the next
 *       item.  Recurse for the next level.</li>
 *   <li>When all items have been decided the current profit is a feasible
 *       solution; update the best if it is higher.</li>
 * </ol>
 */
public class KnapsackAlgorithm {

    // ── Inner classes ──────────────────────────────────────────────────────────

    /**
     * Represents one investable company project.
     */
    public static class Item {
        public final String name;
        public final int    cost;    // weight (₹ crores required)
        public final int    profit;  // value  (₹ crores expected return)
        public final double ratio;   // profit / cost – used for greedy ordering

        public Item(String name, int cost, int profit) {
            this.name   = name;
            this.cost   = cost;
            this.profit = profit;
            // Sorting by this ratio descending gives us the tightest upper
            // bounds via fractional-knapsack relaxation.
            this.ratio  = (cost == 0) ? 0.0 : (double) profit / cost;
        }

        @Override
        public String toString() {
            return name + " (cost=" + cost + ", profit=" + profit + ")";
        }
    }

    /**
     * A node in the Branch-and-Bound search tree.
     *
     * <p>Each node represents a partial assignment of the first {@code level}
     * items (sorted by ratio).  {@code included} records whether the item at
     * {@code level-1} was taken.  {@code upperBound} is the fractional-
     * knapsack upper bound on what any descendant can achieve.</p>
     */
    public static class KnapsackNode {
        public final int         level;       // depth in the tree (0 = root)
        public final int         profit;      // cumulative profit so far
        public final int         weight;      // cumulative cost/weight so far
        public final double      upperBound;  // best possible profit from here
        public final boolean     included;    // did we take the item at level-1?
        public final List<Item>  selected;    // items chosen so far (snapshot)
        public       boolean     pruned;      // set true when we prune this node
        public       boolean     best;        // set true if this is the optimal leaf

        public KnapsackNode(int level, int profit, int weight,
                            double upperBound, boolean included,
                            List<Item> selected) {
            this.level      = level;
            this.profit     = profit;
            this.weight     = weight;
            this.upperBound = upperBound;
            this.included   = included;
            this.selected   = new ArrayList<>(selected);
        }
    }

    // ── Algorithm state ────────────────────────────────────────────────────────

    private List<Item>        items;          // sorted by ratio descending
    private int               budget;         // knapsack capacity (₹ crores)
    private int               bestProfit;
    private List<Item>        bestSelection;
    private int               nodesExplored;
    private int               nodesPruned;
    private List<KnapsackNode> allNodes;      // recorded for tree visualisation

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Run Branch-and-Bound to solve 0/1 Knapsack.
     *
     * <p>Items are sorted by profit/cost ratio descending before the search.
     * A best-first priority queue (max upper-bound) is used so that the most
     * promising partial solutions are expanded first, leading to early pruning.</p>
     *
     * @param inputItems list of candidate projects
     * @param budget     total available investment budget (₹ crores)
     */
    public void solve(List<Item> inputItems, int budget) {
        // Reset state for a fresh run
        this.budget        = budget;
        this.bestProfit    = 0;
        this.bestSelection = new ArrayList<>();
        this.nodesExplored = 0;
        this.nodesPruned   = 0;
        this.allNodes      = new ArrayList<>();

        if (inputItems == null || inputItems.isEmpty()) return;

        // Sort items by profit/cost ratio descending.
        // WHY: The fractional-knapsack relaxation (which gives our upper bound)
        //      is tightest when we consider items in this greedy order.
        this.items = new ArrayList<>(inputItems);
        this.items.sort(Comparator.comparingDouble((Item it) -> it.ratio).reversed());

        // Max-heap ordered by upperBound descending (best-first search).
        // Best-first ensures we find the optimal solution quickly and prune
        // as many branches as possible early.
        PriorityQueue<KnapsackNode> pq = new PriorityQueue<>(
                Comparator.comparingDouble((KnapsackNode n) -> n.upperBound).reversed()
        );

        // Root node: before any item decision (level=0, profit=0, weight=0)
        double rootBound = computeUpperBound(0, 0, 0);
        KnapsackNode root = new KnapsackNode(0, 0, 0, rootBound, false, new ArrayList<>());
        allNodes.add(root);
        pq.offer(root);

        while (!pq.isEmpty()) {
            KnapsackNode current = pq.poll();
            nodesExplored++;

            // If we have assigned all items, this is a leaf – check feasibility.
            if (current.level == items.size()) {
                if (current.profit > bestProfit) {
                    bestProfit    = current.profit;
                    bestSelection = new ArrayList<>(current.selected);
                    current.best  = true;
                }
                continue;
            }

            // Pruning condition:
            // If the upper bound of this node ≤ the best profit found so far,
            // no descendant can beat the current best → prune this branch.
            // WHY: upper bound is an OPTIMISTIC estimate (fractional knapsack
            //      allows partial items); if even this optimistic estimate
            //      cannot beat bestProfit, the whole subtree is useless.
            if (current.upperBound <= bestProfit) {
                current.pruned = true;
                nodesPruned++;
                continue;
            }

            Item nextItem = items.get(current.level);
            int  nextLevel = current.level + 1;

            // ── Branch 1: INCLUDE the next item ───────────────────────────────
            int newWeight = current.weight + nextItem.cost;
            if (newWeight <= budget) {
                int newProfit = current.profit + nextItem.profit;
                List<Item> newSelected = new ArrayList<>(current.selected);
                newSelected.add(nextItem);
                double ub = computeUpperBound(nextLevel, newProfit, newWeight);
                KnapsackNode includeNode = new KnapsackNode(
                        nextLevel, newProfit, newWeight, ub, true, newSelected);
                allNodes.add(includeNode);
                // Only enqueue if the node might improve the best
                if (ub > bestProfit) {
                    pq.offer(includeNode);
                } else {
                    includeNode.pruned = true;
                    nodesPruned++;
                }
                // Update best eagerly (integer feasible solution)
                if (newProfit > bestProfit) {
                    bestProfit    = newProfit;
                    bestSelection = new ArrayList<>(newSelected);
                    includeNode.best = true;
                }
            } else {
                // Item doesn't fit – implicit prune (over budget)
                nodesPruned++;
            }

            // ── Branch 2: EXCLUDE the next item ───────────────────────────────
            double ubExclude = computeUpperBound(nextLevel, current.profit, current.weight);
            KnapsackNode excludeNode = new KnapsackNode(
                    nextLevel, current.profit, current.weight,
                    ubExclude, false, current.selected);
            allNodes.add(excludeNode);
            if (ubExclude > bestProfit) {
                pq.offer(excludeNode);
            } else {
                excludeNode.pruned = true;
                nodesPruned++;
            }
        }
    }

    /**
     * Compute the fractional-knapsack upper bound for a partial assignment.
     *
     * <p>Starting from {@code level} with {@code currentProfit} already earned
     * and {@code currentWeight} already spent, greedily add whole items until
     * the budget would be exceeded, then add the fraction of the next item that
     * fits.  This gives an <em>optimistic</em> (upper) bound because real 0/1
     * knapsack cannot take fractions.</p>
     *
     * <pre>
     *   upperBound = currentProfit
     *              + Σ profit[i]              for all whole items that fit
     *              + profit[k] * (remaining / cost[k])   ← fractional item k
     * </pre>
     *
     * @param level         index of the first undecided item (items sorted by ratio)
     * @param currentProfit cumulative profit of items already included
     * @param currentWeight cumulative cost/weight of items already included
     * @return fractional-knapsack upper bound on achievable profit
     */
    public double computeUpperBound(int level, int currentProfit, int currentWeight) {
        // Start from what we already have
        double bound     = currentProfit;
        int    remaining = budget - currentWeight;

        for (int i = level; i < items.size(); i++) {
            Item item = items.get(i);

            if (remaining <= 0) break;  // budget exhausted

            if (item.cost <= remaining) {
                // Whole item fits – take it entirely
                bound     += item.profit;
                remaining -= item.cost;
            } else {
                // Fractional item: take the fraction that fits
                // bound += profit[i] * (remaining / cost[i])
                bound += (double) item.profit * remaining / item.cost;
                break; // budget exhausted after fraction
            }
        }
        return bound;
    }

    // ── Result accessors ───────────────────────────────────────────────────────

    /** @return the list of items in the optimal selection (after {@link #solve}). */
    public List<Item> getBestSelection() {
        return bestSelection != null ? bestSelection : new ArrayList<>();
    }

    /** @return the maximum profit achieved (after {@link #solve}). */
    public int getBestProfit() {
        return bestProfit;
    }

    /**
     * @return total cost/weight of the selected items (after {@link #solve}).
     */
    public int getTotalCost() {
        if (bestSelection == null) return 0;
        return bestSelection.stream().mapToInt(it -> it.cost).sum();
    }

    /** @return total number of B&B tree nodes explored (after {@link #solve}). */
    public int getNodesExplored() {
        return nodesExplored;
    }

    /** @return total number of B&B tree nodes pruned (after {@link #solve}). */
    public int getNodesPruned() {
        return nodesPruned;
    }

    /**
     * @return all B&B nodes in expansion order (for tree visualisation).
     */
    public List<KnapsackNode> getAllNodes() {
        return allNodes != null ? allNodes : new ArrayList<>();
    }

    // ── Sample data ────────────────────────────────────────────────────────────

    /**
     * Returns 8 sample company-project investment opportunities.
     * Default budget is ₹2000 crores (see {@link #getDefaultBudget()}).
     *
     * @return list of {@link Item} objects ready to pass to {@link #solve}
     */
    public List<Item> defaultItems() {
        List<Item> list = new ArrayList<>();
        list.add(new Item("AI Research Lab",   500, 800));
        list.add(new Item("EV Manufacturing",  750, 1100));
        list.add(new Item("Solar Farm",        600, 850));
        list.add(new Item("Data Center",       400, 550));
        list.add(new Item("Pharma R&D",        350, 480));
        list.add(new Item("Space Tech",        900, 1400));
        list.add(new Item("EdTech Platform",   200, 260));
        list.add(new Item("Green Logistics",   450, 620));
        return list;
    }

    /** Default investment budget (₹ crores). */
    public int getDefaultBudget() {
        return 2000;
    }
}
