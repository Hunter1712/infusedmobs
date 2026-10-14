package io.github.hunter1712.infusedmobs.ability.trigger;

import io.github.hunter1712.infusedmobs.platform.Platform;
import io.github.hunter1712.infusedmobs.test.FakePlatform;
import io.github.hunter1712.infusedmobs.test.IsolatedState;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** All TriggerType paths register through the Platform seam. */
@ExtendWith(IsolatedState.class)
class TriggerRegistrationTest {

    @Test
    void tickRegistersThroughSeam() {
        FakePlatform fake = new FakePlatform();
        Platform.setProvider(fake);
        assertNull(fake.tickHandler);
        MobTickTrigger.register();
        assertNotNull(fake.tickHandler);
    }

    @Test
    void deathRegistersThroughSeam() {
        FakePlatform fake = new FakePlatform();
        Platform.setProvider(fake);
        assertNull(fake.deathHandler);
        MobDeathTrigger.register();
        assertNotNull(fake.deathHandler);
    }

    @Test
    void killerResolvesOnlyLiving() {
        assertNull(MobDeathTrigger.resolveKiller(null));
    }
}
