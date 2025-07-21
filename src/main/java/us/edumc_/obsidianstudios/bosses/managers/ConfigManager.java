package us.edumc_.obsidianstudios.bosses.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import us.edumc_.obsidianstudios.bosses.Bosses;
import us.edumc_.obsidianstudios.bosses.objects.CustomBoss;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final Bosses plugin;
    // Este mapa almacenará las plantillas de jefes cargadas. La clave es el internal_name.
    private final Map<String, CustomBoss> bossTemplates = new HashMap<>();

    public ConfigManager(Bosses plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        // Limpiamos las plantillas viejas antes de recargar
        bossTemplates.clear();

        // 1. Cargar config.yml (no es necesario aquí porque ya lo hace la clase principal)
        // plugin.saveDefaultConfig();

        // 2. Cargar archivos de idioma (lang)
        // TODO: Implementar la lógica para cargar el archivo de idioma correcto (es.yml, en.yml)

        // 3. Cargar las definiciones de los jefes desde la carpeta /bosses/
        plugin.getLogger().info("Buscando archivos de jefes...");
        File bossesFolder = new File(plugin.getDataFolder(), "bosses");
        if (!bossesFolder.exists()) {
            bossesFolder.mkdirs();
            // Guardar el ejemplo por defecto si la carpeta no existe
            plugin.saveResource("bosses/rey_de_la_cripta.yml", false);
        }

        File[] bossFiles = bossesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (bossFiles == null || bossFiles.length == 0) {
            plugin.getLogger().warning("No se encontraron archivos de jefes en la carpeta /bosses/");
            return;
        }

        // --- INICIO DE LA CORRECCIÓN ---
        for (File bossFile : bossFiles) {
            try {
                // Usamos YamlConfiguration para cargar el archivo .yml
                FileConfiguration bossConfig = YamlConfiguration.loadConfiguration(bossFile);

                // Creamos un objeto CustomBoss usando la configuración cargada
                CustomBoss bossTemplate = new CustomBoss(bossConfig);

                // Añadimos la plantilla al mapa, usando su nombre interno como clave
                bossTemplates.put(bossTemplate.getInternalName(), bossTemplate);

                plugin.getLogger().info("¡Jefe cargado correctamente! -> " + bossTemplate.getInternalName());

            } catch (Exception e) {
                plugin.getLogger().severe("Error al cargar el archivo de jefe: " + bossFile.getName());
                e.printStackTrace(); // Imprime el error detallado en la consola
            }
        }
        // --- FIN DE LA CORRECCIÓN ---
    }

    /**
     * Devuelve la plantilla de un jefe por su nombre interno.
     * @param internalName El nombre del jefe a buscar.
     * @return El objeto CustomBoss o null si no se encuentra.
     */
    public CustomBoss getBossTemplate(String internalName) {
        return bossTemplates.get(internalName);
    }

    /**
     * Devuelve un mapa con todas las plantillas de jefes cargadas.
     */
    public Map<String, CustomBoss> getAllBossTemplates() {
        return bossTemplates;
    }
}
