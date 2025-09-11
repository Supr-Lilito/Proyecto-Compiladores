package com.compiler.lexer;


/**
 * Represents a lexical rule: a pair of regular expression pattern and token type.
 * Used to define what patterns in the input should be recognized as which tokens.
 */
public class LexicalRule {
    /**
     * The regular expression pattern for this rule.
     */
    private final String pattern;
    
    /**
     * The token type to assign when this pattern matches.
     */
    private final TokenType tokenType;
    
    /**
     * Priority of this rule (higher numbers = higher priority).
     * Used to resolve conflicts when multiple patterns match.
     */
    private final int priority;
    
    /**
     * Constructs a new lexical rule.
     * @param pattern The regular expression pattern.
     * @param tokenType The token type to assign.
     * @param priority The priority of this rule.
     */
    public LexicalRule(String pattern, TokenType tokenType, int priority) {
        this.pattern = pattern;
        this.tokenType = tokenType;
        this.priority = priority;
    }
    
    /**
     * Constructs a new lexical rule with default priority 0.
     * @param pattern The regular expression pattern.
     * @param tokenType The token type to assign.
     */
    public LexicalRule(String pattern, TokenType tokenType) {
        this(pattern, tokenType, 0);
    }
    
    // Getters
    public String getPattern() { return pattern; }
    public TokenType getTokenType() { return tokenType; }
    public int getPriority() { return priority; }
    
    @Override
    public String toString() {
        return String.format("LexicalRule{pattern='%s', type=%s, priority=%d}", 
                           pattern, tokenType, priority);
    }
}
