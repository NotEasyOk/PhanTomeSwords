package your.package.name;

import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RitualManager implements Listener, org.bukkit.command.CommandExecutor {

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();
        if (!result.hasItemMeta() || !result.getItemMeta().getPersistentDataContainer().has(PhanTomCore.SWORD_KEY, org.bukkit.persistence.PersistentDataType.STRING)) return;

        event.setCancelled(true);
        event.getInventory().setMatrix(new ItemStack[9]);
        startRitual((Player) event.getWhoClicked(), event.getInventory().getLocation(), result);
    }

    public void startRitual(Player p, Location loc, ItemStack sword) {
        String type = sword.getItemMeta().getDisplayName();
        BossBar bar = Bukkit.createBossBar("§6Awakening: " + type, BarColor.PURPLE, BarStyle.SEGMENTED_20);
        Bukkit.getOnlinePlayers().forEach(bar::addPlayer);

        ArmorStand as = loc.getWorld().spawn(loc.clone().add(0.5, 5, 0.5), ArmorStand.class, s -> {
            s.setVisible(false); s.setGravity(false); s.getEquipment().setItemInMainHand(sword);
        });

        new BukkitRunnable() {
            int time = 900;
            public void run() {
                if (time <= 0) {
                    loc.getWorld().strikeLightning(loc);
                    loc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, loc, 5);
                    loc.getWorld().dropItemNaturally(loc, sword);
                    bar.removeAll(); as.remove(); cancel(); return;
                }
                as.setRotation(as.getLocation().getYaw() + 15f, 0);
                loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(0.5, 5, 0.5), 10, 0.5, 0.5, 0.5, 0.05);
                bar.setProgress(time / 900.0);
                bar.setTitle("§6§l" + type + " §f| §c" + (time/60) + "m §f| §e" + loc.getBlockX() + " " + loc.getBlockZ());
                time--;
            }
        }.runTaskTimer(PhanTomCore.get(), 0, 20);
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender s, org.bukkit.command.Command c, String l, String[] a) {
        if (a[0].equalsIgnoreCase("give") && s.isOp()) {
            Player t = Bukkit.getPlayer(a[1]);
            if (a[2].equals("anime")) t.getInventory().addItem(PhanTomCore.createLegendary("ANIME", "§c§lPhanTom Blade", 1001, Color.RED));
            if (a[2].equals("shadow")) t.getInventory().addItem(PhanTomCore.createLegendary("SHADOW", "§8§lShadow Blade", 1002, Color.BLACK));
            if (a[2].equals("fire")) t.getInventory().addItem(PhanTomCore.createLegendary("FIRE", "§6§lFire Lighter", 1003, Color.ORANGE));
        }
        return true;
    }
    }
