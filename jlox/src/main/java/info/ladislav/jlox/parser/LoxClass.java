package info.ladislav.jlox.parser;

import java.util.List;
import java.util.Map;

public class LoxClass implements LoxCallable {
    final String name;
    private final Map<String, LoxFunction> methods;

    public LoxClass(String name, Map<String, LoxFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int arity() {
        LoxFunction initializer = findMethod("init");

        if(initializer == null ) {
            return 0;
        }
        
        return initializer.arity();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
       LoxInstance instance = new LoxInstance(this);
       LoxFunction initializer = findMethod("init");
       if(initializer != null) {
           initializer.bind(instance).call(interpreter, args);
       }

        return instance;
    }

    LoxFunction findMethod(String name){
        return methods.getOrDefault(name, null);
    }

}