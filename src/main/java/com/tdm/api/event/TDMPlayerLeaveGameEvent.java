package com.tdm.api.event;

import com.tdm.GameManager.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player leaves a TeamDeathmatch game.
 */
public class TDMPlayerLeaveGameEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Team team;

    public TDMPlayerLeaveGameEvent(Player player, Team team) {
        this.player = player;
        this.team = team;
    }

    /** The player who left. */
    public Player getPlayer() {
        return player;
    }

    /** The team the player was on. */
    public Team getTeam() {
        return team;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
