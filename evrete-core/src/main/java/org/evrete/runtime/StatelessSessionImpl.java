package org.evrete.runtime;

import org.evrete.api.FactHandle;
import org.evrete.api.StatelessSession;
import org.evrete.api.Type;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

class StatelessSessionImpl extends AbstractRuleSessionIO<StatelessSession> implements StatelessSession {

    StatelessSessionImpl(KnowledgeRuntime knowledge) {
        super(knowledge);
    }

    @Override
    protected StatelessSession thisInstance() {
        return this;
    }

    @Override
    public void fire(BiConsumer<FactHandle, Object> consumer) {
        try {
            fireInner();
            getMemory().forEach(tm -> tm.forEachFact(consumer));
        } finally {
            closeInner();
        }
    }

    @Override
    public Void fire() {
        try {
            fireInner();
            return null;
        } finally {
            closeInner();
        }
    }

    @Override
    public void fire(Consumer<Object> consumer) {
        try {
            fireInner();
            getMemory().forEach(tm -> tm.forEachFact((handle, o) -> consumer.accept(o)));
        } finally {
            closeInner();
        }
    }


    @Override
    @SuppressWarnings("unchecked")
    public <T> void fire(String type, Consumer<T> consumer) {
        try {
            fireInner();
            Type<?> t = getTypeResolver().getType(type);
            if (t == null) {
                throw new IllegalArgumentException("No known type named '" + type + "'");
            }
            getMemory().get(t).forEachFact((factHandle, o) -> consumer.accept((T) o));
        } finally {
            closeInner();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void fire(Class<T> type, Consumer<T> consumer) {
        try {
            fireInner();
            getMemory().forEach(tm -> {
                if (type.getName().equals(tm.type.getJavaType())) {
                    tm.forEachFact((factHandle, o) -> consumer.accept((T) o));
                }
            });
        } finally {
            closeInner();
        }
    }
}
