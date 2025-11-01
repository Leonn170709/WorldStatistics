package net.dungeon.worldStatistics;

import net.dungeon.worldStatistics.command.StatsCommand;
import net.dungeon.worldStatistics.config.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class WorldStatistics extends JavaPlugin {

    private MessageManager messageManager;

    @Override
    public void onEnable() {
        messageManager = new MessageManager(this);
        getCommand("worldstats").setExecutor(new StatsCommand(this));
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    /**
     * Prüft ob der Server auf Folia läuft
     */
    public boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}