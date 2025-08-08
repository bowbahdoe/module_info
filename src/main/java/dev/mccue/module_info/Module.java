package dev.mccue.module_info;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

public record Module(@JsonValue String name) {
    @JsonCreator
    public Module {
        Objects.requireNonNull(name);
    }
}
