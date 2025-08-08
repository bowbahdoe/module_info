package dev.mccue.module_info;

import java.util.Optional;
import java.util.function.Consumer;

/// Mutable version of a {@link Require}.
///
/// This is not directly constructible and is only meant to be used as a temporary
/// object by {@link Require#with(Consumer)} with all the fields to be later
/// fed back into {@link Require}'s canonical constructor.
public final class MutableRequire {
    public Module module;
    public Optional<String> version;
    public boolean static_;
    public boolean transitive;
    public boolean mandated;
    public boolean synthetic;

    MutableRequire(Require require) {
        module = require.module();
        version = require.version();
        static_ = require.static_();
        transitive = require.transitive();
        mandated = require.mandated();
        synthetic = require.synthetic();
    }

    Require finish() {
        return new Require(
                module,
                version,
                static_,
                transitive,
                mandated,
                synthetic
        );
    }
}