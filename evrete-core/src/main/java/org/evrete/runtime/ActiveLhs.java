package org.evrete.runtime;

import org.evrete.util.CommonUtils;

import java.util.function.Function;

abstract class ActiveLhs<FG extends KnowledgeFactGroup> {
    private final FG[] factGroups;

    protected <FG1 extends KnowledgeFactGroup> ActiveLhs(Class<FG> type, ActiveLhs<FG1> parent, Function<FG1, FG> mapper) {
        this(CommonUtils.mapArray(type, parent.factGroups, mapper));
    }

    protected ActiveLhs(FG[] factGroups) {
        this.factGroups = factGroups;
    }

    public FG[] getFactGroups() {
        return factGroups;
    }
}
