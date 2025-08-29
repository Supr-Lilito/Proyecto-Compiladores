package com.compiler.lexer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;
import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.nfa.State;

/**
 * NfaToDfaConverter
 * -----------------
 * This class provides a static method to convert a Non-deterministic Finite Automaton (NFA)
 * into a Deterministic Finite Automaton (DFA) using the standard subset construction algorithm.
 */
public class NfaToDfaConverter {
    /**
     * Default constructor for NfaToDfaConverter.
     */
    public NfaToDfaConverter() {
        // Constructor doesn't need specific implementation
    }

    /**
     * Converts an NFA to a DFA using the subset construction algorithm.
     * Each DFA state represents a set of NFA states. Final states are marked if any NFA state in the set is final.
     *
     * @param nfa The input NFA
     * @param alphabet The input alphabet (set of characters)
     * @return The resulting DFA
     */
    public static DFA convertNfaToDfa(NFA nfa, Set<Character> alphabet) {
        Queue<DfaState> pendingStates = new LinkedList<>();
        List<DfaState> allDfaStates = new ArrayList<>();
        
        Set<State> startSet = new HashSet<>();
        startSet.add(nfa.startState);
        Set<State> startClosure = epsilonClosure(startSet);
        
        DfaState initialDfa = new DfaState(startClosure);
        allDfaStates.add(initialDfa);
        pendingStates.offer(initialDfa);
        
        while (pendingStates.size() > 0) {
            DfaState processingState = pendingStates.remove();
            
            for (char ch : alphabet) {
                Set<State> transitionStates = move(processingState.getNfaStates(), ch);
                
                if (transitionStates.size() > 0) {
                    Set<State> closureStates = epsilonClosure(transitionStates);
                    
                    DfaState existingState = findDfaState(allDfaStates, closureStates);
                    
                    if (existingState != null) {
                        processingState.addTransition(ch, existingState);
                    } else {
                        DfaState newDfaState = new DfaState(closureStates);
                        allDfaStates.add(newDfaState);
                        pendingStates.offer(newDfaState);
                        processingState.addTransition(ch, newDfaState);
                    }
                }
            }
        }
        
        int i = 0;
        while (i < allDfaStates.size()) {
            DfaState currentDfa = allDfaStates.get(i);
            boolean hasFinal = false;
            
            for (State nfaS : currentDfa.getNfaStates()) {
                if (nfaS.isFinal()) {
                    hasFinal = true;
                    break;
                }
            }
            
            if (hasFinal) {
                currentDfa.setFinal(true);
            }
            i++;
        }
        
        DFA resultDfa = new DFA(initialDfa, allDfaStates);
        return resultDfa;
    }

    /**
     * Computes the epsilon-closure of a set of NFA states.
     * The epsilon-closure is the set of states reachable by epsilon (null) transitions.
     *
     * @param states The set of NFA states.
     * @return The epsilon-closure of the input states.
     */
    private static Set<State> epsilonClosure(Set<State> states) {
        Stack<State> workStack = new Stack<>();
        Set<State> reachableStates = new HashSet<>();
        
        for (State s : states) {
            workStack.push(s);
            reachableStates.add(s);
        }
        
        while (!workStack.empty()) {
            State current = workStack.pop();
            
            for (State epsNext : current.getEpsilonTransitions()) {
                if (reachableStates.contains(epsNext)) {
                    continue;
                }
                reachableStates.add(epsNext);
                workStack.push(epsNext);
            }
        }
        
        return reachableStates;
    }

    /**
     * Returns the set of states reachable from a set of NFA states by a given symbol.
     *
     * @param states The set of NFA states.
     * @param symbol The input symbol.
     * @return The set of reachable states.
     */
    private static Set<State> move(Set<State> states, char symbol) {
        Set<State> targetStates = new HashSet<>();
        
        for (State sourceState : states) {
            List<State> destinations = sourceState.getTransitions(symbol);
            for (State dest : destinations) {
                targetStates.add(dest);
            }
        }
        
        return targetStates;
    }

    /**
     * Finds an existing DFA state representing a given set of NFA states.
     *
     * @param dfaStates The list of DFA states.
     * @param targetNfaStates The set of NFA states to search for.
     * @return The matching DFA state, or null if not found.
     */
    private static DfaState findDfaState(List<DfaState> dfaStates, Set<State> targetNfaStates) {
        int idx = 0;
        while (idx < dfaStates.size()) {
            DfaState candidate = dfaStates.get(idx);
            if (candidate.getNfaStates().equals(targetNfaStates)) {
                return candidate;
            }
            idx++;
        }
        return null;
    }
}