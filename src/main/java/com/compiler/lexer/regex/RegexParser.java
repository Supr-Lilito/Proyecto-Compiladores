package com.compiler.lexer.regex;

import java.util.Stack;

import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.nfa.State;
import com.compiler.lexer.nfa.Transition;

/**
 * RegexParser
 * -----------
 * This class provides functionality to convert infix regular expressions into nondeterministic finite automata (NFA)
 * using Thompson's construction algorithm. It supports standard regex operators: concatenation (·), union (|),
 * Kleene star (*), optional (?), and plus (+). The conversion process uses the Shunting Yard algorithm to transform
 * infix regex into postfix notation, then builds the corresponding NFA.
 *
 * Features:
 * - Parses infix regular expressions and converts them to NFA.
 * - Supports regex operators: concatenation, union, Kleene star, optional, plus.
 * - Implements Thompson's construction rules for NFA generation.
 *
 * Example usage:
 * <pre>
 *     RegexParser parser = new RegexParser();
 *     NFA nfa = parser.parse("a(b|c)*");
 * </pre>
 */
public class RegexParser {
    /**
     * Default constructor for RegexParser.
     */
    public RegexParser() {
        // Constructor doesn't need specific implementation
    }

    /**
     * Converts an infix regular expression to an NFA.
     *
     * @param infixRegex The regular expression in infix notation.
     * @return The constructed NFA.
     */
    public NFA parse(String infixRegex) {
        String rpnExpression = ShuntingYard.toPostfix(infixRegex);
        return buildNfaFromPostfix(rpnExpression);
    }

    /**
     * Builds an NFA from a postfix regular expression.
     *
     * @param postfixRegex The regular expression in postfix notation.
     * @return The constructed NFA.
     */
    private NFA buildNfaFromPostfix(String postfixRegex) {
        Stack<NFA> automataStack = new Stack<>();
        
        int idx = 0;
        while (idx < postfixRegex.length()) {
            char symbol = postfixRegex.charAt(idx);
            
            if (!isOperand(symbol)) {
                if (symbol == '·') {
                    handleConcatenation(automataStack);
                } else if (symbol == '|') {
                    handleUnion(automataStack);
                } else if (symbol == '*') {
                    handleKleeneStar(automataStack);
                } else if (symbol == '+') {
                    handlePlus(automataStack);
                } else if (symbol == '?') {
                    handleOptional(automataStack);
                } else {
                    throw new IllegalArgumentException("Invalid operator encountered: " + symbol);
                }
            } else {
                NFA charNfa = createNfaForCharacter(symbol);
                automataStack.push(charNfa);
            }
            idx++;
        }
        
        if (automataStack.size() == 1) {
            return automataStack.pop();
        }
        throw new IllegalStateException("Malformed postfix expression");
    }

    /**
     * Handles the '?' operator (zero or one occurrence).
     * Pops an NFA from the stack and creates a new NFA that accepts zero or one occurrence.
     * @param stack The NFA stack.
     */
    private void handleOptional(Stack<NFA> stack) {
        if (stack.size() < 1) {
            throw new IllegalStateException("Insufficient operands for optional");
        }
        
        NFA operand = stack.pop();
        State startNode = new State();
        State endNode = new State();
        
        startNode.transitions.add(new Transition(null, endNode));
        startNode.transitions.add(new Transition(null, operand.startState));
        
        operand.endState.isFinal = false;
        operand.endState.transitions.add(new Transition(null, endNode));
        
        NFA optionalNfa = new NFA(startNode, endNode);
        stack.push(optionalNfa);
    }

    /**
     * Handles the '+' operator (one or more occurrences).
     * Pops an NFA from the stack and creates a new NFA that accepts one or more occurrences.
     * @param stack The NFA stack.
     */
    private void handlePlus(Stack<NFA> stack) {
        if (stack.size() == 0) {
            throw new IllegalStateException("No operand for plus operator");
        }
        
        NFA base = stack.pop();
        State initialState = new State();
        State finalState = new State();
        
        base.endState.transitions.add(new Transition(null, base.startState));
        base.endState.transitions.add(new Transition(null, finalState));
        initialState.transitions.add(new Transition(null, base.startState));
        
        base.endState.isFinal = false;
        
        NFA plusNfa = new NFA(initialState, finalState);
        stack.push(plusNfa);
    }
    
    /**
     * Creates an NFA for a single character.
     * @param c The character to create an NFA for.
     * @return The constructed NFA.
     */
    private NFA createNfaForCharacter(char c) {
        State initialNode = new State();
        State terminalNode = new State();
        initialNode.transitions.add(new Transition(c, terminalNode));
        NFA result = new NFA(initialNode, terminalNode);
        return result;
    }

    /**
     * Handles the concatenation operator (·).
     * Pops two NFAs from the stack and connects them in sequence.
     * @param stack The NFA stack.
     */
    private void handleConcatenation(Stack<NFA> stack) {
        if (stack.size() <= 1) {
            throw new IllegalStateException("Concatenation requires two operands");
        }
        
        NFA rightOperand = stack.pop();
        NFA leftOperand = stack.pop();
        
        leftOperand.endState.isFinal = false;
        leftOperand.endState.transitions.add(new Transition(null, rightOperand.startState));
        
        NFA concatenated = new NFA(leftOperand.startState, rightOperand.endState);
        stack.push(concatenated);
    }

    /**
     * Handles the union operator (|).
     * Pops two NFAs from the stack and creates a new NFA that accepts either.
     * @param stack The NFA stack.
     */
    private void handleUnion(Stack<NFA> stack) {
        int stackSize = stack.size();
        if (stackSize < 2) {
            throw new IllegalStateException("Union needs two operands");
        }
        
        NFA alternative2 = stack.pop();
        NFA alternative1 = stack.pop();
        
        State branchStart = new State();
        State mergeEnd = new State();
        
        alternative1.endState.isFinal = false;
        alternative2.endState.isFinal = false;
        
        branchStart.transitions.add(new Transition(null, alternative1.startState));
        branchStart.transitions.add(new Transition(null, alternative2.startState));
        
        alternative1.endState.transitions.add(new Transition(null, mergeEnd));
        alternative2.endState.transitions.add(new Transition(null, mergeEnd));
        
        NFA unionResult = new NFA(branchStart, mergeEnd);
        stack.push(unionResult);
    }

    /**
     * Handles the Kleene star operator (*).
     * Pops an NFA from the stack and creates a new NFA that accepts zero or more repetitions.
     * @param stack The NFA stack.
     */
    private void handleKleeneStar(Stack<NFA> stack) {
        if (stack.empty()) {
            throw new IllegalStateException("Kleene star requires an operand");
        }
        
        NFA input = stack.pop();
        
        State newInitial = new State();
        State newAccepting = new State();
        
        newInitial.transitions.add(new Transition(null, newAccepting));
        newInitial.transitions.add(new Transition(null, input.startState));
        
        input.endState.isFinal = false;
        input.endState.transitions.add(new Transition(null, newAccepting));
        input.endState.transitions.add(new Transition(null, input.startState));
        
        NFA starResult = new NFA(newInitial, newAccepting);
        stack.push(starResult);
    }

    /**
     * Checks if a character is an operand (not an operator).
     * @param c The character to check.
     * @return True if the character is an operand, false if it is an operator.
     */
    private boolean isOperand(char c) {
        boolean isOperator = (c == '|' || c == '*' || c == '?' || 
                             c == '+' || c == '(' || c == ')' || c == '·');
        return !isOperator;
    }
}