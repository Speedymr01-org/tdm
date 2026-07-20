package com.Speedymr01;

import com.Speedymr01.api.TDMAPI;
import com.Speedymr01.api.event.TDMGameEndEvent;
import com.tdm.tournament.api.MatchCompleteEvent;
import com.tdm.tournament.api.MinigameProvider;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
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
}
