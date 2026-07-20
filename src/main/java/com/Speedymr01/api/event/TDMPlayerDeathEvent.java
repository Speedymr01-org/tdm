package com.Speedymr01.api.event;

import com.Speedymr01.GameManager.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a player dies in a TeamDeathmatch game.
 */
public class TDMPlayerDeathEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player victim;
    private final Player killer;
    private final Team victimTeam;
    private final Team killerTeam;
    private final int victimKills;
    private final int victimDeaths;
    private final EntityDamageEvent.DamageCause cause;

    public TDMPlayerDeathEvent(Player victim, Player killer, Team victimTeam, Team killerTeam,
                               int victimKills, int victimDeaths, EntityDamageEvent.DamageCause cause) {
        this.victim = victim;
        this.killer = killer;
        this.victimTeam = victimTeam;
        this.killerTeam = killerTeam;
        this.victimKills = victimKills;
        this.victimDeaths = victimDeaths;
        this.cause = cause;
    }

    /** The player who died. */
    public Player getVictim() {
        return victim;
    }

    /** The player who killed the victim, or null if the death was environmental. */
    public @Nullable Player getKiller() {
        return killer;
    }

    /** The victim's team. */
    public Team getVictimTeam() {
        return victimTeam;
    }

    /** The killer's team, or null if the death was environmental. */
    public @Nullable Team getKillerTeam() {
        return killerTeam;
    }

    /** The victim's kill count at the time of death. */
    public int getVictimKills() {
        return victimKills;
    }

    /** The victim's death count at the time of death (includes this death). */
    public int getVictimDeaths() {
        return victimDeaths;
    }

    /** The cause of death. */
    public EntityDamageEvent.DamageCause getCause() {
        return cause;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
