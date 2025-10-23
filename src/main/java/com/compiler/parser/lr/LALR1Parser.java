package com.compiler.parser.lr;

import java.util.List;
import com.compiler.lexer.Token;
import com.compiler.parser.grammar.Symbol; // Importación añadida

/**
 * Implements the LALR(1) parsing engine.
 * Uses a stack and the LALR(1) table to process a sequence of tokens.
 * Complementary task for Practice 9.
 */
public class LALR1Parser {
    private final LALR1Table table;

    public LALR1Parser(LALR1Table table) {
        this.table = table;
    }

   // package-private accessor for tests
   LALR1Table getTable() {
       return table;
   }

   /**
    * Parses a sequence of tokens using the LALR(1) parsing algorithm.
    * @param tokens The list of tokens from the lexer.
    * @return true if the sequence is accepted, false if a syntax error is found.
    */
   public boolean parse(List<Token> tokens) {
        // 1. Initialize a stack for states and push the initial state.
        java.util.Deque<Integer> stack = new java.util.ArrayDeque<>();
        stack.push(table.getInitialState());

        // 2. Create a mutable list of input tokens and add the end-of-input token ("$").
        java.util.List<Token> input = new java.util.ArrayList<>(tokens);
        input.add(new Token("$", "$")); // Marcador de fin de entrada

        // 3. Initialize an instruction pointer `ip` to 0.
        int ip = 0;
        
        // 4. Start the parsing loop.
        while (true) {
            // a. Get the current state from the top of the stack.
            int state = stack.peek();
            
            // b. Get the current token `a`
            Token a_token = input.get(ip);
            // Creamos un Symbol terminal basado en el tipo del token
            Symbol a_symbol = new Symbol(a_token.type, com.compiler.parser.grammar.SymbolType.TERMINAL);

            // c. Look up the action in the ACTION table.
            java.util.Map<Symbol, LALR1Table.Action> actionRow = table.getActionTable().get(state);
            LALR1Table.Action action = (actionRow != null) ? actionRow.get(a_symbol) : null;
            
            // d. If no action is found, it's a syntax error.
            if (action == null) {
                return false; // Error de sintaxis
            }

            // e. If the action is SHIFT(s'):
            if (action.type == LALR1Table.Action.Type.SHIFT) {
                // i. Push the new state s' onto the stack.
                stack.push(action.state);
                // ii. Advance the input pointer: ip++.
                ip++;
                
            // f. If the action is REDUCE(A -> β):
            } else if (action.type == LALR1Table.Action.Type.REDUCE) {
                com.compiler.parser.grammar.Production p = action.reduceProd;
                Symbol A = p.left;
                int betaLength = p.right.size();
                
                // Manejar producciones épsilon (ej. A -> ε)
                // Si la gramática usa un símbolo "ε" explícito
                if (betaLength == 1 && p.right.get(0).name.equals("ε")) {
                    betaLength = 0;
                }
                
                // i. Pop |β| states from the stack.
                for (int i = 0; i < betaLength; i++) {
                    stack.pop();
                }
                
                // ii. Get the new state `s` from the top of the stack.
                int s = stack.peek();
                
                // iii. Look up the GOTO state: goto_state = table.getGotoTable()[s][A].
                java.util.Map<Symbol, Integer> gotoRow = table.getGotoTable().get(s);
                Integer goto_state = (gotoRow != null) ? gotoRow.get(A) : null;
                
                // iv. If no GOTO state is found, it's an error.
                if (goto_state == null) {
                    return false; // Error: GOTO Faltante (tabla inválida)
                }
                
                // v. Push the goto_state onto the stack.
                stack.push(goto_state);
                
            // g. If the action is ACCEPT:
            } else if (action.type == LALR1Table.Action.Type.ACCEPT) {
                // i. The input has been parsed successfully.
                return true;
                
            // h. Error
            } else {
                return false; // Error desconocido
            }
        }
   }
}