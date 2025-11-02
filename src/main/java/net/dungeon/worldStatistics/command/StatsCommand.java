package net.dungeon.worldStatistics.command;

import net.dungeon.worldStatistics.WorldStatistics;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsCommand implements CommandExecutor {

    private final WorldStatistics plugin;

    public StatsCommand(WorldStatistics plugin) {
        this.plugin = plugin;
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("worldstats.use")) {
            plugin.getMessageManager().sendMessage(sender, "commands.no-permission");
            return true;
        }

        // Stats async berechnen um Lag zu vermeiden
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.getMessageManager().loadMessages();
            sender.sendMessage("§a✔ Messages reloaded successfully!");
            return true;
        }
        //if (args.length == 1){
          //  List<String> arguments = new ArrayList<>();
            //arguments.add("reload");                      Tab completion code habs nicht hinbekommen :(
            //return true;
        //}

        if (plugin.isFolia()) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> calculateAndSendStats(sender));
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> calculateAndSendStats(sender));
        }

        return true;
    }

    private void calculateAndSendStats(CommandSender sender) {
        long totalBytes = 0;
        int worldCount = 0;

        for (World world : Bukkit.getWorlds()) {
            File worldFolder = world.getWorldFolder();
            totalBytes += getDirectorySize(worldFolder);
            worldCount++;
        }

        String formattedSize = formatBytes(totalBytes);

        File playerDataFolder = new File(Bukkit.getWorlds().get(0).getWorldFolder().getParentFile(),
                Bukkit.getWorlds().get(0).getName() + "/playerdata");
        int totalPlayers = 0;

        if (playerDataFolder.exists() && playerDataFolder.isDirectory()) {
            File[] playerFiles = playerDataFolder.listFiles((dir, name) -> name.endsWith(".dat"));
            if (playerFiles != null) {
                totalPlayers = playerFiles.length;
            }
        }
        File worldFolder = Bukkit.getWorlds().get(0).getWorldFolder();
        String baseName = worldFolder.getName();
        File parentFolder = worldFolder.getParentFile();

        // Region-Ordner
        File regionOverworld = new File(parentFolder, baseName + "/region");
        File regionNether = new File(parentFolder, baseName + "_nether/region");
        File regionEnd = new File(parentFolder, baseName + "_the_end/region");

        // Regionen zählen
        int overworldRegions = countRegionFiles(regionOverworld);
        int netherRegions = countRegionFiles(regionNether);
        int endRegions = countRegionFiles(regionEnd);

        int totalRegions = overworldRegions + netherRegions + endRegions;
        long totalRegionValue = totalRegions * 1024L;

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("world_size", formattedSize);
        placeholders.put("world_size_bytes", String.valueOf(totalBytes));
        placeholders.put("world_count", String.valueOf(worldCount));
        placeholders.put("total_players", String.valueOf(totalPlayers));
        placeholders.put("total_regions", String.valueOf(totalRegions));
        placeholders.put("total_region_value", String.valueOf(totalRegionValue));
        placeholders.put("online_players", String.valueOf(Bukkit.getOnlinePlayers().size()));

        String finalMessage = buildStatsMessage(placeholders);
        // Zurück zum Main Thread
        if (plugin.isFolia() && sender instanceof Player) {
            Player player = (Player) sender;
            player.getScheduler().run(plugin, task -> sender.sendMessage(finalMessage), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(finalMessage));
        }
    }

    private String buildStatsMessage(Map<String, String> placeholders) {
        StringBuilder message = new StringBuilder();

        for (String line : plugin.getMessageManager().getMessageList("stats.display")) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                line = line.replace("%" + entry.getKey() + "%", entry.getValue());
            }
            message.append(line).append("\n");
        }

        return message.toString().trim();
    }


    private long getDirectorySize(File directory) {
        long size = 0;

        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else if (file.isDirectory()) {
                        size += getDirectorySize(file);
                    }
                }
            }
        }

        return size;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};

        double value = bytes / Math.pow(1024, exp);

        return String.format("%.2f %s", value, units[exp]);
    }
    private int countRegionFiles(File folder) {
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".mca"));
            if (files != null) {
                return files.length;
            }
        }
        return 0;

    }


}