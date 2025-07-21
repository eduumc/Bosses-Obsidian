package us.edumc_.obsidianstudios.bosses.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import us.edumc_.obsidianstudios.bosses.Bosses;
import us.edumc_.obsidianstudios.bosses.objects.Ability;
import us.edumc_.obsidianstudios.bosses.objects.CustomBoss;
import us.edumc_.obsidianstudios.bosses.objects.LootItem;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class BossManager {

    private final Bosses plugin;
    private final Map<UUID, CustomBoss> activeBosses = new HashMap<>();
    private final Map<UUID, BossBar> activeBossBars = new HashMap<>();
    private final Map<UUID, Map<Ability, Long>> abilityCooldowns = new HashMap<>();
    private final Map<UUID, Set<Ability>> usedOneTimeAbilities = new HashMap<>();

    public BossManager(Bosses plugin) {
        this.plugin = plugin;
    }

    public void loadAllBossesFromConfig() {
        plugin.getLogger().info("Se han cargado " + plugin.getConfigManager().getAllBossTemplates().size() + " plantillas de jefes.");
    }

    public void spawnBoss(String internalName, Location location) {
        CustomBoss template = plugin.getConfigManager().getBossTemplate(internalName);
        if (template == null) {
            plugin.getLogger().warning("Intento de invocar un jefe no existente: " + internalName);
            return;
        }

        try {
            EntityType entityType = EntityType.valueOf(template.getBaseMob().replace("minecraft:", "").toUpperCase());
            LivingEntity bossEntity = (LivingEntity) location.getWorld().spawnEntity(location, entityType);

            // Aplicar atributos
            bossEntity.setCustomName(template.getDisplayName());
            bossEntity.setCustomNameVisible(true);
            bossEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(template.getMaxHealth());
            bossEntity.setHealth(template.getMaxHealth());

            // Crear y mostrar la Boss Bar
            BossBar bossBar = Bukkit.createBossBar(template.getDisplayName(), BarColor.PURPLE, BarStyle.SOLID);
            bossBar.setProgress(1.0);
            for (Player player : Bukkit.getOnlinePlayers()) {
                bossBar.addPlayer(player);
            }
            activeBossBars.put(bossEntity.getUniqueId(), bossBar);

            // Registrar jefe en los mapas de seguimiento
            activeBosses.put(bossEntity.getUniqueId(), template);
            abilityCooldowns.put(bossEntity.getUniqueId(), new HashMap<>());
            usedOneTimeAbilities.put(bossEntity.getUniqueId(), new HashSet<>());

            // Anuncio global
            String spawnMessage = "&c¡Un eco helado resuena desde las profundidades! El %boss_name% &cha despertado en &7[%coords%]";
            spawnMessage = spawnMessage.replace("%boss_name%", template.getDisplayName() + ChatColor.RESET);
            String coords = String.format("X: %d, Y: %d, Z: %d", location.getBlockX(), location.getBlockY(), location.getBlockZ());
            spawnMessage = spawnMessage.replace("%coords%", coords);
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', spawnMessage));

        } catch (Exception e) {
            plugin.getLogger().severe("Error al invocar jefe: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void handleBossDeath(LivingEntity bossEntity) {
        if (!isBoss(bossEntity)) return;

        UUID bossId = bossEntity.getUniqueId();
        CustomBoss bossInfo = activeBosses.get(bossId);

        BossBar bossBar = activeBossBars.get(bossId);
        if (bossBar != null) {
            bossBar.removeAll();
        }

        plugin.getLogger().info("El jefe " + bossInfo.getDisplayName() + " ha muerto. Procesando botín...");
        for (LootItem loot : bossInfo.getLootTable()) {
            if (ThreadLocalRandom.current().nextDouble(100) < loot.getChance()) {
                if (loot.isItem()) {
                    try {
                        Material material = Material.matchMaterial(loot.getItemString().replace("minecraft:", ""));
                        if (material != null) {
                            int amount = parseAmount(loot.getAmount());
                            bossEntity.getWorld().dropItemNaturally(bossEntity.getLocation(), new ItemStack(material, amount));
                            plugin.getLogger().info("Soltando botín: " + amount + "x " + material.name());
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error al procesar el botín de item: " + loot.getItemString());
                    }
                } else if (loot.isCommand()) {
                    String command = loot.getCommand();
                    if (bossEntity.getKiller() != null) {
                        command = command.replace("%player_name%", bossEntity.getKiller().getName());
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    plugin.getLogger().info("Ejecutando comando de botín: " + command);
                }
            }
        }

        activeBosses.remove(bossId);
        activeBossBars.remove(bossId);
        abilityCooldowns.remove(bossId);
        usedOneTimeAbilities.remove(bossId);

        Bukkit.broadcastMessage(bossInfo.getDisplayName() + ChatColor.RESET + ChatColor.GREEN + " ha sido derrotado!");
    }

    public void tickBossAbilities() {
        for (Map.Entry<UUID, CustomBoss> entry : new HashMap<>(activeBosses).entrySet()) {
            LivingEntity bossEntity = (LivingEntity) Bukkit.getEntity(entry.getKey());
            if (bossEntity == null || bossEntity.isDead()) continue;

            for (Ability ability : entry.getValue().getAbilities()) {
                if (isAbilityOnCooldown(bossEntity.getUniqueId(), ability)) continue;

                boolean executed = false;
                String trigger = ability.getTrigger();
                if (trigger != null && (trigger.startsWith("on_tick") || trigger.startsWith("on_player_near"))) {
                    executed = executeAbility(bossEntity, ability);
                }

                if (executed) {
                    setAbilityCooldown(bossEntity.getUniqueId(), ability);
                }
            }
        }
    }

    public void checkDamageBasedAbilities(LivingEntity bossEntity) {
        CustomBoss bossTemplate = activeBosses.get(bossEntity.getUniqueId());
        if (bossTemplate == null) return;

        double maxHealth = bossEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double currentHealth = bossEntity.getHealth();
        double healthPercentage = (currentHealth / maxHealth) * 100;

        for (Ability ability : bossTemplate.getAbilities()) {
            if (hasUsedOneTimeAbility(bossEntity.getUniqueId(), ability)) continue;

            String trigger = ability.getTrigger();
            if (trigger != null && trigger.startsWith("on_health_below")) {
                try {
                    double triggerPercentage = Double.parseDouble(trigger.replaceAll("[^0-9.]", ""));
                    if (healthPercentage <= triggerPercentage) {
                        if (executeAbility(bossEntity, ability)) {
                            setOneTimeAbilityAsUsed(bossEntity.getUniqueId(), ability);
                        }
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Formato de trigger inválido: " + trigger);
                }
            }
        }
    }

    private boolean executeAbility(LivingEntity boss, Ability ability) {
        if (ability.getType() == null) return false;

        switch (ability.getType()) {
            case "summon_mobs":
                if (ability.getMobType() == null) return false;
                plugin.getLogger().info("Jefe " + boss.getCustomName() + " usa la habilidad: SUMMON_MOBS");
                for (int i = 0; i < ability.getCount(); i++) {
                    try {
                        EntityType mobToSummon = EntityType.valueOf(ability.getMobType().replace("minecraft:", "").toUpperCase());
                        Location spawnLoc = boss.getLocation().add(Math.random() * 4 - 2, 0.5, Math.random() * 4 - 2);
                        boss.getWorld().spawnEntity(spawnLoc, mobToSummon);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().severe("¡TIPO DE MOB INVÁLIDO en la habilidad 'summon_mobs': " + ability.getMobType() + "!");
                        return false;
                    }
                }
                return true;

            case "apply_effect":
                if (ability.getEffect() == null || !ability.getTrigger().startsWith("on_player_near")) return false;
                PotionEffectType effectType = PotionEffectType.getByName(ability.getEffect().toUpperCase());
                if (effectType == null) {
                    plugin.getLogger().severe("¡EFECTO DE POCIÓN INVÁLIDO: " + ability.getEffect() + "!");
                    return false;
                }
                boolean applied = false;
                for (Entity nearbyEntity : boss.getNearbyEntities(ability.getTriggerRadius(), ability.getTriggerRadius(), ability.getTriggerRadius())) {
                    if (nearbyEntity instanceof Player) {
                        ((Player) nearbyEntity).addPotionEffect(new PotionEffect(effectType, ability.getDuration() * 20, ability.getAmplifier()));
                        applied = true;
                    }
                }
                if (applied) {
                    plugin.getLogger().info("Jefe " + boss.getCustomName() + " aplicó '" + ability.getEffect() + "' a jugadores cercanos.");
                }
                return applied;

            default:
                plugin.getLogger().warning("Tipo de habilidad desconocido encontrado: " + ability.getType());
                return false;
        }
    }

    private int parseAmount(String amount) {
        if (amount.contains("-")) {
            try {
                String[] parts = amount.split("-");
                int min = Integer.parseInt(parts[0]);
                int max = Integer.parseInt(parts[1]);
                return ThreadLocalRandom.current().nextInt(min, max + 1);
            } catch (Exception e) {
                return 1;
            }
        }
        return Integer.parseInt(amount);
    }

    private boolean isAbilityOnCooldown(UUID bossId, Ability ability) {
        Map<Ability, Long> cooldowns = abilityCooldowns.get(bossId);
        if (cooldowns == null || !cooldowns.containsKey(ability)) return false;
        return System.currentTimeMillis() < cooldowns.get(ability);
    }

    private void setAbilityCooldown(UUID bossId, Ability ability) {
        Map<Ability, Long> cooldowns = abilityCooldowns.computeIfAbsent(bossId, k -> new HashMap<>());
        long cooldownEndTime = System.currentTimeMillis() + (ability.getCooldown() * 1000L);
        cooldowns.put(ability, cooldownEndTime);
    }

    private boolean hasUsedOneTimeAbility(UUID bossId, Ability ability) {
        return usedOneTimeAbilities.getOrDefault(bossId, Collections.emptySet()).contains(ability);
    }

    private void setOneTimeAbilityAsUsed(UUID bossId, Ability ability) {
        usedOneTimeAbilities.computeIfAbsent(bossId, k -> new HashSet<>()).add(ability);
    }

    public Map<UUID, CustomBoss> getActiveBosses() { return activeBosses; }
    public Map<UUID, BossBar> getActiveBossBars() { return activeBossBars; }
    public boolean isBoss(Entity entity) { return activeBosses.containsKey(entity.getUniqueId()); }

    public void removeAllActiveBosses() {
        for (UUID bossId : new ArrayList<>(activeBosses.keySet())) {
            Entity boss = Bukkit.getEntity(bossId);
            if (boss instanceof LivingEntity) {
                handleBossDeath((LivingEntity) boss);
                boss.remove();
            }
        }
    }
}
