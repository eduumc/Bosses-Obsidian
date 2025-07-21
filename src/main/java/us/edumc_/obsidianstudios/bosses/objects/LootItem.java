package us.edumc_.obsidianstudios.bosses.objects;

import org.bukkit.configuration.ConfigurationSection;

// Clase para almacenar la información de un objeto del botín.
public class LootItem {

    private final String item;
    private final String command;
    private final double chance;
    private final String amount;

    public LootItem(ConfigurationSection config) {
        this.item = config.getString("item");
        this.command = config.getString("command");
        this.chance = config.getDouble("chance", 100.0);
        this.amount = config.getString("amount", "1");
    }

    // Getters
    public boolean isItem() { return item != null; }
    public boolean isCommand() { return command != null; }
    public String getItemString() { return item; }
    public String getCommand() { return command; }
    public double getChance() { return chance; }
    public String getAmount() { return amount; }
}
