// SimpleTokenGenerator.java
package com.compiler.lexer;

import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.nfa.State;
import com.compiler.lexer.nfa.Transition;
import com.compiler.lexer.regex.RegexParser;
import com.compiler.lexer.LexicalRule;
import com.compiler.lexer.Token;
import com.compiler.lexer.TokenType;

import java.util.*;

/**
 * Simplified token generator that handles basic patterns without complex regex.
 * Creates NFAs manually for symbols that can't be expressed in our regex parser.
 */
public class SimpleTokenGenerator {
    private final List<TokenRule> rules;
    
    /**
     * Internal representation of a token rule with its NFA.
     */
    private static class TokenRule {
        final NFA nfa;
        final TokenType tokenType;
        final int priority;
        
        TokenRule(NFA nfa, TokenType tokenType, int priority) {
            this.nfa = nfa;
            this.tokenType = tokenType;
            this.priority = priority;
        }
    }
    
    public SimpleTokenGenerator(List<LexicalRule> lexicalRules) {
        this.rules = new ArrayList<>();
        RegexParser parser = new RegexParser();
        
        for (LexicalRule rule : lexicalRules) {
            NFA nfa;
            try {
                // Try to parse with regex parser first
                nfa = parser.parse(rule.getPattern());
            } catch (Exception e) {
                // If regex parsing fails, create NFA manually for simple patterns
                nfa = createManualNFA(rule.getPattern());
            }
            
            if (nfa != null) {
                rules.add(new TokenRule(nfa, rule.getTokenType(), rule.getPriority()));
            }
        }
    }
    
    /**
     * Creates NFAs manually for simple single-character patterns.
     */
    private NFA createManualNFA(String pattern) {
        // Handle single character patterns
        if (pattern.length() == 1) {
            State start = new State();
            State end = new State();
            start.transitions.add(new Transition(pattern.charAt(0), end));
            return new NFA(start, end);
        }
        
        // Handle two-character patterns like "=="
        if (pattern.equals("==")) {
            State start = new State();
            State middle = new State();
            State end = new State();
            start.transitions.add(new Transition('=', middle));
            middle.transitions.add(new Transition('=', end));
            return new NFA(start, end);
        }
        
        if (pattern.equals("!=")) {
            State start = new State();
            State middle = new State();
            State end = new State();
            start.transitions.add(new Transition('!', middle));
            middle.transitions.add(new Transition('=', end));
            return new NFA(start, end);
        }
        
        // Handle simple whitespace pattern " "
        if (pattern.equals(" ")) {
            State start = new State();
            State end = new State();
            start.transitions.add(new Transition(' ', end));
            return new NFA(start, end);
        }
        
        return null;
    }
    
    /**
     * Tokenizes input using simple pattern matching.
     */
    public List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        int position = 0;
        
        while (position < input.length()) {
            // Skip whitespace characters at the beginning
            char currentChar = input.charAt(position);
            if (Character.isWhitespace(currentChar)) {
                position++;
                continue;
            }
            
            TokenMatch bestMatch = null;
            
            // Try all rules and find the longest match
            for (TokenRule rule : rules) {
                TokenMatch match = tryMatch(rule, input, position);
                if (match != null) {
                    if (bestMatch == null || 
                        match.length > bestMatch.length ||
                        (match.length == bestMatch.length && match.priority > bestMatch.priority)) {
                        bestMatch = match;
                    }
                }
            }
            
            if (bestMatch != null) {
                // Only add non-whitespace tokens
                if (bestMatch.tokenType != TokenType.WHITESPACE) {
                    Token token = new Token(bestMatch.tokenType, bestMatch.lexeme, 
                                          position, position + bestMatch.length);
                    tokens.add(token);
                }
                position += bestMatch.length;
            } else {
                // Handle unknown non-whitespace character
                tokens.add(new Token(TokenType.UNKNOWN, 
                                   String.valueOf(currentChar), 
                                   position, position + 1));
                position++;
            }
        }
        
        tokens.add(new Token(TokenType.EOF, "", position, position));
        return tokens;
    }
    
    /**
     * Checks if a string consists entirely of whitespace characters.
     */
    private boolean isWhitespace(String str) {
        return str.chars().allMatch(Character::isWhitespace);
    }
    
    /**
     * Tries to match a rule at the given position.
     */
    private TokenMatch tryMatch(TokenRule rule, String input, int startPos) {
        // Simple NFA simulation
        Set<State> currentStates = new HashSet<>();
        currentStates.add(rule.nfa.startState);
        currentStates = epsilonClosure(currentStates);
        
        int position = startPos;
        int lastAcceptPos = -1;
        
        // Check if start state is accepting
        if (currentStates.contains(rule.nfa.endState)) {
            lastAcceptPos = position;
        }
        
        while (position < input.length() && !currentStates.isEmpty()) {
            char c = input.charAt(position);
            Set<State> nextStates = new HashSet<>();
            
            for (State state : currentStates) {
                for (Transition trans : state.transitions) {
                    if (trans.symbol != null && trans.symbol == c) {
                        nextStates.add(trans.toState);
                    }
                }
            }
            
            if (nextStates.isEmpty()) {
                break;
            }
            
            currentStates = epsilonClosure(nextStates);
            position++;
            
            // Check if any current state is accepting
            if (currentStates.contains(rule.nfa.endState)) {
                lastAcceptPos = position;
            }
        }
        
        if (lastAcceptPos > startPos) {
            return new TokenMatch(rule.tokenType, 
                                input.substring(startPos, lastAcceptPos),
                                lastAcceptPos - startPos,
                                rule.priority);
        }
        
        return null;
    }
    
    /**
     * Computes epsilon closure of states.
     */
    private Set<State> epsilonClosure(Set<State> states) {
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
    
    /**
     * Helper class for token matches.
     */
    private static class TokenMatch {
        final TokenType tokenType;
        final String lexeme;
        final int length;
        final int priority;
        
        TokenMatch(TokenType tokenType, String lexeme, int length, int priority) {
            this.tokenType = tokenType;
            this.lexeme = lexeme;
            this.length = length;
            this.priority = priority;
        }
    }
}