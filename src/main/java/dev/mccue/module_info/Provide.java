package dev.mccue.module_info;

import java.util.List;
import java.util.Objects;

public record Provide(
        String service,
        List<String> with
) {
    public Provide(
            String service,
            List<String> with
    ) {
        this.service = Objects.requireNonNull(service);
        this.with = List.copyOf(with);
    }

    public Provide(
            String service
    ) {
        this(service, List.of());
    }
}
