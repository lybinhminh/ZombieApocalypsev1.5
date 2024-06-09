package io.unfish.zombieapocalypse.entity;

import io.unfish.zombieapocalypse.Lib;
import io.unfish.zombieapocalypse.Main;
import io.unfish.zombieapocalypse.type.Drawers;
import io.unfish.zombieapocalypse.type.Pair;
import io.unfish.zombieapocalypse.util.Box;
import io.unfish.zombieapocalypse.util.SimpleLocation;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class livingMassCollection {
    public int state = 1;
    public final Box region;
    private Material m = Material.REDSTONE_BLOCK;
    public double health = 200d;
    private final List<Material> pms = new ArrayList<>(Arrays.asList(Material.REDSTONE_BLOCK, Material.RED_CONCRETE,
            Material.WHITE_CONCRETE,Material.GREEN_CONCRETE, Material.OBSIDIAN, Material.BEDROCK));
    private void a(){
        double e = Math.round(((-Math.pow(region.getCoordinate(region.bottom).first,2)) + (-Math.pow(
                region.getCoordinate(region.bottom).second,2)))/2);
        double height = 0;
        SimpleLocation k = null;
        boolean l = false;
        for(SimpleLocation loc : region.belongs){
            Material m1 = m;
            Pair<Double, Double> coord = region.getCoordinate(loc);
            double b = Math.round(((-Math.pow(coord.first,2)) + (-Math.pow(coord.second,2)))/2);
            if(b - e > 0){
                Block h;
                SimpleLocation loc2 = loc.relative(0,b-e,0);
                if(!l && Main.randomMachine.nextInt(100) <= 29){
                    k = loc2;
                    l = true;
                }
                else if(l && Math.floor(loc2.distance(k)) == 1){
                    if(Main.randomMachine.nextInt(100) <= 39){
                        l = false;
                    }
                    m1 = Material.PURPLE_CONCRETE;
                }

                    (h = loc2.toBlock()).setType(m1);
                    Lib.loadedBlocks.put(loc2, h);

                if(loc2.y > height)height = loc2.y;
                if(b - e > 1){
                    for(int i = 0; i < b - e; ++i){
                        SimpleLocation f = loc.relative(0,i,0);
                        Block g = f.toBlock();
                        if(!pms.contains(g.getType())) {
                            if(g.getType() == Material.PODZOL)
                                Main.c.remove(f);
                            g.setType(m);
                        }
                        Lib.loadedBlocks.put(f,g);
                    }
                }
            }
        }

        if(Math.abs(region.top.y - region.bottom.y) < height)region.reSize(region.bottom, region.top.relative(0,height,0));
        double y = Math.max(0, Math.min(region.bottom.y, region.top.y) - 1);
        for(int atX = 0; atX < Math.abs(region.bottom.x - region.top.x); ++atX){
            for(int atZ = 0; atZ < Math.abs(region.bottom.z - region.top.z); ++atZ){
                SimpleLocation loc = new SimpleLocation(region.bottom.world,
                        Math.min(region.bottom.x, region.top.x) + atX, y, Math.min(region.bottom.z,
                        region.top.z) + atZ);
                Block b = loc.toBlock();
                if(b.getType() != Material.PODZOL){
                    b.setType(Material.PODZOL);
                    Lib.loadedBlocks.put(loc,b);
                    Main.c.add(loc);
                }
            }
        }
        if(state > 10){
            for(SimpleLocation loc : region.belongs){
                Block b = loc.toBlock();
                if(b.getType() != Material.BEDROCK){
                    b.setType(Material.BEDROCK);
                    Lib.loadedBlocks.put(loc,b);
                }
            }
        }
    }
    public livingMassCollection(Box region){
        this.region = region;
        a();
    }
    public void grow(){
        state++;
        if(state <= pms.size()) {
            m = pms.get(state - 1);
            health += 100d;
        }
        else{
            health += 1000d;
        }
        region.increaseEachSideBy1BlockThick(2,2);
        a();
    }
}

