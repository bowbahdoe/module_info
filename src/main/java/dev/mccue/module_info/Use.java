package dev.mccue.module_info;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public record Use(
        String service
) {
    public Use {
        Objects.requireNonNull(service);
    }
}
