package io.roach.workload.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class RandomData {
    private static final Logger logger = LoggerFactory.getLogger(RandomData.class);

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private static final ThreadLocalRandom random = ThreadLocalRandom.current();

    private static final List<String> firstNames = new ArrayList<>();

    private static final List<String> lastNames = new ArrayList<>();

    private static final List<String> cities = new ArrayList<>();

    private static final List<String> countries = new ArrayList<>();

    private static final List<String> currencies = new ArrayList<>();

    private static final List<String> states = new ArrayList<>();

    private static final List<String> stateCodes = new ArrayList<>();

    private static final List<String> lorem = new ArrayList<>();

    private static final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    static {
        firstNames.addAll(readLines("random/firstname_female.txt"));
        firstNames.addAll(readLines("random/firstname_male.txt"));
        lastNames.addAll(readLines(("random/surnames.txt")));
        cities.addAll(readLines(("random/cities.txt")));
        states.addAll(readLines(("random/states.txt")));
        stateCodes.addAll(readLines(("random/state_code.txt")));
        lorem.addAll(readLines(("random/lorem.txt")));

        for (Locale locale : Locale.getAvailableLocales()) {
            if (StringUtils.hasLength(locale.getDisplayCountry(Locale.US))) {
                countries.add(locale.getDisplayCountry(Locale.US));
            }
        }

        for (Currency currency : Currency.getAvailableCurrencies()) {
            currencies.add(currency.getCurrencyCode());
        }
    }

    private static List<String> readLines(String path) {
        try (InputStream resource = new ClassPathResource(path).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(resource))) {
            return reader.lines().collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("", e);
        }
        return Collections.emptyList();
    }

    public static Money randomMoneyBetween(String low, String high, Currency currency) {
        return randomMoneyBetween(Double.parseDouble(low), Double.parseDouble(high), currency);
    }

    public static Money randomMoneyBetween(double low, double high, Currency currency) {
        if (high <= low) {
            throw new IllegalArgumentException("high<=low");
        }
        BigDecimal a = BigDecimal.valueOf(Math.max(low, random.nextDouble() * high))
                .setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
        return Money.of(a, currency);
    }

    public static <T extends Enum<?>> T selectRandom(Class<T> clazz) {
        int x = random.nextInt(clazz.getEnumConstants().length);
        return clazz.getEnumConstants()[x];
    }

    public static <E> E selectRandom(List<E> collection) {
        return collection.get(random.nextInt(collection.size()));
    }

    public static <K> K selectRandom(Set<K> set) {
        Object[] keys = set.toArray();
        return (K) keys[random.nextInt(keys.length)];
    }

    public static <E> E selectRandom(E[] collection) {
        return collection[random.nextInt(collection.length)];
    }

    public static <E> Collection<E> selectRandomUnique(List<E> collection, int count) {
        if (count > collection.size()) {
            throw new IllegalArgumentException("Not enough elements");
        }

        Set<E> uniqueElements = new HashSet<>();
        while (uniqueElements.size() < count) {
            uniqueElements.add(selectRandom(collection));
        }

        return uniqueElements;
    }

    public static <E> Collection<E> selectRandomUnique(E[] array, int count) {
        if (count > array.length) {
            throw new IllegalArgumentException("Not enough elements");
        }

        Set<E> uniqueElements = new HashSet<>();
        while (uniqueElements.size() < count) {
            uniqueElements.add(selectRandom(array));
        }

        return uniqueElements;
    }

    public static <E extends WeightedItem> E selectRandomWeighted(Collection<E> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Empty collection");
        }
        double totalWeight = items.stream().mapToDouble(WeightedItem::getWeight).sum();
        double randomWeight = random.nextDouble() * totalWeight;
        double cumulativeWeight = 0;

        for (E item : items) {
            cumulativeWeight += item.getWeight();
            if (cumulativeWeight >= randomWeight) {
                return item;
            }
        }

        throw new IllegalStateException("This is not possible");
    }

    public static <T> T selectRandomWeighted(Collection<T> items, List<Double> weights) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Empty collection");
        }
        if (items.size() != weights.size()) {
            throw new IllegalArgumentException("Collection and weights mismatch");
        }

        double totalWeight = weights.stream().mapToDouble(w -> w).sum();
        double randomWeight = random.nextDouble() * totalWeight;
        double cumulativeWeight = 0;

        int idx = 0;
        for (T item : items) {
            cumulativeWeight += weights.get(idx++);
            if (cumulativeWeight >= randomWeight) {
                return item;
            }
        }

        throw new IllegalStateException("This is not possible");
    }

    public static String randomUserJson(int items, int nestedItems) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode users = root.putArray("users");

        IntStream.range(0, items).forEach(value -> {
            ObjectNode u = users.addObject();
            u.put("email", randomEmail());
            u.put("firstName", randomFirstName());
            u.put("lastName", randomLastName());
            u.put("telephone", randomPhoneNumber());
            u.put("userName", randomFirstName().toLowerCase());

            ArrayNode addr = u.putArray("addresses");
            IntStream.range(0, nestedItems).forEach(n -> {
                ObjectNode a = addr.addObject();
                a.put("state", randomState());
                a.put("stateCode", randomStateCode());
                a.put("city", randomCity());
                a.put("country", randomCountry());
                a.put("zipCode", randomZipCode());
            });

            users.add(u);
        });

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static int randomInt(int start, int end) {
        return Math.max(start, random.nextInt() * end);
    }

    public static double randomDouble(double start, int end) {
        return Math.max(start, random.nextDouble() * end);
    }

    public static String randomFirstName() {
        return selectRandom(firstNames);
    }

    public static String randomLastName() {
        return selectRandom(lastNames);
    }

    public static String randomCity() {
        return StringUtils.capitalize(selectRandom(cities));
    }

    public static String randomPhoneNumber() {
        StringBuilder sb = new StringBuilder()
                .append("(")
                .append(random.nextInt(9) + 1);
        for (int i = 0; i < 2; i++) {
            sb.append(random.nextInt(10));
        }
        sb.append(") ")
                .append(random.nextInt(9) + 1);
        for (int i = 0; i < 2; i++) {
            sb.append(random.nextInt(10));
        }
        sb.append("-");
        for (int i = 0; i < 4; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    public static String randomCountry() {
        return selectRandom(countries);
    }

    public static String randomCurrency() {
        return selectRandom(currencies);
    }

    public static String randomState() {
        return selectRandom(states);
    }

    public static String randomStateCode() {
        return selectRandom(stateCodes);
    }

    public static String randomZipCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    public static String randomEmail() {
        String sb = randomFirstName().toLowerCase()
                + "."
                + randomLastName().toLowerCase()
                + "@example.com";
        return sb.replace(' ', '.');
    }

    public static String randomLoreIpsum(int min, int max, boolean paragraphs) {
        return new LoreIpsum(min, max, paragraphs).generate();
    }

    public static String randomWord(int min) {
        byte[] buffer = new byte[min];
        random.nextBytes(buffer);
        return encoder.encodeToString(buffer);
    }

    private static final char[] VOWELS = "aeiou".toCharArray();

    private static final char[] CONSONANTS = "bcdfghjklmnpqrstvwxyz".toCharArray();

    private static class User {
        List<Address> addresses = new ArrayList<>();

        private String userName;

        private byte[] password;

        private String firstName;

        private String lastName;

        private String telephone;

        private String email;

        private Address address;
    }

    private static class Address {
        private String address1;

        private String address2;

        private String city;

        private String postcode;

        private String country;
    }

    private static class LoreIpsum {
        private int min = 2;

        private int max = 5;

        private final boolean paragraphs;

        public LoreIpsum(int min, int max, boolean paragraphs) {
            this.min = min;
            this.max = max;
            this.paragraphs = paragraphs;
        }

        public String generate() {
            return StringUtils.capitalize(paragraphs ? getParagraphs(min, max) : getWords(getCount(min, max), false));
        }

        public String getParagraphs(int min, int max) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < getCount(min, max); j++) {
                for (int i = 0; i < random.nextInt(5) + 2; i++) {
                    sb.append(StringUtils.capitalize(getWords(1, false)))
                            .append(getWords(getCount(2, 20), false))
                            .append(". ");
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        }

        private int getCount(int min, int max) {
            min = Math.max(0, min);
            if (max < min) {
                max = min;
            }
            return max != min ? random.nextInt(max - min) + min : min;
        }

        private String getWords(int count, boolean capitalize) {
            StringBuilder sb = new StringBuilder();

            int wordCount = 0;
            while (wordCount < count) {
                String word = lorem.get(random.nextInt(lorem.size()));
                if (capitalize) {
                    if (wordCount == 0 || word.length() > 3) {
                        word = StringUtils.capitalize(word);
                    }
                }
                sb.append(word);
                sb.append(" ");
                wordCount++;
            }
            return sb.toString().trim();
        }
    }
}
