package com.compiler.parser.syntax;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.compiler.parser.grammar.Grammar;
import com.compiler.parser.grammar.Production;
import com.compiler.parser.grammar.Symbol;
import com.compiler.parser.grammar.SymbolType;

/**
 * Calculates the FIRST and FOLLOW sets for a given grammar.
 * Main task of Practice 5.
 */
public class StaticAnalyzer {
    private final Grammar grammar;
    private final Map<Symbol, Set<Symbol>> firstSets;
    private final Map<Symbol, Set<Symbol>> followSets;
    
    // Símbolos especiales
    private static final Symbol EPSILON = new Symbol("ε", SymbolType.TERMINAL);
    private static final Symbol END_OF_INPUT = new Symbol("$", SymbolType.TERMINAL);

    public StaticAnalyzer(Grammar grammar) {
        this.grammar = grammar;
        this.firstSets = new HashMap<>();
        this.followSets = new HashMap<>();
    }

    /**
     * Calculates and returns the FIRST sets for all symbols.
     * @return A map from Symbol to its FIRST set.
     */
    public Map<Symbol, Set<Symbol>> getFirstSets() {
        // Paso 1: Inicializar los conjuntos FIRST
        
        // Para cada terminal, FIRST(terminal) = {terminal}
        for (Symbol terminal : grammar.getTerminals()) {
            Set<Symbol> firstSet = new HashSet<>();
            firstSet.add(terminal);
            firstSets.put(terminal, firstSet);
        }
        
        // Añadir epsilon como un terminal especial si no está ya
        if (!firstSets.containsKey(EPSILON)) {
            Set<Symbol> epsilonSet = new HashSet<>();
            epsilonSet.add(EPSILON);
            firstSets.put(EPSILON, epsilonSet);
        }
        
        // Para cada no-terminal, inicializar con conjunto vacío
        for (Symbol nonTerminal : grammar.getNonTerminals()) {
            firstSets.put(nonTerminal, new HashSet<>());
        }
        
        // Paso 2: Iterar hasta que no haya cambios
        boolean changed = true;
        while (changed) {
            changed = false;
            
            // Para cada producción A -> X1 X2 ... Xn
            for (Production production : grammar.getProductions()) {
                Symbol A = production.getLeft();
                Set<Symbol> firstOfA = firstSets.get(A);
                int originalSize = firstOfA.size();
                
                // Si la producción es A -> ε
                if (production.getRight().size() == 1 && 
                    production.getRight().get(0).equals(EPSILON)) {
                    firstOfA.add(EPSILON);
                } else {
                    // Para cada símbolo Xi en el lado derecho
                    boolean allHaveEpsilon = true;
                    for (Symbol Xi : production.getRight()) {
                        Set<Symbol> firstOfXi = firstSets.get(Xi);
                        
                        if (firstOfXi == null) {
                            firstOfXi = new HashSet<>();
                            firstSets.put(Xi, firstOfXi);
                        }
                        
                        // Añadir FIRST(Xi) - {ε} a FIRST(A)
                        for (Symbol symbol : firstOfXi) {
                            if (!symbol.equals(EPSILON)) {
                                firstOfA.add(symbol);
                            }
                        }
                        
                        // Si ε no está en FIRST(Xi), parar
                        if (!firstOfXi.contains(EPSILON)) {
                            allHaveEpsilon = false;
                            break;
                        }
                    }
                    
                    // Si ε está en FIRST(Xi) para todos los Xi, añadir ε a FIRST(A)
                    if (allHaveEpsilon && production.getRight().size() > 0) {
                        firstOfA.add(EPSILON);
                    }
                }
                
                // Verificar si hubo cambios
                if (firstOfA.size() != originalSize) {
                    changed = true;
                }
            }
        }
        
        return new HashMap<>(firstSets);
    }

    /**
     * Calculates and returns the FOLLOW sets for non-terminals.
     * @return A map from Symbol to its FOLLOW set.
     */
    public Map<Symbol, Set<Symbol>> getFollowSets() {
        // Primero calculamos los conjuntos FIRST si no lo hemos hecho
        if (firstSets.isEmpty()) {
            getFirstSets();
        }
        
        // Paso 1: Inicializar FOLLOW sets para todos los no-terminales
        for (Symbol nonTerminal : grammar.getNonTerminals()) {
            followSets.put(nonTerminal, new HashSet<>());
        }
        
        // Paso 2: Añadir $ al FOLLOW del símbolo inicial
        Symbol startSymbol = grammar.getStartSymbol();
        followSets.get(startSymbol).add(END_OF_INPUT);
        
        // Paso 3: Iterar hasta que no haya cambios
        boolean changed = true;
        while (changed) {
            changed = false;
            
            // Para cada producción B -> X1 X2 ... Xn
            for (Production production : grammar.getProductions()) {
                Symbol B = production.getLeft();
                
                // Para cada símbolo Xi en el lado derecho
                for (int i = 0; i < production.getRight().size(); i++) {
                    Symbol Xi = production.getRight().get(i);
                    
                    // Solo procesamos no-terminales
                    if (Xi.type == SymbolType.NON_TERMINAL) {
                        Set<Symbol> followOfXi = followSets.get(Xi);
                        int originalSize = followOfXi.size();
                        
                        // Calcular FIRST de Xi+1 Xi+2 ... Xn
                        Set<Symbol> firstOfRest = getFirstOfSequence(
                            production.getRight().subList(i + 1, production.getRight().size())
                        );
                        
                        // Añadir FIRST(Xi+1...Xn) - {ε} a FOLLOW(Xi)
                        for (Symbol symbol : firstOfRest) {
                            if (!symbol.equals(EPSILON)) {
                                followOfXi.add(symbol);
                            }
                        }
                        
                        // Si ε está en FIRST(Xi+1...Xn) o Xi está al final
                        if (firstOfRest.contains(EPSILON) || i == production.getRight().size() - 1) {
                            // Añadir FOLLOW(B) a FOLLOW(Xi)
                            Set<Symbol> followOfB = followSets.get(B);
                            if (followOfB != null) {
                                followOfXi.addAll(followOfB);
                            }
                        }
                        
                        // Verificar si hubo cambios
                        if (followOfXi.size() != originalSize) {
                            changed = true;
                        }
                    }
                }
            }
        }
        
        return new HashMap<>(followSets);
    }
    
    /**
     * Helper method to calculate FIRST of a sequence of symbols
     * @param symbols List of symbols
     * @return FIRST set of the sequence
     */
    private Set<Symbol> getFirstOfSequence(java.util.List<Symbol> symbols) {
        Set<Symbol> result = new HashSet<>();
        
        if (symbols.isEmpty()) {
            result.add(EPSILON);
            return result;
        }
        
        boolean allHaveEpsilon = true;
        for (Symbol symbol : symbols) {
            Set<Symbol> firstOfSymbol = firstSets.get(symbol);
            if (firstOfSymbol == null) {
                firstOfSymbol = new HashSet<>();
            }
            
            // Añadir FIRST(symbol) - {ε} al resultado
            for (Symbol s : firstOfSymbol) {
                if (!s.equals(EPSILON)) {
                    result.add(s);
                }
            }
            
            // Si ε no está en FIRST(symbol), parar
            if (!firstOfSymbol.contains(EPSILON)) {
                allHaveEpsilon = false;
                break;
            }
        }
        
        // Si todos tienen ε, añadir ε al resultado
        if (allHaveEpsilon) {
            result.add(EPSILON);
        }
        
        return result;
    }
}