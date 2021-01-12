package org.evrete.showcase.abs.town.types;

import org.evrete.showcase.abs.town.json.GeoData;
import org.evrete.showcase.abs.town.json.Viewport;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class World {
    public final SecureRandom random = new SecureRandom();
    private final Summary[][][] personCountsByZoom = new Summary[Integer.SIZE - Integer.numberOfLeadingZeros(Viewport.MAX_SIZE)][][];
    public List<Entity> homes = new ArrayList<>();
    public List<Entity> businesses = new ArrayList<>();
    public List<Entity> population = new ArrayList<>();

    public World(GeoData data, float fillRatio) {
        for (int z = 0; z < personCountsByZoom.length; z++) {
            int areaSize = 1 << z;
            personCountsByZoom[z] = new Summary[areaSize][areaSize];
            for (int i = 0; i < areaSize; i++) {
                for (int j = 0; j < areaSize; j++) {
                    personCountsByZoom[z][i][j] = new Summary();
                }
            }
        }

        List<XYPoint> homesInUse = data.randomHomes(fillRatio);
        List<XYPoint> businessesInUse = data.randomBusinesses(fillRatio);

        // Seed businesses
        for (XYPoint point : businessesInUse) {
            Entity business = new Entity("business");
            business.set("x", point.x);
            business.set("y", point.y);
            this.businesses.add(business);
        }

        //TODO remove id
        int residentId = 0;
        for (XYPoint point : homesInUse) {
            Entity home = new Entity("home");
            home.set("x", point.x);
            home.set("y", point.y);
            // Defining residents
            int count = random.nextInt(5) + 1;
            for (int p = 0; p < count; p++) {
                Entity resident = new Entity("person");
                resident.set("id", residentId++);
                // Working status
                resident.set("work", randomWorkPlace());
                // Resident's home
                resident.set("home", home);
                // Initial location
                resident.set("location", home);

                population.add(resident);
            }
            this.homes.add(home);
        }

    }

    private Entity randomBusiness() {
        return this.businesses.get(random.nextInt(this.businesses.size()));
    }

    public Entity randomWorkPlace() {
        if (random.nextInt(100) < 20) {
            return randomBusiness();
        } else {
            return null;
        }

    }

    public int randomGaussian(int mean, int stdDev) {
        if (stdDev <= 0) {
            throw new IllegalArgumentException("Standard deviation must be a positive value");
        }
        if (mean <= 0) {
            throw new IllegalArgumentException("In this project mean must be a positive value.");
        }
        int rand = (int) (random.nextGaussian() * stdDev + mean);
        if (rand <= 0) {
            rand = randomGaussian(mean, stdDev);
        }
        return rand;
    }

    public Entity randomShopLocation() {
        return businesses.get(random.nextInt(businesses.size()));
    }

    public Summary getCellSummary(int zoom, int zoomedX, int zoomedY) {
        Summary[][] counts = personCountsByZoom[zoom];
        return counts[zoomedX][zoomedY];
    }

/*
    void addPersonTo(State state, XYPoint point, int count) {
        Objects.requireNonNull(point);
        for (int zoom = 0; zoom < personCountsByZoom.length; zoom++) {
            Summary[][] counts = personCountsByZoom[zoom];
            int div = Viewport.MAX_SIZE >> zoom;


            int zoomedX = point.x / div;
            int zoomedY = point.y / div;
            counts[zoomedX][zoomedY].add(state, count);
        }
    }
*/

    void stateChanged(State from, State to) {


    }

}
