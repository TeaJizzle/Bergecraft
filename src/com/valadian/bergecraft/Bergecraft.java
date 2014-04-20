package com.valadian.bergecraft;

import java.io.File;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.valadian.bergecraft.annotations.*;
import com.valadian.bergecraft.bergeypvp.WeaponTimer;
import com.valadian.bergecraft.interfaces.ApiManager;

public class Bergecraft extends JavaPlugin implements Listener  {
	public static void severe(String message) {
	    log_.severe("[Bergecraft] " + message);
	}
	
	public static void warning(String message) {
	    log_.warning("[Bergecraft] " + message);
	}
	public static void info(String message) {
	    log_.info("[Bergecraft] " + message);
	}
	
	public static void debug(String message) {
	    if (config_.DebugLog) {
	      log_.info("[Bergecraft] " + message);
	    }
	}

    public static Config config_ = null;
		  
    private File configFile;
    private static final Logger log_ = Logger.getLogger("Bergecraft");
    private static Bergecraft global_instance_ = null;
    private static String mainDirectory = "plugins/Bergecraft";
    
    public ApiManager apis;
    
    public Bergecraft() {
    }
    

    HashMap<Player,WeaponTimer> cooldowns = new HashMap<Player,WeaponTimer>();

    @Bergifications ({
	    @Bergification(opt="bergey_pvp_weapons", def="true"),
	    @Bergification(opt="bergey_pvp_weapon_cooldown", def="3000",type=OptType.Int),
    	@Bergification(opt="nerf_sharpness", def="true"),
    	@Bergification(opt="sharpness_damage_per_level", type=OptType.Double, def="0.66"),
    	@Bergification(opt="nerf_strength", def="true"),
    	@Bergification(opt="strength_multiplier", type=OptType.Double, def="1.5")
    })
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
    	if(event.getDamager() instanceof Player)
    	{
        	Player attacker = (Player) event.getDamager();
        	if(apis.isBergecraftDisabledFor(attacker)) return;
        	ItemStack stack = attacker.getItemInHand();
        	stack.getDurability();
        	long now = System.currentTimeMillis();
    		if(config_.get("bergey_pvp_weapons").getBool())
    		{
        		int cooldown = config_.get("bergey_pvp_weapon_cooldown").getInt();
    	    	if(cooldowns.containsKey(attacker) && !cooldowns.get(attacker).cancelled)
    	    	{
    				event.setCancelled(true);
    				//cooldowns.get(attacker).resetTimer();
    				//attacker.sendMessage("[Bergey Pvp] Attacking too fast");
    				return;
        		}
    	    	else
    	    	{
        	    	debug("Scheduling Cooldown!");
        			WeaponTimer timer = new WeaponTimer(attacker, stack, now, cooldown);
        			timer.runTaskTimer(this, 0, 20/5);
        			cooldowns.put(attacker, timer);
        			//Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new WeaponTimer(attacker, stack, now, cooldown), 0, 20/5);
    	    	}
        	}

            if (config_.get("nerf_sharpness").getBool()) {
                if (!(event.getDamager() instanceof Player)) {
                    return;
                  }
                  Player player = (Player)event.getDamager();
                  ItemStack item = player.getItemInHand();
                  //Apply Strength Nerf
                  final double strengthMultiplier = config_.get("strength_multiplier").getDouble();
                  if (player.hasPotionEffect(PotionEffectType.INCREASE_DAMAGE)) {
                    for (PotionEffect effect : player.getActivePotionEffects()) {
                      if (effect.getType().equals(PotionEffectType.INCREASE_DAMAGE)) {
                        final int potionLevel = effect.getAmplifier() + 1;
                        final double unbuffedDamage = event.getDamage() / (1.3 * potionLevel + 1);
                        final double newDamage = unbuffedDamage + (potionLevel * strengthMultiplier);
                        event.setDamage(newDamage);
                        break;
                      }
                    }
                  }
                  //Apply Sharp Nerf
                  int sharpness = item.getEnchantmentLevel(Enchantment.DAMAGE_ALL);
                  final double sharpnessOffset = config_.get("sharpness_damage_per_level").getDouble();
                  if(sharpness>0){
	                  //final double unbuffedDamage = event.getDamage() / potionScale;
	                  final double newDamage = event.getDamage() - 1.25 * sharpness + sharpnessOffset * sharpness;
	                  //final double newDamage = fixedUnbuffedDamage * potionScale;
	          		  debug("Reducing Sharpness damage from: "+event.getDamage()+" to: "+newDamage);
	                  event.setDamage(newDamage);
                  }
            }
    	}
    }
    @Bergifications ({
    	@Bergification(opt="bergey_armor", def="true"),
    	@Bergification(opt="bergey_armor_50_perc_mit", def="10",type=OptType.Int),
    	@Bergification(opt="bergey_prot", def="true"),
    	@Bergification(opt="bergey_prot_50_perc_mit", def="7",type=OptType.Int),
    	@Bergification(opt="bergey_prot_scale", def="0.33",type=OptType.Double),
    })
    @EventHandler(priority = EventPriority.LOWEST) // ignoreCancelled=false
    public void onPlayerTakeDamage(EntityDamageEvent event) {
      if (!config_.get("bergey_armor").getBool()) {
          return;
      }
      double damage = event.getDamage();
      if (damage <= 0.0000001D) {
        return;
      }
      DamageCause cause = event.getCause();
      if (!isCommonDamage(cause)) {
          return;
      }
      
      boolean factorProt = cause.equals(DamageCause.ENTITY_ATTACK) ||
    		    		   cause.equals(DamageCause.PROJECTILE);
      
      Entity entity = event.getEntity();
      if (!(entity instanceof Player)) {
        return;
      }
      Player defender = (Player)entity;

  	  if(apis.isBergecraftDisabledFor(defender)) return;
  	  
      double defense = getDefense(defender);
      double epf = getAverageEPF(defender);
      double bergey_epf = getAverageBergeyEPF(defender);
      
      double vanilla_reduction = defense * 0.04;
      double vanilla_protection_reduction = 0;
      if(factorProt){
    	  vanilla_protection_reduction = epf * 0.04;
      }
      double vanilla_damage_taken_ratio = (1 - vanilla_reduction) * (1 - vanilla_protection_reduction);
      
      double originalDamage = damage / vanilla_damage_taken_ratio;
      
      double bergey_reduction = defense / (defense + config_.get("bergey_armor_50_perc_mit").getInt());
      double bergey_prot_reduction = 0;
      if(factorProt){
    	  bergey_prot_reduction = bergey_epf / (bergey_epf + config_.get("bergey_prot_50_perc_mit").getInt()) * config_.get("bergey_prot_scale").getDouble();
      }
      double bergey_damage_taken_ratio = (1 - bergey_reduction) * (1 - bergey_prot_reduction);
      
      double newDamage = originalDamage * bergey_damage_taken_ratio;
      DecimalFormat df = new DecimalFormat("#.##");
      if(factorProt) {
	      debug(     "[Vanilla] Armor: "+df.format(vanilla_reduction)+", Enchant: "+df.format(vanilla_protection_reduction)+"\n"+
	"                              [Bergey ] Armor: "+df.format(bergey_reduction)+", Enchant: "+df.format(bergey_prot_reduction)+"\n"+
	"                                        Damage Before: "+df.format(damage)+ " Damage After: "+df.format(newDamage));
      }
      else {
    	  debug(     "[Vanilla] Armor: "+df.format(vanilla_reduction)+", \n"+
    "                              [Bergey ] Armor: "+df.format(bergey_reduction)+"\n"+
	"                                        Damage Before: "+df.format(damage)+ " Damage After: "+df.format(newDamage));
      }
      event.setDamage(newDamage);
    }
    
    private boolean isCommonDamage(DamageCause cause)
    {
    	return cause.equals(DamageCause.ENTITY_ATTACK) ||
    		   cause.equals(DamageCause.PROJECTILE) ||
    		   cause.equals(DamageCause.FIRE) ||
    		   cause.equals(DamageCause.LAVA) ||
    		   cause.equals(DamageCause.CONTACT) ||
    		   cause.equals(DamageCause.ENTITY_EXPLOSION) ||
    		   cause.equals(DamageCause.LIGHTNING) ||
    		   cause.equals(DamageCause.BLOCK_EXPLOSION);
    }
    private double getDefense(Player player)
    {
	   PlayerInventory inv = player.getInventory();
	   ItemStack boots = inv.getBoots();
	   ItemStack helmet = inv.getHelmet();
	   ItemStack chest = inv.getChestplate();
	   ItemStack pants = inv.getLeggings();
	   int def = 0;
	   if(helmet!=null){
		   if(helmet.getType() == Material.LEATHER_HELMET)def = def + 1;
		   else if(helmet.getType() == Material.GOLD_HELMET)def = def + 2;
		   else if(helmet.getType() == Material.CHAINMAIL_HELMET)def = def + 2;
		   else if(helmet.getType() == Material.IRON_HELMET)def = def + 2;
		   else if(helmet.getType() == Material.DIAMOND_HELMET)def = def + 3;
	   }
	   //
	   if(boots!=null){
		   if(boots.getType() == Material.LEATHER_BOOTS)def = def + 1;
		   else if(boots.getType() == Material.GOLD_BOOTS)def = def + 1;
		   else if(boots.getType() == Material.CHAINMAIL_BOOTS)def = def + 1;
		   else if(boots.getType() == Material.IRON_BOOTS)def = def + 2;
		   else if(boots.getType() == Material.DIAMOND_BOOTS)def = def + 3;
	   }
	   //
	   if(pants!=null){
		   if(pants.getType() == Material.LEATHER_LEGGINGS)def = def + 2;
		   else if(pants.getType() == Material.GOLD_LEGGINGS)def = def + 3;
		   else if(pants.getType() == Material.CHAINMAIL_LEGGINGS)def = def + 4;
		   else if(pants.getType() == Material.IRON_LEGGINGS)def = def + 5;
		   else if(pants.getType() == Material.DIAMOND_LEGGINGS)def = def + 6;
	   }
	   //
	   if(chest!=null){
		   if(chest.getType() == Material.LEATHER_CHESTPLATE)def = def + 3;
		   else if(chest.getType() == Material.GOLD_CHESTPLATE)def = def + 5;
		   else if(chest.getType() == Material.CHAINMAIL_CHESTPLATE)def = def + 5;
		   else if(chest.getType() == Material.IRON_CHESTPLATE)def = def + 6;
		   else if(chest.getType() == Material.DIAMOND_CHESTPLATE)def = def + 8;
	   }
	   return def;
    }

    private double getAverageEPF(Player player)
    {
 	   PlayerInventory inv = player.getInventory();
 	   
 	   int epf = 0;
	   for (ItemStack armor : inv.getArmorContents()) {
		   int level = armor.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL);
		   if(level == 4)
			   level = 5;
		   epf += level;
	   }
	   return epf*0.75;
    }
    
    private double getAverageBergeyEPF(Player player)
    {
  	   PlayerInventory inv = player.getInventory();
  	   
  	   int epf = 0;
 	   for (ItemStack armor : inv.getArmorContents()) {
 		   epf += armor.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) * 1.25;
 	   }
 	   return epf*0.75;
    }
    
//    @Bergification(opt="bergey_logout", def="true")
//    @EventHandler(priority = EventPriority.LOWEST) // ignoreCancelled=false
//    public void onEntityLogout(PlayerQuitEvent event) {
//    
//    }
    
    @EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLogin(PlayerJoinEvent event){
	    setMaxHealth(event.getPlayer());
	}

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCloseInventory(InventoryCloseEvent event){
    	HumanEntity human = event.getPlayer();
    	if(human instanceof Player){
    		setMaxHealth((Player) human);
    	}
    }

    @Bergifications ({
    	@Bergification(opt="bergey_health", def="true"),
    	@Bergification(opt="bergey_base_health", def="20.0",type=OptType.Double),
    	@Bergification(opt="bergey_max_bonus_health", def="20.0",type=OptType.Double),
    	@Bergification(opt="bergey_health_bonus_50_perc_durability", def="850",type=OptType.Double)
    })
	public void setMaxHealth(Player player){

    	if(apis.isBergecraftDisabledFor(player)) return;
        if (!config_.get("bergey_health").getBool()) {
          return;
        }
		double maxHealth = config_.get("bergey_base_health").getDouble();
		
		double durability = 0;
 	    for (ItemStack armor : player.getInventory().getArmorContents()) {
 	    	durability += armor.getType().getMaxDurability();
 	    }
 	    
 	   maxHealth += config_.get("bergey_max_bonus_health").getDouble() *
 	    		durability / (durability + config_.get("bergey_health_bonus_50_perc_durability").getDouble());
 	    if(maxHealth != ((Damageable) player).getMaxHealth()){
			debug("Setting Player: "+player.getName()+" to "+maxHealth+" health");
			if(((Damageable)player).getHealth()>maxHealth)
			{
				player.setHealth(maxHealth);
			}
			player.setMaxHealth(maxHealth);
 	    }
	}
    
    public boolean onCommand(
        CommandSender sender,
        Command command,
        String label,
        String[] args) {
      if (!(sender instanceof ConsoleCommandSender) ||
          !command.getName().equals("bergecraft") ||
          args.length < 1) {
        return false;
      }
      String option = args[0];
      String value = null;
      String subvalue = null;
      boolean set = false;
      boolean subvalue_set = false;
      String msg = "";
      if (args.length > 1) {
        value = args[1];
        set = true;
      }
      if (args.length > 2) {
        subvalue = args[2];
        subvalue_set = true;
      }
      ConfigOption opt = config_.get(option);
      if (opt != null) {
        if (set) {
          opt.set(value);
        }
        msg = String.format("%s = %s", option, opt.getString());
      } else if (option.equals("debug")) {
        if (set) {
          Config.DebugLog = toBool(value);
        }
        msg = String.format("debug = %s", config_.DebugLog);
      } else if (option.equals("save")) {
        config_.save();
        msg = "Configuration saved";
      } else if (option.equals("reload")) {
        config_.reload();
        msg = "Configuration loaded";
      } else {
        msg = String.format("Unknown option %s", option);
      }
      sender.sendMessage(msg);
      return true;
    }
    // ================================================
    // General
    @Bergifications ({
    	@Bergification(opt="gimmick_api_enabled", def="true"),
    	@Bergification(opt="gimmick_api_pvpmode", def="bergecraft")
    })
    public void onLoad()
    {
      loadConfiguration();
      info("Loaded");
  	  apis = new ApiManager();
    }
    private void loadConfiguration() {
        config_ = Config.initialize(this);
      }

    public void onEnable() {
      registerEvents();
      registerCommands();
      global_instance_ = this;
      info("Enabled");
    }
    private void registerEvents() {
      getServer().getPluginManager().registerEvents(this, this);
    }

    public void registerCommands() {
      ConsoleCommandSender console = getServer().getConsoleSender();
      console.addAttachment(this, "bergecraft.console", true);
    }
    public boolean isInitiaized() {
      return global_instance_ != null;
    }

    public boolean toBool(String value) {
      if (value.equals("1") || value.equalsIgnoreCase("true")) {
        return true;
      }
      return false;
    }
}
