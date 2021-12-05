package io.roach.workload.bank.model;

import java.util.Currency;

public enum Region implements CurrencyCode {
    us_west {
        @Override
        public Currency currency() {
            return Currency.getInstance("USD");
        }
    },
    us_central {
        @Override
        public Currency currency() {
            return Currency.getInstance("USD");
        }
    },
    us_east {
        @Override
        public Currency currency() {
            return Currency.getInstance("USD");
        }
    },
    eu_west {
        @Override
        public Currency currency() {
            return Currency.getInstance("EUR");
        }
    },
    eu_central {
        @Override
        public Currency currency() {
            return Currency.getInstance("EUR");
        }
    },
    eu_east {
        @Override
        public Currency currency() {
            return Currency.getInstance("EUR");
        }
    },
    ap_north {
        @Override
        public Currency currency() {
            return Currency.getInstance("HKD");
        }
    },
    ap_central {
        @Override
        public Currency currency() {
            return Currency.getInstance("SGD");
        }
    },
    ap_south {
        @Override
        public Currency currency() {
            return Currency.getInstance("AUD");
        }
    }
}
