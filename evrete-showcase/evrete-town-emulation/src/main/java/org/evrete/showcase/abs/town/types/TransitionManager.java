/*
package org.evrete.showcase.abs.town.types;

import org.evrete.showcase.abs.town.AppContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.security.SecureRandom;
import java.util.*;

public class TransitionManager {
    private final EnumMap<State, Integer> minTimes = new EnumMap<>(State.class);
    private final EnumMap<State, TreeMap<Integer, RandomStateGenerator>> grouping = new EnumMap<>(State.class);

    public TransitionManager(String configXml, int initialIntervalSeconds) {
        Document config = AppContext.buildConfigXml(configXml);
        XPath xPath = XPathFactory.newInstance().newXPath();

        // Read minimum times
        List<Element> minTimes = elements(config, xPath, "/config/min-time/state");
        for (Element element : minTimes) {
            this.minTimes.put(
                    State.valueOf(element.getAttribute("id")),
                    Integer.parseInt(element.getAttribute("minutes"))
            );
        }

        // Read transition probabilities
        List<Element> probabilities = elements(config, xPath, "/config/transitions/from/time/to");
        for (Element prob : probabilities) {
            double value = Double.parseDouble(prob.getAttribute("probability"));
            if (value < 0 || value > 1.0) {
                throw new IllegalArgumentException("Invalid probability: " + value);
            }

            Element hourNode = (Element) prob.getParentNode();
            Element fromNode = (Element) hourNode.getParentNode();
            State fromState = State.valueOf(fromNode.getAttribute("id"));
            State toState = State.valueOf(prob.getAttribute("id"));
            int hour = Integer.parseInt(hourNode.getAttribute("hour"));

            if (fromState != toState) {
                this.grouping
                        .computeIfAbsent(fromState, k -> new TreeMap<>())
                        .computeIfAbsent(hour, k -> new RandomStateGenerator(fromState))
                        .set(toState, value);
            }
        }
        // Computing how many hours each probability record spans
        for (TreeMap<Integer, RandomStateGenerator> map : grouping.values()) {
            for(Map.Entry<Integer, RandomStateGenerator> entry : map.entrySet()) {
                int hourInConfig = entry.getKey();
                RandomStateGenerator generator = entry.getValue();

                int nextHour = (hourInConfig + 1) % 24;
                while (nextHour != hourInConfig) {
                    if(map.containsKey(nextHour)) {
                        break;
                    } else {
                        generator.spanInHours++;
                    }
                    nextHour = (nextHour + 1) % 24;
                }
            }
        }


        // Filling hour gaps for the sake of performance
        for (TreeMap<Integer, RandomStateGenerator> map : grouping.values()) {
            for (int hour = 23; hour >= 0; hour--) {
                if (!map.containsKey(hour)) {
                    // Gap is found
                    Integer prev = map.floorKey(hour);
                    if (prev == null) {
                        prev = map.floorKey(24); // Because hours are cycled
                    }

                    if (prev == null) {
                        throw new IllegalStateException();
                    } else {
                        map.put(hour, map.get(prev));
                    }
                }
            }
        }

        // Set initial delta time
        setInterval(initialIntervalSeconds);
    }

    int minTimeSeconds(State state) {
        return minTimes.get(state) * 60;
    }

    public void setInterval(int interval) {
        for (TreeMap<Integer, RandomStateGenerator> map : grouping.values()) {
            for (RandomStateGenerator generator : map.values()) {
                generator.adjust(interval);
            }
        }
    }

    public State newRandomState(State initialState, int hour) {
        return this.grouping.get(initialState).get(hour).random();
    }

    private static List<Element> elements(Document document, XPath xPath, String expression) {
        try {
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(document, XPathConstants.NODESET);
            List<Element> result = new ArrayList<>(nodeList.getLength());
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element e = (Element) nodeList.item(i);
                result.add(e);
            }
            return result;
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String toString() {
        return "TransitionManager{" +
                "grouping=" + grouping +
                '}';
    }

    static class RandomStateGenerator {
        private final double[] probabilities = new double[State.values().length];
        private final double[] adjustedProbabilities = new double[State.values().length];
        private final SecureRandom random = new SecureRandom();
        private final State fromState;
        private int spanInHours = 1;

        public RandomStateGenerator(State fromState) {
            this.fromState = fromState;
        }

        void adjust(int deltaSeconds) {
            // As we're dealing with probability density, probabilities need to be adjusted to the delta time
            double adjust = 1.0 * deltaSeconds / (3600 * spanInHours);
            double sum = 0.0;
            for (State state : State.values()) {
                if (state != fromState) {
                    double p = probabilities[state.ordinal()] * adjust;
                    adjustedProbabilities[state.ordinal()] = p;
                    sum += p;
                }
            }
            adjustedProbabilities[fromState.ordinal()] = 1.0 - sum;
        }

        void set(State state, double probability) {
            this.probabilities[state.ordinal()] = probability;
        }

        @Override
        public String toString() {
            return "{probabilities=" + Arrays.toString(probabilities) +
                    ", adjusted=" + Arrays.toString(adjustedProbabilities) +
                    ", span=" + spanInHours +
                    '}';
        }

        State random() {
            double rand = random.nextDouble();
            double sum = 0.0;
            for (State state : State.values()) {
                sum += adjustedProbabilities[state.ordinal()];
                if (sum >= rand) {
                    return state;
                }
            }
            return fromState; // Just in case, the algorithm should never get to this point
        }
    }
}
*/
