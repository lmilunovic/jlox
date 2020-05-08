package info.ladislav.jlox.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import info.ladislav.jlox.lexer.Token;

public class Environment {

    final Environment enclosing;
    private final Map<String, Optional<Object>> values = new HashMap<>();

    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing){
        this.enclosing = enclosing;
    }

    Object get(Token name){
        
        if(values.containsKey(name.lexeme)){
            return values.get(name.lexeme).orElseThrow(()-> new RuntimeError(name, "Variable " + name.lexeme + " is not defined."));
        }

        if (enclosing != null) return enclosing.get(name);

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name);
    }

    private Environment ancestor(int distance) {

        Environment environment = this;
        for (int i = 0; i < distance; i++){
            environment = environment.enclosing;
        }

        return environment;
    }

    void define(String name, Optional<Object> value) {
        values.put(name, value);
    }

    void assign(Token name, Optional<Object> value){

        if(values.containsKey(name.lexeme)){
            values.put(name.lexeme, value);
            return;
        }

        if(enclosing != null){
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    void assignAt(int distance, Token name, Object value){
        ancestor(distance).values.put(name.lexeme, Optional.of(value));
    }
}