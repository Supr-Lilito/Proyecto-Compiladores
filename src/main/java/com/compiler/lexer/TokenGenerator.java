
// TokenGenerator.java
package com.compiler.lexer;

import com.compiler.lexer.dfa.TokenDFA;
import com.compiler.lexer.dfa.TokenDfaState;
import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.regex.RegexParser;
import com.compiler.lexer.LexicalRule;
import com.compiler.lexer.Token;
import com.compiler.lexer.TokenType;

import java.util.*;

/**
 * Main tokenizer class that combines multiple lexical rules and applies
 * the longest match principle to tokenize input strings.
 */
public class TokenGenerator {
    private final TokenDFA combinedDFA;
    private final Set<Character> alphabet;
    private final List<LexicalRule> rules;
    
    /**
     * Constructs a TokenGenerator from a list of lexical rules.
     * @param lexicalRules List of lexical rules to use for tokenization.
     */
    public TokenGenerator(List<LexicalRule> lexicalRules) {
        this.rules = new ArrayList<>(lexicalRules);
        this.alphabet = extractAlphabet(lexicalRules);
        this.combinedDFA = buildCombinedDFA(lexicalRules);
    }
    
    /**
     * Tokenizes an input string according to the lexical rules.
     * Applies longest match principle and handles conflicts by priority.
     * @param input The input string to tokenize.
     * @return List of tokens found in the input.
     */
    public List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        int position = 0;
        
        while (position < input.length()) {
            TokenMatch match = findLongestMatch(input, position);
            
            if (match != null) {
                // Create token from match
                Token token = new Token(match.tokenType, match.lexeme, 
                                      match.startPosition, match.endPosition);
                
                // Skip whitespace tokens (optional)
                if (match.tokenType != TokenType.WHITESPACE) {
                    tokens.add(token);
                }
                
                position = match.endPosition;
            } else {
                // Handle unrecognized character
                tokens.add(new Token(TokenType.UNKNOWN, 
                                   String.valueOf(input.charAt(position)), 
                                   position, position + 1));
                position++;
            }
        }
        
        // Add EOF token
        tokens.add(new Token(TokenType.EOF, "", position, position));
        return tokens;
    }
    
    /**
     * Finds the longest match starting at the given position.
     * @param input The input string.
     * @param startPosition Starting position for matching.
     * @return TokenMatch if found, null if no match.
     */
    private TokenMatch findLongestMatch(String input, int startPosition) {
        TokenDfaState currentState = combinedDFA.getTokenStartState();
        TokenMatch lastValidMatch = null;
        int currentPosition = startPosition;
        
        while (currentPosition < input.length()) {
            char currentChar = input.charAt(currentPosition);
            
            // Try to transition on current character
            TokenDfaState nextState = (TokenDfaState) currentState.getTransition(currentChar);
            
            if (nextState == null) {
                // No more transitions possible
                break;
            }
            
            currentState = nextState;
            currentPosition++;
            
            // If current state is final, update last valid match
            if (currentState.isFinal() && currentState.getTokenType() != null) {
                lastValidMatch = new TokenMatch(
                    currentState.getTokenType(),
                    input.substring(startPosition, currentPosition),
                    startPosition,
                    currentPosition
                );
            }
        }
        
        return lastValidMatch;
    }
    
    /**
     * Builds the combined DFA from all lexical rules.
     */
    private TokenDFA buildCombinedDFA(List<LexicalRule> lexicalRules) {
        Map<NFA, LexicalRule> nfaRules = new HashMap<>();
        RegexParser parser = new RegexParser();
        
        // Convert each rule to NFA
        for (LexicalRule rule : lexicalRules) {
            try {
                NFA nfa = parser.parse(rule.getPattern());
                nfaRules.put(nfa, rule);
            } catch (Exception e) {
                throw new RuntimeException("Error parsing regex pattern: " + rule.getPattern(), e);
            }
        }
        
        // Convert to combined TokenDFA
        return NfaToTokenDfaConverter.convertMultipleNfasToTokenDfa(nfaRules, alphabet);
    }
    
    /**
     * Extracts the alphabet from lexical rule patterns.
     */
    private Set<Character> extractAlphabet(List<LexicalRule> lexicalRules) {
        Set<Character> alphabet = new HashSet<>();
        
        // Add common characters that might appear in patterns
        for (char c = 'a'; c <= 'z'; c++) alphabet.add(c);
        for (char c = 'A'; c <= 'Z'; c++) alphabet.add(c);
        for (char c = '0'; c <= '9'; c++) alphabet.add(c);
        
        // Add common symbols
        alphabet.addAll(Arrays.asList(' ', '\t', '\n', '\r', '+', '-', '*', '/', 
                                    '=', '(', ')', '{', '}', ';', ',', '<', '>', '_'));
        
        return alphabet;
    }
    
    /**
     * Helper class to represent a token match.
     */
    private static class TokenMatch {
        final TokenType tokenType;
        final String lexeme;
        final int startPosition;
        final int endPosition;
        
        TokenMatch(TokenType tokenType, String lexeme, int startPosition, int endPosition) {
            this.tokenType = tokenType;
            this.lexeme = lexeme;
            this.startPosition = startPosition;
            this.endPosition = endPosition;
        }
    }
}