package com.github.lybinhminh.zombieapocalypse.entity;

import com.github.lybinhminh.zombieapocalypse.Lib;
import com.github.lybinhminh.zombieapocalypse.Main;
import com.github.lybinhminh.zombieapocalypse.type.Drawers;
import com.github.lybinhminh.zombieapocalypse.type.Pair;
import com.github.lybinhminh.zombieapocalypse.util.SimpleLocation;

import org.bukkit.*;

import org.bukkit.attribute.Attribute;

import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;


import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import org.bukkit.util.Vector;



public class Host {
    enum ZAErrorCode{ HOST_UNAVAILABLE, TARGET_UNREACHABLE, REPETITIVE_CALL}
    // wrapper class of the bukkit class LivingEntity to provide extra functions for zombie-virus-infected creatures

    public LivingEntity baseEntity; // the base LivingEntity object

    public UUID entityUUID;

    private final int vision = 10;
    // checked
    private LivingEntity target = null; // current target, if it died, the var will be set to null, target is set by target() func
    // if the function is called but the previous target is still alive or wasn't reached by the zombie
    // leave it anyways and try to reach the new target

    public Thread activity = null; // thread of the target() func

    private final List<ZAErrorCode> lastErrors = new ArrayList<>();
    private boolean running = true;
    public double generic_attack_damage, generic_attack_speed,
    generic_attack_knockback;
    Material blockPossessed;
    private int __a = 255;
    public int patience;
    public Host(LivingEntity livingEntity) {
        // only constructor
        baseEntity = livingEntity;
        patience = Main.randomMachine.nextInt(__a)+ 1;

        blockPossessed = livingEntity.getLocation().getBlock().getRelative(0,1,0).getType();
        entityUUID = livingEntity.getUniqueId();
        try {
            if (baseEntity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) == null) {
                generic_attack_damage = ((double) Main.randomMachine.nextInt(10) + 1) * (1 + Main.DIFFICULTY / 10d);
            }
            else
                generic_attack_damage = Objects.requireNonNull(baseEntity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).getValue();
            if(baseEntity.getAttribute(Attribute.GENERIC_ATTACK_SPEED) == null) {
                baseEntity.registerAttribute(Attribute.GENERIC_ATTACK_SPEED);
                generic_attack_speed = ((Main.randomMachine.nextInt(5) + 1)
                        * Main.DIFFICULTY);
            }
            else
                generic_attack_speed = Objects.requireNonNull(baseEntity.getAttribute(Attribute.GENERIC_ATTACK_SPEED)).getValue();
            if(baseEntity.getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK) == null){
                generic_attack_knockback = Main.randomMachine.nextInt(10 + 1) / Math.pow(Main.DIFFICULTY,2);
            }
            else
                generic_attack_knockback = Objects.requireNonNull(baseEntity.getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK)).getValue();
        } catch (Exception e) {
                e.printStackTrace();
        }


    }
    public static void damage(LivingEntity damager, LivingEntity target, double baseDamage, double knockback){

            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 2f, (float)(Main.randomMachine.nextInt(10) + 1)
                    / 10);

            double damage = baseDamage, damageToHealth;
            if(damager.hasPotionEffect(PotionEffectType.INCREASE_DAMAGE)){
                int level = Objects.requireNonNull(damager.getPotionEffect(PotionEffectType.INCREASE_DAMAGE)).getAmplifier();
                damage = damage * Math.pow(1.3,level) + (Math.pow(1.3,level) - 1)/0.3;
            }
            if(Main.randomMachine.nextInt(100) <= 48)
            damage *= (1+ (Main.randomMachine.nextInt(50) + Main.DIFFICULTY ^ 2) / 100f);

            damageToHealth = damage;
            if(target.getEquipment() != null) {
                for (ItemStack equip : target.getEquipment().getArmorContents()) {
                    if(equip == null)continue;
                    ItemMeta im = equip.getItemMeta();
                    if (im instanceof Damageable) {
                        Damageable a = ((Damageable) im);
                        a.setDamage((int) Math.round(damage));
                        equip.setItemMeta((ItemMeta) a);
                    }
                    damageToHealth -= (double) (Main.randomMachine.nextInt(10) + 1) / 10d;
                    if (equip.getEnchantments().containsKey(Enchantment.PROTECTION_ENVIRONMENTAL)) {
                        damageToHealth -= damageToHealth * (equip.getEnchantments().get(Enchantment.PROTECTION_ENVIRONMENTAL) * 4) / 100;
                    }
                }

                ItemStack shield;
                if ((shield = target.getEquipment().getItemInOffHand()).getType() == Material.SHIELD
                        || (shield = target.getEquipment().getItemInMainHand()).getType() == Material.SHIELD) {
                    Damageable a = (Damageable) shield.getItemMeta();
                    a.setDamage((int) damage);
                    shield.setItemMeta((ItemMeta) a);
                    damageToHealth = 0;
                }


            }
            if(target.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)){
                damageToHealth -=
                        damageToHealth * (Objects.requireNonNull(target.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)).getAmplifier()
                                * 20) / 100;
            }
            target.setHealth(Math.max(0, target.getHealth() - Math.max(0, damageToHealth)));
            Vector targetVec = target.getVelocity(),
                    hurtDirection = damager.getLocation().toVector().subtract(target.getLocation().toVector());
            target.setVelocity(targetVec.subtract(new Vector(Math.copySign(knockback,hurtDirection.getX()),
                    -0.5d, Math.copySign(knockback, hurtDirection.getZ()))).normalize());


    }
    public void setTarget(@Nullable LivingEntity prey ) {
        /**
         * params: prey: the living entity that this host will seek for to attack,
         * if set to null, stop chasing the current target.
         *              otherTargets: an array of other possible targets, used in case
         *              the prey is considered as an unreachable target
         * returns: void, if the thread is dead, the result of this func
         *  can be obtained from getLastError() func.
         * Set the 'prey' as the hunting target of this host, the host will try to get close
         *  to the prey and attack it till it dies or host.setTarget(null) is used.
         */

        if((baseEntity = (LivingEntity)Bukkit.getEntity(entityUUID)) == null || baseEntity.isDead()) {
            lastErrors.add(ZAErrorCode.HOST_UNAVAILABLE);
            return;
        }
        if(prey != null) {
            if (target != null)
                if (prey.getUniqueId().compareTo(target.getUniqueId()) == 0) {
                    lastErrors.add(ZAErrorCode.REPETITIVE_CALL);
                    return;
                }
            target = prey;
        }
        else
        {
            target = null;
        }
        if(activity == null || !activity.isAlive()) {
            // if the thread hasn't been created or the previous target has died so the thread died
            running = true;
            activity = new Thread(() -> {

                baseEntity.setAI(false);
                long clock = System.currentTimeMillis(), timer = System.currentTimeMillis();
                while(running) {
                    if (System.currentTimeMillis() - clock >= 1000 / generic_attack_speed) {
                        clock = System.currentTimeMillis();
                        if (target == null || target.isDead() || (baseEntity = (LivingEntity) Bukkit.getEntity(entityUUID)) == null || baseEntity.isDead()) {
                            if (target == null) {
                                lastErrors.add(ZAErrorCode.TARGET_UNREACHABLE);
                                if(Main.debugMode)
                                Main.PLUGIN.getLogger().info("Host is null");
                            } else
                                target = null;
                            if (baseEntity != null && !baseEntity.isDead())
                                baseEntity.setAI(true);
                            else
                                lastErrors.add(ZAErrorCode.HOST_UNAVAILABLE);
                            running = false;
                            break;
                        }
                        Location hLoc = baseEntity.getLocation();
                        double hX = hLoc.getBlockX(),
                                hY = hLoc.getBlockY(),
                                hZ = hLoc.getBlockZ(),
                                tX = target.getLocation().getBlockX(),
                                tY = target.getLocation().getBlockY(),
                                tZ = target.getLocation().getBlockZ();
                        World hWorld = baseEntity.getWorld(),
                                tWorld = target.getWorld();
                        if (hWorld.getUID().compareTo(tWorld.getUID()) != 0) {
                            lastErrors.add(ZAErrorCode.TARGET_UNREACHABLE);
                            target = null;
                            if(Main.debugMode)
                            Main.PLUGIN.getLogger().info("Different worlds");
                            running = false;
                            break;
                        }
                        if (Math.sqrt(Math.pow(hX - tX, 2) + Math.pow(hY - tY, 2) + Math.pow(hZ - tZ, 2)) <= 2 * Math.sqrt(3)) {
                            try {
                                Main._abc.add(this);


                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        List<SimpleLocation> path = new ArrayList<>();
                        SimpleLocation extreme, destination = new SimpleLocation(hWorld.getUID(), tX, tY, tZ);
                        int entityHeight = (int) Math.round(baseEntity.getHeight());
                        boolean worldInteraction = false;
                        if(System.currentTimeMillis() - timer >= 5000)
                        {
                            timer = System.currentTimeMillis();
                            while (Math.sqrt(Math.pow(hX - tX, 2) + Math.pow(hY - tY, 2) + Math.pow(hZ - tZ, 2)) > 2 * Math.sqrt(3)) {

                                SimpleLocation from = new SimpleLocation(hWorld.getUID(), hX, hY, hZ);
                                extreme = from;

                                Drawers<SimpleLocation, List<SimpleLocation>> reachableFromLoc = new Drawers<>();
                                {
                                    double distance = extreme.distance(destination);
                                    List<SimpleLocation> locs = new ArrayList<>();

                                    Drawers<SimpleLocation, Integer> spaceHeight = new Drawers<>();
                                    if (Main.debugMode)
                                        Main.PLUGIN.getLogger().info("-ll1");
                                    locs.add(from);
                                    for (int i = 0; i < locs.size(); ++i) {
                                        SimpleLocation loc = locs.get(i);
                                        reachableFromLoc.put(loc, new ArrayList<>());
                                        for (int j = 0; j < 8; ++j) {
                                            int atX = 1, atZ = 0;
                                            switch (j) {
                                                case 1:
                                                    atX = -1;
                                                    break;
                                                case 2:
                                                    atX = 0;
                                                    atZ = -1;
                                                    break;
                                                case 3:
                                                    atX = 0;
                                                    atZ = 1;
                                                    break;
                                                case 4:
                                                    atX = 1;
                                                    atZ = 1;
                                                    break;
                                                case 5:
                                                    atX = -1;
                                                    atZ = 1;
                                                    break;
                                                case 6:
                                                    atX = -1;
                                                    atZ = -1;
                                                    break;
                                                case 7:
                                                    atX = 1;
                                                    atZ = -1;
                                            }
                                            double x = loc.x + atX, z = loc.z + atZ;
                                            if (x > vision / 2f + from.x
                                                    || x < -vision / 2f + from.x) atX = 0;
                                            if (z > vision / 2f + from.z
                                                    || z < -vision / 2f + from.z) atZ = 0;
                                            x = loc.x + atX;
                                            z = loc.z + atZ;

                                                final double[] X = {x}, Z = {z};


                                            for (int y = entityHeight +
                                                    (int) Math.round(loc.y), emptySpaceHeight = 0; y > Math.max(0, Math.round(loc.y) - (3 + baseEntity.getHealth())); --y) {
                                                SimpleLocation loc2 = new SimpleLocation(loc.world, x, y, z);
                                                if (locs.contains(loc2))break;
                                                if (Lib.dangerZone.stream().anyMatch(s -> ((s.first.x <= loc2.x && loc2.x <= s.second.x)
                                                        || (s.first.x >= loc2.x && loc2.x >= s.second.x)) && ((s.first.y <= loc2.y && loc2.y <= s.second.y)
                                                        || (s.first.y >= loc2.y && loc2.y >= s.second.y) && ((s.first.z <= loc2.z && loc2.z <= s.second.z)
                                                        || (s.first.z >= loc2.z && loc2.z >= s.second.z))))) continue;
                                                Block b = loc2.toBlock();
                                                if ((!(baseEntity instanceof WaterMob) && (b.getType().isAir() || (Main.o.stream().noneMatch(s -> s.region.belongs.contains(loc2)) && worldInteraction &&
                                                        Lib.digspeed(b, baseEntity) <= Main.CONST0x0000)))
                                                        || ((baseEntity instanceof WaterMob) && ((b.getType() == Material.WATER)
                                                        || (worldInteraction && Lib.digspeed(b, baseEntity) <= Main.CONST0x0000))))
                                                    emptySpaceHeight++;
                                                if (b.isSolid() && y == entityHeight + loc.y
                                                        && (!worldInteraction && Lib.digspeed(b, baseEntity) > Main.CONST0x0000))
                                                    break;
                                                if (b.getType() == Material.LAVA && !worldInteraction)
                                                    emptySpaceHeight = 0;
                                                if (b.isSolid() || baseEntity instanceof WaterMob || worldInteraction) {

                                                    if ((!(baseEntity instanceof WaterMob) && emptySpaceHeight >= entityHeight
                                                            && !worldInteraction)
                                                            || ((baseEntity instanceof WaterMob || worldInteraction) && emptySpaceHeight > entityHeight)) {
                                                        if ((y == loc.y + 1 && spaceHeight.get(loc) >= entityHeight + 1)
                                                                || (y <= loc.y && emptySpaceHeight >= loc.y - loc2.y + entityHeight)) {
                                                            SimpleLocation loc3 = loc2.relative(0, 1, 0);
                                                            reachableFromLoc.get(loc).add(loc3);

                                                            if (!locs.contains(loc3)) {
                                                                locs.add(loc3);
                                                                spaceHeight.put(loc3, emptySpaceHeight);
                                                                double distance2 = loc3.distance(destination);
                                                                if (distance2 < distance) {
                                                                    extreme = loc3;
                                                                    distance = distance2;
                                                                }
                                                            }

                                                        }
                                                    }
                                                    if (b.isSolid()) {
                                                        if (Lib.zombie_defense_materials.contains(b.getType())) {
                                                            if (!worldInteraction || Lib.digspeed(b, baseEntity) > 2)
                                                                Lib.dangerZone.add(new Pair<>(loc2.relative(0, 1, 0), loc2.relative(0,
                                                                        emptySpaceHeight, 0)));
                                                            reachableFromLoc.remove(loc2.relative(0, 1, 0));
                                                        }
                                                        if (!worldInteraction && Lib.digspeed(b, baseEntity) > 2)
                                                            emptySpaceHeight = 0;
                                                    }
                                                }
                                                if (baseEntity instanceof WaterMob && b.getType() != Material.WATER)
                                                    emptySpaceHeight = 0;

                                            }
                                        }
                                    }

                                    locs.clear();
                                }
                                if (Main.debugMode) {
                                    Main.PLUGIN.getLogger().info("-ll1 end");
                                    Main.PLUGIN.getLogger().info("from " + from.toString() + " extreme " + extreme.toString());
                                }
                                if (extreme.equals(from)) {
                                    if (worldInteraction) {
                                        baseEntity.setAI(true);
                                        target = null;
                                        lastErrors.add(ZAErrorCode.TARGET_UNREACHABLE);
                                        if (Main.debugMode)
                                            Main.PLUGIN.getLogger().info("Cannot reach in the range to the target. Dead end");
                                        running = false;
                                        break;
                                    } else {
                                        worldInteraction = true;
                                        continue;
                                    }
                                }

                                SimpleLocation pathEnd;
                                List<SimpleLocation> joins = new ArrayList<>();
                                Drawers<SimpleLocation, List<SimpleLocation>> ways = new Drawers<>();
                                joins.add(from);
                                List<SimpleLocation> scannedLocation = new ArrayList<>();
                                boolean finished = false;
                                if (Main.debugMode)
                                    Main.PLUGIN.getLogger().info("-ll2");
                                for (int a = 0; a < joins.size() && !finished; ++a) {
                                    if (Main.debugMode)
                                        Main.PLUGIN.getLogger().info("enter join no " + a);
                                    pathEnd = joins.get(a);
                                    for (int nCoordPlane = 0; nCoordPlane < 4 && !finished; ++nCoordPlane) {
                                        int xnSign = 1, znSign = 1;
                                        switch (nCoordPlane) {
                                            case 1:
                                                xnSign = -1;
                                                break;
                                            case 2:
                                                xnSign = -1;
                                                znSign = -1;
                                                break;
                                            case 3:
                                                znSign = -1;
                                                break;
                                        }
                                        int avgWayLength = 2;
                                        for (int degree = 0; degree < 90 && !finished; ++degree) {

                                            List<SimpleLocation> way = new ArrayList<>();
                                            SimpleLocation previousLoc = pathEnd;
                                            way.add(pathEnd);
                                            int atX = 1;
                                            for (int atZ; ; atX++) {
                                                SimpleLocation move = null;
                                                atZ = znSign * (int) (Math.tan(degree * Math.PI / 180) * atX);
                                                double x = pathEnd.x + xnSign * atX,
                                                        z = pathEnd.z + atZ;
                                                if (reachableFromLoc.get(previousLoc) != null) {
                                                    List<SimpleLocation> possibleMoves = reachableFromLoc.get(previousLoc);
                                                    double b = baseEntity.getHeight() + 3d;
                                                    for (SimpleLocation loc : possibleMoves) {
                                                        if (Math.round(loc.x) == Math.round(x) && Math.round(loc.z) ==
                                                                Math.round(z)) {
                                                            double c = Math.abs(loc.y - extreme.y);
                                                            if (c < b) {
                                                                move = loc;
                                                                b = c;
                                                            }
                                                        }


                                                    }
                                                }
                                                if (move != null) {
                                                    if (!scannedLocation.contains(move))
                                                        scannedLocation.add(move);
                                                    way.add(move);
                                                    previousLoc = move;
                                                    if (move.equals(extreme)) {
                                                        path.addAll(way);
                                                        pathEnd = path.get(0);
                                                        while (!pathEnd.equals(from)) {
                                                            path.addAll(0, ways.get(pathEnd));
                                                            pathEnd = path.get(0);
                                                        }

                                                        finished = true;
                                                        break;
                                                    }
                                                } else {
                                                    break;
                                                }
                                            }


                                            if (ways.get(previousLoc) != null) {
                                                continue;
                                            }

                                            if (Main.debugMode && ways.copyOfValues().stream().anyMatch(s -> s.get(0).equals(way.get(0)) &&
                                                    s.get(s.size() - 1).equals(way.get(way.size() - 1)))) {

                                                Main.PLUGIN.getLogger().info("-ll2 sub duplicate");
                                            }

                                            if (atX > avgWayLength) {
                                                if (Main.debugMode)
                                                    Main.PLUGIN.getLogger().info("join no." + a + " degree " + degree + "" +
                                                            " from " + way.get(0).toString() + " to " + previousLoc.toString());
                                                ways.put(previousLoc, way);
                                                joins.add(previousLoc);
                                            }
                                            avgWayLength = (avgWayLength + atX) / 2;
                                        }
                                        reachableFromLoc.remove(scannedLocation.toArray(new SimpleLocation[0]));
                                        scannedLocation.clear();

                                    }


                                }
                                if (Main.debugMode)
                                    Main.PLUGIN.getLogger().info("-ll2 end");
                                hX = extreme.x;
                                hY = extreme.y;
                                hZ = extreme.z;

                                if (path.size() == 0) {
                                    if (patience == 0) {
                                        if(__a > 0)__a--;
                                        patience = Main.randomMachine.nextInt(Math.max(__a--,1));
                                        running = false;
                                        lastErrors.add(ZAErrorCode.TARGET_UNREACHABLE);
                                        if (Main.debugMode)
                                            Main.PLUGIN.getLogger().info("Cannot draw a path");
                                        baseEntity.setAI(true);
                                        break;
                                    } else {
                                        patience--;
                                        continue;
                                    }
                                }

                                reachableFromLoc.clear();

                                if (running) {
                                    clock = System.currentTimeMillis();

                                    int a = 1;
                                    int b = 0;
                                    SimpleLocation loc = path.get(a);
                                    double c = 0, d = 0, e = 0;
                                    if (Main.debugMode)
                                        Main.PLUGIN.getLogger().info("-ll3");
                                    do {
                                        if (System.currentTimeMillis() - clock >= 100 / Objects.requireNonNull(baseEntity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).getValue()
                                                / 16) {

                                            clock = System.currentTimeMillis();
                                            hX = hLoc.getX();
                                            hY = hLoc.getY();
                                            hZ = hLoc.getZ();
                                            if (b == 0) {
                                                c = (loc.x - hX) / 16;
                                                d = (loc.y - hY) / 16;
                                                e = (loc.z - hZ) / 16;
                                            }
                                            double x = hX + c,
                                                    y = hY + d,
                                                    z = hZ + e;
                                            baseEntity.teleportAsync(new Location(hWorld, x, y, z));
                                            b++;
                                            if (b == 0) {
                                                if (worldInteraction) {
                                                    SimpleLocation loc2 = new SimpleLocation(loc.world, x, y + (int) baseEntity.getHeight() + 1, z);
                                                    Block top;
                                                    if (Lib.loadedBlocks.containsKey(loc2))
                                                        top = Lib.loadedBlocks.get(loc2);
                                                    else {
                                                        top = loc2.toBlock();
                                                        Lib.loadedBlocks.put(loc2, top);
                                                    }
                                                    if (Lib.digspeed(top, baseEntity) <= Main.CONST0x0000)
                                                        top.setType(blockPossessed);
                                                    for (int i = 0; i < (int) baseEntity.getHeight(); ++i) {
                                                        loc2 = new SimpleLocation(loc.world, x, y + i, z);
                                                        if (Lib.loadedBlocks.containsKey(loc2))
                                                            top = Lib.loadedBlocks.get(loc2);
                                                        else {
                                                            top = loc2.toBlock();
                                                            Lib.loadedBlocks.put(loc2, top);
                                                        }
                                                        if (baseEntity instanceof WaterMob) {
                                                            if (top.getType() != Material.WATER)
                                                                top.setType(Material.WATER);
                                                        } else {
                                                            if (!top.isPassable() || Lib.zombie_defense_materials.contains(top.getType()))
                                                                top.setType(Material.AIR);
                                                        }
                                                    }
                                                    loc2 = new SimpleLocation(loc.world, x, y - 1, z);
                                                    if (Lib.loadedBlocks.containsKey(loc2))
                                                        top = Lib.loadedBlocks.get(loc2);
                                                    else {
                                                        top = loc2.toBlock();
                                                        Lib.loadedBlocks.put(loc2, top);
                                                    }
                                                    if (!(baseEntity instanceof WaterMob)) {
                                                        if (!top.isSolid() || Lib.zombie_defense_materials.contains(top.getType()))
                                                            top.setType(blockPossessed);
                                                    }

                                                }
                                            }
                                            if (b == 16) {
                                                b = 0;
                                                if (++a == path.size()) break;
                                                loc = path.get(a);

                                            }

                                        }

                                    } while (a < path.size());
                                    if (Main.debugMode)
                                        Main.PLUGIN.getLogger().info("-ll3 end");
                                    path.clear();
                                }


                            }
                        }


                    }
                }
                if(baseEntity != null && !baseEntity.isDead())
                    baseEntity.setAI(true);

            });
            activity.start();

        }
    }
    public List<ZAErrorCode> getLastErrors(){
        /**
        @params: void
         @return: list of ZAErrorCode enum values
         make a copy of a list of error codes, clear the original list and return the copy.
         */
        List<ZAErrorCode> copy = new ArrayList<>(lastErrors);
        lastErrors.clear();
        return copy;
    }
    public static @Nullable Pair<Double,List<SimpleLocation>> drawPath(@NotNull SimpleLocation from, @NotNull SimpleLocation target,
                               double gapHeight, @Nullable List<Material> transparents){

        Pair<Double, List<SimpleLocation>> result = null;
        double height = 2;
        List<Material> Transparents = new ArrayList<>(Arrays.asList(Material.values()));
        if(transparents != null)Transparents = transparents;
        if(gapHeight > 0)height= gapHeight;
        double fX = from.x, fY = from.y, fZ = from.z,
                tX = target.x, tY = target.y, tZ = target.z;
        UUID world = from.world;
        if(world.compareTo(target.world) != 0)return null;
        SimpleLocation extreme, point;
        List<SimpleLocation> path = new ArrayList<>();
        double finalDistance = 0;
        while (Math.sqrt(Math.pow(fX - tX,2) + Math.pow(fY - tY,2) + Math.pow(fZ - tZ,2)) > 0) {


             point = new SimpleLocation(world, fX, fY, fZ);
            extreme = point;
            finalDistance = extreme.distance(target);
            Drawers<SimpleLocation, List<SimpleLocation>> reachableFromLoc = new Drawers<>();
            {
                double distance = extreme.distance(target);
                List<SimpleLocation> locs = new ArrayList<>();

                Drawers<SimpleLocation, Integer> spaceHeight = new Drawers<>();

                locs.add(point);
                for (int i = 0; i < locs.size(); ++i) {
                    SimpleLocation loc = locs.get(i);
                        reachableFromLoc.put(loc, new ArrayList<>());
                        for (int j = 0; j < 8; ++j) {
                            int atX = 1, atZ = 0;
                            switch (j) {
                                case 1:
                                    atX = -1;
                                    break;
                                case 2:
                                    atX = 0;
                                    atZ = -1;
                                    break;
                                case 3:
                                    atX = 0;
                                    atZ = 1;
                                    break;
                                case 4:
                                    atX = 1;
                                    atZ = 1;
                                    break;
                                case 5:
                                    atX = -1;
                                    atZ = 1;
                                    break;
                                case 6:
                                    atX = -1;
                                    atZ = -1;
                                    break;
                                case 7:
                                    atX = 1;
                                    atZ = -1;
                            }
                            double x = loc.x + atX, z = loc.z + atZ;
                            if (x > 5f + point.x
                                    || x < -5f + point.x) atX = 0;
                            if (z > 5f + point.z
                                    || z < -5f + point.z) atZ = 0;
                            x = loc.x + atX;
                            z = loc.z + atZ;
                            for (int y = (int)Math.round(height) +
                                    (int) Math.round(loc.y), emptySpaceHeight = 0; y > 0; --y) {
                                SimpleLocation loc2 = new SimpleLocation(loc.world, x, y, z);
                                if(locs.contains(loc2))break;
                                Block b = loc2.toBlock();
                                if (Transparents.contains(b.getType()))
                                    emptySpaceHeight++;
                                if (!Transparents.contains(b.getType()) && y == height + loc.y)
                                    break;

                                if (emptySpaceHeight >= height) {
                                    if ((y == loc.y + 1 && spaceHeight.get(loc) >= height + 1)
                                            || (y <= loc.y && emptySpaceHeight >= loc.y - loc2.y + height)) {
                                        SimpleLocation loc3 = loc2.relative(0, 1, 0);
                                        reachableFromLoc.get(loc).add(loc3);

                                        if (!locs.contains(loc3)) {
                                            locs.add(loc3);
                                            spaceHeight.put(loc3, emptySpaceHeight);
                                            double distance2 = loc3.distance(target);
                                            if (distance2 < distance) {
                                                extreme = loc3;
                                                distance = distance2;
                                            }
                                        }

                                    }
                                }
                                if (!Transparents.contains(b.getType())) {
                                    emptySpaceHeight = 0;
                                }

                            }
                        }

                }
                if (extreme.equals(point))
                    return new Pair<>(finalDistance, path);

            }
            SimpleLocation pathEnd;
            List<SimpleLocation> joins = new ArrayList<>();
            Drawers<SimpleLocation, List<SimpleLocation>> ways = new Drawers<>();
            joins.add(from);
            List<SimpleLocation> scannedLocation = new ArrayList<>();
            boolean finished = false;
            for (int a = 0; a < joins.size() && !finished; ++a) {
                pathEnd = joins.get(a);
                for (int nCoordPlane = 0; nCoordPlane < 4 && !finished; ++nCoordPlane) {
                    int xnSign = 1, znSign = 1;
                    switch (nCoordPlane) {
                        case 1:
                            xnSign = -1;
                            break;
                        case 2:
                            xnSign = -1;
                            znSign = -1;
                            break;
                        case 3:
                            znSign = -1;
                            break;
                    }
                    int avgWayLength = 2;
                    for (int degree = 0; degree < 90 && !finished; ++degree) {

                        List<SimpleLocation> way = new ArrayList<>();
                        SimpleLocation previousLoc = pathEnd;
                        way.add(pathEnd);
                        int atX = 1;
                        for (int atZ; ; atX++) {
                            SimpleLocation move = null;
                            atZ = znSign * (int) (Math.tan(degree * Math.PI / 180) * atX);
                            double x = pathEnd.x + xnSign * atX,
                                    z = pathEnd.z + atZ;
                            if (reachableFromLoc.get(previousLoc) != null) {
                                List<SimpleLocation> possibleMoves = reachableFromLoc.get(previousLoc);
                                double b = 2, c;
                                for (SimpleLocation loc : possibleMoves) {
                                    if (loc.x == x && loc.z == z) {
                                        c = Math.abs(loc.y - extreme.y);
                                        if (c < b) {
                                            move = loc;
                                            b = c;
                                        }
                                    }


                                }
                                if (move != null) {
                                    if(!scannedLocation.contains(move))
                                    scannedLocation.add(move);
                                    way.add(move);
                                    previousLoc = move;
                                    if (move.equals(extreme)) {
                                        for(int i = 0; i < ways.size(); ++i){
                                            List<SimpleLocation> w = ways.getByIndex(i);
                                            path.addAll(ways.getByIndex(i).subList(1,w.size()));
                                        }

                                        finished = true;
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            }

                            if (ways.get(previousLoc) != null) {
                                continue;
                            }
                            if (atX > avgWayLength) {
                                ways.put(previousLoc, way);
                                joins.add(previousLoc);
                            }
                            avgWayLength = (avgWayLength + atX) / 2;
                        }
                        reachableFromLoc.remove(scannedLocation.toArray(new SimpleLocation[0]));
                        scannedLocation.clear();

                    }
                }
            }
            fX = extreme.x;
            fY = extreme.y;
            fZ = extreme.z;
        }

        return new Pair<>(finalDistance, path);
        }
    public @Nullable  LivingEntity getTarget(){
        return target;
    }
    public void stop(){
        running = false;
    }
}
