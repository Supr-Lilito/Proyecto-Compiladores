package com.compiler.lexer.regex;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Utility class for regular expression parsing using the Shunting Yard
 * algorithm.
 * <p>
 * Provides methods to preprocess regular expressions by inserting explicit
 * concatenation operators, and to convert infix regular expressions to postfix
 * notation for easier parsing and NFA construction.
 */
public class ShuntingYard {

    /**
     * Default constructor for ShuntingYard.
     */
    public ShuntingYard() {
        // Constructor doesn't need specific implementation
    }

    /**
     * Inserts the explicit concatenation operator ('·') into the regular
     * expression according to standard rules. This makes implicit
     * concatenations explicit, simplifying later parsing.
     *
     * @param regex Input regular expression (may have implicit concatenation).
     * @return Regular expression with explicit concatenation operators.
     */
    public static String insertConcatenationOperator(String regex) {
        if (regex == null) {
            return regex;
        }
        if (regex.isEmpty()) {
            return regex;
        }
        
        StringBuilder result = new StringBuilder();
        int position = 0;
        
        while (position < regex.length()) {
            char ch = regex.charAt(position);
            result.append(ch);
            
            if (position + 1 < regex.length()) {
                char nextChar = regex.charAt(position + 1);
                
                boolean firstCondition = (isOperand(ch) || ch == ')' || 
                                         ch == '*' || ch == '+' || ch == '?');
                boolean secondCondition = (isOperand(nextChar) || nextChar == '(');
                
                if (firstCondition && secondCondition) {
                    result.append('·');
                }
            }
            position++;
        }
        
        return result.toString();
    }

    /**
     * Determines if the given character is an operand (not an operator or
     * parenthesis).
     *
     * @param c Character to evaluate.
     * @return true if it is an operand, false otherwise.
     */
    private static boolean isOperand(char c) {
        boolean isUnion = (c == '|');
        boolean isStar = (c == '*');
        boolean isOptional = (c == '?');
        boolean isPlus = (c == '+');
        boolean isLeftParen = (c == '(');
        boolean isRightParen = (c == ')');
        boolean isConcat = (c == '·');
        
        return !(isUnion || isStar || isOptional || isPlus || 
                isLeftParen || isRightParen || isConcat);
    }

    /**
     * Converts an infix regular expression to postfix notation using the
     * Shunting Yard algorithm. This is useful for constructing NFAs from
     * regular expressions.
     *
     * @param infixRegex Regular expression in infix notation.
     * @return Regular expression in postfix notation.
     */
    public static String toPostfix(String infixRegex) {
        Map<Character, Integer> operatorPrecedence = new HashMap<>();
        operatorPrecedence.put('|', 1);
        operatorPrecedence.put('·', 2);
        operatorPrecedence.put('*', 3);
        operatorPrecedence.put('+', 3);
        operatorPrecedence.put('?', 3);
        
        String withExplicitConcat = insertConcatenationOperator(infixRegex);
        
        Stack<Character> operators = new Stack<>();
        StringBuilder postfixResult = new StringBuilder();
        
        int charIndex = 0;
        while (charIndex < withExplicitConcat.length()) {
            char token = withExplicitConcat.charAt(charIndex);
            
            if (isOperand(token)) {
                postfixResult.append(token);
            } else if (token == '(') {
                operators.push(token);
            } else if (token == ')') {
                while (operators.size() > 0 && operators.peek() != '(') {
                    postfixResult.append(operators.pop());
                }
                if (operators.size() > 0) {
                    operators.pop();
                }
            } else if (operatorPrecedence.containsKey(token)) {
                while (operators.size() > 0) {
                    char topOp = operators.peek();
                    if (topOp == '(') {
                        break;
                    }
                    if (!operatorPrecedence.containsKey(topOp)) {
                        break;
                    }
                    if (operatorPrecedence.get(topOp) < operatorPrecedence.get(token)) {
                        break;
                    }
                    postfixResult.append(operators.pop());
                }
                operators.push(token);
            }
            charIndex++;
        }
        
        while (operators.size() > 0) {
            postfixResult.append(operators.pop());
        }
        
        return postfixResult.toString();
    }
}