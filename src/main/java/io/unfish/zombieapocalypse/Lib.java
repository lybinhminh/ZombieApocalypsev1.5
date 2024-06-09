package io.unfish.zombieapocalypse;


import io.unfish.zombieapocalypse.type.Drawers;
import io.unfish.zombieapocalypse.type.Pair;

import io.unfish.zombieapocalypse.util.SimpleLocation;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import java.util.*;

public class Lib {
    public static final ItemStack cure_apple = new ItemStack(Material.GOLDEN_APPLE);
    public static final Drawers<SimpleLocation, Block> loadedBlocks = new Drawers<>();
    public static final Drawers<SimpleLocation, List<SimpleLocation>> saved_reachableFromLoc = new Drawers<>();
    public static final Drawers<SimpleLocation, List<SimpleLocation>> saved_reachableFromLoc_watermob = new Drawers<>();
    public static final List<Pair<SimpleLocation,SimpleLocation>> dangerZone = new ArrayList<>();
    public static List<Material> zombie_defense_materials = Arrays.asList(Material.CAMPFIRE, Material.SOUL_CAMPFIRE,Material.HOPPER,
        Material.WITHER_ROSE, Material.CACTUS, Material.LAVA, Material.RAIL, Material.STONE_SLAB,
        Material.COBBLESTONE_SLAB, Material.IRON_TRAPDOOR, Material.OAK_TRAPDOOR, Material.DARK_OAK_TRAPDOOR,
        Material.SPRUCE_TRAPDOOR, Material.MAGMA_BLOCK, Material.SOUL_SAND);
    public static final Drawers<SimpleLocation, List<SimpleLocation>> saved_reachableFromLoc_public = new Drawers<>();
    public static int digspeed(@NotNull Block b, @NotNull LivingEntity who){

        float speed_mutiplier = 1;
        int hasteLevel = 0;
        PotionEffect hasteEffect;
        if((hasteEffect = who.getPotionEffect(PotionEffectType.FAST_DIGGING)) != null){
            hasteLevel = hasteEffect.getAmplifier();
        }
        int miningfatigueLevel= 0;
        PotionEffect miningfatigueEffect ;
        if((miningfatigueEffect = who.getPotionEffect(PotionEffectType.SLOW_DIGGING)) != null){
            miningfatigueLevel = miningfatigueEffect.getAmplifier();
        }
        ItemStack tool = new ItemStack(Material.AIR);
        int efficiencyEnchantLevel = 0;
        if(who.getEquipment() != null) {
            tool = who.getEquipment().getItemInMainHand();
            Map<Enchantment, Integer> enchants = tool.getEnchantments();
            for(Map.Entry<Enchantment, Integer> e : enchants.entrySet()){
                if(e.getKey() == Enchantment.DIG_SPEED){
                    efficiencyEnchantLevel = e.getValue();
                    break;
                }
            }

        }
        float blockHardness = b.getType().getHardness();
        if(b.isValidTool(tool)) {
            if(tool.getType().name().toLowerCase().contains("wooden")){
                speed_mutiplier = 2;
            }
            if(tool.getType().name().toLowerCase().contains("stone")){
                speed_mutiplier = 4;
            }
            if(tool.getType().name().toLowerCase().contains("iron")){
                speed_mutiplier = 6;
            }
            if(b.getType().name().toLowerCase().contains("golden")){
                speed_mutiplier = 12;
            }
            if(b.getType().name().toLowerCase().contains("diamond")){
                speed_mutiplier = 8;
            }
            if(b.getType().name().toLowerCase().contains("netherite")){
                speed_mutiplier = 9;
            }
            if(tool.getType() == Material.SHEARS){
                speed_mutiplier = 2;
                if(b.getType().name().toLowerCase().contains("leaves")
                || b.getType() == Material.COBWEB)speed_mutiplier = 15;
                if(b.getType().name().toLowerCase().contains("wool"))speed_mutiplier = 5;
                if(b.getType().name().toLowerCase().contains("vine"))speed_mutiplier = 1;
            }
            if(tool.getType().name().toLowerCase().contains("sword")){
                speed_mutiplier = 1.5f;
                if(b.getType() == Material.COBWEB) speed_mutiplier = 15;
            }
            if(efficiencyEnchantLevel > 0)
            speed_mutiplier += (efficiencyEnchantLevel ^ 2 + 1);
            speed_mutiplier *= (0.2f * hasteLevel + 1);
            switch(miningfatigueLevel){
                case 1:
                    speed_mutiplier *= 0.7f;
                    break;
                case 2:
                    speed_mutiplier *= 0.91;
                    break;
                case 3:
                    speed_mutiplier *= 0.973;
                    break;
                default:
                    speed_mutiplier *= 0.9919;
            }
        }
        if(!who.isOnGround())speed_mutiplier /= 5;
        boolean helmetHasAquaInfinityEnchant = false;
        if(who.getEquipment() != null){
            ItemStack helmet ;
            if((helmet = who.getEquipment().getHelmet()) != null){
                if(helmet.getEnchantments().containsKey(Enchantment.WATER_WORKER))helmetHasAquaInfinityEnchant = true;
            }
        }
        if(who.isInWater() && !helmetHasAquaInfinityEnchant) speed_mutiplier /= 5;

        float damage = speed_mutiplier / blockHardness;

        if(b.isValidTool(tool))
            damage /= 30;
            else
             damage /= 100;

            if(damage > 0)return 0;

            int ticks = Math.round(1 / damage);

            return ticks / 20;
    }
    public static Object select_if_not_null(@NotNull Object notnull_backup, @Nullable Object check){
        if(check != null)return check;
        else return notnull_backup;
    }
    public static int countOccurrenceOfChInStr(char ch, String src){
        int result = 0;
        for(int i = 0; i < src.length(); ++i){
            if(src.charAt(i) == ch){
                result++;
            }
        }
        return result;
    }


}