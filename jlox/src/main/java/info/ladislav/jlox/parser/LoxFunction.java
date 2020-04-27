package info.ladislav.jlox.parser;

import java.util.List;
import java.util.Optional;

public class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;

    public LoxFunction(Stmt.Function declaration) {
        this.declaration = declaration;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        Environment environment = new Environment(interpreter.globals);

        for(int i = 0; i < declaration.params.size(); i++){
            environment.define(declaration.params.get(i).lexeme, Optional.of(args.get(i)));
        }
        interpreter.executeBlock(declaration.body, environment);
        return null;
    }

    @Override                                       
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}