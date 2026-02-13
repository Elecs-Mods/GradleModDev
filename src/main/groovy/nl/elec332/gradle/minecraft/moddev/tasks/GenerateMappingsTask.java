package nl.elec332.gradle.minecraft.moddev.tasks;

import net.neoforged.srgutils.IMappingBuilder;
import net.neoforged.srgutils.IMappingFile;
import net.neoforged.srgutils.INamedMappingFile;
import nl.elec332.gradle.minecraft.moddev.ModLoader;
import nl.elec332.gradle.minecraft.moddev.projects.AbstractPluginSC;
import nl.elec332.gradle.minecraft.moddev.util.AbstractGroovyHelper;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiFunction;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * Created by Elec332 on 15-03-2024
 */
public abstract class GenerateMappingsTask extends DefaultTask {

    public GenerateMappingsTask() {
        getOutputMappings().convention(getProject().getLayout().getBuildDirectory().file("outMaps/output.tsrg"));
        getOutputTargets().convention(() -> {
            File output = getOutputMappings().get().getAsFile();
            if (!output.getName().endsWith(".tsrg")) {
                throw new IllegalArgumentException();
            }
            return new File(output.getParentFile(), output.getName().replace(".tsrg", ".targets"));
        });
    }

    private static final String LEFT = "left";
    private static final String RIGHT = "right";

    @InputFiles
    abstract ConfigurableFileCollection getCommonJar();

    @InputFiles
    abstract ConfigurableFileCollection getModdedJars();

    @OutputFile
    abstract RegularFileProperty getOutputMappings();

    @OutputFile
    abstract RegularFileProperty getOutputTargets();

    @TaskAction
    public void runTask() throws IOException {
        File output = getOutputMappings().get().getAsFile();
        File targetsFile = getOutputTargets().get().getAsFile();
        if (!output.getName().endsWith(".tsrg") || !targetsFile.getName().endsWith(".targets")) {
            throw new IllegalArgumentException();
        }
        try (JarFile commonJar = new JarFile(getCommonJar().getSingleFile())) {
            Set<String> classes = commonJar.stream()
                    .filter(e -> !e.isDirectory())
                    .map(ZipEntry::getName)
                    .filter(name -> name.endsWith(".class")).collect(Collectors.toUnmodifiableSet());
            ModLoader.Mapping rootMappings = getMappings(commonJar);
            Map<String, DiffFinder> control = null;
            List<Map.Entry<ModLoader.Mapping, Map<String, DiffFinder>>> targets = new ArrayList<>();
            for (File f : getModdedJars().getFiles()) {
                try (JarFile moddedJar = new JarFile(f)) {
                    ModLoader.Mapping mappings = getMappings(moddedJar);
                    if (control == null) {
                        control = new HashMap<>();
                        for (String cls : classes.stream().filter(cls -> moddedJar.getEntry(cls) != null).toList()) {
                            DiffFinder cdf = new DiffFinder();
                            new ClassReader(commonJar.getInputStream(commonJar.getEntry(cls))).accept(new ClassRemapper(new ClassNode(), cdf), ClassReader.SKIP_FRAMES);
                            control.put(cls, cdf);
                        }
                    }
                    Map<String, DiffFinder> target = new HashMap<>();
                    for (String cls : classes.stream().filter(cls -> moddedJar.getEntry(cls) != null).toList()) {
                        DiffFinder cdf = new DiffFinder();
                        new ClassReader(moddedJar.getInputStream(moddedJar.getEntry(cls))).accept(new ClassRemapper(new ClassNode(), cdf), ClassReader.SKIP_FRAMES);
                        target.put(cls, cdf);
                    }
                    if (!control.keySet().equals(target.keySet())) {
                        throw new IllegalStateException();
                    }
                    targets.add(Map.entry(mappings, target));
                }
            }
            Set<String> mappedClasses = new HashSet<>();
            INamedMappingFile out = generateMappings(rootMappings, control, targets, mappedClasses);
            if (out != null) {
                out.write(output.toPath(), IMappingFile.Format.TSRG2);
                AbstractGroovyHelper.writeFile(targetsFile, String.join("\n", mappedClasses));
            } else {
                Files.delete(output.toPath());
                Files.delete(targetsFile.toPath());
            }
        }
    }

    private static INamedMappingFile generateMappings(ModLoader.Mapping rootMap, Map<String, DiffFinder> rootData, List<Map.Entry<ModLoader.Mapping, Map<String, DiffFinder>>> targets, Set<String> mappedClasses) {
        if (targets.isEmpty()) {
            return null;
        }
        Map<ModLoader.Mapping, IMappingFile> individualMappings = new EnumMap<>(ModLoader.Mapping.class);
        targets.forEach(e -> {
            IMappingFile ret = createMapping(rootData, e.getValue(), mappedClasses);
            if (ret == null) {
                return;
            }
            IMappingFile old = individualMappings.get(e.getKey());
            if (old == null) {
                individualMappings.put(e.getKey(), ret);
                return;
            }
            if (isMapping(old.reverse().chain(ret))) {
                throw new IllegalStateException("Duplicate mappings aren't identical!");
            }
        });
        if (individualMappings.get(rootMap) != null) {
            throw new IllegalStateException();
        }

        IMappingFile iterator = individualMappings.values().iterator().next();
        Set<ModLoader.Mapping> names = new LinkedHashSet<>();
        names.add(rootMap);
        names.addAll(individualMappings.keySet());
        IMappingBuilder builder = IMappingBuilder.create(names.stream().map(Enum::name).toArray(String[]::new));
        names.remove(rootMap);

        for (var p : iterator.getPackages()) {
            String[] map = squash(p, names.stream().map(individualMappings::get), IMappingFile::getPackage);
            if (isMapping(map)) {
                builder = builder.addPackage(map).build();
            }
        }

        for (var c : iterator.getClasses()) {
            List<IMappingFile.IClass> mappedC = names.stream().map(individualMappings::get).map(f -> f.getClass(c.getOriginal())).toList();
            String[] cMap = squash(c, mappedC.stream());
            IMappingBuilder.IClass cls = null;
            if (isMapping(cMap)) {
                cls = builder.addClass(cMap);
            }
            for (var f : c.getFields()) {
                String[] fMap = squash(f, mappedC.stream(), IMappingFile.IClass::getField);
                if (isMapping(fMap)) {
                    if (cls == null) {
                        cls = builder.addClass(cMap);
                    }
                    cls = cls.field(fMap).descriptor(f.getDescriptor()).build();
                }
            }
            for (var m : c.getMethods()) {
                String[] mMap = squash(m, mappedC.stream(), (sc, n) -> sc.getMethod(n, m.getDescriptor()));
                if (isMapping(mMap)) {
                    if (cls == null) {
                        cls = builder.addClass(cMap);
                    }
                    cls = cls.method(m.getDescriptor(), mMap).build();
                }
            }
            if (cls != null) {
                builder = cls.build();
            }
        }

        return builder.build();
    }

    private static boolean isMapping(String[] names) {
        if (names == null || names.length == 0) {
            throw new IllegalArgumentException();
        }
        if (names.length == 1) {
            return false;
        }
        String first = Objects.requireNonNull(names[0]);
        for (int i = 1; i < names.length; i++) {
            if (!first.equals(names[i])) {
                return true;
            }
        }
        return false;
    }
    private static <T extends IMappingFile.INode, U> String[] squash(T root, Stream<U> stream, BiFunction<U, String, T> mapper) {
        return squash(root, stream.map(u -> u == null ? null : mapper.apply(u, root.getOriginal())));
    }

    private static <T extends IMappingFile.INode> String[] squash(T root, Stream<T> kids) {
        List<String> names = new LinkedList<>();
        names.add(root.getOriginal());
        kids.forEach(f -> names.add(Objects.requireNonNull(f, "Null child of target " + root.getOriginal()).getMapped()));
        return names.toArray(new String[0]);
    }

    @Nullable
    private static IMappingFile createMapping(Map<String, DiffFinder> rootData, Map<String, DiffFinder> modData, Set<String> mappedClasses) {
        IMappingFile mappings = IMappingBuilder.create(LEFT, RIGHT).build().getMap(LEFT, RIGHT);
        if (!rootData.keySet().equals(modData.keySet())) {
            throw new IllegalArgumentException();
        }
        for (String s : rootData.keySet()) {
            IMappingFile extra = createMapping(Objects.requireNonNull(rootData.get(s), "Root"), Objects.requireNonNull(modData.get(s), "Mod"));
            if (isMapping(extra)) {
                mappedClasses.add(s.replace(".class", ""));
            }
            mappings = mappings.merge(extra);
        }
        if (!isMapping(mappings)) {
            return null;
        }
        return mappings;
    }

    private static IMappingFile createMapping(DiffFinder origin, DiffFinder target) {
        if (origin.classes.size() != target.classes.size()) {
            throw new RuntimeException();
        }
        if (origin.packages.size() != target.packages.size()) {
            throw new RuntimeException();
        }
        if (origin.methods.size() != target.methods.size()) {
            throw new RuntimeException();
        }
        if (origin.fields.size() != target.fields.size()) {
            throw new RuntimeException();
        }
        IMappingBuilder builder = IMappingBuilder.create(LEFT, RIGHT);
        for (int i = 0; i < origin.packages.size(); i++) {
            builder = builder.addPackage(origin.packages.get(i), target.packages.get(i)).build();
        }
        for (int i = 0; i < origin.classes.size(); i++) {
            String originCls = origin.classes.get(i);
            IMappingBuilder.IClass cls = builder.addClass(originCls, target.classes.get(i));
            for (int j = 0; j < origin.methods.size(); j++) {
                DiffFinder.MemberEntry originMth = origin.methods.get(j);
                if (Objects.equals(originMth.owner, originCls)) {
                    cls = cls.method(originMth.descriptor, originMth.name, target.methods.get(j).name).build();
                }
            }
            for (int j = 0; j < origin.fields.size(); j++) {
                var originFld = origin.fields.get(j);
                if (Objects.equals(originFld.owner, originCls)) {
                    cls = cls.field(originFld.name, target.fields.get(j).name).descriptor(originFld.descriptor).build();
                }
            }
            builder = cls.build();
        }
        return builder.build().getMap(LEFT, RIGHT);
    }

    private static boolean isMapping(IMappingFile mapping) {
        if (mapping.getClasses().isEmpty() && mapping.getPackages().isEmpty()) {
            return false;
        }
        if (isMapping(mapping.getPackages())) {
            return true;
        }
        for (var c : mapping.getClasses()) {
            if (isMapping(c) || isMapping(c.getMethods()) || isMapping(c.getFields())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMapping(Collection<? extends IMappingFile.INode> nodes) {
        return nodes.stream().anyMatch(GenerateMappingsTask::isMapping);
    }

    private static boolean isMapping(IMappingFile.INode node) {
        return !node.getMapped().equals(node.getOriginal());
    }

    private static ModLoader.Mapping getMappings(JarFile jarFile) throws IOException {
        String type = jarFile.getManifest().getMainAttributes().getValue(AbstractPluginSC.MAPPINGS);
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("No mappings found in " + jarFile.getName());
        }
        ModLoader.Mapping mappings;
        try {
            mappings = Enum.valueOf(ModLoader.Mapping.class, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("No valid mappings found in " + jarFile + ", " + type);
        }
        return mappings;
    }

    private static class DiffFinder extends Remapper {

        private final List<String> packages = new ArrayList<>();
        private final List<String> classes = new ArrayList<>();
        private final List<MemberEntry> methods = new ArrayList<>();
        private final List<MemberEntry> fields = new ArrayList<>();

        @Override
        public String mapPackageName(String name) {
            packages.add(name);
            return super.mapPackageName(name);
        }

        @Override
        public String map(String internalName) {
            classes.add(internalName);
            return super.map(internalName);
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            methods.add(new MemberEntry(owner, name, descriptor));
            return super.mapMethodName(owner, name, descriptor);
        }

        @Override
        public String mapFieldName(String owner, String name, String descriptor) {
            fields.add(new MemberEntry(owner, name, descriptor));
            return super.mapFieldName(owner, name, descriptor);
        }

        @Override
        public String mapRecordComponentName(String owner, String name, String descriptor) {
            return this.mapFieldName(owner, name, descriptor);
        }

        private static final class MemberEntry {

            private MemberEntry(String owner, String name, String descriptor) {
                this.owner = owner;
                this.name = name;
                this.descriptor = descriptor;
            }

            private final String owner;
            private final String name;
            private final String descriptor;

        }

    }

}
