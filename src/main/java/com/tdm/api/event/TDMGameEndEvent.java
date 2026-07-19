package com.tdm.api.event;

import com.tdm.GameManager.Team;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Called when a TeamDeathmatch game ends.
 * Contains the winning team, player rankings, and team scores.
 */
public class TDMGameEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Team winner;
    private final List<Map.Entry<UUID, Integer>> rankings;
    private final Map<Team, Integer> teamScores;

    public TDMGameEndEvent(Team winner, List<Map.Entry<UUID, Integer>> rankings, Map<Team, Integer> teamScores) {
        this.winner = winner;
        this.rankings = rankings;
        this.teamScores = teamScores;
    }

    /** The winning team. */
    public Team getWinner() {
        return winner;
    }

    /**
     * Ordered list of player rankings (highest points first).
     * Each entry is a player UUID mapped to their total points.
     */
    public List<Map.Entry<UUID, Integer>> getRankings() {
        return rankings;
    }

    /** The final team scores (unmodifiable). */
    public Map<Team, Integer> getTeamScores() {
        return Collections.unmodifiableMap(teamScores);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
