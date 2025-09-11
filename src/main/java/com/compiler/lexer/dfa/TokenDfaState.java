package com.compiler.lexer.dfa;
// TokenDfaState.java

import com.compiler.lexer.nfa.State;
import com.compiler.lexer.TokenType;
import java.util.Set;

/**
 * Extended DFA state that can hold token information.
 * When this state is final, it indicates which token type should be produced.
 */
public class TokenDfaState extends DfaState {
    /**
     * The token type associated with this final state.
     * Null if this state is not final or doesn't produce a token.
     */
    private TokenType tokenType;
    
    /**
     * Priority of the token type for conflict resolution.
     * Higher values indicate higher priority.
     */
    private int priority;
    
    /**
     * Constructs a new TokenDfaState.
     * @param nfaStates The set of NFA states this DFA state represents.
     */
    public TokenDfaState(Set<State> nfaStates) {
        super(nfaStates);
        this.tokenType = null;
        this.priority = -1;
    }
    
    /**
     * Sets the token information for this state.
     * @param tokenType The token type this state should produce.
     * @param priority The priority of this token type.
     */
    public void setTokenInfo(TokenType tokenType, int priority) {
        this.tokenType = tokenType;
        this.priority = priority;
        this.setFinal(true);
    }
    
    /**
     * Updates token information if the new priority is higher.
     * @param tokenType The candidate token type.
     * @param priority The candidate priority.
     */
    public void updateTokenInfoIfHigherPriority(TokenType tokenType, int priority) {
        if (this.tokenType == null || priority > this.priority) {
            setTokenInfo(tokenType, priority);
        }
    }
    
    /**
     * Gets the token type for this state.
     * @return The token type, or null if not a token-producing state.
     */
    public TokenType getTokenType() {
        return tokenType;
    }
    
    /**
     * Gets the priority of this state's token type.
     * @return The priority.
     */
    public int getPriority() {
        return priority;
    }
    
    @Override
    public String toString() {
        String tokenInfo = tokenType != null ? ", token=" + tokenType + ", priority=" + priority : "";
        return "TokenDfaState{" +
                "id=" + id +
                ", isFinal=" + isFinal() +
                tokenInfo +
                ", nfaStates=" + nfaStates.size() + " states" +
                '}';
    }
}
