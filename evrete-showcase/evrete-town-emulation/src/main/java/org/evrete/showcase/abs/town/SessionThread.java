package org.evrete.showcase.abs.town;

import org.evrete.api.StatefulSession;
import org.evrete.showcase.abs.town.types.*;
import org.evrete.showcase.shared.Message;
import org.evrete.showcase.shared.SocketMessenger;
import org.evrete.showcase.shared.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

class SessionThread extends Thread {
    private final StatefulSession session;
    private final int intervalSeconds;
    private final SocketMessenger messenger;
    private final World world;
    private final WorldTime worldTime;
    private final UiMap uiMap;
    private final BooleanSupplier gate;
    private final Map<Entity, Entity> personLocations = new HashMap<>();

    SessionThread(StatefulSession session, int intervalSeconds, SocketMessenger messenger, World world, WorldTime worldTime, UiMap uiMap, BooleanSupplier gate) {
        this.intervalSeconds = intervalSeconds;
        this.messenger = messenger;
        this.world = world;
        this.worldTime = worldTime;
        this.uiMap = uiMap;
        this.gate = gate;
        this.session = session;

        // Initial allocation on the UI map (persons)
        for (Entity person : world.population) {
            Entity currentLocation = locationOf(person);
            personLocations.put(person, currentLocation);
            uiMap.getPeopleLocations().addTo(LocationState.valueOf(currentLocation.type), currentLocation, 1);
        }

        // Initial allocation on the UI map (homes)
        for (Entity location : world.homes) {
            uiMap.getBuildingLocations().addTo(LocationState.valueOf(location.type), location, 1);
        }

        // Initial allocation on the UI map (businesses)
        for (Entity location : world.businesses) {
            uiMap.getBuildingLocations().addTo(LocationState.valueOf(location.type), location, 1);
        }
    }

    private static Entity locationOf(Entity person) {
        return person.getProperty("current_location");
    }

    @Override
    public void run() {
        // Initial fire
        try {
            if (runPredicate()) {
                session.fire();
                processState();
                while (runPredicate()) {
                    Utils.delay(50);
                    session.updateAndFire(worldTime.increment(intervalSeconds));
                    processState();
                }
            }
            messenger.sendUnchecked(new Message("LOG", "Session ended"));
            messenger.sendUnchecked(new Message("END"));
        } catch (Throwable t) {
            messenger.send(t);
        }
    }

    private boolean runPredicate() {
        return gate.getAsBoolean() && worldTime.secondsSinceStart() < 3600 * 24 * 2;
    }

    private void processState() throws IOException {
        UiStateCounts locations = uiMap.getPeopleLocations();
        for (Entity person : world.population) {
            Entity currentLocation = locationOf(person);
            Entity previousLocation = personLocations.get(person);
            if (previousLocation != currentLocation) {
                if (currentLocation == null) {
                    // Person in transit
                    locations.addTo(LocationState.valueOf(previousLocation.type), previousLocation, -1);
                    locations.addTo(LocationState.COMMUTING, previousLocation, 1);
                } else if (previousLocation == null) {
                    // Person arrived
                    locations.addTo(LocationState.COMMUTING, currentLocation, -1);
                    locations.addTo(LocationState.valueOf(currentLocation.type), currentLocation, 1);
                } else {
                    // Teleportation. Won't be used by default
                    locations.addTo(LocationState.valueOf(previousLocation.type), previousLocation, -1);
                    locations.addTo(LocationState.valueOf(currentLocation.type), currentLocation, 1);
                }

                // Save current state
                personLocations.put(person, currentLocation);
            }

        }
        messenger.send(uiMap.getState());
    }
}
