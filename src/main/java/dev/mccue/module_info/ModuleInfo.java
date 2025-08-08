package dev.mccue.module_info;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.attribute.*;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.lang.constant.ModuleDesc;
import java.lang.constant.PackageDesc;
import java.lang.reflect.AccessFlag;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// Records all the info stored in a `module-info.class`
/// file.
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record ModuleInfo(
        String name,
        List<Export> exports,
        List<Require> requires,
        List<Provide> provides,
        List<Use> uses,
        Optional<String> version,
        boolean open,
        boolean synthetic,
        boolean mandated,
        List<Package> packages,
        List<Hash> hashes,
        Optional<String> mainClass,
        Optional<String> targetPlatform
) {

    public ModuleInfo(
            String name,
            List<Export> exports,
            List<Require> requires,
            List<Provide> provides,
            List<Use> uses,
            Optional<String> version,
            boolean open,
            boolean synthetic,
            boolean mandated,
            List<Package> packages,
            List<Hash> hashes,
            Optional<String> mainClass,
            Optional<String> targetPlatform
    ) {
        this.name = Objects.requireNonNull(name);
        this.exports = List.copyOf(exports);


        // We want every module to have a java.base require that is mandated, regardless
        // of if that is actually declared (...except for modules which themselves are java.base)
        boolean foundJavaBase = false;
        requires = new ArrayList<>(requires);
        for (int i = 0; i < requires.size(); i++) {
            var require = requires.get(i);
            if (require.module().name().equals("java.base")) {
                foundJavaBase = true;
                if (!require.mandated()) {
                    requires.set(i, require.with(r -> r.mandated = true));
                }
            }
        }
        if (!name.equals("java.base") && !foundJavaBase) {
            requires.addFirst(new Require("java.base")
                    .with(r -> r.mandated = true));
        }

        this.requires = List.copyOf(requires);
        this.provides = List.copyOf(provides);
        this.uses = List.copyOf(uses);
        this.version = Objects.requireNonNull(version);
        this.open = open;
        this.synthetic = synthetic;
        this.mandated = mandated;
        this.packages = List.copyOf(packages);
        this.hashes = List.copyOf(hashes);
        this.mainClass = Objects.requireNonNull(mainClass);
        this.targetPlatform = Objects.requireNonNull(targetPlatform);
    }

    public ModuleInfo(String name) {
        this(
                name,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Optional.empty(),
                false,
                false,
                false,
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.empty()
        );
    }

    public ModuleInfo with(Consumer<MutableModuleInfo> consumer) {
        var mutable = new MutableModuleInfo(this);
        consumer.accept(mutable);
        return mutable.finish();
    }

    public static ModuleInfo fromBytes(byte[] bytes) {
        var classModel = ClassFile.of()
                .parse(bytes);
        return from(classModel);
    }

    private static final Predicate<String> ROOT_MODULE_INFO_PATTERN = Pattern.compile("(classes/|)module-info.class")
            .asMatchPredicate();
    private static final Predicate<String> MULTI_RELEASE_MODULE_INFO_PATTERN = Pattern.compile("(classes/|)META-INF/versions/([0-9]+)/module-info.class")
            .asMatchPredicate();

    public static Optional<ModuleInfo> from(ZipFile jarFile) throws IOException {
        var entries = jarFile.entries();

        boolean foundRoot = false;
        ZipEntry entryToUse = null;
        while (entries.hasMoreElements()) {
            var entry = entries.nextElement();
            // classes/ catches modules in jmods as well
            if (ROOT_MODULE_INFO_PATTERN.test(entry.getName())) {
                entryToUse = entry;
                foundRoot = true;
            }
            if (MULTI_RELEASE_MODULE_INFO_PATTERN.test(entry.getName())) {
                if (!foundRoot) {
                    entryToUse = entry;
                }
            }
        }
        if (entryToUse != null) {
            try (var is = jarFile.getInputStream(entryToUse)) {
                return Optional.of(ModuleInfo.fromBytes(is.readAllBytes()));
            }
        }


        return Optional.empty();
    }

    public static Optional<ModuleInfo> fromJarInputStream(JarInputStream jarInputStream) throws Exception {
        var entry = jarInputStream.getNextJarEntry();
        record EntryAndBytes(JarEntry e, byte[] bytes) {
        }
        boolean foundRoot = false;
        EntryAndBytes entryToUse = null;
        for (; entry != null; entry = jarInputStream.getNextJarEntry()) {
            if (ROOT_MODULE_INFO_PATTERN.test(entry.getName())) {
                entryToUse = new EntryAndBytes(entry, jarInputStream.readAllBytes());
                foundRoot = true;
            }
            if (MULTI_RELEASE_MODULE_INFO_PATTERN.test(entry.getName())) {
                if (!foundRoot) {
                    entryToUse = new EntryAndBytes(entry, jarInputStream.readAllBytes());
                }
            }
        }
        if (entryToUse != null) {
            return Optional.of(ModuleInfo.fromBytes(entryToUse.bytes));
        }

        return Optional.empty();
    }


    public static ModuleInfo from(ClassModel classModel) {
        if (!classModel.isModuleInfo()) {
            throw new IllegalArgumentException("Class file does not represent a module.");
        }

        String name = null;
        List<Export> exports = new ArrayList<>();
        List<Require> requires = new ArrayList<>();
        List<Provide> provides = new ArrayList<>();
        List<Use> uses = new ArrayList<>();
        Optional<String> version = Optional.empty();
        boolean open = false;
        boolean synthetic = false;
        boolean mandated = false;
        List<Package> packages = new ArrayList<>();
        List<Hash> hashes = new ArrayList<>();
        Optional<String> mainClass = Optional.empty();
        Optional<String> targetPlatform = Optional.empty();

        for (var attribute : classModel.attributes()) {
            if (attribute instanceof ModuleAttribute moduleAttribute) {
                name = moduleAttribute.moduleName().asSymbol().name();
                for (var export : moduleAttribute.exports()) {
                    exports.add(new Export(
                            new Package(export.exportedPackage().asSymbol().name()),
                            export.exportsTo().stream()
                                    .map(entry ->
                                            new ExportTo(new Module(entry.asSymbol().name()))
                                    )
                                    .toList(),
                            export.exportsFlags().contains(AccessFlag.SYNTHETIC),
                            export.exportsFlags().contains(AccessFlag.MANDATED)
                    ));
                }

                for (var require : moduleAttribute.requires()) {
                    requires.add(new Require(
                            new Module(require.requires().asSymbol().name()),
                            require.requiresVersion().map(Utf8Entry::stringValue),
                            require.requiresFlags().contains(AccessFlag.STATIC_PHASE),
                            require.requiresFlags().contains(AccessFlag.TRANSITIVE),
                            require.requiresFlags().contains(AccessFlag.MANDATED),
                            require.requiresFlags().contains(AccessFlag.SYNTHETIC)
                    ));
                }

                for (var provide : moduleAttribute.provides()) {
                    var serviceSymbol = provide.provides().asSymbol();

                    provides.add(new Provide(
                                    serviceSymbol.packageName() + "." + serviceSymbol.displayName(),
                                    provide.providesWith().stream()
                                            .map(classEntry -> {
                                                var withSymbol = classEntry.asSymbol();
                                                return withSymbol.packageName() + "." + withSymbol.displayName();
                                            })
                                            .toList()
                            )
                    );
                }

                for (var use : moduleAttribute.uses()) {
                    var serviceSymbol = use.asSymbol();
                    uses.add(new Use(
                            serviceSymbol.packageName() + "." + serviceSymbol.displayName()
                    ));
                }

                version = moduleAttribute.moduleVersion().map(Utf8Entry::stringValue);
                open = moduleAttribute.moduleFlags().contains(AccessFlag.OPEN);
                synthetic = moduleAttribute.moduleFlags().contains(AccessFlag.SYNTHETIC);
                mandated = moduleAttribute.moduleFlags().contains(AccessFlag.MANDATED);
            }
            if (attribute instanceof ModulePackagesAttribute packagesAttribute) {
                packagesAttribute.packages().stream()
                        .map(packageEntry -> new Package(packageEntry.asSymbol().name()))
                        .forEach(packages::add);
            }
            if (attribute instanceof ModuleHashesAttribute hashesAttribute) {
                var algorithm = hashesAttribute.algorithm().stringValue();
                var hashes_ = hashesAttribute.hashes()
                        .stream()
                        .map(hash -> new ModuleHash(
                                new Module(hash.moduleName().asSymbol().name()),
                                HexFormat.of().formatHex(hash.hash())
                        ))
                        .toList();
                hashes.add(new Hash(algorithm, hashes_));

            }
            if (attribute instanceof ModuleMainClassAttribute moduleMainClassAttribute) {
                var mainClassSymbol = moduleMainClassAttribute.mainClass().asSymbol();
                mainClass = Optional.of(mainClassSymbol.packageName() + "." + mainClassSymbol.displayName());
            }
            if (attribute instanceof ModuleTargetAttribute targetAttribute) {
                targetPlatform = Optional.of(targetAttribute.targetPlatform().stringValue());
            }
        }

        return new ModuleInfo(
                name, exports, requires, provides, uses, version,
                open, synthetic, mandated,
                packages, hashes, mainClass, targetPlatform
        );
    }

    private static byte[] toBytesHelper(ModuleInfo mi) {

        return ClassFile.of().buildModule(
                ModuleAttribute.of(
                        ModuleDesc.of(mi.name),
                        moduleAttributeBuilder -> {
                            mi.version.ifPresent(moduleAttributeBuilder::moduleVersion);
                            {
                                var accessFlags = new ArrayList<AccessFlag>();
                                if (mi.open) {
                                    accessFlags.add(AccessFlag.OPEN);
                                }
                                if (mi.synthetic) {
                                    accessFlags.add(AccessFlag.SYNTHETIC);
                                }
                                if (mi.mandated) {
                                    accessFlags.add(AccessFlag.MANDATED);
                                }

                                if (!accessFlags.isEmpty()) {
                                    moduleAttributeBuilder.moduleFlags(accessFlags.toArray(AccessFlag[]::new));
                                }
                            }
                            mi.exports.forEach(export -> {
                                var packageDesc = PackageDesc.of(export.package_().name());
                                var accessFlags = new ArrayList<AccessFlag>();
                                if (export.synthetic()) {
                                    accessFlags.add(AccessFlag.SYNTHETIC);
                                }
                                if (export.mandated()) {
                                    accessFlags.add(AccessFlag.MANDATED);
                                }
                                moduleAttributeBuilder.exports(
                                        packageDesc,
                                        accessFlags,
                                        export.to()
                                                .stream()
                                                .map(ExportTo::module)
                                                .map(Module::name)
                                                .map(ModuleDesc::of)
                                                .toArray(ModuleDesc[]::new)
                                );
                            });

                            mi.requires.forEach(require -> {
                                var accessFlags = new ArrayList<AccessFlag>();
                                if (require.static_()) {
                                    accessFlags.add(AccessFlag.STATIC_PHASE);
                                }
                                if (require.transitive()) {
                                    accessFlags.add(AccessFlag.TRANSITIVE);
                                }
                                if (require.synthetic()) {
                                    accessFlags.add(AccessFlag.SYNTHETIC);
                                }
                                if (require.mandated()) {
                                    accessFlags.add(AccessFlag.MANDATED);
                                }
                                moduleAttributeBuilder.requires(
                                        ModuleDesc.of(require.module().name()),
                                        accessFlags,
                                        require.version().orElse(null)
                                );
                            });
                            mi.provides.forEach(provide ->  {
                                moduleAttributeBuilder.provides(
                                        ModuleProvideInfo.of(
                                                ClassDesc.of(provide.service()),
                                                provide.with()
                                                        .stream()
                                                        .map(ClassDesc::of)
                                                        .toArray(ClassDesc[]::new)
                                        )
                                );
                            });
                            mi.uses.forEach(use -> {
                                moduleAttributeBuilder.uses(ClassDesc.of(use.service()));
                            });

                        }
                ),
                classBuilder -> {
                    if (!mi.packages.isEmpty()) {
                        classBuilder.accept(ModulePackagesAttribute.ofNames(
                                mi.packages.stream()
                                        .map(Package::name)
                                        .map(PackageDesc::of)
                                        .toArray(PackageDesc[]::new)
                        ));
                    }
                    mi.mainClass().ifPresent(mainClass -> {
                        classBuilder.accept(ModuleMainClassAttribute.of(ClassDesc.of(mainClass)));
                    });
                    mi.targetPlatform().ifPresent(targetPlatform -> {
                        classBuilder.accept(ModuleTargetAttribute.of(targetPlatform));
                    });


                    classBuilder.withVersion(ClassFile.JAVA_9_VERSION, 0);
                }
        );



//          (fn [class-builder]
//            (when-let [hashes (:hashes module-info)]
//              (ClassBuilder/.accept
//                class-builder
//                (^[String List]
//                  ModuleHashesAttribute/of
//                  (:algorithm hashes)
//                  (mapv (fn [{:keys [module hash]}]
//                          (ModuleHashInfo/of
//                            (ModuleDesc/of module)
//                            (-> (HexFormat/of)
//                                (HexFormat/.parseHex hash))))
//                        (:hashes hashes)))))
    }
    public byte[] toBytes() {
        ModuleInfo mi = name.equals("java.base") ? this : this.with(info -> {

        });

        return toBytesHelper(mi);
    }

    public static void main(String[] args) {
        var m = new ModuleInfo("org.clojure").with(module -> {
            module.requires = List.of(
                    new Require("a").with(require -> {
                        require.static_ = true;
                    })
            );
            module.open = true;
        });

        System.out.println(m);
        System.out.println(new ModuleInfo("java.base"));

        System.out.println(new ModuleInfo("java.basfe"));
    }
}
