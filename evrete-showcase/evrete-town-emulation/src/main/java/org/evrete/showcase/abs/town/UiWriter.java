package org.evrete.showcase.abs.town;

import org.evrete.showcase.abs.town.json.Area;
import org.evrete.showcase.abs.town.json.StateMessage;
import org.evrete.showcase.abs.town.json.Viewport;
import org.evrete.showcase.abs.town.types.*;
import org.evrete.showcase.shared.SocketMessenger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UiWriter {
    private final World world;
    private final WorldTime time;
    private final SocketMessenger messenger;
    private final Map<String, Integer> cache = new HashMap<>();
    private final List<XYPoint> scanScope = new ArrayList<>();
    private Viewport viewport;
    private int areaSize;
    private int responseZoom;
    private boolean fullResponse = true;

    public UiWriter(SocketMessenger messenger, World world, WorldTime time, Viewport initial) {
        this.world = world;
        this.messenger = messenger;
        this.time = time;

        setViewport(initial);
    }

    public void setViewport(Viewport viewport) {
        this.viewport = viewport;
        this.fullResponse = true;
        this.cache.clear();
        int deltaLevel = 6; // 32x32

        this.responseZoom = viewport.zoom + deltaLevel;
        this.areaSize = Viewport.MAX_SIZE >> responseZoom;


        ///
        this.scanScope.clear();
        int roundBis = Viewport.MAX_ZOOM - responseZoom;
        int viewportX = this.viewport.x;
        int viewportY = this.viewport.y;


        int x = (viewportX >> roundBis) << roundBis;

        int viewportSize = Viewport.MAX_SIZE >> viewport.zoom;
        while (x < viewportX + viewportSize) {
            int y = (viewportY >> roundBis) << roundBis;
            while (y < viewportY + viewportSize) {
                scanScope.add(new XYPoint(x, y));
                y += areaSize;
            }
            x += areaSize;
        }


    }

    void writeState() {

        StateMessage message = new StateMessage(fullResponse, areaSize, time.toString());

        // Rounding start positions
        int roundBis = Viewport.MAX_ZOOM - responseZoom;

        for (XYPoint xy : scanScope) {

            int zoomedX = (xy.x >> roundBis);
            int zoomedY = (xy.y >> roundBis);
            Summary summary = world.getCellSummary(responseZoom, zoomedX, zoomedY);
            for (State s : State.values()) {
                String cellId = "c" + s.ordinal() + "_" + zoomedX + "_" + zoomedY;
                int count = summary.getCount(s);
                message.updateMaxCounts(s, count);
                if (!(count == 0 && fullResponse)) {
                    if (valueIsDifferent(cellId, count)) {
                        message.add(s, new Area(cellId, xy.x, xy.y, count));
                    }
                }

            }
        }

        this.messenger.sendUnchecked(message);
        // After a full response and until viewport changes only changes will be sent
        this.fullResponse = false;
    }

    private boolean valueIsDifferent(String id, int count) {
        Integer cached = cache.get(id);
        if (cached == null || cached != count) {
            cache.put(id, count);
            return true;
        } else {
            return false;
        }

    }

}
