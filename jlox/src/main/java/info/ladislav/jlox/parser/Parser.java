package info.ladislav.jlox.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import info.ladislav.jlox.JLox;
import info.ladislav.jlox.lexer.Token;
import info.ladislav.jlox.lexer.TokenType;

//TODO implement break; statement !

/**
 * 
 * Lox grammar:
 * 
 * program        → declaration* EOF;
 * 
 * declaration    → classDecl | funDecl | varDecl | statement 
 * classDecl      → "class" IDENTIFIER "{" function* "}"
 * varDecl        → "var" IDENTIFIER ("=" expression)? ";" | 
 * funDecl        → "fun" function
 * function       → IDENTIFIER "(" parameters? ")"  block;
 * parameters     → IDENTIFIER ( "," IDENTIFIER )*
 * 
 * lambda         → "fun" "(" parameters? ")"  block;
 * 
 * statement      → exprStmt | ifStmt | printStmt | returnStmt | whileStmt | block
 * ifStmt         → "if" "(" expression ")" statement ( "else" statement)?
 * returnStmt     → "return" expression? ";"
 * whileStmt      → "while" "(" expression ")" statement;
 * forStmt        → "for" "(" (varDecl | exprStmt | ";") expression? ";" expression? ";" ")" statement;
 * 
 * block          → "{" declaration* "}"
 * exprStmt       → expression ";"
 * printStmt      → "print" expression ";"
 * 
 * expression     → comma 
 * comma          → assignment ( (",") assignment)*
 * 
 * assignment     → (call ".")? IDENTIFIER "=" assignment| ternary 
 * 
 * ternary        → logic_or | logic_or ("?") assignment (":") assignment
 * 
 * logic_or       → logic_and ("or" logic_and)*
 * logic and      → equality ("and" equality)*
 * 
 * equality       → comparison ( ( "!=" | "==" ) comparison )* 
 * comparison     → addition ( ( ">" | ">=" | "<" | "<=" ) addition )* 
 * 
 * addition       → multiplication ( ( "-" | "+" ) multiplication )* 
 * multiplication → unary ( ( "/" | "*" ) unary )* 
 * 
 * unary          → ( "!" | "-" ) unary | call 
 * call           → primary ( "(" arguments? ")" | "." IDENTIFIER )* 
 * arguments      → expression ("," expression)*
 * primary        → NUMBER | STRING | "false" | "true" | "nil" | "(" expression ")" | IDENTIFIER | lambda
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

        if(match(TokenType.CLASS)){
          return classDeclaration();
        }

        if (check(TokenType.FUN) && checkNext(TokenType.IDENTIFIER)){
          consume(TokenType.FUN, null);
          return function("function");
        }

        if(match(TokenType.VAR)){
          return varDeclaration();
        }

        return statement();

      }catch(ParseError e){
        synchronize();
        return null;
      }

    }

    private Stmt classDeclaration(){
      Token name = consume(TokenType.IDENTIFIER, "Expect class name");
      consume(TokenType.LEFT_BRACE, "Expect '{' before class bodaaay!");

      List<Stmt.Function> methods = new ArrayList<>();
      while(!check(TokenType.RIGHT_BRACE) && !isAtEnd()){
        methods.add(function("method"));
      }

      consume(TokenType.RIGHT_BRACE,"Expect '}' after class body");

      return new Stmt.Class(name, methods);
    }
    
    private Stmt.Function function(String kind){

      Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
      return new Stmt.Function(name, functionBody(kind));
    }

    private Expr.Function functionBody(String kind){

      consume(TokenType.LEFT_PAREN, "Expect '(' after" + kind + "name.");
      List<Token> params = new ArrayList<>();

      if(!check(TokenType.RIGHT_PAREN)){
        do {
          if(params.size() >= 255){
            error(peek(), "Cannot have more than 255 parameters.");
          }

          params.add(consume(TokenType.IDENTIFIER, "Expect parameter name"));

        }while(match(TokenType.COMMA));
      }

      consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");
      consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body.");
      List<Stmt> body = block();
      
      return new Expr.Function(params, body);
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

      if(match(TokenType.FOR)){
        return forStatement();
      }

      if(match(TokenType.IF)){
        return ifStatement();
      }

      if(match(TokenType.PRINT)){
        return printStatement();
      }

      if(match(TokenType.RETURN)){
        return returnStatement();
      }

      if(match(TokenType.WHILE)){
        return whileStatement();
      }

      if(match(TokenType.LEFT_BRACE)){
        return new Stmt.Block(block());
      }

      return expressionStatement();
    }

    private List<Stmt> block(){
      List<Stmt> statements = new ArrayList<>();

      while(!check(TokenType.RIGHT_BRACE) && !isAtEnd()){
        statements.add(declaration());
      }

      consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
      return statements;
      
    }

    private Stmt ifStatement(){
      consume(TokenType.LEFT_PAREN, "Expect '(' after if");
      Expr condition = expression();
      consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition");

      Stmt thenBranch = statement();
      Stmt elseBranch = null;
      if(match(TokenType.ELSE)){
        elseBranch = statement();
      }

      return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement(){
      Expr value = expression();
      consume(TokenType.SEMICOLON, "Expect ';' after value.");
      return new Stmt.Print(value);
    }


    private Stmt returnStatement(){
      Token keyword = previous();
      Expr value = null;
      if(!check(TokenType.SEMICOLON)){
        value = expression();
      }

      consume(TokenType.SEMICOLON, "Expect ',' after return value");
      return new Stmt.Return(keyword, value);
    }

    private Stmt whileStatement(){
      consume(TokenType.LEFT_PAREN, "Expect '(' ater 'while'.");
      Expr condition = expression();
      consume(TokenType.RIGHT_PAREN, "Expect ')' ater condition.");
      Stmt body = statement();

      return new Stmt.While(condition, body);
    }

    private Stmt forStatement(){
      consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");

      Stmt initializer;

      if(match(TokenType.SEMICOLON)){
        initializer = null;
      }else if (match(TokenType.VAR)){
        initializer = varDeclaration();
      }else{
        initializer = expressionStatement();
      }

      Expr condition = null;

      if(!check(TokenType.SEMICOLON)){
        condition = expression();
      }

      consume(TokenType.SEMICOLON, "Expect ';' after loop condition.");

      Expr increment = null;

      if(!check(TokenType.RIGHT_PAREN)){
        increment = expression();
      }

      consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses."); 

      Stmt body = statement();

      if(increment != null){
        body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
      }

      if(condition == null) {
        condition = new Expr.Literal(true);
      }

      body = new Stmt.While(condition, body);

      if(initializer != null){
        body = new Stmt.Block(Arrays.asList(initializer, body));
      }

      return body;
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
       Expr expr = assignment();

       while(match(TokenType.COMMA)){
        Token operator = previous();
        Expr right = assignment();
        expr = new Expr.Binary(expr, operator, right);
       } 

      return expr;
    }


    private Expr assignment(){
      Expr expr = ternary();

      if(match(TokenType.EQUAL)){

        Token equals = previous();
        Expr value = assignment();

        if(expr instanceof Expr.Variable){
          Token name = ((Expr.Variable) expr).name;
          return new Expr.Assign(name, value);
        }else if (expr instanceof Expr.Get ){
          Expr.Get get = (Expr.Get) expr;
          return new Expr.Set(get.object,get.name, value);
        }

        error(equals, "Invalid assignment target.");
      }

      return expr;
    }

    private Expr ternary(){

      Expr expr = or();

      if(match(TokenType.QUESTION_MARK)){
        Expr if_true = assignment();

        if(!match(TokenType.COLON)){
          throw error(peek(), "Expected colon.");  
        } 

        Expr if_false = assignment();
        return new Expr.Ternary(expr, if_true, if_false);

      }

      return expr;
    }


    private Expr or(){
      Expr expr = and();

      while(match(TokenType.OR)){
        Token operator = previous();
        Expr right = and();
        expr = new Expr.Logical(expr, operator, right);
      }
      return expr;
    }

    private Expr and(){
      Expr expr = equality();
      
      while(match(TokenType.AND)){
        Token operator = previous();
        Expr right = and();
        expr = new Expr.Logical(expr, operator, right);
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
    
        return call();                        
      }       
      
      private Expr call() {
        Expr expr = primary();

        while(true){
          if(match(TokenType.LEFT_PAREN)){
            expr = finishCall(expr);
          }else if (match(TokenType.DOT)){
            Token name = consume(TokenType.IDENTIFIER, "Expect property name after '.'.");
            expr = new Expr.Get(expr, name);
          }else{
            break;
          }
        }

        return expr;
      }

      private Expr finishCall(Expr callee){
        List <Expr> args = new ArrayList<>();
        if(!check(TokenType.RIGHT_PAREN)){
          
          do{
            if(args.size() >= 255){
              error(peek(), "Cannot have more than 255 arguments.");
            }

            args.add(expression());
          } while(match(TokenType.COMMA));

        }

        Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments");

        return new Expr.Call(callee, paren, args);

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
        
        if (match(TokenType.FUN)){
          return functionBody("function");
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

      private boolean checkNext(TokenType tokenType){
        if (isAtEnd()){
          return false;
        }

        if(tokens.get(current + 1).type == TokenType.EOF){
          return false;
        }

        return tokens.get(current + 1).type == tokenType;

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