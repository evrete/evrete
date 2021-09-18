package org.evrete.jsr94;

import org.evrete.api.RuntimeContext;
import org.evrete.api.StatefulSession;

import javax.rules.InvalidRuleSessionException;
import javax.rules.admin.RuleExecutionSetCreateException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

final class Utils {
    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());

    static void setProperty(RuntimeContext<?> ctx, Object key, Object value) {
        if (key == null) {
            LOGGER.warning("Null property keys are not supported");
        }
        if (key instanceof String) {
            ctx.set((String) key, value);
        } else {
            LOGGER.warning("Non-string property keys are not supported");
        }
    }

    static Object getProperty(RuntimeContext<?> ctx, Object key) {
        if (key instanceof String) {
            return ctx.get((String) key);
        } else {
            LOGGER.warning("Non-string property keys are not supported");
            return null;
        }
    }

    static String getStringProperty(Map<?, ?> map, String property) {
        if (map == null) return null;
        Object o = map.get(property);
        if (o instanceof String) {
            return (String) o;
        } else {
            return null;
        }
    }


    static String dslName(Map map) throws RuleExecutionSetCreateException {
        String dsl = Utils.getStringProperty(map, Const.DSL_NAME);
        if (dsl == null) {
            throw new RuleExecutionSetCreateException("Missing DSL name property '" + Const.DSL_NAME + "'");
        } else {
            return dsl;
        }
    }

    static List<?> sessionObjects(StatefulSession delegate) throws InvalidRuleSessionException {
        try {
            List<Object> response = new LinkedList<>();
            delegate.forEachFact((handle, o) -> response.add(o));
            return response;
        } catch (Exception e) {
            throw new InvalidRuleSessionException(e.getMessage(), e);
        }
    }

    static void copyConfiguration(RuntimeContext<?> destination, Map<?, ?> from) {
        if (from != null) {
            from.forEach((BiConsumer<Object, Object>) (key, val) -> {
                if ((key instanceof String) && (val instanceof String)) {
                    destination.getConfiguration().setProperty((String) key, (String) val);
                }
            });
        }
    }
}
