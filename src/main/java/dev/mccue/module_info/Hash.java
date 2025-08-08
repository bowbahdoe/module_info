package dev.mccue.module_info;

import java.util.List;
import java.util.Objects;

public record Hash(
        String algorithm,
        List<ModuleHash> hashes) {
    public Hash(String algorithm, List<ModuleHash> hashes) {
        this.algorithm = Objects.requireNonNull(algorithm);
        this.hashes = List.copyOf(hashes);
    }
}
