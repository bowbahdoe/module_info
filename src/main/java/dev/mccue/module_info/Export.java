package dev.mccue.module_info;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record Export(
        @JsonProperty("package") Package package_,
        List<ExportTo> to,
        boolean synthetic,
        boolean mandated
) {
    public Export(
            Package package_,
            List<ExportTo> to,
            boolean synthetic,
            boolean mandated
    ) {
        this.package_ = Objects.requireNonNull(package_);
        this.to = List.copyOf(to);
        this.synthetic = synthetic;
        this.mandated = mandated;
    }

    public Export(String package_) {
        this(new Package(package_));
    }

    public Export(Package package_) {
        this(package_, List.of());
    }

    public Export(Package package_, List<ExportTo> exportTo) {
        this(package_, exportTo, false, false);
    }

    public Export(String package_, List<ExportTo> exportTo) {
        this(new Package(package_), exportTo);
    }

    public Export with(Consumer<MutableExport> consumer) {
        var mutable = new MutableExport(this);
        consumer.accept(mutable);
        return mutable.finish();
    }
}
