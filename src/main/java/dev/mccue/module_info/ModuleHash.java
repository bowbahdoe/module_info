package dev.mccue.module_info;

import java.util.Objects;

public record ModuleHash(
        Module module,
        String hash
) {
    public ModuleHash {
        Objects.requireNonNull(module);
        Objects.requireNonNull(hash);
    }
}
