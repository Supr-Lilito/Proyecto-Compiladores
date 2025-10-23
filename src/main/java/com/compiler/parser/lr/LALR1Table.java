package com.compiler.parser.lr;

/**
 * Builds the LALR(1) parsing table (ACTION/GOTO).
 * Main task for Practice 9.
 */
public class LALR1Table {
    private final LR1Automaton automaton;

    // merged LALR states and transitions
    private java.util.List<java.util.Set<LR1Item>> lalrStates = new java.util.ArrayList<>();
    private java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Integer>> lalrTransitions = new java.util.HashMap<>();
    
    // ACTION table: state -> terminal -> Action
    public static class Action {
        public enum Type { SHIFT, REDUCE, ACCEPT }
        public final Type type;
        public final Integer state; // for SHIFT
        public final com.compiler.parser.grammar.Production reduceProd; // for REDUCE

        private Action(Type type, Integer state, com.compiler.parser.grammar.Production prod) {
            this.type = type; this.state = state; this.reduceProd = prod;
        }

        public static Action shift(int s) { return new Action(Type.SHIFT, s, null); }
        public static Action reduce(com.compiler.parser.grammar.Production p) { return new Action(Type.REDUCE, null, p); }
        public static Action accept() { return new Action(Type.ACCEPT, null, null); }
    }

    private final java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Action>> action = new java.util.HashMap<>();
    private final java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Integer>> gotoTable = new java.util.HashMap<>();
    private final java.util.List<String> conflicts = new java.util.ArrayList<>();
    private int initialState = 0;

    public LALR1Table(LR1Automaton automaton) {
        this.automaton = automaton;
    }

    /**
     * Builds the LALR(1) parsing table.
     */
    public void build() {
        // Step 1: Ensure the underlying LR(1) automaton is built.
        automaton.build();
        java.util.List<java.util.Set<LR1Item>> lr1States = automaton.getStates();
        java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Integer>> lr1Transitions = automaton.getTransitions();

        // Step 2: Merge LR(1) states to create LALR(1) states.
        
        // Map from Kernel (Set<KernelEntry>) to List<LR(1) state IDs>
        java.util.Map<java.util.Set<KernelEntry>, java.util.List<Integer>> kernelToStateIds = new java.util.HashMap<>();
        
        // Map from old LR(1) state ID to new LALR(1) state ID
        int[] lr1ToLalrStateMap = new int[lr1States.size()];
        
        // a. Group LR(1) states by kernel
        for (int i = 0; i < lr1States.size(); i++) {
            java.util.Set<LR1Item> lr1State = lr1States.get(i);
            java.util.Set<KernelEntry> kernel = new java.util.HashSet<>();
            for (LR1Item item : lr1State) {
                kernel.add(new KernelEntry(item.production, item.dotPosition));
            }
            kernelToStateIds.computeIfAbsent(kernel, k -> new java.util.ArrayList<>()).add(i);
        }

        // b. Create LALR(1) states by merging
        lalrStates.clear();
        for (java.util.List<Integer> mergingStateIds : kernelToStateIds.values()) {
            java.util.Set<LR1Item> mergedState = new java.util.HashSet<>();
            int newLalrStateId = lalrStates.size();
            
            for (int lr1StateId : mergingStateIds) {
                // Add all items from the LR(1) state.
                // Items that only differ in lookahead will co-exist in the set.
                mergedState.addAll(lr1States.get(lr1StateId));
                
                // c. Create mapping
                lr1ToLalrStateMap[lr1StateId] = newLalrStateId;
            }
            lalrStates.add(mergedState);
        }

        // Step 3: Build the transitions for the new LALR(1) automaton.
        lalrTransitions.clear();
        for (int lr1StateId_S = 0; lr1StateId_S < lr1States.size(); lr1StateId_S++) {
            // Find the LALR state s = merged(lr1StateId_S)
            int lalrStateId_S = lr1ToLalrStateMap[lr1StateId_S];
            
            java.util.Map<com.compiler.parser.grammar.Symbol, Integer> lr1Trans = lr1Transitions.get(lr1StateId_S);
            if (lr1Trans != null) {
                for (java.util.Map.Entry<com.compiler.parser.grammar.Symbol, Integer> entry : lr1Trans.entrySet()) {
                    com.compiler.parser.grammar.Symbol X = entry.getKey();
                    int lr1StateId_T = entry.getValue();
                    // Find the LALR state t = merged(lr1StateId_T)
                    int lalrStateId_T = lr1ToLalrStateMap[lr1StateId_T];
                    
                    // Add transition: merged(s) -X-> merged(t)
                    lalrTransitions.computeIfAbsent(lalrStateId_S, k -> new java.util.HashMap<>()).put(X, lalrStateId_T);
                }
            }
        }

        // Set the initial state (el estado LALR(1) que corresponde al estado LR(1) 0)
        this.initialState = lr1ToLalrStateMap[0];

        // Step 4: Fill the ACTION and GOTO tables.
        fillActionGoto();
    }

    private void fillActionGoto() {
        // 1. Clear the tables and conflicts
        action.clear();
        gotoTable.clear();
        conflicts.clear();
        
        // Símbolo $ (fin de entrada)
        com.compiler.parser.grammar.Symbol dollar = new com.compiler.parser.grammar.Symbol("$", com.compiler.parser.grammar.SymbolType.TERMINAL);
        // Nombre de la producción aumentada (ej. "S'")
        String augStartName = automaton.getAugmentedLeftName();
        if (augStartName == null) {
            throw new IllegalStateException("LR1Automaton no fue construido o no generó un nombre aumentado.");
        }

        // 2. Iterate through each LALR state `s`
        for (int s = 0; s < lalrStates.size(); s++) {
            java.util.Map<com.compiler.parser.grammar.Symbol, Action> actionRow = action.computeIfAbsent(s, k -> new java.util.HashMap<>());
            
            // 3. For each item `it` in state `s`
            for (LR1Item it : lalrStates.get(s)) {
                
                com.compiler.parser.grammar.Symbol X = it.getSymbolAfterDot();
                
                // 3b. If `X` is a terminal (SHIFT action)
                if (X != null && X.type == com.compiler.parser.grammar.SymbolType.TERMINAL) {
                    // Find the destination state `t`
                    Integer t = lalrTransitions.get(s).get(X);
                    if (t == null) continue; // Error en el autómata
                    
                    Action shiftAction = Action.shift(t);
                    
                    // Check for conflicts
                    if (actionRow.containsKey(X)) {
                        Action existing = actionRow.get(X);
                        if (existing.type == Action.Type.REDUCE) {
                            conflicts.add("Shift/Reduce conflict in state " + s + " on " + X.name + ". (Prefiriendo SHIFT)");
                        }
                    }
                    
                    // Standard: S/R conflicts se resuelven a favor de SHIFT.
                    actionRow.put(X, shiftAction);
                
                // 3c. If the dot is at the end (`X` is null) (REDUCE or ACCEPT)
                } else if (X == null) {
                    // This is an item like `[A -> α •, a]`
                    com.compiler.parser.grammar.Symbol a = it.lookahead; // El lookahead
                    
                    // If it's the augmented start production `[S' -> S •, $]` -> ACCEPT
                    if (it.production.left.name.equals(augStartName)) {
                        if (a.equals(dollar)) {
                            Action acceptAction = Action.accept();
                            if (actionRow.containsKey(a) && actionRow.get(a).type != Action.Type.ACCEPT) {
                                conflicts.add("Conflict on ACCEPT in state " + s);
                            }
                            actionRow.put(a, acceptAction);
                        }
                    // Otherwise, it's a REDUCE action `[A -> α •, a]`
                    } else {
                        Action reduceAction = Action.reduce(it.production);
                        
                        // Check for conflicts
                        if (actionRow.containsKey(a)) {
                            Action existing = actionRow.get(a);
                            if (existing.type == Action.Type.SHIFT) {
                                conflicts.add("Reduce/Shift conflict in state " + s + " on " + a.name + ". (Prefiriendo SHIFT, no se agrega REDUCE)");
                                // No sobrescribimos el SHIFT
                            } else if (existing.type == Action.Type.REDUCE) {
                                if (!existing.reduceProd.equals(it.production)) {
                                    conflicts.add("Reduce/Reduce conflict in state " + s + " on " + a.name);
                                    // Sobrescribimos (o no), pero reportamos el conflicto
                                }
                            }
                        } else {
                            // No hay conflicto, agregamos la acción
                            actionRow.put(a, reduceAction);
                        }
                    }
                }
            }
        }
        
        // 4. Populate the GOTO table
        // Iterate through all LALR states `s`
        for (int s = 0; s < lalrStates.size(); s++) {
            java.util.Map<com.compiler.parser.grammar.Symbol, Integer> transitions = lalrTransitions.get(s);
            if (transitions != null) {
                java.util.Map<com.compiler.parser.grammar.Symbol, Integer> gotoRow = gotoTable.computeIfAbsent(s, k -> new java.util.HashMap<>());
                // For each transition on a NON-TERMINAL symbol `B`
                for (java.util.Map.Entry<com.compiler.parser.grammar.Symbol, Integer> entry : transitions.entrySet()) {
                    com.compiler.parser.grammar.Symbol B = entry.getKey();
                    if (B.type == com.compiler.parser.grammar.SymbolType.NON_TERMINAL) {
                        gotoRow.put(B, entry.getValue());
                    }
                }
            }
        }
    }
    
    // ... (Getters and KernelEntry class)
    public java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Action>> getActionTable() { return action; }
    public java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Integer>> getGotoTable() { return gotoTable; }
    public java.util.List<String> getConflicts() { return conflicts; }
    private static class KernelEntry {
        public final com.compiler.parser.grammar.Production production;
        public final int dotPosition;
        KernelEntry(com.compiler.parser.grammar.Production production, int dotPosition) {
            this.production = production;
            this.dotPosition = dotPosition;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof KernelEntry)) return false;
            KernelEntry o = (KernelEntry) obj;
            return dotPosition == o.dotPosition && production.equals(o.production);
        }
        @Override
        public int hashCode() {
            int r = production.hashCode();
            r = 31 * r + dotPosition;
            return r;
        }
    }
    public java.util.List<java.util.Set<LR1Item>> getLALRStates() { return lalrStates; }
    public java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Integer>> getLALRTransitions() { return lalrTransitions; }
    public int getInitialState() { return initialState; }
}