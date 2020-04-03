package info.ladislav.jlox.parser;

import info.ladislav.jlox.lexer.Token;
import info.ladislav.jlox.lexer.TokenType;
import info.ladislav.jlox.parser.Expr.Binary;
import info.ladislav.jlox.parser.Expr.Grouping;
import info.ladislav.jlox.parser.Expr.Literal;
import info.ladislav.jlox.parser.Expr.Ternary;
import info.ladislav.jlox.parser.Expr.Unary;

public class AstPrinter implements Expr.Visitor<String> {

    public static void main(String[] args) {                 
        Expr expression = new Expr.Binary(                     
            new Expr.Literal(123),                        
            new Token(TokenType.COMMA, ",", null, 1),                                        
            new Expr.Literal(123)
            );
    
        System.out.println(new AstPrinter().print(expression));
      }                       

    public String print(Expr expr){
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Grouping expr) {
        return parenthesize("group", expr.expression);     

    }

    @Override
    public String visitLiteralExpr(Literal expr) {
        if (expr.value == null) return "nil";                            
            return expr.value.toString();    
    }

    @Override
    public String visitUnaryExpr(Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);         
    }

    @Override
    public String visitTernaryExpr(Ternary expr) {
        return parenthesize("ternary", expr.condition, expr.if_true, expr.if_false);         
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();
    
        builder.append("(");                
        for (Expr expr : exprs) {                              
          builder.append(" ");                                 
          builder.append(expr.accept(this));                   
        }                                                      
        builder.append(name).append(")");                                   
    
        return builder.toString();                             
      }                        

}