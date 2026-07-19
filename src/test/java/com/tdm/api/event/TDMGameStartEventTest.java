package com.tdm.api.event;

import com.tdm.GameManager.GameMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TDMGameStartEventTest {

    @Test
    void constructor_setsGameMode() {
        TDMGameStartEvent event = new TDMGameStartEvent(GameMode.FREE_FOR_ALL);
        assertEquals(GameMode.FREE_FOR_ALL, event.getGameMode());
    }

    @Test
    void constructor_setsFourVsFour() {
        TDMGameStartEvent event = new TDMGameStartEvent(GameMode.FOUR_VS_FOUR);
        assertEquals(GameMode.FOUR_VS_FOUR, event.getGameMode());
    }

    @Test
    void getHandlers_returnsStaticHandlerList() {
        TDMGameStartEvent event = new TDMGameStartEvent(GameMode.FREE_FOR_ALL);
        assertNotNull(event.getHandlers());
        assertSame(TDMGameStartEvent.getHandlerList(), event.getHandlers());
    }
}
