package nl.elec332.gradle.minecraft.moddev.util;

import java.util.*;

/**
 * Created by Elec332 on 24-03-2024
 */
public class J8Helper {

    @SafeVarargs
    public static <T> List<T> listOf(T... objects) {
        return Collections.unmodifiableList(Arrays.asList(objects));
    }

    public static <K, V> Map.Entry<K, V> entry(K k, V v) {
        return new AbstractMap.SimpleEntry<>(k, v);
    }

    @SafeVarargs
    public static <K, V> Map<K, V> mapOf(Map.Entry<K, V>... entries) {
        if (entries == null || entries.length == 0) {
            return Collections.emptyMap();
        }
        Map<K, V> ret = new HashMap<>();
        for (Map.Entry<K, V> e : entries) {
            ret.put(e.getKey(), e.getValue());
        }
        return ret;
    }

}
