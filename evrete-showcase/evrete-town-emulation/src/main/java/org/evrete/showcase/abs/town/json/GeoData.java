package org.evrete.showcase.abs.town.json;

import org.evrete.showcase.abs.town.types.XYPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GeoData {
    public List<XYPoint> homes = new ArrayList<>();
    public List<XYPoint> businesses = new ArrayList<>();

    private static List<XYPoint> randomSubList(List<XYPoint> fullData, float fillRatio) {
        List<XYPoint> selected = new ArrayList<>(fullData);
        Collections.shuffle(selected);
        int homesCount = Math.max((int) (selected.size() * fillRatio), 2);

        selected = selected.subList(0, homesCount);
        return selected;
    }

    public List<XYPoint> randomHomes(float fillRatio) {
        return randomSubList(homes, fillRatio);
    }

    public List<XYPoint> randomBusinesses(float fillRatio) {
        return randomSubList(businesses, fillRatio);
    }
}
