package us.edumc_.obsidianstudios.bosses;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Complements {

    private final JavaPlugin plugin;
    private final String licenseKey;
    private final String validationUrl = "http://149.130.180.213:1057/index.php?action=api_verify";

    public Complements(JavaPlugin plugin) {
        this.plugin = plugin;
        this.licenseKey = plugin.getConfig().getString("license.key", "");
    }
    private String getPublicIp() {
        try {
            URL url = new URL("https://api.ipify.org");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                return reader.readLine();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error 10-Conection");
            return Bukkit.getIp().isEmpty() ? "127.0.0.1" : Bukkit.getIp();
        }
    }

    public void validate() {
        new Thread(() -> {
            try {
                String serverIp = getPublicIp();

                URL url = new URL(this.validationUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                String jsonInputString = "{\"license_key\": \"" + this.licenseKey +
                        "\", \"plugin_name\": \"" + plugin.getDescription().getName() +
                        "\", \"server_ip\": \"" + serverIp + "\"}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }

                String jsonResponse = response.toString();
                boolean isValid = jsonResponse.contains("\"valid\":true");

                if (isValid) {
                    plugin.getLogger().info("Successfully Verification. Enabling plugin.");
                    if (jsonResponse.contains("new_key")) {
                        String newKey = jsonResponse.split("\"new_key\":\"")[1].split("\"")[0];
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.getConfig().set("license.key", newKey);
                            plugin.saveConfig();
                        });
                    }
                } else {
                    String reason = "UNKNOWN";
                    if (jsonResponse.contains("reason")) {
                        reason = jsonResponse.split("\"reason\":\"")[1].split("\"")[0];
                    }
                    disablePlugin("Licencia inválida. Razón: " + reason);
                }

            } catch (Exception e) {
                disablePlugin("Please contact to Edumc_ if you bought this plugin. Error 102.");
                e.printStackTrace();
            }
        }).start();
    }

    private void disablePlugin(String reason) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getLogger().severe("==================================================");
            plugin.getLogger().severe("  El plugin " + plugin.getDescription().getName() + " ha sido deshabilitado.");
            plugin.getLogger().severe("  Razón: " + reason);
            plugin.getLogger().severe("==================================================");
            Bukkit.getPluginManager().disablePlugin(plugin);
        });
    }
}
