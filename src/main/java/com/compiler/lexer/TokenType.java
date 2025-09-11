package com.compiler.lexer;

/**
 * Enumeration of different token types that can be recognized by the lexer.
 */
public enum TokenType {
    // Literals
    NUMBER("NUMBER"),
    IDENTIFIER("IDENTIFIER"),
    STRING("STRING"),
    
    // Keywords
    IF("IF"),
    ELSE("ELSE"),
    WHILE("WHILE"),
    FOR("FOR"),
    FUNCTION("FUNCTION"),
    VAR("VAR"),
    RETURN("RETURN"),
    
    // Operators
    PLUS("PLUS"),
    MINUS("MINUS"),
    MULTIPLY("MULTIPLY"),
    DIVIDE("DIVIDE"),
    ASSIGN("ASSIGN"),
    EQUALS("EQUALS"),
    NOT_EQUALS("NOT_EQUALS"),
    LESS_THAN("LESS_THAN"),
    GREATER_THAN("GREATER_THAN"),
    
    // Delimiters
    SEMICOLON("SEMICOLON"),
    COMMA("COMMA"),
    LEFT_PAREN("LEFT_PAREN"),
    RIGHT_PAREN("RIGHT_PAREN"),
    LEFT_BRACE("LEFT_BRACE"),
    RIGHT_BRACE("RIGHT_BRACE"),
    
    // Special
    WHITESPACE("WHITESPACE"),
    EOF("EOF"),
    UNKNOWN("UNKNOWN");
    
    private final String name;
    
    TokenType(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
