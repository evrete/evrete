/*
package org.evrete.showcase.abs.town.types;

public class Person {
    public int id;
    public Location home;
    public Location work;
    private State state;
    private XYPoint location;
    private final World world;
    public int nextStateChangeTime = -1;

    public Person(int id, World world, Location home, State initial) {
        this.id = id;
        this.home = home;
        this.world = world;
        this.state = initial;
        world.stateChanged(null, state);
        this.location =  home;
    }

    public void setRandomState(WorldTime time, TransitionManager transitionManager) {
        if (time.absoluteTimeSeconds() < nextStateChangeTime) {
            return;
        }

        State prevState = this.state;
        State newState = transitionManager.newRandomState(prevState, time.hour());

        if (newState == State.WORKING && this.work == null) {
            return;
        }

        if (prevState == newState) {
            return;
        }


        this.nextStateChangeTime = time.absoluteTimeSeconds() + transitionManager.minTimeSeconds(newState);
        this.state = newState;

        this.world.stateChanged(prevState, newState);


        XYPoint oldLocation = this.location;
        XYPoint newLocation;

        switch (newState) {
            case HOME:
                newLocation = home;
                break;
            case WORKING:
                newLocation = work;
                break;
            case SHOPPING:
                newLocation = world.randomShopLocation();
                break;
            default:
                throw new IllegalStateException();

        }
        this.location = newLocation;

        world.addPersonTo(prevState, oldLocation, -1);
        world.addPersonTo(newState, newLocation, 1);
    }

    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                '}';
    }
}
*/
