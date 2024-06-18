package com.github.lybinhminh.zombieapocalypse.type;
import com.github.lybinhminh.zombieapocalypse.Main;
import org.jetbrains.annotations.Nullable;

import java.util.*;
/**
 * Similar to HashMap but get element by comparing the keys' value to
 *  the request key instead of memory address
 */
public class Drawers <T1,T2>{

    List<T1> list = new ArrayList<>();
    List<T2> map = new ArrayList<>();
    public void put(T1 k , T2 v){
        fix();
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
        fix();
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
        fix();
        return list.contains(k);
    }

    /**
     * Drawer is most suitable for being call synchorously, however, if you wanna go with
     *  asynchorous thread, it may cause the Drawer to have extra data and fake length
     *  which can lead to waste of memory or IndexOutBoundForLength exception, well,
     *   these errors of the Drawer forms ConcurrentModificationException, so the Drawer
     *   have to remove those extra data, though it's definately deleting data, it's
     *    to prevent errors. You don't have to call this function on your own, the Drawer
     *    always call it whenever you use any function of the Drawer.
     */
    public void fix(){
        if(list.size() != map.size()){
            List<Object> extraList;
            if(list.size() > map.size())extraList =(List<Object>) list;
            else extraList = (List<Object>)map;
            for(int i = size(); i < extraList.size();){
                extraList.remove(i);
            }
        }

    }
    public boolean containsValue(T2 v){
        fix();
        return map.contains(v);
    }
    @SafeVarargs
    public final void remove(T1... keys){
        fix();
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
        fix();
        for(int i = 0; i < another.list.size(); ++i){

            T1 a = another.list.get(i);
            if(replaceExists.length == 0 || (replaceExists[0] && !list.contains(a)))
            put(a, another.get(a));
        }
    }
    public Set<Pair<T1,T2>> entrySet(){
        fix();
        Set<Pair<T1,T2>> result = new HashSet<>();
        for(int i = 0; i < list.size(); ++i){

                T1 t = list.get(i);
                result.add(new Pair<>(t, map.get(i)));

        }
        return result;
    }
    public int size(){
        return Math.min(list.size(), map.size());
    }
    public void changeKey(T1 k, T1 newKey){
        fix();
        if(list.contains(k)){
            T2 v = get(k);
            remove(k);
            put(newKey, v);
        }
    }
    public T2 getByIndex(int i){
        fix();
        if(i >= size())
            return null;
        return map.get(i);
    }
    public int getIndexOfKey(T1 k){
        fix();
        return list.indexOf(k);
    }
    /**
    find the first occurence of the value from the given index 'start' to the end
      of the Drawer
     return the index on sucess and -1 if the value isnot in the Drawer
     **/
    public int getIndexOfValue(T2 v, int start){
        fix();
        int rPos;
        for(rPos = start; rPos< size(); ++rPos){
            T2 e = map.get(rPos);
            if(e.equals(v) && rPos >= start)return rPos;
        }
        return -1;
    }
    public @Nullable Pair<T1,T2> getPairByIndex(int i){
        fix();
        if(i >= size()) return null;
                return new Pair<>(list.get(i), map.get(i));
    }
    public void putPair(Pair<T1,T2> p){
        fix();
        put(p.first, p.second);
    }
    public List<T1> copyOfKeys(){
        fix();
        return new ArrayList<T1>(list);
    }
    public List<T2> copyOfValues(){
        fix();
        return new ArrayList<T2>( map);
    }
}