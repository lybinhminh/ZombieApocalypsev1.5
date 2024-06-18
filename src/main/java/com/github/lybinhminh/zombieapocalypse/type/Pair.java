package com.github.lybinhminh.zombieapocalypse.type;

public class Pair <T1, T2> extends Object{
    public T1 first;
    public T2 second;
    public Pair(T1 key, T2 value){
        first = key;
        second = value;
    }
    @Override
    public boolean equals(Object b){
        if(!(b instanceof Pair))return false;
        return first.equals(((Pair)b).first) && second.equals(((Pair)b).second);
    }
    public T1 getKey(){
        return first;
    }
    public T2 getValue(){
        return second;
    }
    public String toString(){
        return String.valueOf(first) + ": " + String.valueOf(second);
    }
}
