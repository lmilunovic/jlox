package info.ladislav.jlox.parser;

import java.util.ArrayList;
import java.util.List;

import info.ladislav.jlox.JLox;
import info.ladislav.jlox.lexer.Token;
import info.ladislav.jlox.lexer.TokenType;


/**
 * 
 * Lox grammar:
 * 
 * program        → declaration* EOF;
 * 
 * declaration    → varDecl | statement 
 * varDecl        → "var" IDENTIFIER ("=" expression)? ";"
 * statement      → exprStmt | printStmt
 * exprStmt       → expression ";"
 * printStmt      → "print" expression ";"
 * 
 * expression     → comma
 * comma          → ternary ( (",") ternary)*
 * 
 *  TODO assignment expression comes between comma and ternary
 * 
 * ternary        → equality | equality ("?") comma (":") ternary
 * equality       → comparison ( ( "!=" | "==" ) comparison )* 
 * comparison     → addition ( ( ">" | ">=" | "<" | "<=" ) addition )* 
 * addition       → multiplication ( ( "-" | "+" ) multiplication )* 
 * multiplication → unary ( ( "/" | "*" ) unary )* 
 * unary          → ( "!" | "-" ) unary | primary 
 * primary        → NUMBER | STRING | "false" | "true" | "nil" | "(" expression ")" | IDENTIFIER
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
    
    public List<Stmt> parse() {                

        try{
          List<Stmt> statements = new ArrayList<>();

          while(!isAtEnd()){
            statements.add(declaration());
          }
  
          return statements;
        }catch(ParseError e){
          return null;
        }
      
    }  

    /** AST */

    private Stmt declaration(){

      try{

        if(match(TokenType.VAR)){
          return varDeclaration();
        }

        return statement();

      }catch(ParseError e){
        synchronize();
        return null;
      }

    }

    private Stmt varDeclaration(){

      Token name = consume(TokenType.IDENTIFIER, "Variable name expected.");

      Expr initializer = null;

      if(match(TokenType.EQUAL)){
        initializer = expression();
      }

       consume(TokenType.SEMICOLON, "Expected ';' after variable declaration.");
       return new Stmt.Var(name, initializer);
    }


    private Stmt statement(){

      if(match(TokenType.PRINT)){
        return printStatement();
      }

      return expressionStatement();
    } 

    private Stmt printStatement(){
      Expr value = expression();
      consume(TokenType.SEMICOLON, "Expect ';' after value.");
      return new Stmt.Print(value);
    }

    private Stmt expressionStatement(){
      Expr expr = expression();
      consume(TokenType.SEMICOLON, "Expect ';' after value.");
      return new Stmt.Expression(expr);
    }


    private Expr expression(){
      return comma();
    }

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

      Expr expr = equality();

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
        
        if(match(TokenType.IDENTIFIER)){
          return new Expr.Variable(previous());
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

      private void synchronize() {
        advance();

        while (!isAtEnd()) {
          if (previous().type == TokenType.SEMICOLON) return;

          switch (peek().type) {
            case CLASS:
            case FUN:
            case VAR:
            case FOR:
            case IF:
            case WHILE:
            case PRINT:
            case RETURN:
              return;
          }

          advance();
        }                                          
      }
  }                           