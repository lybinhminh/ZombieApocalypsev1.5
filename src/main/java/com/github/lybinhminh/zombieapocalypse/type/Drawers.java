package com.github.lybinhminh.zombieapocalypse.type;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Drawers <T1,T2>{
    /**
     * Similar to HashMap but get element by comparing the keys' value to
     *  the request key instead of memory address
     */
    List<T1> list = new ArrayList<>();
    List<T2> map = new ArrayList<>();
    public void put(T1 k , T2 v){

        if(list.contains(k)){
            int a = list.indexOf(k);
            map.remove(a);
            map.add(a, v);
        }
        else{
            list.add(k);
            map.add(v);
        }
    }
    public T2 get(T1 k){
        if(list.contains(k)){
            return map.get(list.indexOf(k));
        }
        else{
            return null;
        }
    }
    public void clear(){
        list.clear();
        map.clear();
    }
    public boolean containsKey(T1 k){
        return list.contains(k);
    }
    public boolean containsValue(T2 v){
        return map.contains(v);
    }
    @SafeVarargs
    public final void remove(T1... keys){
        for(T1 k : keys){
            int a = list.indexOf(k);
            if(a != -1)
            {
                map.remove(a);
                list.remove(a);
            }
        }
    }

    public void copyFrom(Drawers<T1, T2> another, boolean... replaceExists){
        for(int i = 0; i < another.list.size(); ++i){

            T1 a = another.list.get(i);
            if(replaceExists.length == 0 || (replaceExists[0] && !list.contains(a)))
            put(a, another.get(a));
        }
    }
    public Set<Pair<T1,T2>> entrySet(){
        Set<Pair<T1,T2>> result = new HashSet<>();
        for(int i = 0; i < list.size(); ++i){
            T1 t = list.get(i);
            result.add(new Pair<>(t, map.get(i)));
        }
        return result;
    }
    public int size(){
        return list.size();
    }
    public void changeKey(T1 k, T1 newKey){
        if(list.contains(k)){
            T2 v = get(k);
            remove(k);
            put(newKey, v);
        }
    }
    public T2 getByIndex(int i){
        return map.get(i);
    }
    public int getIndexOfKey(T1 k){
        return list.indexOf(k);
    }
    public int getIndexOfValue(T2 v, int start){
        int rPos;
        for(rPos = 0; rPos< list.size(); ++rPos){
            T2 e = map.get(rPos);
            if(e.equals(v) && rPos >= start)return rPos;
        }
        return -1;
    }
    public @Nullable Pair<T1,T2> getPairByIndex(int i){
        if(i >= list.size()) return null;
                return new Pair<>(list.get(i), map.get(i));
    }
    public void putPair(Pair<T1,T2> p){
        put(p.first, p.second);
    }
    public List<T1> copyOfKeys(){
        return new ArrayList<T1>(list);
    }
    public List<T2> copyOfValues(){
        return new ArrayList<T2>( map);
    }
}