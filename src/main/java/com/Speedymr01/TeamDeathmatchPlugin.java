package com.Speedymr01;

import com.Speedymr01.api.TDMAPI;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class TeamDeathmatchPlugin extends JavaPlugin {
    private GameManager gameManager;
    private SpawnManager spawnManager;
    private static TeamDeathmatchPlugin instance;
    private TDMMinigameProvider provider;

    // GUI handlers for config menu
    private final Map<UUID, BiFunction<Player, Integer, Boolean>> guiHandlers = new HashMap<>();
    // Tracks shift-click state so handlers can query it
    private final Map<UUID, Boolean> shiftClickStates = new HashMap<>();
    // Chat input handlers for precise value entry
    private final Map<UUID, Consumer<String>> chatInputHandlers = new HashMap<>();

    private final Listener guiListener = new Listener() {
        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();

            BiFunction<Player, Integer, Boolean> handler = guiHandlers.get(player.getUniqueId());
            if (handler != null) {
                shiftClickStates.put(player.getUniqueId(), event.isShiftClick());
                try {
                    boolean handled = handler.apply(player, event.getSlot());
                    if (handled) {
                        event.setCancelled(true);
                    }
                } finally {
                    shiftClickStates.remove(player.getUniqueId());
                }
            }
        }

        @EventHandler
        public void onInventoryClose(InventoryCloseEvent event) {
            if (!(event.getPlayer() instanceof Player)) return;
            Player player = (Player) event.getPlayer();
            guiHandlers.remove(player.getUniqueId());
            chatInputHandlers.remove(player.getUniqueId());
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onPlayerChat(AsyncChatEvent event) {
            Player player = event.getPlayer();
            Consumer<String> handler = chatInputHandlers.get(player.getUniqueId());
            if (handler == null) return;

            event.setCancelled(true);
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());

            getServer().getScheduler().runTask(TeamDeathmatchPlugin.this, () -> {
                handler.accept(message);
                chatInputHandlers.remove(player.getUniqueId());
            });
        }
    };

    @Override
    public void onEnable() {
        instance = this;

        // Register GUI click listener
        getServer().getPluginManager().registerEvents(guiListener, this);

        // bStats
        int pluginId = 33301;
        new Metrics(this, pluginId);

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

    /**
     * Set a handler for inventory clicks.
     */
    public void setGuiHandler(UUID playerId, BiFunction<Player, Integer, Boolean> handler) {
        guiHandlers.put(playerId, handler);
    }

    /**
     * Returns true if the current click event for this player was a shift-click.
     * Only valid during handler execution; returns false otherwise.
     */
    public boolean isShiftClick(Player player) {
        return shiftClickStates.getOrDefault(player.getUniqueId(), false);
    }

    /**
     * Set a handler for the next chat message from this player.
     * The handler is removed once a message is received or the inventory closes.
     */
    public void setChatInputHandler(UUID playerId, Consumer<String> handler) {
        chatInputHandlers.put(playerId, handler);
    }
}
