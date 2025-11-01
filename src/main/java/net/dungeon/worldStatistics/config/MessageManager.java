package net.dungeon.worldStatistics.config;

import net.dungeon.worldStatistics.WorldStatistics;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageManager {

    private final WorldStatistics plugin;
    private File messagesFile;
    private FileConfiguration messages;

    public MessageManager(WorldStatistics plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);
        plugin.getLogger().info("Messages.yml erfolgreich geladen!");
    }

    public void reloadMessages() {
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        plugin.getLogger().info("messages.yml neu geladen!");
    }

    public void saveMessages() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Konnte messages.yml nicht speichern: " + e.getMessage());
        }
    }

    public String getRawMessage(String path) {
        return messages.getString(path, "Nachricht nicht gefunden: " + path);
    }

    public String getMessage(String path) {
        return ColorUtil.translate(getRawMessage(path));
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getRawMessage(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return ColorUtil.translate(message);
    }

    public List<String> getMessageList(String path) {
        return messages.getStringList(path).stream()
                .map(ColorUtil::translate)
                .collect(Collectors.toList());
    }

    public List<String> getMessageList(String path, Map<String, String> placeholders) {
        return messages.getStringList(path).stream()
                .map(message -> {
                    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                        message = message.replace("{" + entry.getKey() + "}", entry.getValue());
                    }
                    return ColorUtil.translate(message);
                })
                .collect(Collectors.toList());
    }

    public void sendMessage(CommandSender sender, String path) {
        sender.sendMessage(getMessage(path));
    }

    public void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(getMessage(path, placeholders));
    }

    public void sendMessageWithPrefix(CommandSender sender, String path) {
        String prefix = getMessage("prefix");
        sender.sendMessage(prefix + " " + getMessage(path));
    }

    public void sendMessageWithPrefix(CommandSender sender, String path, Map<String, String> placeholders) {
        String prefix = getMessage("prefix");
        sender.sendMessage(prefix + " " + getMessage(path, placeholders));
    }

    // Folia Support: Nachrichten über Entity Scheduler senden
    public void sendMessageFolia(Player player, String path) {
        String message = getMessage(path);
        player.getScheduler().run(plugin, task -> player.sendMessage(message), null);
    }

    public void sendMessageFolia(Player player, String path, Map<String, String> placeholders) {
        String message = getMessage(path, placeholders);
        player.getScheduler().run(plugin, task -> player.sendMessage(message), null);
    }

    // Helper für Placeholder Maps
    public static Map<String, String> placeholder(String key, String value) {
        Map<String, String> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    public static Map<String, String> placeholders(String... keyValues) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValues.length - 1; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}