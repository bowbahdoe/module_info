package dev.mccue.module_info;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record Require(
        Module module,
        Optional<String> version,
        @JsonProperty("static") boolean static_,
        boolean transitive,
        boolean mandated,
        boolean synthetic
) {
    public Require(Module module) {
        this(module, Optional.empty(), false, false, false, false);
    }

    public Require {
        Objects.requireNonNull(module);
        Objects.requireNonNull(version);
    }

    public Require(String module) {
        this(new Module(module));
    }

    public Require with(Consumer<MutableRequire> consumer) {
        var mutableRequire = new MutableRequire(this);
        consumer.accept(mutableRequire);
        return mutableRequire.freeze();
    }
}
