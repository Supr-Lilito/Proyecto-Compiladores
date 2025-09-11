/**
 * DfaMinimizer
 * -------------
 * This class provides an implementation of DFA minimization using the table-filling algorithm.
 * It identifies and merges equivalent states in a deterministic finite automaton (DFA),
 * resulting in a minimized DFA with the smallest number of states that recognizes the same language.
 *
 * Main steps:
 *   1. Initialization: Mark pairs of states as distinguishable if one is final and the other is not.
 *   2. Iterative marking: Mark pairs as distinguishable if their transitions lead to distinguishable states,
 *      or if only one state has a transition for a given symbol.
 *   3. Partitioning: Group equivalent states and build the minimized DFA.
 *
 * Helper methods are provided for partitioning, union-find operations, and pair representation.
 */
package com.compiler.lexer;

import java.util.*;

import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;

/**
 * Implements DFA minimization using the table-filling algorithm.
 */
public class DfaMinimizer {
    /**
     * Default constructor for DfaMinimizer.
     */
    public DfaMinimizer() {
        // Constructor implementation (empty as no initialization needed)
    }

    /**
     * Minimizes a given DFA using the table-filling algorithm.
     *
     * @param originalDfa The original DFA to be minimized.
     * @param alphabet The set of input symbols.
     * @return A minimized DFA equivalent to the original.
     */
    public static DFA minimizeDfa(DFA originalDfa, Set<Character> alphabet) {
        // Step 1: Collect and sort all DFA states
        List<DfaState> allStates = new ArrayList<>(originalDfa.getAllStates());
        allStates.sort((s1, s2) -> Integer.compare(s1.id, s2.id));
        
        // Step 2: Initialize table of state pairs
        Map<Pair, Boolean> table = new HashMap<>();
        
        // Mark pairs as distinguishable if one is final and the other is not
        for (int i = 0; i < allStates.size(); i++) {
            for (int j = i + 1; j < allStates.size(); j++) {
                DfaState s1 = allStates.get(i);
                DfaState s2 = allStates.get(j);
                Pair pair = new Pair(s1, s2);
                
                // Mark as distinguishable if finality differs
                if (s1.isFinal() != s2.isFinal()) {
                    table.put(pair, true);
                } else {
                    table.put(pair, false);
                }
            }
        }
        
        // Step 3: Iteratively mark pairs as distinguishable
        boolean changed = true;
        while (changed) {
            changed = false;
            
            for (int i = 0; i < allStates.size(); i++) {
                for (int j = i + 1; j < allStates.size(); j++) {
                    DfaState s1 = allStates.get(i);
                    DfaState s2 = allStates.get(j);
                    Pair pair = new Pair(s1, s2);
                    
                    // Skip if already marked as distinguishable
                    if (table.get(pair)) {
                        continue;
                    }
                    
                    // Check if transitions lead to distinguishable states
                    for (Character symbol : alphabet) {
                        DfaState next1 = s1.getTransition(symbol);
                        DfaState next2 = s2.getTransition(symbol);
                        
                        // If only one has a transition, mark as distinguishable
                        if ((next1 == null) != (next2 == null)) {
                            table.put(pair, true);
                            changed = true;
                            break;
                        }
                        
                        // If both have transitions, check if they lead to distinguishable states
                        if (next1 != null && next2 != null && !next1.equals(next2)) {
                            Pair nextPair = new Pair(next1, next2);
                            if (table.get(nextPair)) {
                                table.put(pair, true);
                                changed = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        // Step 4: Partition states into equivalence classes
        List<Set<DfaState>> partitions = createPartitions(allStates, table);
        
        // Step 5: Create new minimized states for each partition
        Map<Set<DfaState>, DfaState> partitionToState = new HashMap<>();
        List<DfaState> minimizedStates = new ArrayList<>();
        
        for (Set<DfaState> partition : partitions) {
            // Create new state representing this partition
            DfaState representative = partition.iterator().next();
            DfaState newState = new DfaState(representative.getNfaStates());
            newState.setFinal(representative.isFinal());
            
            partitionToState.put(partition, newState);
            minimizedStates.add(newState);
        }
        
        // Step 6: Reconstruct transitions for minimized states
        for (Set<DfaState> partition : partitions) {
            DfaState newState = partitionToState.get(partition);
            DfaState representative = partition.iterator().next();
            
            // Add transitions based on representative's transitions
            for (Character symbol : alphabet) {
                DfaState nextState = representative.getTransition(symbol);
                if (nextState != null) {
                    // Find which partition the next state belongs to
                    for (Set<DfaState> targetPartition : partitions) {
                        if (targetPartition.contains(nextState)) {
                            DfaState targetState = partitionToState.get(targetPartition);
                            newState.addTransition(symbol, targetState);
                            break;
                        }
                    }
                }
            }
        }
        
        // Step 7: Set start state and return minimized DFA
        DfaState minimizedStartState = null;
        for (Set<DfaState> partition : partitions) {
            if (partition.contains(originalDfa.getStartState())) {
                minimizedStartState = partitionToState.get(partition);
                break;
            }
        }
        
        return new DFA(minimizedStartState, minimizedStates);
    }

    /**
     * Groups equivalent states into partitions using union-find.
     *
     * @param allStates List of all DFA states.
     * @param table Table indicating which pairs are distinguishable.
     * @return List of partitions, each containing equivalent states.
     */
    private static List<Set<DfaState>> createPartitions(List<DfaState> allStates, Map<Pair, Boolean> table) {
        // Step 1: Initialize each state as its own parent
        Map<DfaState, DfaState> parent = new HashMap<>();
        for (DfaState state : allStates) {
            parent.put(state, state);
        }
        
        // Step 2: For each pair not marked as distinguishable, union the states
        for (int i = 0; i < allStates.size(); i++) {
            for (int j = i + 1; j < allStates.size(); j++) {
                DfaState s1 = allStates.get(i);
                DfaState s2 = allStates.get(j);
                Pair pair = new Pair(s1, s2);
                
                // If not distinguishable, they are equivalent - union them
                if (!table.get(pair)) {
                    union(parent, s1, s2);
                }
            }
        }
        
        // Step 3: Group states by their root parent
        Map<DfaState, Set<DfaState>> partitionMap = new HashMap<>();
        for (DfaState state : allStates) {
            DfaState root = find(parent, state);
            partitionMap.computeIfAbsent(root, k -> new HashSet<>()).add(state);
        }
        
        // Step 4: Return list of partitions
        return new ArrayList<>(partitionMap.values());
    }

    /**
     * Finds the root parent of a state in the union-find structure.
     * Implements path compression for efficiency.
     *
     * @param parent Parent map.
     * @param state State to find.
     * @return Root parent of the state.
     */
    private static DfaState find(Map<DfaState, DfaState> parent, DfaState state) {
        // If parent[state] == state, return state (it's the root)
        if (parent.get(state).equals(state)) {
            return state;
        }
        
        // Recursively find parent and apply path compression
        DfaState root = find(parent, parent.get(state));
        parent.put(state, root);  // Path compression
        return root;
    }

    /**
     * Unites two states in the union-find structure.
     *
     * @param parent Parent map.
     * @param s1 First state.
     * @param s2 Second state.
     */
    private static void union(Map<DfaState, DfaState> parent, DfaState s1, DfaState s2) {
        // Find roots of s1 and s2
        DfaState root1 = find(parent, s1);
        DfaState root2 = find(parent, s2);
        
        // If roots are different, set parent of one to the other
        if (!root1.equals(root2)) {
            parent.put(root2, root1);
        }
    }

    /**
     * Helper class to represent a pair of DFA states in canonical order.
     * Used for table indexing and comparison.
     */
    private static class Pair {
        final DfaState s1;
        final DfaState s2;

        /**
         * Constructs a pair in canonical order (lowest id first).
         * @param s1 First state.
         * @param s2 Second state.
         */
        public Pair(DfaState s1, DfaState s2) {
            // Assign s1 and s2 so that s1.id <= s2.id (canonical order)
            if (s1.id <= s2.id) {
                this.s1 = s1;
                this.s2 = s2;
            } else {
                this.s1 = s2;
                this.s2 = s1;
            }
        }

        @Override
        public boolean equals(Object o) {
            // Return true if both s1 and s2 ids match
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pair pair = (Pair) o;
            return s1.id == pair.s1.id && s2.id == pair.s2.id;
        }

        @Override
        public int hashCode() {
            // Return hash of s1.id and s2.id
            return Objects.hash(s1.id, s2.id);
        }
        
        @Override
        public String toString() {
            return "(" + s1.id + "," + s2.id + ")";
        }
    }
}