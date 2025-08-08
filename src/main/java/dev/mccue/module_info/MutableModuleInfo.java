package dev.mccue.module_info;


import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/// Mutable version of a {@link ModuleInfo}.
///
/// This is not directly constructible and is only meant to be used as a temporary
/// object by {@link ModuleInfo#with(Consumer)} with all the fields to be later
/// fed back into {@link ModuleInfo}'s canonical constructor.
public final class MutableModuleInfo {
    public String name;
    public List<Export> exports;
    public List<Require> requires;
    public List<Provide> provides;
    public List<Use> uses;
    public Optional<String> version;
    public boolean open;
    public boolean synthetic;
    public boolean mandated;
    public List<Package> packages;
    public List<Hash> hashes;
    public Optional<String> mainClass;
    public Optional<String> targetPlatform;

    MutableModuleInfo(ModuleInfo moduleInfo) {
        this.name = moduleInfo.name();
        this.exports = moduleInfo.exports();
        this.requires = moduleInfo.requires();
        this.provides = moduleInfo.provides();
        this.uses = moduleInfo.uses();
        this.version = moduleInfo.version();
        this.open = moduleInfo.open();
        this.synthetic = moduleInfo.synthetic();
        this.mandated = moduleInfo.mandated();
        this.packages = moduleInfo.packages();
        this.hashes = moduleInfo.hashes();
        this.mainClass = moduleInfo.mainClass();
        this.targetPlatform = moduleInfo.targetPlatform();
    }

    ModuleInfo finish() {
        return new ModuleInfo(
                name, exports, requires, provides, uses, version,
                open, synthetic, mandated,
                packages, hashes, mainClass, targetPlatform
        );
    }
}
