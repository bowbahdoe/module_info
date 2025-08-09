package dev.mccue.module_info;

import java.util.List;
import java.util.function.Consumer;

/// Mutable version of a {@link Export}.
///
/// This is not directly constructible and is only meant to be used as a temporary
/// object by {@link Export#with(Consumer)} with all the fields to be later
/// fed back into {@link Export}'s canonical constructor.
public final class MutableExport {
    /// Corresponds to {@link Export#package_()}
    public Package package_;
    /// Corresponds to {@link Export#to()}
    public List<ExportTo> to;
    /// Corresponds to {@link Export#synthetic()}
    public boolean synthetic;
    /// Corresponds to {@link Export#mandated()}
    public boolean mandated;

    MutableExport(Export export) {
        this.package_ = export.package_();
        this.to = export.to();
        this.synthetic = export.synthetic();
        this.mandated = export.mandated();
    }

    Export freeze() {
        return new Export(
                package_, to, synthetic, mandated
        );
    }
}
