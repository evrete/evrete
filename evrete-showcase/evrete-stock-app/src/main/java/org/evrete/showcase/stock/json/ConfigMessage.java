package org.evrete.showcase.stock.json;

import org.evrete.showcase.shared.JsonMessage;
import org.evrete.showcase.stock.OHLC;

public class ConfigMessage extends JsonMessage {
    String rules;

    OHLC[] prices;

    public ConfigMessage(String rules, OHLC[] prices) {
        super("CONFIG");
        this.rules = rules;
        this.prices = prices;
    }
}
