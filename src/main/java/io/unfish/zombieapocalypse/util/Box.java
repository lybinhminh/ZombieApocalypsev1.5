package io.unfish.zombieapocalypse.util;

import io.unfish.zombieapocalypse.type.Pair;
import org.bukkit.Bukkit;
import sun.java2d.pipe.SpanShapeRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Box {
    public SimpleLocation bottom, top, centre;
    public List<SimpleLocation> belongs = new ArrayList<>();
    public double area = -1d, volume = -1;
    public double shortSide, longSide;
    public Box(SimpleLocation bottom, SimpleLocation top){
        a(bottom,top);
    }
    private int a(SimpleLocation bottom, SimpleLocation top){
        shortSide = Math.min(Math.abs(bottom.x - top.x), Math.abs(bottom.z - top.z));
        longSide = shortSide == Math.abs(bottom.x - top.x)  ? Math.abs(bottom.z - top.z) : Math.abs(bottom.x - top.x);
        if(bottom.world.compareTo(top.world) != 0)return -1;
        area = Math.abs(bottom.x - top.x) * Math.abs(bottom.z - top.z);
        volume = area * Math.abs(bottom.y - top.y);
        this.bottom = bottom.y <= top.y ? bottom : top;
        this.top = top.y >= bottom.y ? top : bottom;
        int count = 0;
        List<SimpleLocation> locs = new ArrayList<>();
        for(int atX = 0; atX < Math.max(bottom.x - top.x, top.x - bottom.x); ++atX){
            for(int atZ = 0; atZ < Math.max(bottom.z - top.z, top.z - bottom.z); ++atZ){
                for(int atY = 0; atY < Math.max(bottom.y - top.y, top.y - bottom.y); ++atY){
                    double x = -Math.copySign(atX, bottom.x - top.x) + bottom.x,
                            z = -Math.copySign(atZ, bottom.z - top.z) + bottom.z,
                            y = -Math.copySign(atY,bottom.y - top.y) + bottom.y;

                    SimpleLocation loc = new SimpleLocation(bottom.world, x,y,z);
                    if(atX == Math.round(Math.abs(bottom.x-top.x) / 2) &&
                            atZ == Math.round(Math.abs(bottom.z - top.z)/2))
                        centre = loc;

                    if(!belongs.contains(loc)) {
                        locs.add(loc);
                        count++;
                    }
                }
            }
        }
        if(belongs.size() == 0)belongs.addAll(locs);
        else{
            belongs.clear();
            belongs.addAll(locs);
        }
        return count;
    }
    public int reSize(SimpleLocation bottom, SimpleLocation top){
        return a(bottom, top);
    }
    public Pair<Double, Double> getCoordinate(SimpleLocation loc){
        /**
         * @params: loc that is a member of this box
         * @returns: the coordinate on a 2D Oxy coordinate plane where y presents z axis value with 'centre' as
         * the O centre
         */
        if(belongs.contains(loc)){
            return new Pair<Double,Double>(loc.x - centre.x, loc.z - centre.z);
        }
        else
            return null;
    }
    public int increaseEachSideBy1BlockThick(int depthDown, int depthUp){
        SimpleLocation bottom = null, top = null;
        try {
            if (Math.min(this.bottom.y, this.top.y) - depthDown < 0 ||
                    Math.min(this.bottom.y, this.top.y) - depthUp > Objects.requireNonNull(Bukkit.getWorld(this.bottom.world)).getMaxHeight()
            || Math.max(this.bottom.y, this.top.y) + depthUp < 0 ||
                    Math.max(this.bottom.y, this.top.y) + depthUp > Objects.requireNonNull(Bukkit.getWorld(this.bottom.world)).getMaxHeight())
                return -1;
        }catch(Exception e){
            e.printStackTrace();
        }
        for(int i = 0; i < 2; ++i){
            SimpleLocation loc;
            if(i == 0)
                loc = this.bottom;
            else
                loc = this.top;
            for(int j = 0; j < 4; ++j){
                int atX = 1, atZ = 1;
                switch(j){
                    case 1:
                        atX = -1;
                        break;
                    case 2:
                        atX = -1;
                        atZ = -1;
                        break;
                    case 3:
                        atZ = -1;
                        break;
                }
                int y;
                if(i == 0)
                    y = depthDown;
                else
                    y = depthUp;
                SimpleLocation loc2 = loc.relative(atX, y , atZ);
                boolean isolated = belongs.stream().noneMatch(s->Math.sqrt(Math.pow(s.x - loc2.x, 2) + Math.pow(s.z - loc2.z,2)) == 1 &&
                        !s.equals(loc));
                if(isolated){
                    if(i == 0)
                        bottom = loc2;
                    else
                        top = loc2;

                    break;
                }
            }


        }
        if(bottom != null && top != null)
        return a(bottom,top);
        else
            return -1;
    }
}
