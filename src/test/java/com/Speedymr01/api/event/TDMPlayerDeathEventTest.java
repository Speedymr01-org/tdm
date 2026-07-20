package com.Speedymr01.api.event;

import com.Speedymr01.GameManager.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TDMPlayerDeathEventTest {

    @Test
    void constructor_setsAllFields() {
        Player victim = TestStubs.player("Victim");
        Player killer = TestStubs.player("Killer");
        TDMPlayerDeathEvent event = new TDMPlayerDeathEvent(
                victim, killer, Team.RED, Team.BLUE,
                5, 3, EntityDamageEvent.DamageCause.ENTITY_ATTACK);

        assertSame(victim, event.getVictim());
        assertSame(killer, event.getKiller());
        assertEquals(Team.RED, event.getVictimTeam());
        assertEquals(Team.BLUE, event.getKillerTeam());
        assertEquals(5, event.getVictimKills());
        assertEquals(3, event.getVictimDeaths());
        assertEquals(EntityDamageEvent.DamageCause.ENTITY_ATTACK, event.getCause());
    }

    @Test
    void constructor_nullKiller() {
        Player victim = TestStubs.player("Victim");
        TDMPlayerDeathEvent event = new TDMPlayerDeathEvent(
                victim, null, Team.BLUE, null,
                2, 1, EntityDamageEvent.DamageCause.FALL);

        assertSame(victim, event.getVictim());
        assertNull(event.getKiller());
        assertEquals(Team.BLUE, event.getVictimTeam());
        assertNull(event.getKillerTeam());
    }

    @Test
    void constructor_environmentalDeath() {
        Player victim = TestStubs.player("Victim");
        TDMPlayerDeathEvent event = new TDMPlayerDeathEvent(
                victim, null, Team.RED, null,
                0, 1, EntityDamageEvent.DamageCause.VOID);

        assertEquals(0, event.getVictimKills());
        assertEquals(1, event.getVictimDeaths());
        assertEquals(EntityDamageEvent.DamageCause.VOID, event.getCause());
    }

    @Test
    void getHandlers_returnsStaticHandlerList() {
        Player victim = TestStubs.player("V");
        TDMPlayerDeathEvent event = new TDMPlayerDeathEvent(
                victim, null, Team.RED, null,
                0, 0, EntityDamageEvent.DamageCause.CUSTOM);
        assertNotNull(event.getHandlers());
        assertSame(TDMPlayerDeathEvent.getHandlerList(), event.getHandlers());
    }
}
