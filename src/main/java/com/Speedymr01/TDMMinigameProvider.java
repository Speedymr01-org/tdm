package com.Speedymr01;

import com.Speedymr01.api.TDMAPI;
import com.Speedymr01.api.event.TDMGameEndEvent;
import com.tdm.tournament.TournamentPlugin;
import com.tdm.tournament.api.MatchCompleteEvent;
import com.tdm.tournament.api.MinigameProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.ServicePriority;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Bridges TeamDeathmatch with the TournamentManager plugin via {@link MinigameProvider}.
 */
public class TDMMinigameProvider implements MinigameProvider, Listener {

    private final TeamDeathmatchPlugin plugin;
    private final TDMAPI api;

    private final Map<String, MatchContext> activeMatches = new HashMap<>();

    // When true, TDM game end event should be ignored (cleanup from failed createMatch)
    private volatile boolean endingForCleanup = false;

    // TDM team assignment: first tournament team -> RED, second -> BLUE
    private static final GameManager.Team TEAM1_SLOT = GameManager.Team.RED;
    private static final GameManager.Team TEAM2_SLOT = GameManager.Team.BLUE;

    public TDMMinigameProvider(TeamDeathmatchPlugin plugin, TDMAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    public void register() {
        Bukkit.getServicesManager().register(MinigameProvider.class, this, plugin, ServicePriority.Normal);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("Registered MinigameProvider for TournamentManager");
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
        Bukkit.getServicesManager().unregister(MinigameProvider.class, this);
        activeMatches.clear();
    }

    // ==================== MinigameProvider ====================

    @Override
    public String getPluginName() {
        return "TeamDeathmatch";
    }

    @Override
    public String getDisplayName() {
        return "TDM";
    }

    @Override
    public Material getIcon() {
        return Material.IRON_SWORD;
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public List<String> getAvailableArenas() {
        // TDM doesn't have a named arena list; return a placeholder
        return List.of("default");
    }

    @Override
    public boolean createMatch(String arena, List<UUID> team1, List<UUID> team2, String matchId) {
        plugin.verbose("createMatch called: arena=" + arena + " matchId=" + matchId
                + " team1=" + team1.size() + " players, team2=" + team2.size() + " players");

        // Step 1: Check if game is already active
        boolean gameActive = api.isGameActive();
        plugin.verbose("createMatch: api.isGameActive() returned " + gameActive);
        if (gameActive) {
            plugin.getLogger().warning("TDM game already active, cannot start tournament match " + matchId);
            return false;
        }

        // Step 2: Activate the game so players can join
        plugin.verbose("createMatch: calling api.activateGame()");
        api.activateGame();
        plugin.verbose("createMatch: after activateGame, isGameActive=" + api.isGameActive()
                + " isGameStarted=" + api.isGameStarted());

        // Track whether we need to end the game on failure (already activated)
        boolean needCleanup = true;

        try {
            // Step 3: Join team1 players to RED
            List<Player> team1Players = new ArrayList<>();
            for (UUID uid : team1) {
                Player p = Bukkit.getPlayer(uid);
                boolean found = (p != null && p.isOnline());
                plugin.verbose("createMatch: team1 player uid=" + uid + " found=" + found + " name=" + (p != null ? p.getName() : "N/A"));
                if (found) {
                    boolean joined = api.joinPlayer(p, TEAM1_SLOT);
                    plugin.verbose("createMatch: joinPlayer(team1) returned " + joined);
                    if (joined) {
                        team1Players.add(p);
                    } else {
                        // Player couldn't join — check why
                        plugin.verbose("createMatch: team1 player " + p.getName() + " failed to join (inGame="
                                + api.isPlayerInGame(uid) + " gameActive=" + api.isGameActive() + " gameStarted=" + api.isGameStarted() + ")");
                    }
                }
            }

            // Step 4: Join team2 players to BLUE
            List<Player> team2Players = new ArrayList<>();
            for (UUID uid : team2) {
                Player p = Bukkit.getPlayer(uid);
                boolean found = (p != null && p.isOnline());
                plugin.verbose("createMatch: team2 player uid=" + uid + " found=" + found + " name=" + (p != null ? p.getName() : "N/A"));
                if (found) {
                    boolean joined = api.joinPlayer(p, TEAM2_SLOT);
                    plugin.verbose("createMatch: joinPlayer(team2) returned " + joined);
                    if (joined) {
                        team2Players.add(p);
                    } else {
                        plugin.verbose("createMatch: team2 player " + p.getName() + " failed to join (inGame="
                                + api.isPlayerInGame(uid) + " gameActive=" + api.isGameActive() + " gameStarted=" + api.isGameStarted() + ")");
                    }
                }
            }

            // Step 5: Check if any players actually joined
            plugin.verbose("createMatch: team1Players.size=" + team1Players.size() + " team2Players.size=" + team2Players.size());
            if (team1Players.isEmpty() && team2Players.isEmpty()) {
                plugin.verbose("createMatch: FAILED — both teams empty, no players joined");
                return false;
            }

            // Step 6: Store context
            activeMatches.put(matchId, new MatchContext(arena, team1, team2));

            // Step 7: Start the game
            plugin.verbose("createMatch: calling api.startGame() (isGameActive=" + api.isGameActive()
                    + " isGameStarted=" + api.isGameStarted() + ")");
            boolean started = api.startGame();
            plugin.verbose("createMatch: api.startGame() returned " + started);
            if (!started) {
                plugin.verbose("createMatch: FAILED — api.startGame() returned false");
                activeMatches.remove(matchId);
                return false;
            }

            plugin.verbose("createMatch: SUCCESS — match " + matchId + " started");
            needCleanup = false;
            return true;
        } finally {
            // If we activated the game but something failed, end it so next attempt can work
            if (needCleanup) {
                plugin.verbose("createMatch: cleaning up — ending TDM game after failed match start");
                endingForCleanup = true;
                try {
                    api.endGame();
                } finally {
                    endingForCleanup = false;
                }
                activeMatches.remove(matchId);
            }
        }
    }

    @Override
    public void cancelMatch(String matchId) {
        MatchContext ctx = activeMatches.remove(matchId);
        if (ctx != null) {
            api.endGame();
        }
    }

    // ==================== Listen for TDM game end ====================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTDMGameEnd(TDMGameEndEvent event) {
        // Ignore game-end events triggered by cleanup of a failed createMatch
        if (endingForCleanup) return;
        if (activeMatches.isEmpty()) return;

        // Find which match just ended (take the first/only active one)
        Map.Entry<String, MatchContext> entry = activeMatches.entrySet().iterator().next();
        String matchId = entry.getKey();
        MatchContext ctx = entry.getValue();
        activeMatches.remove(matchId);

        // Determine winners from TDM result
        GameManager.Team tdmWinner = event.getWinner();
        boolean tie = tdmWinner == null;
        List<UUID> winnerUuids;

        if (tie) {
            winnerUuids = List.of();
        } else {
            // Map TDM team back to tournament team
            List<UUID> winningTeamIds = (tdmWinner == TEAM1_SLOT) ? ctx.team1 : ctx.team2;
            winnerUuids = winningTeamIds;
        }

        // Fire MatchCompleteEvent
        MatchCompleteEvent completeEvent = new MatchCompleteEvent(
                getPluginName(), matchId, winnerUuids, ctx.arena, tie);
        Bukkit.getPluginManager().callEvent(completeEvent);
    }

    // ==================== Context ====================

    private static class MatchContext {
        final String arena;
        final List<UUID> team1;
        final List<UUID> team2;

        MatchContext(String arena, List<UUID> team1, List<UUID> team2) {
            this.arena = arena;
            this.team1 = team1;
            this.team2 = team2;
        }
    }

    // ==================== Config Menu ====================

    /** Describes one configurable slot in the config GUI. */
    private static final class ConfigSlot {
        final String configPath;
        final String label;
        final int defaultVal;
        final int[] options;    // null for bools
        final boolean isPercent;
        final boolean isBool;

        ConfigSlot(String configPath, String label) {
            this.configPath = configPath;
            this.label = label;
            this.defaultVal = 0;
            this.options = null;
            this.isPercent = false;
            this.isBool = true;
        }

        ConfigSlot(String configPath, String label, int defaultVal, int[] options, boolean isPercent) {
            this.configPath = configPath;
            this.label = label;
            this.defaultVal = defaultVal;
            this.options = options;
            this.isPercent = isPercent;
            this.isBool = false;
        }
    }

    private static final Map<Integer, ConfigSlot> CONFIG_SLOTS = new LinkedHashMap<>();
    static {
        // Row 1 (1-5): Game settings    (header at 0)
        CONFIG_SLOTS.put(1,  new ConfigSlot("game.wins-needed",           "Kills Needed",     30, new int[]{5,10,15,20,25,30,50},             false));
        CONFIG_SLOTS.put(2,  new ConfigSlot("game.respawn-time",          "Respawn Time",     5,  new int[]{3,5,10,15,30},                   false));
        CONFIG_SLOTS.put(3,  new ConfigSlot("game.auto-balance-teams",    "Auto Balance"));
        CONFIG_SLOTS.put(4,  new ConfigSlot("game.max-team-difference",   "Max Team Diff",    2,  new int[]{1,2,3,4,5},                     false));
        CONFIG_SLOTS.put(5,  new ConfigSlot("game.auto-start-delay",      "Auto Start Delay", 0,  new int[]{0,10,30,60,120},                false));
        // Row 2 (10-12): Scoring          (header at 9)
        CONFIG_SLOTS.put(10, new ConfigSlot("scoring.kill-points",        "Kill Points",      20, new int[]{5,10,15,20,25,50},               false));
        CONFIG_SLOTS.put(11, new ConfigSlot("scoring.assist-points",      "Assist Points",    10, new int[]{0,5,10,15,20},                  false));
        CONFIG_SLOTS.put(12, new ConfigSlot("scoring.headshot-bonus",     "Headshot Bonus",   5,  new int[]{0,5,10,15,20},                  false));
        // Row 3 (19-21): Respawn          (header at 18)
        CONFIG_SLOTS.put(19, new ConfigSlot("respawn.show-title",             "Show Title"));
        CONFIG_SLOTS.put(20, new ConfigSlot("respawn.show-countdown",         "Show Countdown"));
        CONFIG_SLOTS.put(21, new ConfigSlot("respawn.reset-hunger-on-respawn", "Reset Hunger"));
        // Row 4 (28-29): Rules            (header at 27)
        CONFIG_SLOTS.put(28, new ConfigSlot("rules.friendly-fire",        "Friendly Fire"));
        CONFIG_SLOTS.put(29, new ConfigSlot("rules.tnt-block-damage",     "TNT Block Damage"));
        // Row 5 (37-41): Damage multipliers  (header at 36)
        CONFIG_SLOTS.put(37, new ConfigSlot("damage.global-damage-multiplier",     "Global Boost",    100, new int[]{0,25,50,75,100,125,150,175,200,250,300}, true));
        CONFIG_SLOTS.put(38, new ConfigSlot("damage.fall-damage-multiplier",      "Fall Boost",      100, new int[]{0,25,50,75,100,125,150,175,200},       true));
        CONFIG_SLOTS.put(39, new ConfigSlot("damage.fire-damage-multiplier",      "Fire Boost",      100, new int[]{0,25,50,75,100,125,150,175,200},       true));
        CONFIG_SLOTS.put(40, new ConfigSlot("damage.projectile-damage-multiplier", "Projectile Boost",100, new int[]{0,25,50,75,100,125,150,175,200},       true));
        CONFIG_SLOTS.put(41, new ConfigSlot("damage.melee-damage-multiplier",     "Melee Boost",     100, new int[]{0,25,50,75,100,125,150,175,200},       true));
        // Row 6 (46-48): Features         (header at 45)
        CONFIG_SLOTS.put(46, new ConfigSlot("features.enable-kill-messages",  "Kill Messages"));
        CONFIG_SLOTS.put(47, new ConfigSlot("features.enable-death-messages", "Death Messages"));
        CONFIG_SLOTS.put(48, new ConfigSlot("features.enable-kill-streaks",   "Kill Streaks"));
    }

    // ======================== Build / Open ========================

    @Override
    public void openConfigMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("TDM Config", NamedTextColor.DARK_AQUA));

        // Section headers
        inv.setItem(0,  sectionItem("Game"));
        inv.setItem(9,  sectionItem("Scoring"));
        inv.setItem(18, sectionItem("Respawn"));
        inv.setItem(27, sectionItem("Rules"));
        inv.setItem(36, sectionItem("Damage"));
        inv.setItem(45, sectionItem("Features"));

        // Config items
        for (Map.Entry<Integer, ConfigSlot> e : CONFIG_SLOTS.entrySet()) {
            inv.setItem(e.getKey(), buildItem(e.getValue()));
        }

        // Back button
        inv.setItem(53, makeItem(Material.ARROW, Component.text("Back", NamedTextColor.YELLOW)));

        player.openInventory(inv);
        plugin.setGuiHandler(player.getUniqueId(), (p, s) -> {
            if (s == 53) {
                player.closeInventory();
                // Navigate back to the tournament Installed Minigames menu
                openTournamentMinigames(player);
                return true;
            }
            return handleConfigClick(p, s, inv);
        });
    }

    /** Section header label — not clickable. */
    private static ItemStack sectionItem(String name) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("── " + name + " ──", NamedTextColor.DARK_GRAY, TextDecoration.BOLD));
            item.setItemMeta(meta);
        }
        return item;
    }

    // ======================== Item Builders ========================

    private ItemStack buildItem(ConfigSlot cs) {
        if (cs.isBool) return buildBoolItem(cs);
        return buildValueItem(cs);
    }

    private ItemStack buildBoolItem(ConfigSlot cs) {
        boolean value = plugin.getConfig().getBoolean(cs.configPath, false);
        Material mat = value ? Material.GREEN_CONCRETE : Material.RED_CONCRETE;
        return makeItem(mat,
                Component.text(cs.label, NamedTextColor.WHITE, TextDecoration.BOLD),
                Component.text("Current: ", NamedTextColor.GRAY)
                        .append(Component.text(value ? "ON" : "OFF", value ? NamedTextColor.GREEN : NamedTextColor.RED)),
                Component.text("Click to toggle", NamedTextColor.DARK_GRAY));
    }

    private ItemStack buildValueItem(ConfigSlot cs) {
        int value = readConfigInt(cs);
        String displayVal = cs.isPercent ? value + "%" : String.valueOf(value);
        return makeItem(Material.PAPER,
                Component.text(cs.label, NamedTextColor.WHITE, TextDecoration.BOLD),
                Component.text("Current: " + displayVal, cs.isPercent ? NamedTextColor.AQUA : NamedTextColor.YELLOW),
                Component.text(formatOptions(cs.options, cs.isPercent), NamedTextColor.GRAY),
                Component.text("Click to cycle", NamedTextColor.DARK_GRAY));
    }

    /** Read an int from config, handling both plain ints and doubles (percentages).
     *  Config stores damage multipliers as {@code 1.0} (= 100%) — convert to 100. */
    private int readConfigInt(ConfigSlot cs) {
        Object val = plugin.getConfig().get(cs.configPath, cs.defaultVal);
        if (cs.isPercent && val instanceof Double) {
            // config stores 1.0 for 100%, convert to percentage integer
            return (int) Math.round((Double) val * 100);
        }
        if (val instanceof Integer)  return (Integer) val;
        if (val instanceof Double)   return (int) Math.round((Double) val);
        return cs.defaultVal;
    }

    private static String formatOptions(int[] options, boolean isPercent) {
        if (options.length == 0) return "";
        StringBuilder sb = new StringBuilder("Options: ");
        for (int i = 0; i < options.length; i++) {
            sb.append(isPercent ? options[i] + "%" : options[i]);
            if (i < options.length - 1) sb.append(", ");
        }
        return sb.toString();
    }

    // ======================== Click Handling ========================

    /** @return true if the click was handled (always true for known slots). */
    private boolean handleConfigClick(Player player, int slot, Inventory inv) {
        ConfigSlot cs = CONFIG_SLOTS.get(slot);
        if (cs == null) return false;

        if (cs.isBool) {
            handleBoolClick(player, cs, inv, slot);
        } else {
            handleValueClick(player, cs, inv, slot);
        }
        return true;
    }

    private void handleBoolClick(Player player, ConfigSlot cs, Inventory inv, int slot) {
        // Toggle
        boolean current = plugin.getConfig().getBoolean(cs.configPath, false);
        boolean newVal = !current;
        plugin.getConfig().set(cs.configPath, newVal);
        saveAndReload();

        // Update item in-place
        inv.setItem(slot, buildBoolItem(cs));
        player.sendMessage(Component.text(cs.configPath + " = " + newVal, NamedTextColor.GREEN));
    }

    private void handleValueClick(Player player, ConfigSlot cs, Inventory inv, int slot) {
        // Shift-click → ask for precise value in chat
        if (plugin.isShiftClick(player)) {
            promptPreciseValue(player, cs);
            return;
        }

        // Normal click → cycle to next option
        int current = readConfigInt(cs);
        int nextIdx = 0;
        for (int i = 0; i < cs.options.length; i++) {
            if (cs.options[i] == current) { nextIdx = (i + 1) % cs.options.length; break; }
        }
        int newVal = cs.options[nextIdx];

        plugin.getConfig().set(cs.configPath, newVal);
        saveAndReload();
        inv.setItem(slot, buildValueItem(cs));
        player.sendMessage(Component.text(cs.configPath + " = " + (cs.isPercent ? newVal + "%" : newVal), NamedTextColor.GREEN));
    }

    /** Ask the player to type a precise value in chat. */
    private void promptPreciseValue(Player player, ConfigSlot cs) {
        player.closeInventory();
        String unit = cs.isPercent ? "% (100 = normal, 200 = double)" : "";
        player.sendMessage(Component.text("Type a precise value for \"" + cs.label + "\" " + unit + ":", NamedTextColor.AQUA));
        plugin.setChatInputHandler(player.getUniqueId(), input -> {
            try {
                int value = Integer.parseInt(input.trim());
                if (!cs.isPercent && value < 0) {
                    player.sendMessage(Component.text("Value cannot be negative.", NamedTextColor.RED));
                    return;
                }
                if (cs.isPercent && (value < 0 || value > 1000)) {
                    player.sendMessage(Component.text("Value out of range (0-1000%).", NamedTextColor.RED));
                    return;
                }
                plugin.getConfig().set(cs.configPath, value);
                saveAndReload();
                player.sendMessage(Component.text(cs.configPath + " = " + (cs.isPercent ? value + "%" : value), NamedTextColor.GREEN));
                openConfigMenu(player);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid number.", NamedTextColor.RED));
            }
        });
    }

    /** Persist config and reload GameManager caches. */
    private void saveAndReload() {
        plugin.saveConfig();
        plugin.reloadConfig();
        plugin.getGameManager().loadConfigSettings();
    }

    /** Navigate back to the tournament Installed Minigames menu (next tick to avoid handler races). */
    private void openTournamentMinigames(Player player) {
        TournamentPlugin tp = (TournamentPlugin) Bukkit.getPluginManager().getPlugin("TournamentManager");
        if (tp == null || !tp.isEnabled()) return;
        TournamentPlugin tpRef = tp;
        Bukkit.getScheduler().runTask(plugin, () ->
                tpRef.getAdminGUI().openInstalledMinigames(player));
    }

    private ItemStack makeItem(Material material, Component name, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            if (lore.length > 0) {
                meta.lore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
