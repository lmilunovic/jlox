package info.ladislav.jlox.parser;

import java.util.List;

import info.ladislav.jlox.JLox;
import info.ladislav.jlox.lexer.Token;
import info.ladislav.jlox.lexer.TokenType;


/**
 * 
 * Expression grammar:
 * comma          → ternary ( (",") ternary)*
 * ternary        → expression | expression ("?") comma (":") ternary
 * expression     → equality ;
 * equality       → comparison ( ( "!=" | "==" ) comparison )* ;
 * comparison     → addition ( ( ">" | ">=" | "<" | "<=" ) addition )* ;
 * addition       → multiplication ( ( "-" | "+" ) multiplication )* ;
 * multiplication → unary ( ( "/" | "*" ) unary )* ;
 * unary          → ( "!" | "-" ) unary | primary ;
 * primary        → NUMBER | STRING | "false" | "true" | "nil" | "(" expression ")" ;
 * 
 */


/**  !TODO
 *
 * Add error productions to handle each binary operator appearing without a left-hand operand.
 * In other words, detect a binary operator appearing at the beginning of an expression.
 * Report that as an error, but also parse and discard a right-hand operand with the appropriate precedence.
 *
 */

public class Parser {  

    private final List<Token> tokens;                    
    private int current = 0;                             
    private static class ParseError extends RuntimeException {}

    public Parser(List<Token> tokens) {                         
      this.tokens = tokens;                              
    }                 
    
    public Expr parse() {                
        try {
          return comma();
        } catch (ParseError error) {
          return null;
        }
    }  

    /** AST */

    private Expr comma(){
       Expr expr = ternary();

       while(match(TokenType.COMMA)){
        Token operator = previous();
        Expr right = ternary();
        expr = new Expr.Binary(expr, operator, right);
       } 

      return expr;
    }

    private Expr ternary(){

      Expr expr = expression();

      if(match(TokenType.QUESTION_MARK)){
        Expr if_true = comma();

        if(!match(TokenType.COLON)){
          throw error(peek(), "Expected colon.");  
        } 

        Expr if_false = ternary();
        return new Expr.Ternary(expr, if_true, if_false);

      }

      return expr;
    }

    private Expr expression(){
        return equality();
    }

    private Expr equality(){
        Expr expr = comparison();

        while(match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL) ){
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }
    
    private Expr comparison(){
        Expr expr = addition();

        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();                           
            Expr right = addition();                               
            expr = new Expr.Binary(expr, operator, right);         
        }                                                        

        return expr;                                             
    }

    private Expr addition() {                         
        Expr expr = multiplication();
    
        while (match(TokenType.MINUS, TokenType.PLUS)) {                    
          Token operator = previous();                  
          Expr right = multiplication();                
          expr = new Expr.Binary(expr, operator, right);
        }                                               
    
        return expr;                                    
      }                                                 
    
      private Expr multiplication() {                   
        Expr expr = unary();                            
    
        while (match(TokenType.SLASH, TokenType.STAR)) {                    
          Token operator = previous();                  
          Expr right = unary();                         
          expr = new Expr.Binary(expr, operator, right);
        }                                               
    
        return expr;                                    
      }         
      
      private Expr unary() {                     
        if (match(TokenType.BANG, TokenType.MINUS)) {                
          Token operator = previous();           
          Expr right = unary();                  
          return new Expr.Unary(operator, right);
        }
    
        return primary();                        
      }         

      private Expr primary() {                                 
        if (match(TokenType.FALSE)) return new Expr.Literal(false);      
        if (match(TokenType.TRUE)) return new Expr.Literal(true);        
        if (match(TokenType.NIL)) return new Expr.Literal(null);
    
        if (match(TokenType.NUMBER, TokenType.STRING)) {                           
          return new Expr.Literal(previous().literal);         
        }                                                      
    
        if (match(TokenType.LEFT_PAREN)) {                               
          Expr expr = comma();                            
          consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
          return new Expr.Grouping(expr);                      
        }           
        
        throw error(peek(), "Expect expression.");  
      }  

      /** HELPER METHODS */

    private boolean match(TokenType... types) {
        for (TokenType type : types) {           
          if (check(type)) {                     
            advance();                           
            return true;                         
          }                                      
        }
    
        return false;                            
      }                     

      private boolean check(TokenType type) {
        if (isAtEnd()) return false;         
        return peek().type == type;          
      }                   

      private Token advance() {   
        if (!isAtEnd()) current++;
        return previous();        
      } 

      private boolean isAtEnd() {      
        return peek().type == TokenType.EOF;     
      }
    
      /** Returns token yet to consume */
      private Token peek() {           
        return tokens.get(current);    
      }                                
    
      /** Returns most recently consumed token */
      private Token previous() {       
        return tokens.get(current - 1);
      }               

      private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
    
        throw error(peek(), message);                        
      }                    
      
      private ParseError error(Token token, String message) {
        JLox.error(token, message);                           
        return new ParseError();                             
      }                    

  }                           