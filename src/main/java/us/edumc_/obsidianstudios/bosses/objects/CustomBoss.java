package us.edumc_.obsidianstudios.bosses.objects;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class CustomBoss {

    private final String internalName;
    private final String baseMob;
    private final String displayName;
    private final double maxHealth;
    private final List<Ability> abilities;
    private final List<LootItem> lootTable; // <-- NUEVO

    public CustomBoss(FileConfiguration config) {
        this.internalName = config.getString("internal_name", "default_boss");
        this.baseMob = config.getString("base_mob", "minecraft:zombie");
        this.displayName = ChatColor.translateAlternateColorCodes('&', config.getString("display_name", "&cDefault Boss"));
        this.maxHealth = config.getDouble("health", 100.0);

        this.abilities = new ArrayList<>();
        if (config.isConfigurationSection("abilities")) {
            for (String key : config.getConfigurationSection("abilities").getKeys(false)) {
                this.abilities.add(new Ability(config.getConfigurationSection("abilities." + key)));
            }
        }

        // --- INICIO DE LA MODIFICACIÓN: LEER EL BOTÍN ---
        this.lootTable = new ArrayList<>();
        if (config.isConfigurationSection("loot")) {
            for (String key : config.getConfigurationSection("loot").getKeys(false)) {
                this.lootTable.add(new LootItem(config.getConfigurationSection("loot." + key)));
            }
        }
        // --- FIN DE LA MODIFICACIÓN ---
    }

    // Getters
    public String getInternalName() { return internalName; }
    public String getBaseMob() { return baseMob; }
    public String getDisplayName() { return displayName; }
    public double getMaxHealth() { return maxHealth; }
    public List<Ability> getAbilities() { return abilities; }
    public List<LootItem> getLootTable() { return lootTable; } // <-- NUEVO
}
