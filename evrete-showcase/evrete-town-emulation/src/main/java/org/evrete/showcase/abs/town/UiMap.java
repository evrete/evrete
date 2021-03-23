package org.evrete.showcase.abs.town;

import org.evrete.showcase.abs.town.json.Area;
import org.evrete.showcase.abs.town.json.StateMessage;
import org.evrete.showcase.abs.town.json.Viewport;
import org.evrete.showcase.abs.town.types.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UiMap {
    private final WorldTime time;
    private final World world;
    private final Map<String, Integer> cache = new HashMap<>();
    private final List<XYPoint> scanScope = new ArrayList<>();
    private final UiStateCounts peopleLocations = new UiStateCounts();
    private final UiStateCounts buildingLocations = new UiStateCounts();
    private int areaSize;
    private int responseZoom;
    private int roundBits;
    private boolean resetMessage = true;

    UiMap(World world, WorldTime time, Viewport initial) {
        this.time = time;
        this.world = world;
        setViewport(initial);
    }

    private static double sigmoidAdjusted(double x) {
        double sigmoid = 1 / (1 + Math.exp(-x));

        double sig = 2.0 * (sigmoid - 0.5);
        return Math.max(sig, 0.0);
    }

    void setViewport(Viewport viewport) {
        //this.viewport = viewport;
        this.resetMessage = true;
        this.cache.clear();
        int deltaLevel = 5; // 32x32

        this.responseZoom = viewport.zoom + deltaLevel;
        this.areaSize = Viewport.MAX_SIZE >> responseZoom;

        this.scanScope.clear();
        this.roundBits = Viewport.MAX_ZOOM - responseZoom;
        int viewportX = viewport.x;
        int viewportY = viewport.y;


        int x = (viewportX >> this.roundBits) << this.roundBits;

        int viewportSize = Viewport.MAX_SIZE >> viewport.zoom;
        while (x < viewportX + viewportSize) {
            int y = (viewportY >> roundBits) << roundBits;
            while (y < viewportY + viewportSize) {
                scanScope.add(new XYPoint(x, y));
                y += areaSize;
            }
            x += areaSize;
        }


    }

    UiStateCounts getPeopleLocations() {
        return peopleLocations;
    }

    UiStateCounts getBuildingLocations() {
        return buildingLocations;
    }

    StateMessage getState() {

        StateMessage message = new StateMessage(resetMessage, areaSize, time.toString());

        message.setTotal(peopleLocations.getTotal());
        for (XYPoint xy : scanScope) {
            int zoomedX = (xy.x >> roundBits);
            int zoomedY = (xy.y >> roundBits);
            UiStateCounter peopleCounter = peopleLocations.getCellSummary(responseZoom, zoomedX, zoomedY);
            UiStateCounter buildingCounter = buildingLocations.getCellSummary(responseZoom, zoomedX, zoomedY);
            for (LocationState s : LocationState.values()) {
                if (s != LocationState.COMMUTING) {
                    String cellId = "c" + s.ordinal() + "_" + zoomedX + "_" + zoomedY;
                    int personCount = peopleCounter.getCount(s);
                    int locationCount = buildingCounter.getCount(s);
                    if (updateCache(cellId, personCount) || resetMessage) {
                        message.add(s, new Area(cellId, xy.x, xy.y, cellOpacity(s, personCount, locationCount)));
                    }
                }
            }
        }
        this.resetMessage = false;
        return message;
    }

    private double cellOpacity(LocationState s, int personCount, int locationCount) {
        if (personCount == 0 || locationCount == 0) return 0.0;

        double density = 1.0 * personCount / locationCount;


        int totalPopulation = world.population.size();
        int totalHomeLocations = world.homes.size();
        int totalBusinessLocations = world.businesses.size();
        double referenceDensity;
        switch (s) {
            case BUSINESS:
                referenceDensity = 1.0 * totalPopulation / totalBusinessLocations;
                break;
            case RESIDENTIAL:
                referenceDensity = 1.0 * totalPopulation / totalHomeLocations;
                break;
            default:
                throw new IllegalStateException();
        }
        return 0.3 * sigmoidAdjusted(density / referenceDensity);
    }

    private boolean updateCache(String id, int count) {
        Integer cached = cache.get(id);
        if (cached == null || cached != count) {
            cache.put(id, count);
            return true;
        } else {
            return false;
        }

    }

}
