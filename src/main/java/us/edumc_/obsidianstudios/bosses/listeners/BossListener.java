package us.edumc_.obsidianstudios.bosses.listeners;

import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import us.edumc_.obsidianstudios.bosses.Bosses;
import us.edumc_.obsidianstudios.bosses.managers.BossManager;

public class BossListener implements Listener {

    private final Bosses plugin;
    private final BossManager bossManager;

    public BossListener(Bosses plugin) {
        this.plugin = plugin;
        this.bossManager = plugin.getBossManager();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (bossManager.isBoss(event.getEntity())) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            bossManager.handleBossDeath(event.getEntity());
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (bossManager.isBoss(event.getEntity())) {
            LivingEntity bossEntity = (LivingEntity) event.getEntity();

            // --- INICIO DE LA MODIFICACIÓN ---
            // Se usa un BukkitRunnable para asegurar que la vida se actualice DESPUÉS del evento de daño
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                // Actualizar la barra de jefe
                BossBar bossBar = bossManager.getActiveBossBars().get(bossEntity.getUniqueId());
                if (bossBar != null) {
                    double maxHealth = bossEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                    double currentHealth = bossEntity.getHealth();
                    bossBar.setProgress(Math.max(0, currentHealth / maxHealth));
                }

                // Llamar al BossManager para que revise si se debe activar alguna habilidad por daño
                bossManager.checkDamageBasedAbilities(bossEntity);

            }, 1L); // 1 tick de retraso
            // --- FIN DE LA MODIFICACIÓN ---
        }
    }
}
