// NfaToTokenDfaConverter.java
package com.compiler.lexer;

import com.compiler.lexer.dfa.TokenDFA;
import com.compiler.lexer.dfa.TokenDfaState;
import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.nfa.State;
import com.compiler.lexer.LexicalRule;
import com.compiler.lexer.TokenType;

import java.util.*;

/**
 * Converts multiple NFAs (from lexical rules) into a single TokenDFA
 * that can recognize multiple token types and handle priority resolution.
 */
public class NfaToTokenDfaConverter {
    
    /**
     * Converts multiple NFAs into a single TokenDFA.
     * @param nfaRules Map of NFA to corresponding lexical rule.
     * @param alphabet The alphabet of input symbols.
     * @return A TokenDFA that can recognize all the patterns.
     */
    public static TokenDFA convertMultipleNfasToTokenDfa(Map<NFA, LexicalRule> nfaRules, Set<Character> alphabet) {
        // Step 1: Create a combined start state that has epsilon transitions to all NFA start states
        Set<State> combinedStartStates = new HashSet<>();
        Map<State, LexicalRule> finalStateToRule = new HashMap<>();
        
        // Collect all start states and map final states to their rules
        for (Map.Entry<NFA, LexicalRule> entry : nfaRules.entrySet()) {
            NFA nfa = entry.getKey();
            LexicalRule rule = entry.getValue();
            
            combinedStartStates.add(nfa.getStartState());
            finalStateToRule.put(nfa.endState, rule);
        }
        
        // Step 2: Apply epsilon closure to get initial DFA state
        Set<State> startClosure = epsilonClosure(combinedStartStates);
        TokenDfaState startDfaState = new TokenDfaState(startClosure);
        
        // Check if start state is final (contains any final NFA states)
        updateTokenInfoForState(startDfaState, finalStateToRule);
        
        // Step 3: Build DFA using subset construction
        List<TokenDfaState> allStates = new ArrayList<>();
        Map<Set<State>, TokenDfaState> stateMap = new HashMap<>();
        Queue<TokenDfaState> queue = new LinkedList<>();
        
        allStates.add(startDfaState);
        stateMap.put(startClosure, startDfaState);
        queue.add(startDfaState);
        
        // Step 4: Process each DFA state
        while (!queue.isEmpty()) {
            TokenDfaState currentDfaState = queue.poll();
            
            // For each symbol in alphabet
            for (Character symbol : alphabet) {
                Set<State> nextStates = new HashSet<>();
                
                // Collect all states reachable by this symbol
                for (State nfaState : currentDfaState.getNfaStates()) {
                    nextStates.addAll(nfaState.getTransitions(symbol));
                }
                
                if (!nextStates.isEmpty()) {
                    // Apply epsilon closure
                    Set<State> nextClosure = epsilonClosure(nextStates);
                    
                    // Check if this state configuration already exists
                    TokenDfaState nextDfaState = stateMap.get(nextClosure);
                    if (nextDfaState == null) {
                        // Create new DFA state
                        nextDfaState = new TokenDfaState(nextClosure);
                        updateTokenInfoForState(nextDfaState, finalStateToRule);
                        
                        allStates.add(nextDfaState);
                        stateMap.put(nextClosure, nextDfaState);
                        queue.add(nextDfaState);
                    }
                    
                    // Add transition
                    currentDfaState.addTransition(symbol, nextDfaState);
                }
            }
        }
        
        return new TokenDFA(startDfaState, allStates);
    }
    
    /**
     * Updates token information for a DFA state based on contained final NFA states.
     */
    private static void updateTokenInfoForState(TokenDfaState dfaState, Map<State, LexicalRule> finalStateToRule) {
        for (State nfaState : dfaState.getNfaStates()) {
            if (finalStateToRule.containsKey(nfaState)) {
                LexicalRule rule = finalStateToRule.get(nfaState);
                dfaState.updateTokenInfoIfHigherPriority(rule.getTokenType(), rule.getPriority());
            }
        }
    }
    
    /**
     * Computes epsilon closure of a set of states.
     */
    private static Set<State> epsilonClosure(Set<State> states) {
        Set<State> closure = new HashSet<>(states);
        Stack<State> stack = new Stack<>();
        stack.addAll(states);
        
        while (!stack.isEmpty()) {
            State state = stack.pop();
            for (State epsilonState : state.getEpsilonTransitions()) {
                if (!closure.contains(epsilonState)) {
                    closure.add(epsilonState);
                    stack.push(epsilonState);
                }
            }
        }
        
        return closure;
    }
}