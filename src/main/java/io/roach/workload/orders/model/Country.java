package io.roach.workload.orders.model;

import java.util.Locale;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Models a country. A country contains a country ISO-3166 code
 * and a display name.
 */
@Embeddable
public class Country implements Comparable {
    /**
     * Returns a new Country instance based on the current locale.
     *
     * @return a new Country instance
     */
    public static Country getDefault() {
        return new Country(Locale.getDefault());
    }

    @Column(length = 255)
    private String code;

    @Column(length = 255)
    private String name;

    public Country() {
    }

    public Country(Locale loc) {
        this(loc.getCountry(), loc.getDisplayCountry());
    }

    /**
     * Creates a new instance of this class.
     *
     * @param code the country code
     * @param name the name of the country
     */
    public Country(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int compareTo(Object o) {
        return this.code.compareTo(((Country) o).code);
    }
}
