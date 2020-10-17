package org.evrete.showcase.stock.json;

import org.evrete.showcase.shared.JsonMessage;
import org.evrete.showcase.stock.OHLC;

public class OHLCMessage extends JsonMessage {
    static final String TYPE_OHLC = "OHLC";
    public OHLC ohlc;

    public OHLCMessage(OHLC ohlc) {
        super(TYPE_OHLC);
        this.ohlc = ohlc;
    }

    @Override
    public String toString() {
        return "OHLCMessage{" +
                "ohlc=" + ohlc +
                '}';
    }
}
