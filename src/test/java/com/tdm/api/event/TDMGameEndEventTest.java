package com.tdm.api.event;

import com.tdm.GameManager.Team;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TDMGameEndEventTest {

    @Test
    void constructor_setsWinner() {
        Map<Team, Integer> scores = new HashMap<>();
        scores.put(Team.RED, 5);
        scores.put(Team.BLUE, 3);
        List<Map.Entry<UUID, Integer>> rankings = new ArrayList<>();

        TDMGameEndEvent event = new TDMGameEndEvent(Team.RED, rankings, scores);
        assertEquals(Team.RED, event.getWinner());
    }

    @Test
    void constructor_setsRankings() {
        Map<Team, Integer> scores = new HashMap<>();
        scores.put(Team.RED, 5);
        scores.put(Team.BLUE, 3);

        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        List<Map.Entry<UUID, Integer>> rankings = new ArrayList<>();
        rankings.add(new AbstractMap.SimpleEntry<>(p1, 100));
        rankings.add(new AbstractMap.SimpleEntry<>(p2, 50));

        TDMGameEndEvent event = new TDMGameEndEvent(Team.RED, rankings, scores);
        assertEquals(2, event.getRankings().size());
        assertEquals(p1, event.getRankings().get(0).getKey());
        assertEquals(100, event.getRankings().get(0).getValue());
        assertEquals(p2, event.getRankings().get(1).getKey());
        assertEquals(50, event.getRankings().get(1).getValue());
    }

    @Test
    void constructor_protectsTeamScores() {
        Map<Team, Integer> scores = new HashMap<>();
        scores.put(Team.RED, 5);
        scores.put(Team.BLUE, 3);
        List<Map.Entry<UUID, Integer>> rankings = new ArrayList<>();

        TDMGameEndEvent event = new TDMGameEndEvent(Team.RED, rankings, scores);

        // Should return unmodifiable map
        assertThrows(UnsupportedOperationException.class, () ->
                event.getTeamScores().put(Team.GREEN, 1));
    }

    @Test
    void getHandlers_returnsStaticHandlerList() {
        TDMGameEndEvent event = new TDMGameEndEvent(Team.RED, new ArrayList<>(), new HashMap<>());
        assertNotNull(event.getHandlers());
        assertSame(TDMGameEndEvent.getHandlerList(), event.getHandlers());
    }
}
