package com.Speedymr01;

import com.Speedymr01.api.TDMAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TeamDeathmatchPlugin extends JavaPlugin {
    private GameManager gameManager;
    private SpawnManager spawnManager;
    private static TeamDeathmatchPlugin instance;
    private TDMMinigameProvider provider;

    @Override
    public void onEnable() {
        instance = this;
        
        // Detect if this is a reload
        boolean isReload = gameManager != null;
        if (isReload) {
            getLogger().warning("Plugin reload detected! Cleaning up active games...");
            cleanupOnReload();
        }
        
        saveDefaultConfig();

        // Load classes from config
        PlayerClass.loadClassesFromConfig(getConfig().getConfigurationSection("classes"));

        spawnManager = new SpawnManager(this);
        gameManager = new GameManager(this, spawnManager);

        TDMCommand tdmCommand = new TDMCommand(this, gameManager, spawnManager);
        getCommand("tdm").setExecutor(tdmCommand);
        getCommand("tdm").setTabCompleter(tdmCommand);

        getServer().getPluginManager().registerEvents(new GameListener(gameManager), this);

        // Register unofficial API for other plugins (e.g. tournament)
        TDMAPI tdmAPI = new TDMAPI(this, gameManager);
        getServer().getServicesManager().register(TDMAPI.class, tdmAPI, this, org.bukkit.plugin.ServicePriority.Normal);
        getLogger().info("Registered TDMAPI for external plugins");

        // Register TournamentManager MinigameProvider (if TournamentManager is installed)
        if (isTournamentManagerInstalled()) {
            this.provider = new TDMMinigameProvider(this, tdmAPI);
            this.provider.register();
        }

        getLogger().info("TeamDeathmatch plugin enabled!");
        getLogger().info("Loaded " + PlayerClass.getAllClasses().size() + " classes from config");
        
        if (isReload) {
            getLogger().warning("Plugin reloaded. Active games have been ended and players reset.");
        }
    }

    @Override
    public void onDisable() {
        if (provider != null) {
            provider.unregister();
        }
        if (gameManager != null && gameManager.isGameActive()) {
            getLogger().info("Ending active game due to plugin disable...");
            gameManager.endGame();
        }
        
        // Clean up all player states
        cleanupAllPlayers();
        
        getLogger().info("TeamDeathmatch plugin disabled!");
    }
    
    /**
     * Clean up when a reload is detected
     */
    private void cleanupOnReload() {
        if (gameManager != null && gameManager.isGameActive()) {
            gameManager.endGame();
        }
        cleanupAllPlayers();
    }
    
    /**
     * Reset all players to a clean state
     */
    private void cleanupAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Reset scoreboard
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            
            // Clear inventory if they were in a game
            if (gameManager != null && gameManager.isPlayerInGame(player.getUniqueId())) {
                player.getInventory().clear();
                player.setHealth(20.0);
                player.setFoodLevel(20);
                player.setSaturation(20.0f);
            }
        }
    }

    private boolean isTournamentManagerInstalled() {
        try {
            Class.forName("com.tdm.tournament.api.MinigameProvider");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public GameManager getGameManager() {
        return gameManager;
    }
    
    public static TeamDeathmatchPlugin getInstance() {
        return instance;
    }

    /**
     * Log a verbose diagnostic message (prefixed with [VERBOSE]).
     * Controlled by {@code verbose-logging} in config.yml.
     */
    public void verbose(String message) {
        if (getConfig().getBoolean("verbose-logging", true)) {
            getLogger().info("[VERBOSE] " + message);
        }
    }
}
