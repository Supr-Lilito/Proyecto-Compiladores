// Token.java
package com.compiler.lexer;

/**
 * Represents a token produced by the lexical analyzer.
 * Contains the token type, lexeme (matched text), and position information.
 */
public class Token {
    /**
     * The type of this token (e.g., IDENTIFIER, NUMBER, KEYWORD).
     */
    private final TokenType type;
    
    /**
     * The actual text that was matched to produce this token.
     */
    private final String lexeme;
    
    /**
     * The starting position of this token in the input string.
     */
    private final int startPosition;
    
    /**
     * The ending position of this token in the input string.
     */
    private final int endPosition;
    
    /**
     * Constructs a new token.
     * @param type The token type.
     * @param lexeme The matched text.
     * @param startPosition Starting position in input.
     * @param endPosition Ending position in input.
     */
    public Token(TokenType type, String lexeme, int startPosition, int endPosition) {
        this.type = type;
        this.lexeme = lexeme;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }
    
    // Getters
    public TokenType getType() { return type; }
    public String getLexeme() { return lexeme; }
    public int getStartPosition() { return startPosition; }
    public int getEndPosition() { return endPosition; }
    
    @Override
    public String toString() {
        return String.format("Token{type=%s, lexeme='%s', start=%d, end=%d}", 
                           type, lexeme, startPosition, endPosition);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Token token = (Token) obj;
        return startPosition == token.startPosition &&
               endPosition == token.endPosition &&
               type == token.type &&
               lexeme.equals(token.lexeme);
    }
}