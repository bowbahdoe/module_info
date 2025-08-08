package dev.mccue.module_info;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

public record Package(@JsonValue String name) {
    @JsonCreator
    public Package {
        Objects.requireNonNull(name);
    }
}
