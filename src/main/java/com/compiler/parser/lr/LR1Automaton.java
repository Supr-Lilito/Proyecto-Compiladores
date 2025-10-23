package com.compiler.parser.lr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.compiler.parser.grammar.Grammar;
import com.compiler.parser.grammar.Symbol;
import com.compiler.parser.syntax.StaticAnalyzer; // Importación añadida

/**
 * Builds the canonical collection of LR(1) items (the DFA automaton).
 * Items contain a lookahead symbol.
 */
public class LR1Automaton {
    private final Grammar grammar;
    private final List<Set<LR1Item>> states = new ArrayList<>();
    private final Map<Integer, Map<Symbol, Integer>> transitions = new HashMap<>();
    private String augmentedLeftName = null;

    // --- CAMPOS REQUERIDOS PARA LA IMPLEMENTACIÓN ---
    /** Almacena los conjuntos FIRST precalculados. */
    private Map<Symbol, Set<Symbol>> firstSets;
    /** Representa el símbolo épsilon (ε). */
    private Symbol epsilon;
    /** Representa el marcador de fin de entrada ($). */
    private Symbol dollar;
    // --- FIN DE CAMPOS REQUERIDOS ---

    public LR1Automaton(Grammar grammar) {
        this.grammar = Objects.requireNonNull(grammar);
    }

    public List<Set<LR1Item>> getStates() { return states; }
    public Map<Integer, Map<Symbol, Integer>> getTransitions() { return transitions; }

    /**
     * CLOSURE for LR(1): standard algorithm using FIRST sets to compute lookaheads for new items.
     */
    private Set<LR1Item> closure(Set<LR1Item> items) {
        // 1. Initialize a new set `closure` with the given `items`.
        Set<LR1Item> closure = new HashSet<>(items);
        
        // 2. Create a worklist and add all items from `items` to it.
        // Usaremos una List como worklist para evitar ConcurrentModificationException
        List<LR1Item> worklist = new ArrayList<>(items);
        int i = 0;

        // 4. While the worklist has items to process:
        while (i < worklist.size()) {
            // a. Dequeue an item `[A -> α • B β, a]`.
            LR1Item item = worklist.get(i++); // Dequeue
            
            Symbol B = item.getSymbolAfterDot();
            
            // b. If `B` is a non-terminal:
            if (B != null && B.type == com.compiler.parser.grammar.SymbolType.NON_TERMINAL) {
                
                // Obtenemos β (la secuencia después de B)
                List<Symbol> beta = item.production.right.subList(item.dotPosition + 1, item.production.right.size());
                // Creamos la secuencia βa
                List<Symbol> beta_a = new ArrayList<>(beta);
                beta_a.add(item.lookahead);
                
                // i. For each production of `B` (e.g., `B -> γ`):
                for (com.compiler.parser.grammar.Production p : grammar.getProductions()) {
                    if (p.left.equals(B)) {
                        // - Calculate the FIRST set of the sequence `βa`.
                        // ii. - For each terminal `b` in FIRST(βa):
                        for (Symbol b : computeFirstOfSequence(beta_a)) {
                            // - Create a new item `[B -> • γ, b]`.
                            LR1Item newItem = new LR1Item(p, 0, b);
                            
                            // - If this new item is not already in the `closure` set:
                            if (closure.add(newItem)) {
                                // - Add it to `closure` (hecho por `closure.add()`).
                                // - Enqueue it to the worklist.
                                worklist.add(newItem);
                            }
                        }
                    }
                }
            }
        }
        // 5. Return the `closure` set.
        return closure;
    }

    /**
     * Compute FIRST of a sequence of symbols (βa).
     * Utiliza los campos 'firstSets' y 'epsilon' de la clase.
     */
    private Set<Symbol> computeFirstOfSequence(List<Symbol> seq) {
        // 1. Initialize an empty result set.
        Set<Symbol> first = new HashSet<>();

        // 2. If the sequence is empty
        if (seq.isEmpty()) {
            first.add(this.epsilon);
            return first;
        }

        boolean allNullable = true;
        // 3. Iterate through the symbols `X` in the sequence:
        for (Symbol X : seq) {
            // a. Get `FIRST(X)`
            Set<Symbol> firstX = this.firstSets.get(X);
            
            // Caso de fallback: si un símbolo no tiene FIRST set (ej. $ o un terminal)
            if (firstX == null) {
                 if (X.type == com.compiler.parser.grammar.SymbolType.TERMINAL) {
                     first.add(X);
                 }
                 allNullable = false;
                 break;
            }

            // b. Add all symbols from `FIRST(X)` to the result, except for epsilon.
            for (Symbol s : firstX) {
                if (!s.equals(this.epsilon)) {
                    first.add(s);
                }
            }

            // c. If `FIRST(X)` does not contain epsilon, stop.
            if (!firstX.contains(this.epsilon)) {
                allNullable = false;
                break;
            }
            // d. Si contiene epsilon, continuamos con el siguiente símbolo.
        }

        // d. (cont.) If all symbols in the sequence were nullable
        if (allNullable) {
            first.add(this.epsilon);
        }
        
        // 4. Return the result set.
        return first;
    }


    /**
     * GOTO for LR(1): moves dot over symbol and takes closure.
     */
    private Set<LR1Item> goTo(Set<LR1Item> state, Symbol symbol) {
        // 1. Initialize an empty set `movedItems`.
        Set<LR1Item> movedItems = new HashSet<>();
        
        // 2. For each item `[A -> α • X β, a]` in the input `state`:
        for (LR1Item item : state) {
            Symbol X = item.getSymbolAfterDot();
            
            // a. If `X` is equal to the input `symbol`:
            if (X != null && X.equals(symbol)) {
                // - Add the new item `[A -> α X • β, a]` to `movedItems`.
                movedItems.add(new LR1Item(item.production, item.dotPosition + 1, item.lookahead));
            }
        }
        // 3. Return the `closure` of `movedItems`.
        return closure(movedItems);
    }

    /**
     * Build the LR(1) canonical collection: states and transitions.
     */
    public void build() {
        // 1. Clear any existing states and transitions.
        states.clear();
        transitions.clear();

        // 3. (Pre-paso) Initialize FIRST sets and symbols
        StaticAnalyzer analyzer = new StaticAnalyzer(grammar);
        this.firstSets = analyzer.getFirstSets();
        // NOTA: Asumimos que la gramática y el analizador usan "ε"
        this.epsilon = new Symbol("ε", com.compiler.parser.grammar.SymbolType.TERMINAL);
        this.dollar = new Symbol("$", com.compiler.parser.grammar.SymbolType.TERMINAL);

        // 2. Create the augmented grammar (virtually)
        Symbol S = grammar.getStartSymbol();
        this.augmentedLeftName = S.name + "'"; // S'
        Symbol S_prime = new Symbol(this.augmentedLeftName, com.compiler.parser.grammar.SymbolType.NON_TERMINAL);
        // Production S' -> S
        com.compiler.parser.grammar.Production augProd = new com.compiler.parser.grammar.Production(S_prime, java.util.List.of(S));
        
        // 3. Create the initial item: `[S' -> • S, $]`.
        Set<LR1Item> initialItems = new HashSet<>();
        initialItems.add(new LR1Item(augProd, 0, dollar));
        
        // 4. The first state, `I0`, is the `closure` of this initial item set.
        Set<LR1Item> I0 = closure(initialItems);
        states.add(I0);

        // Map para buscar rápidamente el índice de un estado existente
        Map<Set<LR1Item>, Integer> stateIndexMap = new HashMap<>();
        stateIndexMap.put(I0, 0);
        
        // 5. Create a worklist (queue) and add `I0` to it.
        List<Set<LR1Item>> worklist = new ArrayList<>();
        worklist.add(I0);
        
        // Obtenemos todos los símbolos de la gramática (T y NT)
        Set<Symbol> allSymbols = new HashSet<>(grammar.getTerminals());
        allSymbols.addAll(grammar.getNonTerminals());
        
        int k = 0;
        // 6. While the worklist is not empty:
        while (k < worklist.size()) {
            // a. Dequeue a state `I`.
            Set<LR1Item> I = worklist.get(k++);
            int stateIdx_I = stateIndexMap.get(I);
            
            // b. For each grammar symbol `X`:
            for (Symbol X : allSymbols) {
                // i. Calculate `J = goTo(I, X)`.
                Set<LR1Item> J = goTo(I, X);
                
                // ii. If `J` is not empty:
                if (!J.isEmpty()) {
                    Integer stateIdx_J = stateIndexMap.get(J);
                    // ...and not already in the list of states:
                    if (stateIdx_J == null) {
                        // - Add `J` to the list of states.
                        stateIdx_J = states.size();
                        states.add(J);
                        // - Enqueue `J` to the worklist.
                        worklist.add(J);
                        stateIndexMap.put(J, stateIdx_J);
                    }
                    
                    // iii. Create a transition from state `I` to state `J` on symbol `X`.
                    transitions.computeIfAbsent(stateIdx_I, m -> new HashMap<>()).put(X, stateIdx_J);
                }
            }
        }
    }

    public String getAugmentedLeftName() { return augmentedLeftName; }
}