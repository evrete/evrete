package org.evrete.showcase.newton.rules;

import org.evrete.api.RhsContext;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;
import org.evrete.showcase.newton.model.Particle;
import org.evrete.showcase.newton.model.SpaceTime;
import org.evrete.showcase.newton.model.Vector;

public class MainRuleset {

    @Rule("Init computing acceleration")
    public static void rule1(@Fact("$particle") Particle $subject) {
        // Clearing particle acceleration vector
        $subject.set("acceleration", new Vector());
    }

    @Rule("Computing acceleration as a vector sum")
    @Where("$subject != $other")
    public static void rule2(RhsContext ctx, @Fact("$subject") Particle $subject, @Fact("$other") Particle $other) {
        double G = ctx.getRuntime().get("G");

        Vector d = $other.get("position").minus($subject.get("position"));
        double r = d.size();

        // Gravity acceleration, absolute value
        double acc = G * $other.mass / (r * r);

        // Gravity acceleration, vector value
        Vector delta = d.unitVector().multiply(acc);
        Vector current = $subject.get("acceleration");
        $subject.set("acceleration", current.plus(delta));
    }

    @Rule("Update position")
    public static void rule3(RhsContext ctx, @Fact("$particle") Particle particle) {
        double dt = ctx.getRuntime().get("time-step");

        Vector position = particle.get("position");
        Vector velocity = particle.get("velocity");

        particle.set("position", position.plus(velocity.multiply(dt)));
    }

    @Rule("Update velocity")
    public static void rule4(RhsContext ctx, @Fact("$particle") Particle particle) {
        double dt = ctx.getRuntime().get("time-step");

        Vector acceleration = particle.get("acceleration");
        Vector velocity = particle.get("velocity");

        particle.set("velocity", velocity.plus(acceleration.multiply(dt)));
    }

    @Rule("Update time")
    public static void rule5(RhsContext ctx, @Fact("$t") SpaceTime time) {
        double dt = ctx.getRuntime().get("time-step");
        time.value = time.value + dt;
        // As the time instance is declared in every previous rule,
        // updating it will cause the session to start over
        ctx.updateFact("$t");
    }

}
