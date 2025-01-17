package info.ladislav.jlox.parser;

import info.ladislav.jlox.lexer.Token;
import info.ladislav.jlox.lexer.TokenType;
import info.ladislav.jlox.parser.Expr.Assign;
import info.ladislav.jlox.parser.Expr.Binary;
import info.ladislav.jlox.parser.Expr.Call;
import info.ladislav.jlox.parser.Expr.Function;
import info.ladislav.jlox.parser.Expr.Get;
import info.ladislav.jlox.parser.Expr.Grouping;
import info.ladislav.jlox.parser.Expr.Literal;
import info.ladislav.jlox.parser.Expr.Logical;
import info.ladislav.jlox.parser.Expr.Set;
import info.ladislav.jlox.parser.Expr.Super;
import info.ladislav.jlox.parser.Expr.Ternary;
import info.ladislav.jlox.parser.Expr.This;
import info.ladislav.jlox.parser.Expr.Unary;
import info.ladislav.jlox.parser.Expr.Variable;

public class AstPrinter implements Expr.Visitor<String> {

    public static void main(String[] args) {
        Expr expression = new Expr.Binary(new Expr.Literal(123), new Token(TokenType.COMMA, ",", null, 1),
                new Expr.Literal(123));

        System.out.println(new AstPrinter().print(expression));
    }

    public String print(Expr expr) {
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
        if (expr.value == null)
            return "nil";
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

    @Override
    public String visitVariableExpr(Variable expr) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String visitAssignExpr(Assign expr) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String visitLogicalExpr(Logical expr) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String visitCallExpr(Call expr) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String visitFunctionExpr(Function expr) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String visitGetExpr(Get expr) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String visitSetExpr(Set expr) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String visitThisExpr(This expr) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String visitSuperExpr(Super expr) {
        // TODO Auto-generated method stub
        return null;
    }

}