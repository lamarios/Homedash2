package com.ftpix.homedash.plugins.pihole.models;

import java.util.stream.Stream;

public enum AnswerType {
    GRAVITY("Gravity List", 1, true),
    FORWARDED("Forwared", 2, false),
    LOCAL_CACHE("Local cache", 3, false),
    WILDCARD("Wildcard", 4, true);

    private final String label;
    private final int mapping;
    private final boolean blocked;

    AnswerType(String label, int mapping, boolean blocked) {
        this.label = label;
        this.mapping = mapping;
        this.blocked = blocked;
    }

    public String getLabel() {
        return label;
    }

    public int getMapping() {
        return mapping;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public static AnswerType getByMapping(int mapping) {
        return Stream.of(AnswerType.values())
                .filter(m -> m.getMapping() == mapping)
                .findFirst()
                .get();
    }

    @Override
    public String toString() {
        return label;
    }
}
