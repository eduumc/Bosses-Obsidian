package us.edumc_.obsidianstudios.bosses.objects;

import org.bukkit.configuration.ConfigurationSection;

// Clase para almacenar la información de una habilidad del archivo de configuración.
public class Ability {

    private final String type;
    private final String trigger;

    // Campos para SUMMON_MOBS
    private final String mobType;
    private final int count;

    // Campos para APPLY_EFFECT
    private final String effect;
    private final int duration;
    private final int amplifier;
    private final double triggerRadius; // Para on_player_near

    private final int cooldown;

    public Ability(ConfigurationSection config) {
        this.type = config.getString("type");
        this.trigger = config.getString("trigger");
        this.cooldown = config.getInt("cooldown", 10); // Cooldown por defecto de 10s

        // Cargar datos específicos de la habilidad
        this.mobType = config.getString("mob_type");
        this.count = config.getInt("count");

        this.effect = config.getString("effect");
        this.duration = config.getInt("duration");
        this.amplifier = config.getInt("amplifier");

        // Extraer el radio del trigger on_player_near(15)
        if (trigger != null && trigger.startsWith("on_player_near")) {
            this.triggerRadius = Double.parseDouble(trigger.replaceAll("[^0-9.]", ""));
        } else {
            this.triggerRadius = 0;
        }
    }

    // Getters
    public String getType() { return type; }
    public String getTrigger() { return trigger; }
    public String getMobType() { return mobType; }
    public int getCount() { return count; }
    public String getEffect() { return effect; }
    public int getDuration() { return duration; }
    public int getAmplifier() { return amplifier; }
    public double getTriggerRadius() { return triggerRadius; }
    public int getCooldown() { return cooldown; }
}
