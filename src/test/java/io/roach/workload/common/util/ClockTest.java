package io.roach.workload.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClockTest {
    @Test
    public void testBasics() throws Exception {
        Timer clock = new Timer("test");
        Timer.Fork a = clock.fork("a");
        Timer.Fork b = clock.fork("b");
        Timer.Fork c = clock.fork("c");

        Thread.sleep(1500);

        a.stop();
        b.stop();
        c.stop();

        Assertions.assertTrue(a.durationMillis() >= 1_000);
        Assertions.assertTrue(b.durationMillis() >= 1_000);
        Assertions.assertTrue(c.durationMillis() >= 1_000);
        Assertions.assertTrue(clock.totalTime() >= 3 * 1_000);

        System.out.println(clock);
    }
}
