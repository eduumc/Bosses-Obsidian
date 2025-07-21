package us.edumc_.obsidianstudios.bosses.tasks;

import org.bukkit.scheduler.BukkitRunnable;
import us.edumc_.obsidianstudios.bosses.Bosses;
import us.edumc_.obsidianstudios.bosses.managers.BossManager;

// Tarea que se ejecuta cada segundo para procesar las habilidades de los jefes.
public class BossAbilityTask extends BukkitRunnable {

    private final BossManager bossManager;

    public BossAbilityTask(Bosses plugin) {
        this.bossManager = plugin.getBossManager();
    }

    @Override
    public void run() {
        // Si no hay jefes activos, no hacemos nada.
        if (bossManager.getActiveBosses().isEmpty()) {
            return;
        }

        // Llamamos a un método en BossManager para que procese las habilidades de cada jefe.
        bossManager.tickBossAbilities();
    }
}