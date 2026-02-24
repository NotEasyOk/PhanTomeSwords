package your.package.name;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.HashMap;
import java.util.UUID;

public class SwordManager implements Listener {

    // --- 1. PHANTOM BLADE (Curse Power) ---
    private void executePhanTomBlade(Player p, String action) {
        switch (action) {
            case "RIGHT": // Curse Dash
                p.setVelocity(p.getLocation().getDirection().multiply(2.0).setY(0.2));
                p.getWorld().spawnParticle(Particle.REDSTONE, p.getLocation(), 60, new Particle.DustOptions(Color.RED, 2));
                damageNearby(p, 3.5, 7.0);
                p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1f, 1.5f);
                break;

            case "LEFT": // Rapid Slash (3 Fast Hits Simulation)
                p.swingMainHand();
                for (int i = 1; i <= 3; i++) {
                    final double damage = 4.0 + i; // Increasing damage: 5, 6, 7
                    Bukkit.getScheduler().runTaskLater(PhanTomCore.get(), () -> {
                        damageNearby(p, 4.0, damage);
                        p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, p.getLocation().add(p.getLocation().getDirection().multiply(1.5)), 1);
                    }, i * 2L);
                }
                break;

            case "ULT": // Domain Slash (Shift + F)
                p.getWorld().spawnParticle(Particle.SQUID_INK, p.getLocation(), 300, 5, 2, 5);
                p.getWorld().spawnParticle(Particle.SMOKE_NORMAL, p.getLocation(), 100, 5, 1, 5);
                p.sendTitle("§4§lDOMAIN SLASH", "§7Darkness Consumes...", 5, 30, 5);
                p.getNearbyEntities(8, 8, 8).forEach(en -> {
                    if (en instanceof LivingEntity && en != p) {
                        ((LivingEntity) en).damage(16.0, p);
                        en.setVelocity(en.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(2).setY(1.0)); // Knockback
                    }
                });
                break;
        }
    }

    // --- 2. SHADOW BLADE (Soul Power) ---
    private void executeShadowBlade(Player p, String action) {
        switch (action) {
            case "RIGHT": // Shadow Step (Teleport)
                Location loc = p.getTargetBlock(null, 10).getLocation().add(0, 1, 0);
                p.getWorld().spawnParticle(Particle.SMOKE_LARGE, p.getLocation(), 40);
                p.teleport(loc.setDirection(p.getLocation().getDirection()));
                p.getWorld().spawnParticle(Particle.SMOKE_LARGE, p.getLocation(), 40);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                break;

            case "LEFT": // Soul Slash (Energy Wave)
                Entity target = getAimTarget(p, 8);
                p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, p.getEyeLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                if (target instanceof LivingEntity) {
                    ((LivingEntity) target).damage(10.0, p);
                    target.getWorld().spawnParticle(Particle.SOUL, target.getLocation(), 30);
                }
                break;

            case "ULT": // Hollow Mode (Shift + F)
                p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 400, 1)); // Strength
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 1));
                p.getWorld().spawnParticle(Particle.DRAGON_BREATH, p.getLocation(), 150, 1, 2, 1);
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.5f);
                p.sendMessage("§8§l[!] HOLLOW MODE ACTIVATED");
                break;
        }
    }

    // --- 3. FIRE LIGHTER BLADE (Fire + Lightning) ---
    private void executeFireLighter(Player p, String action) {
        switch (action) {
            case "RIGHT": // Fire Dash
                p.setVelocity(p.getLocation().getDirection().multiply(2.2).setY(0.1));
                Bukkit.getScheduler().runTaskTimer(PhanTomCore.get(), task -> {
                    p.getWorld().spawnParticle(Particle.FLAME, p.getLocation(), 5, 0.1, 0.1, 0.1, 0.02);
                }, 0, 1).cancel(); // Just a trail effect
                p.playSound(p.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1f);
                break;

            case "LEFT": // Thunder Strike (Aim Based)
                Entity t = getAimTarget(p, 12);
                if (t != null) {
                    t.getWorld().strikeLightningEffect(t.getLocation());
                    ((LivingEntity) t).damage(12.0, p);
                    t.setFireTicks(60);
                }
                break;

            case "ULT": // Flame Burst (Shift + F)
                p.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, p.getLocation(), 15, 3, 3, 3);
                p.getWorld().spawnParticle(Particle.LAVA, p.getLocation(), 50, 4, 1, 4);
                p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
                p.getNearbyEntities(7, 7, 7).forEach(en -> {
                    if (en instanceof LivingEntity && en != p) {
                        ((LivingEntity) en).damage(14.0, p);
                        en.setFireTicks(160);
                    }
                });
                break;
        }
    }

    // --- EVENT HANDLERS ---

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        String type = getSwordType(item);
        if (type == null) return;

        if (e.getAction().name().contains("RIGHT")) {
            if (checkCD(p, "RIGHT")) handleChoice(p, type, "RIGHT");
        } else if (e.getAction().name().contains("LEFT")) {
            if (checkCD(p, "LEFT")) handleChoice(p, type, "LEFT");
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        String type = getSwordType(p.getInventory().getItemInMainHand());
        if (type != null && p.isSneaking()) {
            e.setCancelled(true);
            if (checkCD(p, "ULT")) handleChoice(p, type, "ULT");
        }
    }

    private void handleChoice(Player p, String type, String action) {
        if (type.equals("PHANTOM_BLADE")) executePhanTomBlade(p, action);
        else if (type.equals("SHADOW_BLADE")) executeShadowBlade(p, action);
        else if (type.equals("FIRE_LIGHTER")) executeFireLighter(p, action);
    }

    private String getSwordType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(PhanTomCore.SWORD_KEY, PersistentDataType.STRING);
    }

    private Entity getAimTarget(Player p, int range) {
        return p.getNearbyEntities(range, range, range).stream()
                .filter(e -> e instanceof LivingEntity && e != p)
                .findFirst().orElse(null);
    }

    private void damageNearby(Player p, double r, double d) {
        p.getNearbyEntities(r, r, r).forEach(e -> { if (e instanceof LivingEntity && e != p) ((LivingEntity) e).damage(d, p); });
    }

    private boolean checkCD(Player p, String id) {
        long last = PhanTomCore.get().cooldowns.getOrDefault(p.getUniqueId(), 0L);
        if (System.currentTimeMillis() - last < 35000) {
            p.sendMessage("§c§lCooldown: §f" + (35 - (System.currentTimeMillis() - last) / 1000) + "s");
            return false;
        }
        PhanTomCore.get().cooldowns.put(p.getUniqueId(), System.currentTimeMillis());
        return true;
    }
                  }
