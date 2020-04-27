package info.ladislav.jlox.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import info.ladislav.jlox.JLox;
import info.ladislav.jlox.lexer.Token;
import info.ladislav.jlox.lexer.TokenType;
import info.ladislav.jlox.parser.Expr.Assign;
import info.ladislav.jlox.parser.Expr.Binary;
import info.ladislav.jlox.parser.Expr.Call;
import info.ladislav.jlox.parser.Expr.Grouping;
import info.ladislav.jlox.parser.Expr.Literal;
import info.ladislav.jlox.parser.Expr.Logical;
import info.ladislav.jlox.parser.Expr.Ternary;
import info.ladislav.jlox.parser.Expr.Unary;
import info.ladislav.jlox.parser.Expr.Variable;
import info.ladislav.jlox.parser.Stmt.Block;
import info.ladislav.jlox.parser.Stmt.Expression;
import info.ladislav.jlox.parser.Stmt.Function;
import info.ladislav.jlox.parser.Stmt.If;
import info.ladislav.jlox.parser.Stmt.Print;
import info.ladislav.jlox.parser.Stmt.Var;
import info.ladislav.jlox.parser.Stmt.While;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    Interpreter() {
        globals.define("clock", Optional.of(new LoxCallable() {

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                return (double) System.currentTimeMillis() / 1000.0;
            }

            @Override
            public int arity() {
                return 0;
            }
        }));
    }

    final Environment globals = new Environment();

    private Environment environment = globals;

    public void interpret(List<Stmt> statements) {

        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError e) {
            JLox.runtimeError(e);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    @Override
    public Object visitBinaryExpr(Binary expr) {

        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            // Equality
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            // Comparison operators
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;

            // Arithmetical operators
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case PLUS:

                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }

                if (left instanceof String || right instanceof String) {
                    String leftStr = left.toString();
                    String rightStr = right.toString();

                    if (left instanceof Double) {
                        leftStr = removeTrailing(".0", leftStr);
                    }

                    if (right instanceof Double) {
                        rightStr = removeTrailing(".0", rightStr);
                    }

                    return leftStr + rightStr;
                }

                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");

            case SLASH:
                checkNumberOperands(expr.operator, left, right);

                if ((double) right == 0) {
                    throw new RuntimeError(expr.operator, "Division by zero.");
                }

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

        if (!isTruthy(cond)) {
            return evaluate(expr.if_false);
        }

        if (cond instanceof Double) {

            if ((double) cond > 0) {
                return evaluate(expr.if_true);
            }
            return evaluate(expr.if_false);

        }

        if (cond instanceof Boolean) {
            if ((boolean) cond == true) {
                return evaluate(expr.if_true);
            } else {
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
                return -(double) right;
            case BANG:
                return !isTruthy(right);
        }

        return null;
    }

    @Override
    public Void visitExpressionStmt(Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Var stmt) {
        Optional<Object> value = Optional.empty();

        if (stmt.initializer != null) {
            value = Optional.of(evaluate(stmt.initializer));
        }
        environment.define(stmt.name.lexeme, value);

        return null;
    }

    @Override
    public Object visitVariableExpr(Variable expr) {
        return environment.get(expr.name);
    }

    @Override
    public Object visitAssignExpr(Assign expr) {
        Optional<Object> value = Optional.of(evaluate(expr.value));
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Void visitBlockStmt(Block stmt) {
        executeBlock(stmt.statements, new Environment(this.environment));
        return null;
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;

        try {
            this.environment = environment;
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitIfStmt(If stmt) {

        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }

        return null;
    }

    @Override
    public Object visitLogicalExpr(Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) {
                return left;
            }
        } else {
            if (!isTruthy(left)) {
                return left;
            }
        }
        return evaluate(expr.right);
    }

    @Override
    public Void visitFunctionStmt(Function stmt) {
        LoxFunction fn = new LoxFunction(stmt);
        environment.define(stmt.name.lexeme, Optional.of(fn));
        return null;
    }
 
    /** HELPER METHODS */

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Void visitWhileStmt(While stmt) {

        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }

        return null;
    }

    /** Like in Ruby "false" and "nil" are falsey and everything else is truthy */
    private boolean isTruthy(Object obj) {

        if (obj instanceof Boolean) {
            return (boolean) obj;
        }

        return obj != null;
    }

    private boolean isEqual(Object a, Object b) {

        if (a == null && b == null) {
            return true;
        }

        if (a == null) {
            return false;
        }

        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {

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

        if (s.endsWith(t)) {
            return s.substring(0, s.length() - t.length());
        }

        return s;
    }

    @Override
    public Object visitCallExpr(Call expr) {
        Object callee = evaluate(expr.callee);

        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes");
        }

        List<Object> args = new ArrayList<>();
        for (Expr arg : expr.arguments) {
            args.add(evaluate(arg));
        }

        LoxCallable function = (LoxCallable) callee;

        if (args.size() != function.arity()) {
            throw new RuntimeError(expr.paren,
                    "Expected " + function.arity() + "arguments, but got" + args.size() + ".");
        }

        return function.call(this, args);
    }
}