package io.roach.workload.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MultiplierTest {
    @Test
    public void whenUsingDecimalMultipliers_thenReturnMultipliedValues() {
        Assertions.assertEquals(15.0, Multiplier.parseDouble("15"));
        Assertions.assertEquals(.15, Multiplier.parseDouble(".15"));
        Assertions.assertEquals(-.15, Multiplier.parseDouble("-.15"));
        Assertions.assertEquals(.15, Multiplier.parseDouble("+.15"));
        Assertions.assertEquals(1.15, Multiplier.parseDouble("+1.15"));
        Assertions.assertEquals(-1.15, Multiplier.parseDouble("-1.15"));
        Assertions.assertEquals(15.5 * 1_000, Multiplier.parseDouble("15.5K"));
        Assertions.assertEquals(15.5 * 1_000_000, Multiplier.parseDouble("15.5M"));
        Assertions.assertEquals(0.5 * 1_000_000, Multiplier.parseDouble("0.5M"));
        Assertions.assertEquals(100 * 1_000_000, Multiplier.parseDouble("100M"));
        Assertions.assertEquals(5 * 1_000_000_000.0, Multiplier.parseDouble("5G"));

        Assertions.assertThrows(NumberFormatException.class, () -> Multiplier.parseDouble("100MM"));
        Assertions.assertThrows(NumberFormatException.class, () -> Multiplier.parseDouble("100B"));
        Assertions.assertThrows(NumberFormatException.class, () -> Multiplier.parseDouble("M"));
    }

    @Test
    public void whenUsingIntegerMultipliers_thenReturnMultipliedValues() {
        Assertions.assertEquals(15, Multiplier.parseInt("15"));
        Assertions.assertEquals(-15, Multiplier.parseInt("-15"));
        Assertions.assertEquals(155 * 1_000, Multiplier.parseInt("155K"));
        Assertions.assertEquals(155 * 1_000_000, Multiplier.parseInt("155M"));
        Assertions.assertEquals(5 * 1_000_000, Multiplier.parseInt("5M"));
        Assertions.assertEquals(5 * 100_000, Multiplier.parseInt("500K"));
        Assertions.assertEquals(100 * 1_000_000, Multiplier.parseInt("100M"));

        Assertions.assertThrows(NumberFormatException.class, () -> Multiplier.parseInt("100MM"));
        Assertions.assertThrows(NumberFormatException.class, () -> Multiplier.parseInt("100B"));
        Assertions.assertThrows(NumberFormatException.class, () -> Multiplier.parseInt("M"));
    }
}
