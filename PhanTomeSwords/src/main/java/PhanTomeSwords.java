package com.phantom.swords;

import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public class PhanTomCore extends JavaPlugin {
    private static PhanTomCore instance;
    public Map<UUID, Long> cooldowns = new HashMap<>();
    public static NamespacedKey SWORD_KEY;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        SWORD_KEY = new NamespacedKey(this, "sword_type");

        AdminCommand adminCmd = new AdminCommand();
        getCommand("phantomswords").setExecutor(adminCmd);
        getCommand("phantomswords").setTabCompleter(adminCmd);
        
        getServer().getPluginManager().registerEvents(new RitualManager(), this);
        getServer().getPluginManager().registerEvents(new SwordManager(), this);

        registerUnique9SlotRecipes();
        getLogger().info("§aPhanTom Swords Enabled - Java 21");
    }

    public static PhanTomCore get() { return instance; }

    public ItemStack createLegendary(String type) {
        var section = getConfig().getConfigurationSection("swords." + type);
        if (section == null) return new ItemStack(Material.BARRIER);

        Material mat = Material.valueOf(section.getString("material", "NETHERITE_SWORD"));
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', section.getString("name")));
        meta.setCustomModelData(section.getInt("custom_model_data"));
        
        List<String> lore = new ArrayList<>();
        for (String line : section.getStringList("lore")) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(lore);
        
        meta.getPersistentDataContainer().set(SWORD_KEY, PersistentDataType.STRING, type);
        item.setItemMeta(meta);
        return item;
    }

    private void registerUnique9SlotRecipes() {
        var section = getConfig().getConfigurationSection("swords");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            var recipeItems = getConfig().getStringList("swords." + key + ".recipe_slots");
            if (recipeItems.size() < 9) continue;

            ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "unique_9_" + key), createLegendary(key));
            recipe.shape("ABC", "DEF", "GHI");

            char[] keys = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};
            for (int i = 0; i < 9; i++) {
                recipe.setIngredient(keys[i], Material.valueOf(recipeItems.get(i).toUpperCase()));
            }
            Bukkit.addRecipe(recipe);
        }
    }
          }
