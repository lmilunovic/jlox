package info.ladislav.jlox.parser;

import java.util.HashMap;
import java.util.Map;

import info.ladislav.jlox.lexer.Token;

public class LoxInstance {
    private LoxClass clazz;
    private final Map<String, Object> fields = new HashMap<>();
    
    LoxInstance(LoxClass clazz){
        this.clazz = clazz;
    }
    
    @Override
    public String toString(){
        return clazz.name + " instance";
    }

    public Object get(Token name){
        
        if(fields.containsKey(name.lexeme)){
            return fields.get(name.lexeme);
        }

        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");

    }
}