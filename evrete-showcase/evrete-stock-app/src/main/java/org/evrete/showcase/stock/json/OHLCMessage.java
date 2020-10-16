package org.evrete.showcase.stock.json;

import org.evrete.showcase.stock.OHLC;

public class OHLCMessage extends JsonMessage {
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
