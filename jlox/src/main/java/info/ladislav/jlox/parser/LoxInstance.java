package info.ladislav.jlox.parser;

public class LoxInstance {
    private LoxClass clazz;

    LoxInstance(LoxClass clazz){
        this.clazz = clazz;
    }
    
    @Override
    public String toString(){
        return clazz.name + " instance";
    }
}