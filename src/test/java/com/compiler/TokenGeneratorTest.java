// TokenGeneratorTest.java
package com.compiler;

import com.compiler.lexer.SimpleTokenGenerator;
import com.compiler.lexer.LexicalRule;
import com.compiler.lexer.Token;
import com.compiler.lexer.TokenType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for the SimpleTokenGenerator.
 * Tests tokenization of various input strings with multiple lexical rules,
 * demonstrating longest match principle and priority handling.
 */
public class TokenGeneratorTest {
    
    private SimpleTokenGenerator tokenGenerator;
    
    @BeforeEach
    void setUp() {
        // Define lexical rules for a simple programming language
        List<LexicalRule> rules = Arrays.asList(
            // Keywords (high priority to override identifiers)
            new LexicalRule("if", TokenType.IF, 10),
            new LexicalRule("else", TokenType.ELSE, 10),
            new LexicalRule("while", TokenType.WHILE, 10),
            new LexicalRule("for", TokenType.FOR, 10),
            new LexicalRule("function", TokenType.FUNCTION, 10),
            new LexicalRule("var", TokenType.VAR, 10),
            new LexicalRule("return", TokenType.RETURN, 10),
            
            // Identifiers (letters followed by letters/digits/underscores)
            new LexicalRule("(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z|A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z)(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z|A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z|0|1|2|3|4|5|6|7|8|9|_)*", TokenType.IDENTIFIER, 5),
            
            // Numbers (one or more digits)
            new LexicalRule("(0|1|2|3|4|5|6|7|8|9)(0|1|2|3|4|5|6|7|8|9)*", TokenType.NUMBER, 5),
            
            // Two-character operators (higher priority)
            new LexicalRule("==", TokenType.EQUALS, 8),
            new LexicalRule("!=", TokenType.NOT_EQUALS, 8),
            
            // Single-character operators and symbols
            new LexicalRule("=", TokenType.ASSIGN, 6),
            new LexicalRule("+", TokenType.PLUS, 6),        // Will be handled manually
            new LexicalRule("-", TokenType.MINUS, 6),
            new LexicalRule("*", TokenType.MULTIPLY, 6),    // Will be handled manually
            new LexicalRule("/", TokenType.DIVIDE, 6),
            new LexicalRule("<", TokenType.LESS_THAN, 6),
            new LexicalRule(">", TokenType.GREATER_THAN, 6),
            
            // Delimiters
            new LexicalRule(";", TokenType.SEMICOLON, 6),
            new LexicalRule(",", TokenType.COMMA, 6),
            new LexicalRule("(", TokenType.LEFT_PAREN, 6),  // Will be handled manually
            new LexicalRule(")", TokenType.RIGHT_PAREN, 6), // Will be handled manually
            new LexicalRule("{", TokenType.LEFT_BRACE, 6),
            new LexicalRule("}", TokenType.RIGHT_BRACE, 6),
            
            // Whitespace
            new LexicalRule(" ", TokenType.WHITESPACE, 1)
        );
        
        tokenGenerator = new SimpleTokenGenerator(rules);
    }
    
    @Test
    void testSimpleExpression() {
        String input = "x = 42 + y";
        List<Token> tokens = tokenGenerator.tokenize(input);
        
        // Expected: IDENTIFIER(x), ASSIGN(=), NUMBER(42), PLUS(+), IDENTIFIER(y), EOF
        assertEquals(6, tokens.size());
        
        assertToken(tokens.get(0), TokenType.IDENTIFIER, "x", 0, 1);
        assertToken(tokens.get(1), TokenType.ASSIGN, "=", 2, 3);
        assertToken(tokens.get(2), TokenType.NUMBER, "42", 4, 6);
        assertToken(tokens.get(3), TokenType.PLUS, "+", 7, 8);
        assertToken(tokens.get(4), TokenType.IDENTIFIER, "y", 9, 10);
        assertToken(tokens.get(5), TokenType.EOF, "", 10, 10);
    }
    
    @Test
    void testKeywordVsIdentifier() {
        String input = "if ifVar else elseif";
        List<Token> tokens = tokenGenerator.tokenize(input);
        
        // Expected: IF(if), IDENTIFIER(ifVar), ELSE(else), IDENTIFIER(elseif), EOF
        assertEquals(5, tokens.size());
        
        assertToken(tokens.get(0), TokenType.IF, "if", 0, 2);
        assertToken(tokens.get(1), TokenType.IDENTIFIER, "ifVar", 3, 8);
        assertToken(tokens.get(2), TokenType.ELSE, "else", 9, 13);
        assertToken(tokens.get(3), TokenType.IDENTIFIER, "elseif", 14, 20);
        assertToken(tokens.get(4), TokenType.EOF, "", 20, 20);
    }
    
    @Test
    void testOperatorPrecedence() {
        String input = "a == b != c = d";
        List<Token> tokens = tokenGenerator.tokenize(input);
        
        // Expected: IDENTIFIER(a), EQUALS(==), IDENTIFIER(b), NOT_EQUALS(!=), 
        //          IDENTIFIER(c), ASSIGN(=), IDENTIFIER(d), EOF
        assertEquals(8, tokens.size());
        
        assertToken(tokens.get(0), TokenType.IDENTIFIER, "a", 0, 1);
        assertToken(tokens.get(1), TokenType.EQUALS, "==", 2, 4);
        assertToken(tokens.get(2), TokenType.IDENTIFIER, "b", 5, 6);
        assertToken(tokens.get(3), TokenType.NOT_EQUALS, "!=", 7, 9);
        assertToken(tokens.get(4), TokenType.IDENTIFIER, "c", 10, 11);
        assertToken(tokens.get(5), TokenType.ASSIGN, "=", 12, 13);
        assertToken(tokens.get(6), TokenType.IDENTIFIER, "d", 14, 15);
        assertToken(tokens.get(7), TokenType.EOF, "", 15, 15);
    }
    
    @Test
    void testComplexProgram() {
        String input = "function factorial(n) {\n" +
                      "  if (n == 0) {\n" +
                      "    return 1;\n" +
                      "  } else {\n" +
                      "    return n * factorial(n - 1);\n" +
                      "  }\n" +
                      "}";
        
        List<Token> tokens = tokenGenerator.tokenize(input);
        
        // Verify key tokens (not all for brevity)
        assertTrue(containsToken(tokens, TokenType.FUNCTION, "function"));
        assertTrue(containsToken(tokens, TokenType.IDENTIFIER, "factorial"));
        assertTrue(containsToken(tokens, TokenType.LEFT_PAREN, "("));
        assertTrue(containsToken(tokens, TokenType.IDENTIFIER, "n"));
        assertTrue(containsToken(tokens, TokenType.RIGHT_PAREN, ")"));
        assertTrue(containsToken(tokens, TokenType.LEFT_BRACE, "{"));
        assertTrue(containsToken(tokens, TokenType.IF, "if"));
        assertTrue(containsToken(tokens, TokenType.EQUALS, "=="));
        assertTrue(containsToken(tokens, TokenType.NUMBER, "0"));
        assertTrue(containsToken(tokens, TokenType.RETURN, "return"));
        assertTrue(containsToken(tokens, TokenType.NUMBER, "1"));
        assertTrue(containsToken(tokens, TokenType.SEMICOLON, ";"));
        assertTrue(containsToken(tokens, TokenType.ELSE, "else"));
        assertTrue(containsToken(tokens, TokenType.MULTIPLY, "*"));
        assertTrue(containsToken(tokens, TokenType.MINUS, "-"));
        assertTrue(containsToken(tokens, TokenType.RIGHT_BRACE, "}"));
        
        // Verify that the last token is EOF
        assertEquals(TokenType.EOF, tokens.get(tokens.size() - 1).getType());
    }
    
    @Test
    void testLongestMatchPrinciple() {
        // Test that "==" is recognized as EQUALS, not ASSIGN followed by ASSIGN
        String input = "a==b";
        List<Token> tokens = tokenGenerator.tokenize(input);
        
        assertEquals(4, tokens.size());
        assertToken(tokens.get(0), TokenType.IDENTIFIER, "a", 0, 1);
        assertToken(tokens.get(1), TokenType.EQUALS, "==", 1, 3);
        assertToken(tokens.get(2), TokenType.IDENTIFIER, "b", 3, 4);
        assertToken(tokens.get(3), TokenType.EOF, "", 4, 4);
    }
    
    @Test
    void testNumbersAndIdentifiers() {
        String input = "var x123 = 456 + y789";
        List<Token> tokens = tokenGenerator.tokenize(input);
        
        assertEquals(7, tokens.size());
        assertToken(tokens.get(0), TokenType.VAR, "var", 0, 3);
        assertToken(tokens.get(1), TokenType.IDENTIFIER, "x123", 4, 8);
        assertToken(tokens.get(2), TokenType.ASSIGN, "=", 9, 10);
        assertToken(tokens.get(3), TokenType.NUMBER, "456", 11, 14);
        assertToken(tokens.get(4), TokenType.PLUS, "+", 15, 16);
        assertToken(tokens.get(5), TokenType.IDENTIFIER, "y789", 17, 21);
        assertToken(tokens.get(6), TokenType.EOF, "", 21, 21);
    }
    
    @Test
    void testUnknownCharacters() {
        String input = "a @ b # c";
        List<Token> tokens = tokenGenerator.tokenize(input);
        
        assertEquals(6, tokens.size());
        assertToken(tokens.get(0), TokenType.IDENTIFIER, "a", 0, 1);
        assertToken(tokens.get(1), TokenType.UNKNOWN, "@", 2, 3);
        assertToken(tokens.get(2), TokenType.IDENTIFIER, "b", 4, 5);
        assertToken(tokens.get(3), TokenType.UNKNOWN, "#", 6, 7);
        assertToken(tokens.get(4), TokenType.IDENTIFIER, "c", 8, 9);
        assertToken(tokens.get(5), TokenType.EOF, "", 9, 9);
    }
    
    @ParameterizedTest
    @CsvSource({
        "'if (x > 0) { return x; }',           'IF LEFT_PAREN IDENTIFIER GREATER_THAN NUMBER RIGHT_PAREN LEFT_BRACE RETURN IDENTIFIER SEMICOLON RIGHT_BRACE EOF'",
        "'while (i < 10) i = i + 1;',         'WHILE LEFT_PAREN IDENTIFIER LESS_THAN NUMBER RIGHT_PAREN IDENTIFIER ASSIGN IDENTIFIER PLUS NUMBER SEMICOLON EOF'",
        "'function add(a, b) { return a + b; }', 'FUNCTION IDENTIFIER LEFT_PAREN IDENTIFIER COMMA IDENTIFIER RIGHT_PAREN LEFT_BRACE RETURN IDENTIFIER PLUS IDENTIFIER SEMICOLON RIGHT_BRACE EOF'",
        "'var result = factorial(5);',        'VAR IDENTIFIER ASSIGN IDENTIFIER LEFT_PAREN NUMBER RIGHT_PAREN SEMICOLON EOF'"
    })
    void testVariousExpressions(String input, String expectedTokenTypes) {
        List<Token> tokens = tokenGenerator.tokenize(input);
        String[] expected = expectedTokenTypes.split(" ");
        
        assertEquals(expected.length, tokens.size(), 
                    "Token count mismatch for input: " + input);
        
        for (int i = 0; i < expected.length; i++) {
            assertEquals(TokenType.valueOf(expected[i]), tokens.get(i).getType(),
                        "Token type mismatch at position " + i + " for input: " + input);
        }
    }
    
    @Test
    void testCompleteTokenizationBreakdown() {
        String input = "for (var i = 0; i < 10; i = i + 1) { }";
        List<Token> tokens = tokenGenerator.tokenize(input);
        
        System.out.println("=== Complete Tokenization Breakdown ===");
        System.out.println("Input: \"" + input + "\"");
        System.out.println("Tokens found:");
        
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            System.out.printf("  [%2d] %-15s '%s' (pos %d-%d)%n", 
                            i, token.getType(), token.getLexeme(), 
                            token.getStartPosition(), token.getEndPosition());
        }
        
        // Verify specific tokens
        int tokenIndex = 0;
        assertToken(tokens.get(tokenIndex++), TokenType.FOR, "for", 0, 3);
        assertToken(tokens.get(tokenIndex++), TokenType.LEFT_PAREN, "(", 4, 5);
        assertToken(tokens.get(tokenIndex++), TokenType.VAR, "var", 5, 8);
        assertToken(tokens.get(tokenIndex++), TokenType.IDENTIFIER, "i", 9, 10);
        assertToken(tokens.get(tokenIndex++), TokenType.ASSIGN, "=", 11, 12);
        assertToken(tokens.get(tokenIndex++), TokenType.NUMBER, "0", 13, 14);
        assertToken(tokens.get(tokenIndex++), TokenType.SEMICOLON, ";", 14, 15);
        assertToken(tokens.get(tokenIndex++), TokenType.IDENTIFIER, "i", 16, 17);
        assertToken(tokens.get(tokenIndex++), TokenType.LESS_THAN, "<", 18, 19);
        assertToken(tokens.get(tokenIndex++), TokenType.NUMBER, "10", 20, 22);
        assertToken(tokens.get(tokenIndex++), TokenType.SEMICOLON, ";", 22, 23);
        assertToken(tokens.get(tokenIndex++), TokenType.IDENTIFIER, "i", 24, 25);
        assertToken(tokens.get(tokenIndex++), TokenType.ASSIGN, "=", 26, 27);
        assertToken(tokens.get(tokenIndex++), TokenType.IDENTIFIER, "i", 28, 29);
        assertToken(tokens.get(tokenIndex++), TokenType.PLUS, "+", 30, 31);
        assertToken(tokens.get(tokenIndex++), TokenType.NUMBER, "1", 32, 33);
        assertToken(tokens.get(tokenIndex++), TokenType.RIGHT_PAREN, ")", 33, 34);
        assertToken(tokens.get(tokenIndex++), TokenType.LEFT_BRACE, "{", 35, 36);
        assertToken(tokens.get(tokenIndex++), TokenType.RIGHT_BRACE, "}", 37, 38);
        assertToken(tokens.get(tokenIndex++), TokenType.EOF, "", 38, 38);
    }
    
    // Helper methods
    private void assertToken(Token actual, TokenType expectedType, String expectedLexeme, 
                           int expectedStart, int expectedEnd) {
        assertEquals(expectedType, actual.getType(), 
                    "Token type mismatch");
        assertEquals(expectedLexeme, actual.getLexeme(), 
                    "Token lexeme mismatch");
        assertEquals(expectedStart, actual.getStartPosition(), 
                    "Token start position mismatch");
        assertEquals(expectedEnd, actual.getEndPosition(), 
                    "Token end position mismatch");
    }
    
    private boolean containsToken(List<Token> tokens, TokenType expectedType, String expectedLexeme) {
        return tokens.stream().anyMatch(token -> 
            token.getType() == expectedType && token.getLexeme().equals(expectedLexeme));
    }
    
    @Test
    void testEmptyInput() {
        String input = "";
        List<Token> tokens = tokenGenerator.tokenize(input);
        
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), TokenType.EOF, "", 0, 0);
    }
}