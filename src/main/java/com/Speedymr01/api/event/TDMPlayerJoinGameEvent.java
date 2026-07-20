package com.Speedymr01.api.event;

import com.Speedymr01.GameManager.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player joins a TeamDeathmatch game.
 */
public class TDMPlayerJoinGameEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Team team;

    public TDMPlayerJoinGameEvent(Player player, Team team) {
        this.player = player;
        this.team = team;
    }

    /** The player who joined. */
    public Player getPlayer() {
        return player;
    }

    /** The team the player was assigned to. */
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
