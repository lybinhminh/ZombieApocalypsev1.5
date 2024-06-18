package com.github.lybinhminh.zombieapocalypse;

import com.github.lybinhminh.zombieapocalypse.type.Drawers;
import com.github.lybinhminh.zombieapocalypse.type.Pair;
import com.github.lybinhminh.zombieapocalypse.util.SimpleLocation;
import com.github.lybinhminh.zombieapocalypse.entity.Host;
import com.github.lybinhminh.zombieapocalypse.entity.livingMassCollection;
import com.github.lybinhminh.zombieapocalypse.util.Box;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.InetAddress;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class Main extends JavaPlugin implements Listener {
    final List<UUID> a =  new ArrayList<>(); // list of minecraft vanilla zombie
    final  List<Host> b = new ArrayList<>(); // list of plugin's infected creatures
    public static final List<SimpleLocation> c = new ArrayList<>(); // list of graves' locations.
    final Map<UUID,  Long> d = new ConcurrentHashMap<>(); // list of sane period of the bitten-by-zombies creatures
    public static final double CONST0x0000 = 2.0d;
    final int CONST0x0001 = 200,CONST0x0002= 10, CONST0x0003 = 1000,    CONST0x0004 = 5;
    public static final Random randomMachine = new Random();
    Component e = null; // cure apple name
    List<Component> f = null; // cure apple lore
    YamlConfiguration Config;
    public static int DIFFICULTY = 3;

    final List<UUID> g = new ArrayList<>(); // list of entities which will be burnt under sunlight

    final Map<String, String> statistics = new HashMap<>();
    final List <EntityType> h = new ArrayList<>(); // list of mob kinds of both vanilla and plugin's zombies that got beaten and in queue to be revive
    private final Drawers<SimpleLocation, Block> __a = new Drawers<>();
    Book terminal = null;
    public static Plugin PLUGIN;

    final List<Material> ZOMBIE_BLOCK = new ArrayList<>(Arrays.asList(Material.DIRT, Material.COBBLESTONE,Material.OAK_LEAVES,
    Material.TNT));

    Drawers<UUID, Host> l = new Drawers<>();

    Drawers<SimpleLocation, Long> ____b = new Drawers<>();
    long zombieLimit = 2000;
    List<String> reverseMovingOnPlayers = new ArrayList<>();
    Map<String, UUID> ipToPlayer = new HashMap<>();
    public static final List<livingMassCollection> o = new ArrayList<>();
    public static final List<Host> _abc = new ArrayList<>();
    public final Drawers<SimpleLocation, Material> _xyz = new Drawers<>();
    Drawers<SimpleLocation, Integer> _abc_xyz = new Drawers<>();
    public final static int[] noThreads = {0};
    public static boolean debugMode = false;
    public final List<Chunk> _def = new ArrayList<>();
    public static final List<Chunk> __def = new ArrayList<>();
    @SuppressWarnings({"deprecation", "ResultOfMethodCallIgnored"})
    @Override
    public void onEnable() {
        // Plugin startup logic
        PLUGIN = this;
        {
            try {
                File pluginDir = new File("./plugins/" + this.getName());
                if (!pluginDir.exists()) {
                    pluginDir.mkdir();
                }
                File config = new File("./plugins/"+ this.getName() +"/config.yml");

                if(!config.exists()){
                    InputStream is = getResource("config.yml");
                    Files.copy(Objects.requireNonNull(is), config.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    is.close();
                }

                Config = YamlConfiguration.loadConfiguration(config);
                Config.addDefault("danger_tps",15);
                DIFFICULTY = Math.max(1, Config.getInt("difficulty"));
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        {
            NamespacedKey nsk = new NamespacedKey(this, "cure_apple");
            ItemMeta im = Lib.cure_apple.getItemMeta();
            im.displayName(Component.text("Magic").color(TextColor.color(0,255,255)).append(Component.text(" Apple").color(TextColor.color(255,0,0))));
            e = im.displayName();
            im.lore(Arrays.asList(Component.text(nsk.asString()),
                    Component.text("Give the consumer a 50/50 opportunity to completely \n").append(
                            Component.text(" remove the zombie virus from their body.")).color(TextColor.color(TextColor.color(250,250,51)))));
            f = im.lore();
            Lib.cure_apple.setItemMeta(im);

            ShapedRecipe craft_recipe = new ShapedRecipe(nsk,Lib.cure_apple);
            craft_recipe.shape("ABC");
            craft_recipe.setIngredient('A',Material.GOLDEN_APPLE,1);
            craft_recipe.setIngredient('B',Material.FERMENTED_SPIDER_EYE,1);
            craft_recipe.setIngredient('C',Material.ROTTEN_FLESH,1);

            Bukkit.addRecipe(craft_recipe);
        }
        {
            int no_zombie_loaded = 0, no_grave_loaded = 0;
            try {
                DirectoryStream<Path> save = Files.newDirectoryStream(new File("./plugins/" + this.getName()).toPath());
                for(Path p : save){
                    File f = p.toFile();
                    if(f.getName().substring(f.getName().lastIndexOf(".") + 1).equals("save")){
                        Scanner sc = new Scanner(f);
                        while(sc.hasNextLine()){
                            String ln = sc.nextLine();
                            ln = ln.substring(ln.indexOf(".") + 1);
                            String[] tokens =ln.split(":");
                            String type = tokens[0];
                            if(type.equals("ZOMBIE") || type.equals("HOST"))
                            {
                                try {
                                    World world =  Bukkit.getWorld(UUID.fromString(tokens[4]));
                                    if(world == null)continue;
                                    SimpleLocation loc = new SimpleLocation(world.getUID(), Double.parseDouble(tokens[1]),
                                    Double.parseDouble(tokens[2]), Double.parseDouble(tokens[3]));

                                    LivingEntity entity = (LivingEntity)world
                                            .spawnEntity(loc.toLocation(), EntityType.valueOf(tokens[5]));

                                            if (type.equals("ZOMBIE")) {
                                                a.add(entity.getUniqueId());
                                            } else {
                                                b.add(new Host(entity));
                                            }

                                        no_zombie_loaded++;
                                }catch(Exception e){
                                    e.printStackTrace();
                                }
                            }
                            else if(type.equals("GRAVE")){
                                try{

                                    int x = (int)Math.round(Double.parseDouble(tokens[1])),
                                            y = (int)Math.round(Double.parseDouble(tokens[2])),
                                            z = (int)Math.round(Double.parseDouble(tokens[3]));
                                    UUID world = UUID.fromString(tokens[4]);
                                    SimpleLocation loc = new SimpleLocation(world,x,y,z);
                                    c.add(loc);
                                    ____b.put(loc, 120L);
                                    no_grave_loaded++;
                                }catch(Exception e){
                                    e.printStackTrace();
                                }
                            }
                            else if(type.equals("ENTITY_CORE")){
                                try{
                                    UUID world = UUID.fromString(tokens[1]);
                                    SimpleLocation bottom = new SimpleLocation(world, Double.parseDouble(tokens[2]),
                                            Double.parseDouble(tokens[3]), Double.parseDouble(tokens[4])),
                                            top = new SimpleLocation(world, Double.parseDouble(tokens[5]),Double.parseDouble(tokens[6]),
                                                    Double.parseDouble(tokens[7]));
                                    int state = Integer.parseInt(tokens[8]);
                                    double health = Double.parseDouble(tokens[9]);
                                    livingMassCollection entity_core = new livingMassCollection(new Box(bottom,top));
                                    entity_core.state = state;
                                    entity_core.health = health;
                                    entity_core.grow();
                                    o.add(entity_core);
                                }catch(Exception e){
                                    e.printStackTrace();
                                }
                            }
                            else if(type.equals("ZOMBIE_BLOCK")){
                                UUID world = UUID.fromString(tokens[1]);
                                double x = Double.parseDouble(tokens[2]),
                                        y = Double.parseDouble(tokens[3]),
                                        z = Double.parseDouble(tokens[4]);
                                int existingTime = Integer.parseInt(tokens[5]);
                                SimpleLocation loc = new SimpleLocation(world,x,y,z);
                                _abc_xyz.put(loc,existingTime);
                            }
                        }
                        sc.close();

                        f.delete();
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            getLogger().info("Loaded " + no_zombie_loaded +" zombies\n" + " and " + no_grave_loaded + " graves");
        }
       Drawers<List<UUID>, List<UUID>> nearbyTargets = new Drawers<>();

        int[] count = {0};
        int schedule1 = Bukkit.getScheduler().scheduleSyncRepeatingTask(this,
               () ->{
            ++count[0];
            if(count[0]  == 20) {
                count[0] = 0;
                nearbyTargets.clear();



                for (Player p : getServer().getOnlinePlayers()) {
                    List<List<UUID>> groups = new ArrayList<>();
                    Drawers<UUID, SimpleLocation> locs = new Drawers<>();
                    Drawers<List<UUID>, SimpleLocation> group_locs = new Drawers<>();
                    List<List<UUID>> target_groups = new ArrayList<>();
                    Drawers<List<UUID>, SimpleLocation> target_group_locs = new Drawers<>();
                    for (Entity entity : p.getNearbyEntities(500, p.getLocation().getY(), 500)) {
                        if (entity.getType().isAlive()) {
                            LivingEntity livingEntity = (LivingEntity) entity;
                            SimpleLocation loc = new SimpleLocation(livingEntity.getLocation());
                            if (!livingEntity.isDead()) {
                                Host[] h = new Host[1];
                                UUID uid = livingEntity.getUniqueId();
                                locs.put(uid, new SimpleLocation(livingEntity.getLocation()));
                                if(livingEntity instanceof Zombie || b.stream().anyMatch(s->( h[0] = s).entityUUID.compareTo(uid) == 0)) {

                                    List<UUID>[] group = new ArrayList[1];
                                    try {
                                        if (groups.stream().noneMatch(s -> (group[0] = s).stream().anyMatch(s2 -> locs.get(s2)
                                                .distance(loc) <= Math.sqrt(3) * 5 * Math.sqrt(Main.DIFFICULTY)))) {
                                            group[0] = new ArrayList<>();
                                            groups.add(group[0]);
                                            group[0].add(uid);

                                            group_locs.put(group[0], loc);
                                        }
                                        else{
                                            group[0].add(uid);
                                            SimpleLocation centreLoc = group_locs.get(group[0]).midPoint(loc);
                                            group_locs.put(group[0], centreLoc);
                                        }
                                    }catch(Exception e){
                                        e.printStackTrace();
                                    }
                                }
                                else{

                                    if(!(livingEntity instanceof Monster) &&
                                    !livingEntity.hasPotionEffect(PotionEffectType.INVISIBILITY)
                                    && !(livingEntity instanceof Player && (((Player)livingEntity).getGameMode()
                                    != GameMode.SURVIVAL && ((Player)livingEntity).getGameMode() != GameMode.ADVENTURE))){
                                        List<UUID>[] group = new ArrayList[1];
                                        if(target_groups.stream().noneMatch(s->(group[0] = s).stream().anyMatch(s2->locs.get(s2).distance(loc) <= Math.sqrt(3) * 5 * Math.sqrt(Main.DIFFICULTY)))){
                                            group[0] = new ArrayList<>();
                                            group[0].add(uid);
                                            target_groups.add(group[0]);
                                            target_group_locs.put(group[0], loc);
                                        }
                                        else{
                                            group[0].add(uid);
                                            SimpleLocation centre_loc = target_group_locs.get(group[0]).midPoint(loc);
                                            target_group_locs.put(group[0], centre_loc);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    List<UUID> group3 = new ArrayList<>();
                    int size = 0;
                    List<List<UUID>> free_groups = new ArrayList<>(groups);

                    for(Pair<List<UUID>, SimpleLocation> e : target_group_locs.entrySet()){
                        List<UUID> group = null, group2 = null;
                        SimpleLocation loc = e.second;
                        double distance = Integer.MAX_VALUE, distance3 = Integer.MAX_VALUE;

                        for(Pair<List<UUID> ,SimpleLocation>a : group_locs.entrySet()){
                            if(a.first.size() > size){
                                size = a.first.size();
                                group3 = a.first;
                            }
                            double distance2 = a.second.distance(loc);
                            if(nearbyTargets.get(a.first) == null
                            && distance2 < distance3){
                                group2 = a.first;
                                distance3 = distance2;
                            }
                            if(distance2 < distance){
                                distance = distance2;
                                group = a.first;
                            }
                        }
                        if(group2 == null)
                            nearbyTargets.get(group3).addAll(e.first);

                        else if(!group2.equals(group)) {
                            nearbyTargets.put(group2, e.first);
                            free_groups.remove(group2);
                        }
                          else {
                            nearbyTargets.put(group, e.first);
                            free_groups.remove(group);
                        }
                    }
                    if(free_groups.size() > 0) {
                        int n = 0;
                        for (Pair<List<UUID>, List<UUID>> e : nearbyTargets.entrySet()) {
                            if(n == free_groups.size())break;
                            List<UUID> copy = new ArrayList<>(e.first);
                            e.first.addAll(free_groups.get(n++));
                            nearbyTargets.changeKey(copy, e.first);
                        }
                    }
                    for(Pair<List<UUID>, List<UUID>> e : nearbyTargets.entrySet()){
                    }
                }
            }


        if(nearbyTargets.size() > 0){
            List<Pair<List<UUID>, List<UUID>>> set = new ArrayList<>(nearbyTargets.entrySet());
            for(int i = 0 ; i < set.size();){
                Pair<List<UUID>, List<UUID>> e = set.get(i);
                if(e.second.size() > 0)
                {
                    boolean isFree = true;
                    for(int j = 0; j < e.first.size();){
                        try {
                            UUID zUid = e.first.get(j);
                            Host h[] = new Host[1];
                            if(b.stream().anyMatch(( s->(h[0] = s).entityUUID.compareTo(zUid) == 0))){
                                if(h[0].getTarget() != null){
                                    isFree = false;
                                    break;
                                }
                            }
                            LivingEntity livingEntity = (LivingEntity)Objects.requireNonNull(Bukkit.getEntity(zUid));
                            if(livingEntity instanceof Zombie){
                                Zombie zombie = (Zombie)livingEntity;
                                if(zombie.getTarget() != null && !zombie.getTarget().isDead()){
                                    isFree = false;
                                    break;
                                }
                            }
                            ++j;
                        }catch(Exception ex){
                            List<UUID> copy = new ArrayList<>(e.first);
                            e.first.remove(i);
                            nearbyTargets.changeKey(copy,e.first);
                        }
                    }
                    if(isFree) {
                        UUID tUid = e.second.get(0);
                        LivingEntity target = null;
                        boolean fail = false;
                        while(target == null){
                            try {
                                target = (LivingEntity)Objects.requireNonNull(Bukkit.getEntity(tUid));
                            }catch(Exception ex){
                                if(e.second.size() == 0){
                                    nearbyTargets.remove(e.first);
                                    set.remove(e);
                                    fail = true;
                                    break;
                                }
                                e.second.remove(0);
                            }
                        }
                        if(fail)continue;
                        e.second.remove(0);
                        for(int j = 0; j < e.first.size();){
                            try {
                                UUID zUid = e.first.get(j);
                                Host h[] = new Host[1];
                                if(b.stream().anyMatch(( s->(h[0] = s).entityUUID.compareTo(zUid) == 0))){
                                    h[0].setTarget(target);
                                }
                                LivingEntity livingEntity = (LivingEntity)Objects.requireNonNull(Bukkit.getEntity(zUid));
                                if(livingEntity instanceof Zombie){
                                    Zombie zombie = (Zombie)livingEntity;
                                    zombie.setTarget(target);
                                }
                                ++j;
                            }catch(Exception ex){
                                List<UUID> copy = new ArrayList<>(e.first);
                                e.first.remove(i);
                                nearbyTargets.changeKey(copy,e.first);
                            }
                        }
                        nearbyTargets.put(e.first, e.second);
                    }
                    set.remove(e);
                }
                else{
                    set.remove(e);
                    nearbyTargets.remove(e.first);
                }

            }
        }
        }, 0 , 100);

        int schedule2 = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, ()-> {
            {
                for(int i = 0; i < a.size(); ++i){
                    UUID uid = a.get(i);
                    try {
                        Zombie zombie = (Zombie) Objects.requireNonNull(Bukkit.getEntity(uid));
                        SimpleLocation zomLoc = new SimpleLocation(zombie.getLocation());
                        if(zombie.getTarget() != null ){
                            LivingEntity target = zombie.getTarget();
                            SimpleLocation tarLoc = new SimpleLocation(target.getLocation());
                            if(zomLoc.distance(tarLoc) > 50 * Math.sqrt(22))continue;
                            List<Block> bs = zombie.getLineOfSight(null, 2);
                            for(Block b : bs) {
                                List<SimpleLocation>loc = new ArrayList<>();
                                loc.add(new SimpleLocation(b.getLocation()));
                                loc.add(loc.get(0).relative(0,-1,0));
                                loc.add(loc.get(0).relative(0,1,0));
                                SimpleLocation   ground = loc.get(0).relative(0, -1, 0),
                                        top = loc.get(0).relative(0, 2, 0);
                                for(SimpleLocation l : loc){
                                    b = l.toBlock();
                                    if (o.stream().noneMatch(s->s.region.belongs.contains(l))&&(!b.isPassable() && Lib.digspeed(b, zombie) <= CONST0x0000)
                                            || (Lib.zombie_defense_materials.contains(b.getType()) && Lib.digspeed(b, zombie) <= CONST0x0000))
                                        b.setType(Material.AIR);
                                    _abc_xyz.put(l,120);
                                        Lib.loadedBlocks.put(l,b);
                                }

                                b = ground.toBlock();
                                if(b.isPassable() || (Lib.zombie_defense_materials.contains(b.getType()) && Lib.digspeed(b, zombie) <=
                                        CONST0x0000))
                                    b.setType(ZOMBIE_BLOCK.get(randomMachine.nextInt(ZOMBIE_BLOCK.size())));
                                _abc_xyz.put(ground,120);
                                Lib.loadedBlocks.put(ground,b);

                                b = top.toBlock();
                                if(Lib.digspeed(b,zombie) <= CONST0x0000)
                                    b.setType(ZOMBIE_BLOCK.get(randomMachine.nextInt(ZOMBIE_BLOCK.size())));
                                _abc_xyz.put(top,120);
                                    Lib.loadedBlocks.put(top,b);
                            }

                        }
                    }catch(Exception e){
                        if(e instanceof NullPointerException){
                            a.remove(i--);
                        }
                    }
                }

            }

                final Set<Map.Entry<UUID, Long>> $a = new HashSet<>(d.entrySet());

                for(Map.Entry<UUID, Long> b : $a){
                    LivingEntity c = (LivingEntity)Bukkit.getEntity(b.getKey());
                    if(c == null){
                        d.remove(b.getKey());
                        continue;
                    }
                    else if(c.isDead()){
                        c.resetMaxHealth();
                        d.remove(b.getKey());
                        continue;
                    }
                    if(c instanceof Player){
                        Player p = (Player)c;
                        if(randomMachine.nextInt(1000) == 0
                        && p.getFoodLevel() > 18){
                            boolean hasHarmfulPotionEffect = false;
                            for(PotionEffect effect : p.getActivePotionEffects()){
                               if( effect.getType().getId() % 2 == 0){
                                   // https://mcreator.net/wiki/potion-effect-ids#google_vignette
                                   // you can see on the page, a similarity between most harmful effects is that their ids are odd numbers.
                                   hasHarmfulPotionEffect = true;
                                   break;
                               }
                            }

                                d.put(b.getKey(), Math.max(d.get(b.getKey()) - 14,0));

                        }
                    }
                    long lefttime = Math.max(0, b.getValue() - 1);
                    if(lefttime > 0)
                    c.setMaxHealth(((c.getHealth() + 0.1d)* lefttime / (double)CONST0x0001));
                    if(lefttime == 0){
                        c.setHealth(0d);
                        if(c instanceof Player)
                        c.resetMaxHealth();
                        d.remove(b.getKey());
                        continue;

                    }
                    if(c instanceof Player){
                        c.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER,20,1),true);
                    }
                    d.put(b.getKey(), lefttime);
                }

                double latestTPSUpdate = getServer().getTPS()[0];
                if(latestTPSUpdate <= Config.getInt("danger_tps")){
                    for(int i = 0; i < a.size();){
                        try {
                            Zombie zombie = (Zombie)Bukkit.getEntity(a.get(i));
                            if(zombie != null)
                            zombie.remove();
                            a.remove(i);
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                    for(int i = 0; i < b.size() ;){
                        if(b.get(i).activity != null )
                            b.get(i).stop();
                        LivingEntity livingEntity  = b.get(i).baseEntity;
                        if(livingEntity != null)
                            livingEntity.remove();
                        b.remove(i);

                    }

                }

                for(int i = 0 ; i < g.size(); ++i){
                    LivingEntity livingEntity =(LivingEntity) Bukkit.getEntity(g.get(i));
                    if(livingEntity != null && livingEntity.getWorld().isDayTime()){
                        boolean isInShadow = false;
                        int x = livingEntity.getLocation().getBlockX(),
                                z = livingEntity.getLocation().getBlockZ();
                        World world = livingEntity.getWorld();
                        for(int y = 255; y > livingEntity.getLocation().getBlockY() - 1 + Math.round(livingEntity.getHeight()); --y){
                            SimpleLocation loc = new SimpleLocation(world.getUID(),x,y,z);
                            Block b = loc.toBlock();
                            if(b.getType().isOccluding()){
                              isInShadow = true;
                                break;
                            }

                    }

                        if(!isInShadow){
                            livingEntity.setFireTicks(40);
                            livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION,200,1));
                        }
                }
                    else if(livingEntity == null){
                        g.remove(i--);
                    }
        }

                StringBuilder intermediate;
            {
                if (getServer().getOnlinePlayers().size() > 0)
                 {
                final List<SimpleLocation > copy = new ArrayList<>(c);

                for (int i = 0 ;i < copy.size(); ++ i) {
                    SimpleLocation loc = copy.get(i);

                    boolean inLoadingArea = false;
                    for (Player p : getServer().getOnlinePlayers()) {
                        SimpleLocation pLoc = new SimpleLocation(p.getLocation());
                        double distance = pLoc.distance(loc);
                        if (distance < 1428.29d) {
                            inLoadingArea = true;
                            break;
                        }
                    }
                    if (!inLoadingArea) continue;

                    livingMassCollection[] f = new livingMassCollection[1];
                    boolean g = o.stream().anyMatch(s -> (f[0] = s).region.centre.distance(loc) < 367.42
                            && s.health > 0d);
                    try {
                        if (!g && DIFFICULTY >= 5 && Objects.requireNonNull(Bukkit.getWorld(loc.world)).getFullTime()
                                > 4800000L && randomMachine.nextInt(10000) == 1) {
                            SimpleLocation loc1 = loc.relative(1, 0, 1),
                                    loc2 = loc.relative(-1, 3, -1);
                            f[0] = new livingMassCollection(new Box(loc1, loc2));
                            o.add(f[0]);
                            g = true;
                        }
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    }
                    Block $b = loc.toBlock();
                    if ($b.getType() != Material.PODZOL) {
                        c.remove(i--);
                        continue;
                    }
                    for(int j = 0; j < 8; ++j){
                        int xMod = 1, zMod = 0;
                        switch(j){
                            case 1:
                                xMod = -1;
                                break;
                            case 2:
                                xMod = 0;
                                zMod = -1;
                                break;
                            case 3:
                                xMod = 0;
                                zMod = 1;
                                break;
                            case 4:
                                zMod = 1;
                                break;
                            case 5:
                                xMod = -1;
                                zMod = 1;
                                break;
                            case 6:
                                xMod = -1;
                                zMod = -1;
                                break;
                            case 7:
                                xMod = 1;
                                zMod = -1;
                        }
                        for(int y = (int)Math.round( loc.y - 1); y <= loc.y + 1; ++y) {
                            SimpleLocation loc2 = loc.relative(xMod, y, zMod);
                            Block b2 = loc2.toBlock();
                            if(randomMachine.nextBoolean() && b2.isSolid()){
                                _xyz.put(loc2, Material.COARSE_DIRT);
                            }
                        }
                    }
                    long c = ____b.get(loc) - 1;
                    if (c == 0) {
                        ____b.put(loc, 120L);
                        try {
                            if (a.size() + b.size() <= zombieLimit && !loc.relative(0, 1, 0).toBlock().isSolid())
                                if (randomMachine.nextBoolean()) {
                                    int n = 0, o = randomMachine.nextInt(10) + 1;
                                    do {
                                        Zombie zombie = (Zombie) Objects.requireNonNull(Bukkit.getWorld(loc.world)).spawnEntity(loc.relative(0, 1, 0).toLocation(),
                                                EntityType.ZOMBIE);
                                        a.add(zombie.getUniqueId());
                                        n++;
                                    } while (g && b.size() + a.size() <= zombieLimit
                                            && n < o);
                                } else {
                                    int n = 0, o = randomMachine.nextInt(5) + 1;
                                    do {
                                        EntityType[] basic_types = {EntityType.COW, EntityType.CHICKEN, EntityType.SHEEP, EntityType.PIG};
                                        LivingEntity d = (LivingEntity) Objects.requireNonNull(Bukkit.getWorld(loc.world)).spawnEntity(loc.relative(0, 1, 0).toLocation(),
                                                basic_types[randomMachine.nextInt(4)]);
                                        Host h = new Host(d);
                                        b.add(h);
                                        n++;
                                    } while (g && b.size() + a.size() <= zombieLimit
                                            && n < o);
                                }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else {
                        ____b.put(loc, c - 1);
                    }
                }
            }
            }
            {
                for(livingMassCollection $b : o){
                    boolean loaded = false;
                    for(Player p : getServer().getOnlinePlayers()){
                        if(p.getWorld().getUID().compareTo($b.region.centre.world) == 0){
                            if(new SimpleLocation(p.getLocation()).distance($b.region.centre) < 1428.29d){
                                loaded = true;
                                break;
                            }
                        }
                    }
                    if(!loaded)continue;
                    Location loc = $b.region.centre.toLocation();
                    for(Entity e : loc.getNearbyEntities( $b.region.longSide  + $b.state * 8, $b.region.bottom.y * 2,
                             $b.region.longSide  + $b.state * 8)){
                        if(e.getType().isAlive()){
                            if(e instanceof Player){
                                Player p = (Player)e;
                                if(p.isOp() || p.getGameMode() == GameMode.CREATIVE ||
                                        p.getGameMode() == GameMode.SPECTATOR){
                                    continue;
                                }
                            }
                            LivingEntity livingEntity = (LivingEntity)e;
                            if(!livingEntity.isDead()){
                                UUID uid = livingEntity.getUniqueId();
                                final List<SimpleLocation> way = new ArrayList<>();
                                final int[] count2 = new int[1];
                                if(!a.contains(uid) && b.stream().noneMatch(s->s.entityUUID.compareTo(uid) == 0)){
                                     new BukkitRunnable() {
                                         public void run() {
                                             SimpleLocation eLoc = new SimpleLocation(livingEntity.getLocation());
                                             double distance = $b.region.centre.distance(eLoc);
                                             if (distance > Math.sqrt(2 * Math.pow($b.region.longSide + $b.state * 8, 2) + Math.pow($b.region.bottom.y, 2))) {
                                                 this.cancel();
                                             }
                                             if (distance <= Math.sqrt(3 * Math.pow(2 * $b.state, 2))) {
                                                 livingEntity.damage($b.state * 5 + DIFFICULTY * 15);
                                                 $b.health += 5 * $b.state;
                                                 if(livingEntity instanceof Player) {
                                                     livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 21, 1));
                                                     livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 21, 1));
                                                     livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 21, 1));
                                                     livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 21, 1));
                                                     livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 21, 1));
                                                 }

                                             } else {
                                                 if (livingEntity instanceof Player) {
                                                     try {
                                                         Player p = (Player) livingEntity;
                                                         String ip = Objects.requireNonNull(p.getAddress()).getAddress().getHostAddress();
                                                         if(!reverseMovingOnPlayers.contains(ip))
                                                             reverseMovingOnPlayers.add(ip);
                                                     } catch (Exception ex2) {
                                                         ex2.printStackTrace();
                                                         this.cancel();
                                                     }
                                                 }
                                                 try {
                                                     if (way.size() == 0 || count2[0] >= 7) {
                                                         if(way.size() > 0)way.clear();
                                                         way.addAll(Objects.requireNonNull(Host.drawPath(eLoc, $b.region.centre, livingEntity.getHeight(), null)).getValue());
                                                         ++count2[0];
                                                     } else if ( !eLoc.equals(way.get(way.size() - 1))){
                                                         way.addAll(Objects.requireNonNull(Host.drawPath(eLoc,way.get(way.size() - 1), livingEntity.getHeight(),null)).getValue());
                                                         ++count2[0];
                                                     }
                                                     for(int i = way.size() - 1; i > 0; --i){
                                                         try {
                                                             SimpleLocation loc = way.get(i);
                                                             World w = Objects.requireNonNull(Bukkit.getWorld(loc.world));
                                                             w.spawnParticle(Particle.CRIT,loc.toLocation(),1);
                                                             for(int j = 0; j < Math.round(livingEntity.getHeight()); ++j){
                                                                 SimpleLocation loc2 = loc.relative(0,j,0);
                                                                 Block b = loc2.toBlock();
                                                                 if(!b.isPassable()) {
                                                                     b.setType(Material.AIR);
                                                                     loc2.toLocation().createExplosion(1f,true,true);
                                                                 }
                                                             }
                                                             SimpleLocation ground = loc.relative(0,-1,0);
                                                             if(!ground.toBlock().isSolid()) {
                                                                 ground.toBlock().setType(Material.PODZOL);
                                                                 c.add(ground);
                                                             }
                                                         }catch(Exception ex4){
                                                             ex4.printStackTrace();
                                                             this.cancel();
                                                         }
                                                     }
                                                     livingEntity.teleport(way.get(way.size() - 1).toLocation());
                                                     way.remove(way.size() - 1);
                                                 }catch(Exception ex3){
                                                     ex3.printStackTrace();
                                                     this.cancel();
                                                 }
                                                         livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1));
                                                 try{
                                                     if(livingEntity instanceof Player) {
                                                         Player p = (Player) livingEntity;
                                                         String ip = Objects.requireNonNull(p.getAddress()).getAddress().getHostAddress();
                                                         if(reverseMovingOnPlayers.contains(ip))
                                                             reverseMovingOnPlayers.remove(ip);
                                                     }
                                                 }catch(Exception ex5){
                                                     ex5.printStackTrace();
                                                     this.cancel();
                                                 }
                                             }
                                         }
                                    }.runTaskTimer(this,0,10);
                                }
                            }
                        }
                    }
                }
            }
            {
                for(int i = 0; _abc.size() > 0;){
                    Host h = _abc.get(0);
                    if(h.getTarget() != null)
                        Host.damage(h.baseEntity, h.getTarget(), h.generic_attack_damage, h.generic_attack_knockback);

                    _abc.remove(0);
                }
            }
            {
                for(Pair<SimpleLocation,Integer> e : _abc_xyz.entrySet()){
                    int countdown = e.getValue() - 1;
                    if(countdown == 0){
                        if(ZOMBIE_BLOCK.contains(e.getKey().toBlock().getType())) {
                            e.getKey().toBlock().setType(Material.AIR);

                            _abc_xyz.remove(e.getKey());
                        }
                    }
                    else{
                        _abc_xyz.put(e.getKey(), countdown);
                    }
                }
            }
            if(getServer().getOnlinePlayers().size() > 0 && _xyz.size() > 0){
                int i = 0;
                Pair<SimpleLocation, Material> p;
                boolean inLoadedArea = false;
                do {

                    p = _xyz.getPairByIndex(i++);
                    for (Player plr : getServer().getOnlinePlayers()) {
                        SimpleLocation pLoc = new SimpleLocation(plr.getLocation());
                        assert p != null;
                        if (pLoc.distance(p.first) < 1428.29d) {
                            inLoadedArea = true;
                            break;
                        }
                    }
                }while(inLoadedArea && i < _xyz.size());
                if(inLoadedArea) {
                    assert p != null;
                    _xyz.remove(p.first);
                    Block b = p.first.toBlock();
                    b.setType(p.second);
                }
            }
            {
                while(_def.size() > 0){
                    Chunk c = _def.get(0);
                    if(c != null) {
                        c.unload();
                        c.setForceLoaded(false);
                    }
                    _def.remove(0);
                }
            }
        }, 0, 20);


        int schedule3 = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () ->{
            for(Player p : getServer().getOnlinePlayers()){
                for(int i = 0; i < c.size(); ++i){
                    try {
                        SimpleLocation grave = c.get(i);
                        if (grave.x >= p.getLocation().getBlockX() - CONST0x0003 / 2f &&
                                grave.x <= p.getLocation().getBlockX() + CONST0x0003 / 2f
                                && grave.z >= p.getLocation().getBlockZ() - CONST0x0003 / 2f
                                && grave.z <= p.getLocation().getBlockZ() + CONST0x0003 / 2f
                                && grave.y >= p.getLocation().getBlockY() - 25
                                && grave.y <= p.getLocation().getBlockY() + 25) {
                            if (randomMachine.nextBoolean() &&
                            a.size() + b.size() <= zombieLimit) {
                                a.add(Objects.requireNonNull(Bukkit.getWorld(grave.world)).spawnEntity(grave.relative(0, 1, 0)
                                        .toLocation(), EntityType.ZOMBIE).getUniqueId());
                            }
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
            for(int i = 0; i < h.size();) {
                try {


                    SimpleLocation loc = c.get(randomMachine.nextInt(c.size()));
                    LivingEntity livingEntity = (LivingEntity) Objects.requireNonNull(Bukkit.getWorld(loc.world)).spawnEntity(loc.relative(0, 1, 0).toLocation(),
                            h.get(i));
                    if (livingEntity instanceof Zombie) {
                        a.add(livingEntity.getUniqueId());
                    } else {
                        Host h = new Host(livingEntity);
                        b.add(h);
                    }

                    h.remove(i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Lib.loadedBlocks.clear();
        }, 0, 3000);
        int schedule4 = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, ()->{
                DIFFICULTY = randomMachine.nextInt(20) + 1;
                zombieLimit = DIFFICULTY * 500;
                if(DIFFICULTY > 2){
                    for(int i = 0; i < a.size(); ++i){
                        try {
                            Zombie zombie = (Zombie)Bukkit.getEntity(a.get(i));
                            if(zombie != null)
                            zombie.setShouldBurnInDay(false);
                            else
                                a.remove(i--);
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                    for(int i = 0; i < g.size();++i){
                        LivingEntity livingEntity = (LivingEntity)Bukkit.getEntity(g.get(i));
                        if(livingEntity == null){
                            g.remove(i--);
                            continue;
                        }
                        boolean isAHost = b.stream().anyMatch(s->s.entityUUID.compareTo(livingEntity.getUniqueId()) == 0);

                        if(isAHost){
                            g.remove(i--);
                        }

                    }
                }
                else{
                    for(int i = 0; i < a.size(); ++i){
                        try{
                        Zombie zombie = (Zombie)Bukkit.getEntity(a.get(i));
                        if(zombie != null)
                        zombie.setShouldBurnInDay(true);
                        else
                            a.remove(i--);
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                    for (int i = 0; i < b.size() ; ++i) {
                        Host h = b.get(i);
                        if(h.baseEntity == null)
                        {
                            b.remove(i--);
                            continue;
                        }
                        g.add(h.entityUUID);
                    }
                }
                    StringBuilder intermediate = new StringBuilder(),
                            intermediate2 = new StringBuilder();
                    for(World w : getServer().getWorlds()){
                        w.setMonsterSpawnLimit((int)Math.round(w.getMonsterSpawnLimit() + Math.pow(DIFFICULTY,2) * w.getTime()));
                        intermediate.append(w.getName()).append(":").append(w.getMonsterSpawnLimit()).append("\n");
                        w.setTicksPerMonsterSpawns((int)Math.round(200 / Math.pow(DIFFICULTY,2)));
                        intermediate2.append(w.getName()).append(":").append("Animal spawn tick:").append(w.getTicksPerAnimalSpawns()).append("\n");
                        w.setTicksPerAnimalSpawns((int)(400 * DIFFICULTY * (w.getTime() * 2/3)));
                        intermediate2.append(w.getName()).append(":").append("Monster spawn tick:").append(w.getTicksPerMonsterSpawns()).append("\n");
                    }
                    statistics.put("monster spawn limit", intermediate.toString());
                    statistics.put("mob spawn tick", intermediate2.toString());

                },
                18000, 12000);
        final int[] p1 = {600};
        int schedule5 = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for(int i = 0; i < a.size();++i){
                try {
                    LivingEntity livingEntity = (LivingEntity)Bukkit.getEntity(a.get(i));
                    if(livingEntity != null)
                    {
                        if (Objects.requireNonNull(livingEntity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).getBaseValue() < 1.5)
                            Objects.requireNonNull(livingEntity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(1.5d);
                        if (Objects.requireNonNull(livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue() < 20 * DIFFICULTY) {
                            Objects.requireNonNull(livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(20d * DIFFICULTY);
                            livingEntity.setHealth(20d * DIFFICULTY);
                        }
                    }
                    else
                        a.remove(i--);
                }catch(Exception e){
                        e.printStackTrace();
                }
            }
            for(int i = 0; i < b.size();){
                try {
                    LivingEntity livingEntity = b.get(i).baseEntity;
                    if(livingEntity == null){
                        b.remove(i--);
                        continue;
                    }
                    if(randomMachine.nextInt(100) <= 29){
                        livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,p1[0]/3,1));
                    }
                    ++i;
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }, 0, p1[0]);
        int schedule6 = Bukkit.getScheduler().scheduleAsyncRepeatingTask(this,()->{
            {
                StringBuilder intermediate = new StringBuilder();
                intermediate.append("Total number of chunks:").append(SimpleLocation.recentlyLoadedChunks.size()).append("\n");
                for(Pair<SimpleLocation, Double> c : SimpleLocation.recentlyLoadedChunks.entrySet()){
                    try {
                        intermediate.append(c.first.toString()).append(" -> ").append(c.second).append("\n");
                    }catch(Exception e){
                        getLogger().info(" " + c.first + " " + c.second);
                    }
                }
                statistics.put("Chunk unload wait:",intermediate.toString());
            }
            {
              StringBuilder intermediate = new StringBuilder();
                for(Map.Entry<String,UUID> e : ipToPlayer.entrySet()){
                    try {
                        intermediate.append(e.getKey()).append(" -> ").append(Objects.requireNonNull(Bukkit.getPlayer(e.getValue())).getName())
                                .append("\n");
                    }catch(Exception ex2){
                        ex2.printStackTrace();
                    }
                }
                statistics.put("Ip to players:", intermediate.toString());
            }
            {
                statistics.put("Vanilla zombie quantity", String.valueOf(a.size()));
                statistics.put("Plugin modified zombie quantity", String.valueOf(b.size()));
                StringBuilder intermediate = new StringBuilder();
                for (Map.Entry<UUID, Long> e : d.entrySet()) {
                    LivingEntity livingEntity = (LivingEntity) Bukkit.getEntity(e.getKey());
                    if (livingEntity == null) {
                        d.remove(e.getKey());
                        continue;
                    }
                    intermediate.append(livingEntity.getName()).append(":").append(livingEntity.getUniqueId()).append(" -> ").append(e.getValue())
                            .append("\n");
                }
                statistics.put("Victims and their left time of being conscious", intermediate.toString());
                intermediate = new StringBuilder();
                for (int i = 0; i < c.size(); ++i) {
                    SimpleLocation loc = c.get(i);
                    intermediate.append(loc.x).append(", ").append(loc.y).append(", ").append(loc.z).append("\n");
                }
                statistics.put("Graves", intermediate.toString());
                statistics.put("Difficulty", Integer.toString(DIFFICULTY));

                {
                    List<Component> pages = new ArrayList<>();
                    for (Map.Entry<String, String> e : statistics.entrySet()) {
                        List<String> dataLines = new ArrayList<>(Arrays.asList(e.getValue().split("\n")));
                        if (dataLines.size() == 1)
                            pages.add(Component.text(e.getKey()).append(Component.text(" : ")).append(Component.text(e.getValue())));
                        else {
                            intermediate = new StringBuilder();
                            intermediate.append(e.getKey()).append(":");
                            for (int i = 0; i < dataLines.size() / 5 + 1; ++i) {
                                for (int j = 0; j < 5 && j < dataLines.size(); ++j) {
                                    intermediate.append(dataLines.get(0)).append("\n");
                                    dataLines.remove(0);
                                }
                                pages.add(Component.text(intermediate.toString()));
                                intermediate = new StringBuilder();
                            }


                        }
                    }
                    if (terminal == null)
                        terminal = Book.book(Component.text("terminal"), Component.text("Ly Binh Minh"),
                                pages);
                    else
                        terminal = terminal.pages(pages);
                }

            }
            for(Pair<SimpleLocation, Double> c : SimpleLocation.recentlyLoadedChunks.entrySet()){
                double loadPeriod = Math.max(0,c.second - 1d);
                if(loadPeriod == 0){
                    try{
                        Chunk chunk = Objects.requireNonNull(Bukkit.getWorld(c.first.world)).getChunkAt((int)Math.round(c.first.x),
                                (int)Math.round(c.first.z));
                        _def.add(chunk);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    SimpleLocation.recentlyLoadedChunks.remove(c.first);
                }
                else
                SimpleLocation.recentlyLoadedChunks.put(c.first, loadPeriod);
            }
        }, 0, 20);
        this.getServer().getPluginManager().registerEvents(this,this);

        int schedule7 = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, ()->{
            if(__def.size() > 0){
                while( __def.size() > 0){
                    Chunk c = __def.get(0);
                    if(c != null) {
                        c.load(true);
                        c.setForceLoaded(true);
                    }
                    __def.remove(0);
                }
            }

        },0,1);

        getLogger().info("A1");

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onDisable() {
        // Plugin shutdown logic
        //type:Location(x:y:z):others
        try {
            int n = 1;
            File folder = new File("./plugins/ZombieApocalypse");
            if(!folder.exists()){
                folder.mkdir();
            }
            File f = new File("./plugins/ZombieApocalypse/data" + n++  + ".save");
            f.createNewFile();
            PrintWriter pw = new PrintWriter(f);
            String type;
            for (int i = 0, j = 0; i < a.size() + b.size(); ++i) {
                try{
                if (i / 500 > 0 && i % 500 == 0) {
                    pw.flush();
                    pw.close();
                    f = new File("./plugins/ZombieApocalypse/data" + n++  + ".save");
                    f.createNewFile();
                    pw = new PrintWriter(f);
                }
                LivingEntity e;

                if (i < a.size()) {
                    e = (Zombie)Bukkit.getEntity(a.get(i));
                    if(e == null)continue;
                    type = "ZOMBIE";
                } else {
                    e = b.get(j).baseEntity;
                    if(e == null)continue;
                    if (b.get(j).activity != null)
                        b.get(j).stop();
                    type = "HOST";
                    ++j;
                }
                SimpleLocation loc = new SimpleLocation(e.getLocation());
                pw.append(Integer.toString(i% 500)).append(".").append(type).append(":").append(Double.toString(loc.x)).append(":")
                        .append(Double.toString(loc.y)).append(":").append(Double.toString(loc.z)).append(":").
                        append(loc.world.toString()).append(":").append(e.getType().toString()).append("\n");
            }catch(Exception e){
                e.printStackTrace();
            }
            }

            pw.checkError();
            {
                f = new File("./plugins/" + this.getName()+"/data" + n++ + ".save");
                f.createNewFile();
                pw = new PrintWriter(f);
                type = "GRAVE";
                int a = 0;
                for (SimpleLocation loc : c) {
                    if(a == 200){
                        a = 0;
                        pw.flush();
                        pw.close();
                        f = new File("./plugins/ZombieApocalypse/data" + n++  + ".save");
                        f.createNewFile();
                        pw = new PrintWriter(f);
                    }
                    ++a;
                    pw.append(Integer.toString(a)).append(".").append(type).append(":").append(Double.toString(Math.round(loc.x))).append(":").append(Double.toString(Math.round(loc.y))).append(":").append(Double.toString(Math.round(loc.z))).append(":")
                            .append(loc.world.toString()).append("\n");
                }
                pw.checkError();

            }
            {
                f = new File("./plugins/ZombieApocalypse/data" + n++  + ".save");
                f.createNewFile();
                pw = new PrintWriter(f);
                type = "ENTITY_CORE";
                int a = 0;
                for (livingMassCollection b : o) {
                    if(b.health == 0)continue;
                    if(a == 200){
                        a = 0;
                        pw.flush();
                        pw.close();
                        f = new File("./plugins/ZombieApocalypse/data" + n++  + ".save");
                        f.createNewFile();
                        pw = new PrintWriter(f);
                    }
                    SimpleLocation bottom = b.region.bottom, top = b.region.top;
                    String world = bottom.world.toString(),
                     health = Double.toString(b.health), state = Integer.toString(b.state);
                    ++a;
                    pw.append(Integer.toString(a)).append(".").append(type).append(":").append(world).append(":").
                            append(Double.toString(Math.round(bottom.x))).append(":").append(Double.toString(Math.round(bottom.y))).append(":").
                            append(Double.toString(Math.round(bottom.z))).append(":").append(Double.toString(top.x)).append(":").append(
                                    Double.toString(top.y)).append(":").append(Double.toString(top.z)).append(":").append(state).append(":").
                            append(health).append("\n");
                }
                pw.checkError();

            }
            {
                f = new File("./plugins/ZombieApocalypse/data" + n++  + ".save");
                f.createNewFile();
                pw = new PrintWriter(f);
                type = "ZOMBIE_BLOCK";
                int a = 0;
                for (Pair<SimpleLocation, Integer> e : _abc_xyz.entrySet()) {
                    if(a == 200){
                        a = 0;
                        pw.flush();
                        pw.close();
                        f = new File("./plugins/ZombieApocalypse/data" + n++  + ".save");
                        f.createNewFile();
                        pw = new PrintWriter(f);
                    }
                    ++a;
                    SimpleLocation loc = e.first;
                    pw.append(Integer.toString(a)).append(".").append(type).append(":").append(loc.world.toString()).append(":").
                            append(Double.toString(loc.x)).append(":").append(Double.toString(loc.y)).append(":").
                            append(Double.toString(loc.z)).append(":").append(Integer.toString(e.second)).append("\n");
                }
                pw.checkError();

            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    @EventHandler
    public void playerMove(PlayerMoveEvent event){
        Player p = event.getPlayer();
        SimpleLocation destination = new SimpleLocation(event.getTo()),
                ground = destination.relative(0,-1,0);
        UUID uid = p.getUniqueId();

        if(c.contains(ground)){
            if(d.get(uid) != null){
                d.put(uid, (long)( randomMachine.nextInt(CONST0x0001) + CONST0x0002) % CONST0x0001);
            }
            else{
                d.put(uid, Math.max(d.get(uid) - 1,
                        0));
            }
        }
        try {
            String ip = Objects.requireNonNull(p.getAddress()).getAddress().getHostAddress();
            if (reverseMovingOnPlayers.contains(ip)) {
                org.bukkit.util.Vector v = event.getTo().toVector().subtract(event.getFrom().toVector()).normalize();
                event.setTo(event.getFrom().toVector().subtract(v).toLocation(event.getFrom().getWorld()));
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    @EventHandler
    public void entityDie(EntityDeathEvent event){
        LivingEntity entity = event.getEntity();
        boolean isZombie = false;
        if(entity instanceof Zombie){
            Zombie zombie = (Zombie)entity;
            if(a.contains(zombie.getUniqueId())){
                a.remove(zombie.getUniqueId());
                isZombie = true;
            }
        }
        else
        {
            for(int i = 0; i < b.size(); ++i){
                Host host = b.get(i);
                if(host.baseEntity == null){
                    b.remove(i--);
                    continue;
                }
                if(host.entityUUID.equals(entity.getUniqueId())){
                    isZombie = true;
                    b.remove(i);
                    break;
                }
            }
        }
        if(isZombie){

            h.add(entity.getType());

            if(Lib.digspeed(entity.getLocation().add(0,-1,0).getBlock(), entity )
                    <= CONST0x0000){
                SimpleLocation loc = new SimpleLocation(entity.getLocation().add(0,-1,0));
                Block b;
                (b = loc.toBlock()).setType(Material.PODZOL);

                Lib.loadedBlocks.put(loc,b);
                c.add(loc);
                ____b.put(loc,120L);
            }

            Player killer = entity.getKiller();
            if(killer != null){
                if(killer.getHealth() < 19d){
                    if(d.get(killer.getUniqueId()) == null){
                        d.put(killer.getUniqueId(),(long)( randomMachine.nextInt(CONST0x0001) + CONST0x0002) % CONST0x0001);
                    }
                    else if(d.get(killer.getUniqueId()) == 0){
                        d.put(killer.getUniqueId(),(long)( randomMachine.nextInt(CONST0x0001) + CONST0x0002) % CONST0x0001);
                    }
                    else {
                        d.put(killer.getUniqueId(), Math.max(d.get(killer.getUniqueId()) - 10, 0));
                    }
                }
            }
        }
        else if(entity instanceof Animals){

            Bukkit.getScheduler().scheduleSyncDelayedTask(this,()-> {
                Host h = new Host((LivingEntity) entity.getWorld().spawnEntity(entity.getLocation(), entity.getType()));
                b.add(h);
            }, 800);
        }

    }
    @EventHandler
    public void entityGetsDamage(EntityDamageByEntityEvent event){
        Entity casualty = event.getEntity(), attacker = event.getDamager();
        if(casualty.getType().isAlive() &&attacker.getType().isAlive()
               && !casualty.isDead()){
            if( !(casualty instanceof Monster)){
                boolean isAttackerZombie = attacker instanceof Zombie;
                if(!isAttackerZombie)
                for(int i = 0; i < b.size(); ++i){
                    Host host = b.get(i);
                    if(host.baseEntity == null){
                        b.remove(i--);
                        continue;
                    }
                    if(host.entityUUID.compareTo(attacker.getUniqueId()) == 0){
                        isAttackerZombie = true;
                        break;
                    }
                }

                if(isAttackerZombie){
                    if(d.get(casualty.getUniqueId()) == null){
                        d.put(casualty.getUniqueId(), (long)((randomMachine.nextInt(CONST0x0001) + CONST0x0002)
                                % CONST0x0001));
                    }
                    else if(d.get(casualty.getUniqueId()) == 0){
                        d.put(casualty.getUniqueId(), (long)((randomMachine.nextInt(CONST0x0001) + CONST0x0002)
                                % CONST0x0001));
                    }
                    else{
                        d.put(casualty.getUniqueId(), Math.max(0, d.get(casualty.getUniqueId()) - 10));
                    }
                }
            }
        }
    }


   @EventHandler
    public void blockBreak(BlockBreakEvent event){
        Block b = event.getBlock();
        Player p = event.getPlayer();
        if(b.getType() == Material.PODZOL){
            c.remove(new SimpleLocation(event.getBlock().getLocation()));
            if(d.get(p.getUniqueId()) == null){
                d.put(p.getUniqueId(),100L);
            }
            else{
                d.put(p.getUniqueId(), d.get(p.getUniqueId()) - 15L);
            }
        }
        SimpleLocation loc = new SimpleLocation(b.getLocation());
        if(Lib.loadedBlocks.containsKey(loc)){
            Lib.loadedBlocks.remove(loc);
        }

        if(_abc_xyz.containsKey(loc)){
            _abc_xyz.remove(loc);
        }
        livingMassCollection[] a = new livingMassCollection[1];
        if(o.stream().anyMatch(s->(a[0] = s).region.belongs.contains(loc))){
            a[0].health = Math.max(a[0].health - 100d, 0);
            if(a[0].health == 0){
                o.remove(a[0]);
            }
        }
    }
    @EventHandler
    public void blockInteract(BlockPlaceEvent event){
        Block b = event.getBlock();
        SimpleLocation loc = new SimpleLocation(b.getLocation());

        if(Lib.loadedBlocks.containsKey(loc)){
            Lib.loadedBlocks.remove(loc);
        }

    }
    @SuppressWarnings("deprecation")
    @EventHandler
    public void foodConsume(PlayerItemConsumeEvent event){
        ItemStack item = event.getItem();
        try{
        if(item.displayName().equals(e)
        && item.lore() != null && item.lore().equals(f)){
            if(d.containsKey(event.getPlayer().getUniqueId())){
                if(randomMachine.nextBoolean()){
                    d.remove(event.getPlayer().getUniqueId());
                    event.getPlayer().resetMaxHealth();
                }
            }
        }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    @SuppressWarnings("deprecation")
    @EventHandler
    public void feed(PlayerInteractAtEntityEvent event){
        try {
            Player p = event.getPlayer();
            Entity entity = event.getRightClicked();
            ItemStack itemInHand = p.getInventory().getItem(event.getHand());
            if (itemInHand != null) {
                if (itemInHand.displayName().equals(e) &&
               itemInHand.lore() != null && itemInHand.lore().equals(f))
                    if (entity.getType().isAlive()) {
                        LivingEntity livingEntity = (LivingEntity) entity;
                        if (d.containsKey(livingEntity.getUniqueId())) {
                                itemInHand.setAmount(itemInHand.getAmount() - 1);
                                d.remove(livingEntity.getUniqueId());
                                livingEntity.resetMaxHealth();
                        }
                    }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    @EventHandler
    public void entitySpawn(EntitySpawnEvent event){
        Entity entity = event.getEntity();
        if(entity.getType().isAlive()){
            LivingEntity livingEntity = (LivingEntity)entity;
            if(livingEntity instanceof Zombie){
                a.add(livingEntity.getUniqueId());
            }
        }
    }
    @SuppressWarnings("deprecation")
    @EventHandler
    public void entityDamgeEntity(EntityDamageByEntityEvent event){
        Entity damager = event.getDamager(),
                victim = event.getEntity();
        if(damager.getType().isAlive() &&
        victim.getType().isAlive()){
            LivingEntity Damager = (LivingEntity)damager,
                    Victim = (LivingEntity)victim;
            if(Victim.isDead()) {
                if(l.containsKey(Victim.getUniqueId()))
                    l.remove(Victim.getUniqueId());
                return;
            }

            if(Damager instanceof Zombie || b.stream().anyMatch(s->s.entityUUID.compareTo(damager.getUniqueId()) == 0)){
                if(randomMachine.nextInt(100) <= 48)
                Victim.damage(event.getDamage() * ((float)(randomMachine.nextInt(50) + DIFFICULTY ^ 2) / 100f),
                        Damager);
                if(DIFFICULTY >= 2 && randomMachine.nextBoolean()) {
                    try{
                    Victim.addPotionEffect(new PotionEffect( Objects.requireNonNull(PotionEffectType.getById(randomMachine.nextInt(27) + 1)),
                            20 * ((randomMachine.nextInt(20) + DIFFICULTY)^ 2),DIFFICULTY ^ 2 % 100));
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
            if(l.get(Victim.getUniqueId()) == null ||
                    l.get(Victim.getUniqueId()).getTarget() == null)
            if((Victim instanceof Animals && !(Victim instanceof Wolf || Victim instanceof Hoglin || Victim instanceof Bee)
            ) || (Victim instanceof Fish && !(Victim instanceof PufferFish))){
                if(l.get(Victim.getUniqueId()) == null) {
                    Host h = new Host(Victim);
                    h.setTarget(Damager);
                    l.put(Victim.getUniqueId(), h);
                }
                else{
                    l.get(Victim.getUniqueId()).setTarget(Damager);
                }
            }
        }
    }
    @EventHandler
    public void playerJoin(AsyncPlayerPreLoginEvent event){
        InetAddress inet_addr =  event.getAddress();
       ipToPlayer.put( inet_addr.getHostAddress(),event.getPlayerProfile().getId());
    }
    @EventHandler
    public void playerLeave(PlayerQuitEvent event){
        Player p = event.getPlayer();
        try{
            ipToPlayer.remove(Objects.requireNonNull(p.getAddress()).getAddress().getHostAddress());
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, String label, String[] args){
        if(label.equalsIgnoreCase("maps")){
            if(sender.hasPermission("za.infoshow") || sender.isOp()){
                if(sender instanceof Player){
                    Player p = (Player)sender;
                    p.openBook(terminal);
                }
                else{
                    sender.sendMessage("Sorry, but this command is only executable by a player.\n");
                }

            }
        }
        if(label.equals("debug")){
            if(sender.hasPermission("za.plugin")){
              debugMode = !debugMode;
              getLogger().info("Debug mode is now :" + Boolean.toString(debugMode).toUpperCase());
            }
        }
        if(label.equalsIgnoreCase("infect")){
            if(sender.hasPermission("za.game")){
                if(args.length == 0 && sender instanceof Player)
                {
                    Player p = (Player)sender;
                    Entity e = p.getTargetEntity(10, true);
                    if(e != null)
                    if(e.getType().isAlive() && !e.isDead()){
                        if(e instanceof Zombie){
                            p.sendMessage("Zombies cannot be infected again!");
                        }
                        else {
                            Host h = new Host((LivingEntity)e.getWorld().spawnEntity(e.getLocation(),e.getType()));
                            b.add(h);
                            e.remove();
                            p.sendMessage("Infected the target mob");
                        }
                    }
                    else{
                        p.sendMessage("The target entity is not insentient!");
                    }
                    else{
                        p.sendMessage("There must be an entity in front of you!");
                    }
                }
                else if(args.length >= 2 && args[0].equalsIgnoreCase("entity_core")){
                   int state = 0;
                        try{
                            state = Integer.parseInt(args[1]);
                        }catch(Exception e){
                            sender.sendMessage("Wrong format for the second argument, set the state to the default value of 0");
                        }

                    SimpleLocation loc = null;
                        if(args.length == 6){
                            World w = Bukkit.getWorld(args[2]);
                            if(w == null){
                                sender.sendMessage("Unknown world. Valid options: ");
                                getServer().getWorlds().forEach(s->sender.sendMessage(s.getName()));
                            }
                            else{
                                try{
                                    double x = Double.parseDouble(args[3]),
                                            y = Double.parseDouble(args[4]),
                                            z = Double.parseDouble(args[5]);
                                    w.loadChunk((int)(x / 16), (int)(z / 16));
                                    loc = new SimpleLocation(w.getUID(), x,y,z);
                                }catch(Exception e){
                                    sender.sendMessage("Wrong format for arguments 4, 5, 6, use a random location!");
                                }
                            }
                        }
                        if(loc == null){
                            World w =getServer().getWorlds().get(randomMachine.nextInt(getServer().getWorlds().size()));
                            double x = randomMachine.nextDouble() % getServer().getMaxWorldSize(), z
                                    = randomMachine.nextDouble() % getServer().getMaxWorldSize(),
                                    y;
                            w.loadChunk((int)Math.round(x),(int)Math.round(z));
                            for(y = 255; y > 0; --y){
                                SimpleLocation loc2 = new SimpleLocation(w.getUID(),x,y,z);
                                Block b = loc2.toBlock();
                                if(b.isSolid()){
                                    y--;
                                    break;
                                }
                            }
                            loc = new SimpleLocation(w.getUID(),x,y,z);

                        }
                    livingMassCollection entity_core = new livingMassCollection(new Box(loc.relative(1,0,1),
                            loc.relative(-1,0,-1)));
                    while(entity_core.state  <= state){
                        entity_core.grow();
                    }
                    o.add(entity_core);
                    entity_core.region.centre.toLocation().getWorld().unloadChunk((int)(loc.x / 16),
                            (int)(loc.z / 16));

                    sender.sendMessage("summon a entity_core at " + entity_core.region.centre.toString() + " at state " + args[2]);
                }
                else if(args.length < 4){
                    sender.sendMessage("Too few arguments: at least 4 is needed! <world_name> <x> <y> <z>");
                }
                else{
                    if(args.length == 4){
                        World w = Bukkit.getWorld(args[0]);
                        if(w == null){
                            sender.sendMessage("Unknown world. Valid options: ");
                            getServer().getWorlds().forEach(s->sender.sendMessage(s.getName()));
                        }
                        else {
                            Zombie zom = (Zombie)w.spawnEntity(new Location(w, Double.parseDouble(args[1]),
                                    Double.parseDouble(args[2]),
                                    Double.parseDouble(args[3])), EntityType.ZOMBIE);
                            a.add(zom.getUniqueId());
                            sender.sendMessage("Summoned a new zombie at " + zom.getLocation().toString());
                        }
                    }
                    if(args.length == 5){
                        World w = Bukkit.getWorld(args[0]);
                        if(w == null){
                            sender.sendMessage("Unknown world. Valid options: ");
                            getServer().getWorlds().forEach(s->sender.sendMessage(s.getName()));
                        }
                        else {
                            EntityType t = null;
                            try {
                               t = EntityType.valueOf(args[4]);
                                Host h = new Host((LivingEntity)w.spawnEntity(new Location(w, Double.parseDouble(args[1]),
                                        Double.parseDouble(args[2]),
                                        Double.parseDouble(args[3])), t));
                                b.add(h);
                                sender.sendMessage("Summoned a new infected entity at " + h.baseEntity.getLocation().toString());
                            }
                            catch(Exception e){
                                sender.sendMessage("Unknown entity type for api version " + PLUGIN.getDescription().getAPIVersion() + ". Valid options:");
                                Arrays.stream(EntityType.values()).forEach(s->sender.sendMessage(s.name()));
                            }

                        }
                    }
                }
            }
            else{
                sender.sendMessage("you don\'t have the required permission: za.game!");
            }
        }
         return super.onCommand(sender, cmd, label, args);
    }
}
