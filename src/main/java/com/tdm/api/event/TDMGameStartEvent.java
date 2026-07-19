package com.tdm.api.event;

import com.tdm.GameManager.GameMode;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a TeamDeathmatch game starts (players begin fighting).
 */
public class TDMGameStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GameMode gameMode;

    public TDMGameStartEvent(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    /** The mode the game was started in. */
    public GameMode getGameMode() {
        return gameMode;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
