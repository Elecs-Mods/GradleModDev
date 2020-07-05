package nl.elec332.gradle.util;

import org.gradle.api.file.SourceDirectorySet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Created by Elec332 on 25-5-2020
 */
public class InstalledDependency {

    public InstalledDependency(String bin, Predicate<String> binT, String lib, Predicate<String> libT, String include) {
        this.bin = bin;
        this.lib = lib;
        this.include = include;
        this.libT = libT;
        this.binT = binT;
    }

    private final String bin, lib, include;
    private final Predicate<String> libT, binT;

    public List<String> getBinaries() {
        return getMatching(bin, binT);
    }

    public List<String> getLibs() {
        return getMatching(lib, libT);
    }

    private List<String> getMatching(String folderName, Predicate<String> test) {
        if (!System.getProperty("os.name").toLowerCase().contains("win") || folderName == null) {
            return Collections.emptyList();
        }
        File folder = new File(folderName);
        if (folder.exists()) {
            File[] stuff = folder.listFiles();
            if (stuff != null) {
                List<String> ret = new ArrayList<>();
                for (File f : stuff) {
                    String path = f.getAbsolutePath();
                    if (test == null || test.test(path)) {
                        ret.add(path);
                    }
                }
                return ret;
            }

        }
        return Collections.emptyList();
    }

    public void addIncludes(SourceDirectorySet headers) {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            headers.srcDir(new File(include));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstalledDependency that = (InstalledDependency) o;
        return Objects.equals(bin, that.bin) &&
                Objects.equals(lib, that.lib) &&
                Objects.equals(include, that.include);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bin, lib, include);
    }

}
