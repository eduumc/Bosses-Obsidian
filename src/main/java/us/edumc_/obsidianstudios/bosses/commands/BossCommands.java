package us.edumc_.obsidianstudios.bosses.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.edumc_.obsidianstudios.bosses.Bosses;

public class BossCommands implements CommandExecutor {

    private final Bosses plugin;

    public BossCommands(Bosses plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // TODO: Enviar mensaje de ayuda
            sender.sendMessage("Uso: /boss <spawn|reload|list>");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "spawn":
                // TODO: Lógica para /boss spawn <nombre>
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    plugin.getBossManager().spawnBoss(args[1], player.getLocation());
                }
                break;
            case "reload":
                // TODO: Lógica para /boss reload
                plugin.getConfigManager().loadConfigs();
                plugin.getBossManager().loadAllBossesFromConfig();
                sender.sendMessage("Configuración de ObsidianBosses recargada.");
                break;
            case "list":
                // TODO: Lógica para /boss list
                sender.sendMessage("Jefes disponibles: " + plugin.getConfigManager().getAllBossTemplates().keySet());
                break;
            default:
                sender.sendMessage("Comando desconocido.");
                break;
        }

        return true;
    }
}