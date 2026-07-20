package com.Speedymr01.api.event;

import com.Speedymr01.GameManager.Team;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TDMPlayerLeaveGameEventTest {

    @Test
    void constructor_setsPlayerAndTeam() {
        Player player = TestStubs.player("LeavingPlayer");
        TDMPlayerLeaveGameEvent event = new TDMPlayerLeaveGameEvent(player, Team.RED);
        assertSame(player, event.getPlayer());
        assertEquals(Team.RED, event.getTeam());
    }

    @Test
    void constructor_blueTeam() {
        Player player = TestStubs.player("BlueLeaver");
        TDMPlayerLeaveGameEvent event = new TDMPlayerLeaveGameEvent(player, Team.BLUE);
        assertEquals(Team.BLUE, event.getTeam());
    }

    @Test
    void getHandlers_returnsStaticHandlerList() {
        Player player = TestStubs.player("Test");
        TDMPlayerLeaveGameEvent event = new TDMPlayerLeaveGameEvent(player, Team.RED);
        assertNotNull(event.getHandlers());
        assertSame(TDMPlayerLeaveGameEvent.getHandlerList(), event.getHandlers());
    }
}
