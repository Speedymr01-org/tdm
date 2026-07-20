package com.Speedymr01.api.event;

import com.Speedymr01.GameManager.Team;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TDMPlayerJoinGameEventTest {

    @Test
    void constructor_setsPlayerAndTeam() {
        Player player = TestStubs.player("TestPlayer");
        TDMPlayerJoinGameEvent event = new TDMPlayerJoinGameEvent(player, Team.RED);
        assertSame(player, event.getPlayer());
        assertEquals(Team.RED, event.getTeam());
    }

    @Test
    void constructor_blueTeam() {
        Player player = TestStubs.player("BluePlayer");
        TDMPlayerJoinGameEvent event = new TDMPlayerJoinGameEvent(player, Team.BLUE);
        assertEquals(Team.BLUE, event.getTeam());
    }

    @Test
    void getHandlers_returnsStaticHandlerList() {
        Player player = TestStubs.player("Test");
        TDMPlayerJoinGameEvent event = new TDMPlayerJoinGameEvent(player, Team.RED);
        assertNotNull(event.getHandlers());
        assertSame(TDMPlayerJoinGameEvent.getHandlerList(), event.getHandlers());
    }
}
