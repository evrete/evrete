package org.evrete.showcase.newton.rules;

import org.evrete.api.RhsContext;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;
import org.evrete.showcase.newton.model.Particle;
import org.evrete.showcase.newton.model.SpaceTime;
import org.evrete.showcase.newton.model.Vector;

public class MainRuleset {

    @Rule(value = "Init computing acceleration", salience = 10)
    public static void rule1(RhsContext ctx, @Fact("$particle") Particle $p, @Fact("$t") SpaceTime time) {
        // Clearing particle acceleration vector
        $p.acceleration.x = 0.0;
        $p.acceleration.y = 0.0;
    }

    @Rule(value = "Computing acceleration as a vector sum", salience = 9)
    @Where("$subject != $other")
    public static void rule2(RhsContext ctx, @Fact("$subject") Particle $subject, @Fact("$other") Particle $other) {
        double G = 0.0;//ctx.getRuntime().get("G");

        Vector d = $other.position.minus($subject.position);
        double r = d.size();

        // Gravity acceleration, absolute value
        double acc = G * $other.mass / (r * r);

        // Gravity acceleration, vector value
        Vector delta = d.unitVector().multiply(acc);
        Vector current = $subject.acceleration;
        $subject.acceleration = current.plus(delta);
    }

    @Rule(value = "Update position", salience = 8)
    public static void rule3(RhsContext ctx, @Fact("$particle") Particle particle, @Fact("$t") SpaceTime time) {
        double dt = ctx.getRuntime().get("time-step");

        Vector position = particle.position;
        Vector velocity = particle.velocity;

        particle.position = position.plus(velocity.multiply(dt));
    }

    @Rule(value = "Update velocity", salience = 7)
    public static void rule4(RhsContext ctx, @Fact("$particle") Particle particle, @Fact("$t") SpaceTime time) {
        double dt = ctx.getRuntime().get("time-step");

        Vector acceleration = particle.acceleration;
        Vector velocity = particle.velocity;

        particle.velocity = velocity.plus(acceleration.multiply(dt));
    }
}
