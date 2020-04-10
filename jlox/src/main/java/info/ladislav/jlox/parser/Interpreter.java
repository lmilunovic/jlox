package info.ladislav.jlox.parser;

import info.ladislav.jlox.JLox;
import info.ladislav.jlox.lexer.Token;
import info.ladislav.jlox.parser.Expr.Binary;
import info.ladislav.jlox.parser.Expr.Grouping;
import info.ladislav.jlox.parser.Expr.Literal;
import info.ladislav.jlox.parser.Expr.Ternary;
import info.ladislav.jlox.parser.Expr.Unary;

public class Interpreter implements Expr.Visitor<Object> {

    public void interpret(Expr expression){

        try{
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        }catch( RuntimeError e){
            JLox.runtimeError(e);
        }
    }

    @Override
    public Object visitBinaryExpr(Binary expr) {

        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch(expr.operator.type){
            //Equality
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            // Comparison operators
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
             case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;

            // Arithmetical operators
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case PLUS:

                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }

                if (left instanceof String || right instanceof String) {
                    String leftStr =  left.toString();
                    String rightStr = right.toString();
                    
                    if(left instanceof Double){
                        leftStr = removeTrailing(".0", leftStr);
                    }
                    
                    if(right instanceof Double ){
                        rightStr = removeTrailing(".0", rightStr);
                    }

                    return leftStr + rightStr;
                }

                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");

            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double) left / (double) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
        }

        return null;
    }

    @Override
    public Object visitTernaryExpr(Ternary expr) {

        Object cond = evaluate(expr.condition);

        if(!isTruthy(cond)){
            return evaluate(expr.if_false);
        }

        if(cond instanceof Double){

            if ( (double) cond > 0) {
                return evaluate(expr.if_true);
            }
            return evaluate(expr.if_false);

        }

        if(cond instanceof Boolean){
            if((boolean) cond == true){
                return evaluate(expr.if_true);
            }else {
                return evaluate(expr.if_false);
            }
        }

        return evaluate(expr.if_true);
    }

    @Override
    public Object visitGroupingExpr(Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Unary expr) {

        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
            case BANG:
                return !isTruthy(right);
        }

        return null;
    }

    /** HELPER METHODS */

    private Object evaluate(Expr expr){
        return expr.accept(this);
    }

    /** Like in Ruby "false" and "nil" are falsey and everything else is truthy */
    private boolean isTruthy(Object obj){

        if (obj instanceof Boolean){
            return (boolean) obj;
        }

        return obj != null;
    }

    private boolean isEqual(Object a, Object b){

        if(a == null && b == null) {
            return true;
        }

        if(a == null){
            return false;
        }

        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand){

        if (operand instanceof Double) {
            return;
        }

        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {

        if (left instanceof Double && right instanceof Double) {
            return;
        }

        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private String stringify(Object object) {
        if (object == null) {
            return "nil";
        }

        // Hack. Work around Java adding ".0" to integer-valued doubles.
        if (object instanceof Double) {
          return removeTrailing(".0", object.toString());
        }

        return object.toString();
      }

      private String removeTrailing(String t, String s) {

        if(s.endsWith(t)){
            return s.substring(0, s.length() - t.length());
        }

        return s;
      }
}