package com.github.lybinhminh.zombieapocalypse.util;

import com.github.lybinhminh.zombieapocalypse.Lib;
import com.github.lybinhminh.zombieapocalypse.Main;
import com.github.lybinhminh.zombieapocalypse.type.Drawers;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class SimpleLocation extends Object {
    public double x, y, z;
    public UUID world;
    public final static Drawers<SimpleLocation, Double> recentlyLoadedChunks = new Drawers<>();
    public SimpleLocation cLoc;
    public SimpleLocation(){}
    public SimpleLocation(UUID w, double x, double y, double z){

        world = w;

        this.x = x;
        this.y = y;
        this.z = z;
        cLoc = this;
    }
    public SimpleLocation(UUID w, int chunkX, int chunkZ){
        this.world = w;
        this.x = chunkX;
        this.z = chunkZ;
        cLoc = this;
    }
    public SimpleLocation(Location loc){
        world = loc.getWorld().getUID();
        x = loc.getX();
        y = loc.getY();
        z = loc.getZ();
        cLoc = this;
    }
    @Override
    public boolean equals(Object b){
        if(!(b instanceof SimpleLocation))return false;
        SimpleLocation another = (SimpleLocation)b;
        return Math.ceil(x) == Math.ceil(another.x)
                && Math.ceil(y) == Math.ceil(another.y)
                && Math.ceil(z) == Math.ceil(another.z)
                && world.compareTo(another.world) == 0;
    }
    public Block toBlock(){
        Block b;
        if(Lib.loadedBlocks.containsKey(this) && (b = Lib.loadedBlocks.get(this)) != null)
                return b;
        else {
            if(!recentlyLoadedChunks.containsKey(cLoc))
            {

                try{
                    World w = Objects.requireNonNull(Bukkit.getWorld(world));
                    Chunk c =
                            w.getChunkAt((int)cLoc.x,(int) cLoc.z);
                    Main.__def.add(c);
                    recentlyLoadedChunks.put(cLoc ,1.225d * Bukkit.getTPS()[0]);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            else{

                double loadPeriod = recentlyLoadedChunks.get(cLoc);
                recentlyLoadedChunks.put(cLoc, loadPeriod + Math.round(0.365 * Bukkit.getTPS()[0]));
            }
            b = Objects.requireNonNull(Bukkit.getWorld(world)).getBlockAt((int) Math.round(x), (int) Math.round(y), (int) Math.round(z));
            Lib.loadedBlocks.put(this, b);
            return b;
        }

    }
    public SimpleLocation relative(double xMod, double yMod, double zMod) {
        return new SimpleLocation(world, x + xMod, y + yMod, z + zMod);
    }
    public double distance(SimpleLocation another){
        if(another.world.compareTo(world) != 0)return -1d;
        return Math.sqrt(Math.pow(x - another.x,2) + Math.pow(y - another.y,2) + Math.pow(z - another.z,2));
    }
    public @Nullable SimpleLocation midPoint(SimpleLocation... others){
        double x = this.x, y = this.y, z = this.z;
        for(SimpleLocation other : others){
            if(other.world.compareTo(world) != 0)return null;
            x = (x + other.x) / 2;
            y = (y + other.y) / 2;
            z = (z + other.z) / 2;
        }
        return new SimpleLocation(world,x,y,z);
    }
    public Location toLocation(){
        return new Location(Bukkit.getWorld(world), x,y,z);
    }
    public String toString(){
        return world.toString() + " " + Double.toString(x) + " " + Double.toString(y) + " " + Double.toString(z);
    }

}
