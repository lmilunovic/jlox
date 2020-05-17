package info.ladislav.jlox.parser;

import java.util.List;
import java.util.Optional;

public class LoxFunction implements LoxCallable {
    final String name;
    private final Expr.Function declaration;
    private final Environment closure;

    public LoxFunction(String name, Expr.Function declaration, Environment closure) {
        this.name = name;
        this.declaration = declaration;
        this.closure = closure;
    }

    @Override
    public int arity() {
        return declaration.parameters.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        Environment environment = new Environment(closure);

        for(int i = 0; i < declaration.parameters.size(); i++){
            environment.define(declaration.parameters.get(i).lexeme, Optional.of(args.get(i)));
        }

        try{
            interpreter.executeBlock(declaration.body, environment);
        }catch (Return returnValue) {
            return returnValue.value;
        }
       
        return null;
    }

    LoxFunction bind(LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", Optional.of(instance));
        return new LoxFunction(name, declaration, environment);
    }

    @Override                                       
    public String toString() {

        if(name == null){
            return "<λ>";
        }

        return "<fn " + name + ">";
    }
}