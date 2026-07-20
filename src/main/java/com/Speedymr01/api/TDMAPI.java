package com.Speedymr01.api;

import com.Speedymr01.GameManager;
import com.Speedymr01.GameManager.Team;
import com.Speedymr01.GameManager.GameMode;
import com.Speedymr01.TeamDeathmatchPlugin;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Unofficial API for the TeamDeathmatch plugin.
 * <p>
 * Other plugins (e.g. a tournament plugin) can obtain this API via Bukkit's ServiceManager:
 * <pre>{@code
 * RegisteredServiceProvider<TDMAPI> provider = Bukkit.getServicesManager().getRegistration(TDMAPI.class);
 * if (provider != null) {
 *     TDMAPI tdmAPI = provider.getProvider();
 * }
 * }</pre>
 */
public class TDMAPI {

    private final TeamDeathmatchPlugin plugin;
    private final GameManager gameManager;

    public TDMAPI(TeamDeathmatchPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    // ──────────────────────────────────────────────
    //  Game state
    // ──────────────────────────────────────────────

    /** Returns true if a game is active (players can join). */
    public boolean isGameActive() {
        return gameManager.isGameActive();
    }

    /** Returns true if the game has started (players are fighting). */
    public boolean isGameStarted() {
        return gameManager.isGameStarted();
    }

    /** Returns the current game mode (FREE_FOR_ALL or FOUR_VS_FOUR). */
    public GameMode getCurrentGameMode() {
        return gameManager.getCurrentGameMode();
    }

    /** Returns the plugin instance. */
    public TeamDeathmatchPlugin getPlugin() {
        return plugin;
    }

    // ──────────────────────────────────────────────
    //  Player queries
    // ──────────────────────────────────────────────

    /** Returns the team a player is on, or null if not in a game. */
    public Team getPlayerTeam(UUID playerId) {
        return gameManager.getPlayerTeam(playerId);
    }

    /** Returns the player's current kill count. */
    public int getPlayerKills(UUID playerId) {
        return gameManager.getPlayerKills(playerId);
    }

    /** Returns the player's current assist count. */
    public int getPlayerAssists(UUID playerId) {
        return gameManager.getPlayerAssists(playerId);
    }

    /** Returns the player's current headshot count. */
    public int getPlayerHeadshots(UUID playerId) {
        return gameManager.getPlayerHeadshots(playerId);
    }

    /** Returns the player's current total points. */
    public int getPlayerPoints(UUID playerId) {
        return gameManager.getPlayerPoints(playerId);
    }

    /** Returns the player's current death count. */
    public int getPlayerDeaths(UUID playerId) {
        return gameManager.getPlayerDeaths(playerId);
    }

    /** Returns true if the player is currently in the game. */
    public boolean isPlayerInGame(UUID playerId) {
        return gameManager.isPlayerInGame(playerId);
    }

    /** Returns an unmodifiable set of all player UUIDs currently in the game. */
    public Set<UUID> getAllPlayers() {
        return gameManager.getAllPlayers();
    }

    // ──────────────────────────────────────────────
    //  Team queries
    // ──────────────────────────────────────────────

    /** Returns the current score for the given team. */
    public int getTeamScore(Team team) {
        return gameManager.getTeamScore(team);
    }

    /** Returns the number of wins (team score) needed to win the game. */
    public int getWinsNeeded() {
        return gameManager.getWinsNeeded();
    }

    /** Returns a list of player UUIDs on the given team. */
    public List<UUID> getPlayersInTeam(Team team) {
        return gameManager.getPlayersInTeam(team);
    }

    /** Returns the set of enabled FFA teams. */
    public Set<Team> getEnabledFfaTeams() {
        return gameManager.getEnabledFfaTeams();
    }

    /** Returns a map of team scores (unmodifiable). */
    public Map<Team, Integer> getTeamScores() {
        return gameManager.getTeamScores();
    }

    // ──────────────────────────────────────────────
    //  Game control
    // ──────────────────────────────────────────────

    /**
     * Activates the game so players can join.
     * Must be called before {@link #joinPlayer} and {@link #startGame}.
     */
    public void activateGame() {
        gameManager.activateGame();
    }

    /**
     * Starts the game if it is active and not yet started.
     * @return true if the game was started, false otherwise.
     */
    public boolean startGame() {
        if (!gameManager.isGameActive() || gameManager.isGameStarted()) {
            return false;
        }
        gameManager.startGame();
        return true;
    }

    /** Ends the currently active game. */
    public void endGame() {
        gameManager.endGame();
    }

    /**
     * Adds a player to the game on the specified team.
     * @param player the player to join
     * @param team   the team to join (null for automatic assignment)
     * @return true if the player joined successfully
     */
    public boolean joinPlayer(Player player, Team team) {
        if (!gameManager.isGameActive() || gameManager.isGameStarted()) {
            return false;
        }
        if (gameManager.isPlayerInGame(player.getUniqueId())) {
            return false;
        }
        gameManager.joinGame(player, team);
        return true;
    }

    /**
     * Returns a live list of all online players currently in the game.
     */
    public List<Player> getPlayersInGame() {
        List<Player> result = new ArrayList<>();
        for (UUID uuid : gameManager.getAllPlayers()) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                result.add(p);
            }
        }
        return result;
    }
}
