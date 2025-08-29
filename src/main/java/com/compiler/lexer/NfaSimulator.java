package com.compiler.lexer;

import java.util.HashSet;
import java.util.Set;

import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.nfa.State;

/**
 * NfaSimulator
 * ------------
 * This class provides functionality to simulate a Non-deterministic Finite Automaton (NFA)
 * on a given input string. It determines whether the input string is accepted by the NFA by processing
 * each character and tracking the set of possible states, including those reachable via epsilon (ε) transitions.
 *
 * Simulation steps:
 * - Initialize the set of current states with the ε-closure of the NFA's start state.
 * - For each character in the input, compute the next set of states by following transitions labeled with that character,
 *   and include all states reachable via ε-transitions from those states.
 * - After processing the input, check if any of the current states is a final (accepting) state.
 *
 * The class also provides a helper method to compute the ε-closure of a given state, which is the set of all states
 * reachable from the given state using only ε-transitions.
 */
public class NfaSimulator {
    /**
     * Default constructor for NfaSimulator.
     */
    public NfaSimulator() {
        // Constructor doesn't need specific implementation
    }

    /**
     * Simulates the NFA on the given input string.
     * Starts at the NFA's start state and processes each character, following transitions and epsilon closures.
     * If any final state is reached after processing the input, the string is accepted.
     *
     * @param nfa The NFA to simulate.
     * @param input The input string to test.
     * @return True if the input is accepted by the NFA, false otherwise.
     */
    public boolean simulate(NFA nfa, String input) {
        Set<State> activeStates = new HashSet<>();
        addEpsilonClosure(nfa.startState, activeStates);
        
        int charPos = 0;
        while (charPos < input.length()) {
            char currentChar = input.charAt(charPos);
            Set<State> reachableStates = new HashSet<>();
            
            for (State s : activeStates) {
                for (State target : s.getTransitions(currentChar)) {
                    addEpsilonClosure(target, reachableStates);
                }
            }
            
            activeStates = reachableStates;
            charPos++;
        }
        
        boolean isAccepted = false;
        for (State s : activeStates) {
            if (s.isFinal()) {
                isAccepted = true;
                break;
            }
        }
        
        return isAccepted;
    }

    /**
     * Computes the epsilon-closure: all states reachable from 'start' using only epsilon (null) transitions.
     *
     * @param start The starting state.
     * @param closureSet The set to accumulate reachable states.
     */
    private void addEpsilonClosure(State start, Set<State> closureSet) {
        if (closureSet.contains(start)) {
            return;
        }
        
        closureSet.add(start);
        
        for (State epsReachable : start.getEpsilonTransitions()) {
            addEpsilonClosure(epsReachable, closureSet);
        }
    }
}