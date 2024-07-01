package org.evrete.dsl;

import org.evrete.api.annotations.Nullable;
import org.evrete.util.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

class ResourceCollection {
    private final Class<?> type;
    private final Collection<Object> resources;

    @SuppressWarnings("unchecked")
    <T> Collection<T> cast() {
        Collection<T> result = new ArrayList<>(resources.size());
        for (Object resource : resources) {
            result.add((T) resource);
        }
        return result;
    }

    ResourceCollection(Class<?> type, Collection<Object> resources) {
        this.type = type;
        this.resources = resources;
    }

    Class<?> getComponentType() {
        return type;
    }

    @Nullable
    static ResourceCollection factory(Object o) {
        Collection<?> col = CommonUtils.toCollection(o);
        if (col.isEmpty()) {
            return null;
        } else {
            Collection<Object> result = new ArrayList<>(col.size());
            Iterator<?> it = col.iterator();
            Object first = it.next();
            result.add(first);
            Class<?> type = first.getClass();
            it.forEachRemaining(object -> {
                if (object != null) {
                    Class<?> c = object.getClass();
                    if (c.isAssignableFrom(type)) {
                        result.add(object);
                    } else {
                        throw new MalformedResourceException("Cannot cast " + object + " to " + type);
                    }
                }
            });

            if(result.isEmpty()) {
                return null;
            } else {
                return new ResourceCollection(type, result);
            }

        }
    }
}
