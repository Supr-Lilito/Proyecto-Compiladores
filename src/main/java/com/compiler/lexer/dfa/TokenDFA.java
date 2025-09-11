package com.compiler.lexer.dfa;
// TokenDFA.java  

import java.util.List;

/**
 * Extended DFA that can produce tokens.
 * Contains TokenDfaState instances that hold token type information.
 */
public class TokenDFA extends DFA {
    /**
     * Constructs a new TokenDFA.
     * @param startState The starting state (must be a TokenDfaState).
     * @param allStates A list of all states (must be TokenDfaState instances).
     */
    public TokenDFA(TokenDfaState startState, List<TokenDfaState> allStates) {
        super(startState, (List<DfaState>)(List<?>)allStates);
    }
    
    /**
     * Returns the starting state as a TokenDfaState.
     * @return The start state.
     */
    public TokenDfaState getTokenStartState() {
        return (TokenDfaState) startState;
    }
    
    /**
     * Returns all states as TokenDfaState instances.
     * @return List of all TokenDFA states.
     */
    @SuppressWarnings("unchecked")
    public List<TokenDfaState> getTokenStates() {
        return (List<TokenDfaState>)(List<?>) allStates;
    }
}
