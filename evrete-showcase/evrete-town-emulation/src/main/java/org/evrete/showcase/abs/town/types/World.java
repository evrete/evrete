package org.evrete.showcase.abs.town.types;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import org.evrete.showcase.abs.town.json.GeoData;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class World {
    public List<Entity> homes = new ArrayList<>();
    public List<Entity> businesses = new ArrayList<>();
    public List<Entity> population = new ArrayList<>();
    private final double shoppingProbability;

    private World(double shoppingProbability) {
        this.shoppingProbability = shoppingProbability;
    }

    public static World factory(GeoData data, float fillRatio, float workingPeopleRatio, double shoppingProbability) {
        World world = new World(shoppingProbability);

        List<XYPoint> homesInUse = data.randomHomes(fillRatio);
        List<XYPoint> businessesInUse = data.randomBusinesses(fillRatio);

        // Seed businesses
        //TODO remove id
        int businessId = 0;
        for (XYPoint point : businessesInUse) {
            Entity business = new Entity(LocationState.BUSINESS.name());
            business.set("x", point.x);
            business.set("y", point.y);
            business.set("bid", businessId++);
            world.businesses.add(business);
        }

        int residentId = 0;
        int homeId = 0;
        //TODO remove id
        for (XYPoint point : homesInUse) {
            Entity home = new Entity(LocationState.RESIDENTIAL.name());
            home.set("hid", homeId++);
            home.set("x", point.x);
            home.set("y", point.y);
            // Defining residents
            int count = RandomUtils.randomGaussian1(3, 2, 6);
            for (int p = 0; p < count; p++) {
                Entity resident = new Entity("person");
                resident.set("id", residentId++);
                //Initial configuration
                resident.set("home", home);
                int wakeup = RandomUtils.randomGaussian1(6 * 3600, 4 * 3600, 24 * 3600);
                resident.set("wakeup", wakeup);
                if (RandomUtils.randomBoolean(workingPeopleRatio)) {
                    resident.set("work", RandomUtils.randomListElement(world.businesses));
                }
                world.population.add(resident);
            }
            world.homes.add(home);
        }


        return world;
    }

    public static int randomGaussian(int mean, int stdDev, int maximum) {
        return RandomUtils.randomGaussian1(mean, stdDev, maximum);
    }

    public static World readFromFile() {
        try {
            File file = new File("/Users/oswald/Desktop/world.json");
            if (file.exists()) {
                Gson gson = new Gson();
                JsonReader reader = gson.newJsonReader(new FileReader(file));
                return gson.fromJson(reader, World.class);
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public double getShoppingProbability() {
        return shoppingProbability;
    }

    public Entity randomBusiness() {
        return RandomUtils.randomListElement(this.businesses);
    }

    public void saveToFile() {
        try {
            for (Entity person : population) {
                person.tmpClear();
            }
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileWriter writer = new FileWriter("/Users/oswald/Desktop/world.json");
            writer.write(gson.toJson(this));
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
