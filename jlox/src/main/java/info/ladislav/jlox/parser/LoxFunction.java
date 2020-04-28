package info.ladislav.jlox.parser;

import java.util.List;
import java.util.Optional;

public class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;
    private final Environment closure;

    public LoxFunction(Stmt.Function declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        Environment environment = new Environment(closure);

        for(int i = 0; i < declaration.params.size(); i++){
            environment.define(declaration.params.get(i).lexeme, Optional.of(args.get(i)));
        }

        try{
            interpreter.executeBlock(declaration.body, environment);
        }catch (Return returnValue) {
            return returnValue.value;
        }
       
        return null;
    }

    @Override                                       
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}