package dev.mccue.module_info;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

public record ExportTo(@JsonValue Module module) {
    public ExportTo {
        Objects.requireNonNull(module);
    }

    @JsonCreator
    public ExportTo(String module) {
        this(new Module(module));
    }
}
