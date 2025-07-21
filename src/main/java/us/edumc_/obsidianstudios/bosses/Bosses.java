package us.edumc_.obsidianstudios.bosses;

import org.bukkit.plugin.java.JavaPlugin;
import us.edumc_.obsidianstudios.bosses.commands.BossCommands;
import us.edumc_.obsidianstudios.bosses.listeners.BossListener;
import us.edumc_.obsidianstudios.bosses.managers.BossManager;
import us.edumc_.obsidianstudios.bosses.managers.ConfigManager;
import us.edumc_.obsidianstudios.bosses.tasks.BossAbilityTask; // IMPORTANTE

public final class Bosses extends JavaPlugin {

    private static Bosses instance;
    private ConfigManager configManager;
    private BossManager bossManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        Complements complements = new Complements(this);
        complements.validate();

        this.configManager = new ConfigManager(this);
        configManager.loadConfigs();

        this.bossManager = new BossManager(this);
        bossManager.loadAllBossesFromConfig();

        getCommand("boss").setExecutor(new BossCommands(this));
        getServer().getPluginManager().registerEvents(new BossListener(this), this);

        // --- INICIO DE LA MODIFICACIÓN ---
        // Inicia la tarea de habilidades para que se ejecute cada segundo (20 ticks)
        new BossAbilityTask(this).runTaskTimer(this, 100L, 20L); // Inicia después de 5s, repite cada 1s
        // --- FIN DE LA MODIFICACIÓN ---

        getLogger().info("ObsidianBosses ha sido habilitado correctamente.");
    }

    @Override
    public void onDisable() {
        if (bossManager != null) {
            bossManager.removeAllActiveBosses();
        }
        getLogger().info("ObsidianBosses ha sido deshabilitado.");
    }

    // Getters
    public static Bosses getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public BossManager getBossManager() { return bossManager; }
}
