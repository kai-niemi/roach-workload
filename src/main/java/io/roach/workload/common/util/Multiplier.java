package io.roach.workload.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Multiplier {
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^[+-]?([0-9]+\\.?[0-9]*|\\.[0-9]+)\\s?([KMG]+)");

    private static final Pattern INTEGER_PATTERN = Pattern.compile("^[+-]?([0-9]+)\\s?([KMG]+)");

    private Multiplier() {
    }

    public static double parseDouble(String expression) throws NumberFormatException {
        expression = expression.replace("_", "");
        Matcher matcher = DECIMAL_PATTERN.matcher(expression);
        double value = 0;
        while (matcher.find()) {
            value = Double.parseDouble(matcher.group(1));
            String token = matcher.group(2);
            switch (token) {
                case "K":
                    value = 1000 * value;
                    break;
                case "M":
                    value = 1_000_000 * value;
                    break;
                case "G":
                    value = 1_000_000_000 * value;
                    break;
                default:
                    throw new NumberFormatException("Invalid token " + token);
            }
        }
        if (value == 0) {
            return Double.parseDouble(expression);
        }
        return value;
    }

    public static int parseInt(String expression) throws NumberFormatException {
        expression = expression.replace("_", "");
        Matcher matcher = INTEGER_PATTERN.matcher(expression);
        int value = 0;
        while (matcher.find()) {
            value = Integer.parseInt(matcher.group(1));
            String token = matcher.group(2);
            switch (token) {
                case "K":
                    value = 1000 * value;
                    break;
                case "M":
                    value = 1_000_000 * value;
                    break;
                case "G":
                    value = 1_000_000_000 * value;
                    break;
                default:
                    throw new NumberFormatException("Invalid token " + token);
            }
        }
        if (value == 0) {
            return Integer.parseInt(expression);
        }
        return value;
    }

}
