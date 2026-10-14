package io.github.hunter1712.infusedmobs.ability.trigger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobTickScheduleTest {

    @Test
    void firesOnceEveryTwentyTicks() {
        int counter = 0;
        int fires = 0;
        for (int i = 0; i < 40; i++) {
            counter = MobTickTrigger.advance(counter);
            if (MobTickTrigger.shouldFire(counter)) fires++;
        }
        assertEquals(2, fires);
    }

    @Test
    void onlyZeroFires() {
        assertTrue(MobTickTrigger.shouldFire(0));
        assertFalse(MobTickTrigger.shouldFire(1));
        assertFalse(MobTickTrigger.shouldFire(19));
    }
}
