package nl.elec332.gradle.minecraft.moddev.util;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Elec332 on 14-03-2024
 */
public class MappedData implements Serializable {

    public static MappedData ofMapCollection(String key, Collection<MappedData> value) {
        return of(b -> b.putMapCollection(key, value));
    }

    public static MappedData ofStringCollection(String key, Collection<String> value) {
        return of(b -> b.putStringCollection(key, value));
    }

    public static MappedData of(String key, MappedData value) {
        return of(b -> b.putMap(key, value));
    }

    public static MappedData of(String key, String value) {
        return of(b -> b.putString(key, value));
    }

    public static MappedData of(Consumer<MappedData> builder) {
        MappedData ret = new MappedData(new HashMap<>(), false);
        builder.accept(ret);
        return new MappedData(ret.data);
    }

    public MappedData() {
        this(new HashMap<>());
    }

    private MappedData(Map<String, Object> backedData) {
        this(backedData, true);
    }

    private MappedData(Map<String, Object> backedData, boolean root) {
        this.data = backedData;
        this.root = root;
    }

    private final transient boolean root;
    private final Map<String, Object> data;

    public void putString(String key, String value) {
        this.data.put(key, value);
    }

    public void put(String key, Object value) {
        validateSingleObject(value);
        this.data.put(key, value);
    }

    public void putStringCollection(String key, Collection<String> value) {
        if (root) {
            throw new UnsupportedOperationException();
        }
        this.data.put(key, new ArrayList<>(value));
    }

    public void putMapCollection(String key, Collection<MappedData> value) {
        if (root) {
            throw new UnsupportedOperationException();
        }
        this.data.put(key, value.stream().map(m -> m.data).collect(Collectors.toList()));
    }

    public void putMap(String key, MappedData value) {
        if (root) {
            throw new UnsupportedOperationException();
        }
        this.data.put(key, value.data);
    }

    public void putMap(String key, Map<String, ?> value) {
        if (root) {
            throw new UnsupportedOperationException();
        }
        validateMap(value);
        this.data.put(key, value);
    }

    public String getString(String entry) {
        return (String) this.data.get(entry);
    }

    @SuppressWarnings("unchecked")
    public Collection<String> getStringCollection(String entry) {
        return (Collection<String>) this.data.get(entry);
    }

    @SuppressWarnings("unchecked")
    public Stream<MappedData> getMapCollection(String entry) {
        return ((Collection<?>) this.data.get(entry)).stream().map(o -> new MappedData((Map<String, Object>) o));
    }

    @SuppressWarnings("unchecked")
    public MappedData getMap(String key) {
        return new MappedData((Map<String, Object>) this.data.get(key));
    }

    @SuppressWarnings("unchecked")
    public MappedData getOrCreateMap(String key) {
        return new MappedData((Map<String, Object>) this.data.computeIfAbsent(key, s -> new HashMap<>()));
    }

    public String removeString(String entry) {
        return (String) this.data.remove(entry);
    }

    @SuppressWarnings("unchecked")
    public Collection<String> removeStringCollection(String entry) {
        return (Collection<String>) this.data.remove(entry);
    }

    @SuppressWarnings("unchecked")
    public Stream<MappedData> removeMapCollection(String entry) {
        return ((Collection<?>) this.data.remove(entry)).stream().map(o -> new MappedData((Map<String, Object>) o));
    }

    @SuppressWarnings("unchecked")
    public MappedData removeMap(String key) {
        return new MappedData((Map<String, Object>) this.data.remove(key));
    }

    public void mergeWith(MappedData other) {
        merge(this.data, other == null ? null : other.data);
    }

    public Set<String> keySet() {
        return this.data.keySet();
    }

    public boolean containsKey(String key) {
        return this.data.containsKey(key);
    }

    public void clear() {
        this.data.clear();
    }

    public MappedData copy() {
        return new MappedData(new HashMap<>(this.data));
    }

    public Map<String, ?> getbackedMap() {
        return Collections.unmodifiableMap(this.data);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void merge(Map origin, Map extra) {
        if (extra == null || extra.isEmpty()){
            return;
        }
        if (origin.isEmpty()) {
            origin.putAll(extra);
            return;
        }
        extra.forEach((key, value) -> {
            if (!(key instanceof String)) {
                throw new UnsupportedOperationException();
            }
            Object ok = origin.get(key);
            if (ok == null) {
                origin.put(key, value);
                return;
            }
            if (ok instanceof Map && value instanceof Map) {
                merge((Map) ok, (Map) value);
                return;
            }
            if (value instanceof Collection && ok instanceof Collection) {
                ((Collection) ok).addAll((Collection) value);
                return;
            }
            throw new UnsupportedOperationException();
        });
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MappedData && ((MappedData) obj).data.equals(data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

    private static void validateMap(Map<?, ?> map) {
        map.keySet().forEach(o -> {
            if (o instanceof String) {
                return;
            }
            throw new IllegalArgumentException("Key must be a String!");
        });
        map.values().forEach(MappedData::validateObject);
    }

    private static void validateObject(Object o) {
        if (o instanceof Collection) {
            ((Collection<?>) o).forEach(MappedData::validateObject);
            return;
        }
        if (o instanceof Map) {
            validateMap((Map<?, ?>) o);
            return;
        }
        validateSingleObject(o);
    }

    private static void validateSingleObject(Object o) {
        if (o instanceof String) {
            return;
        }
        if (o instanceof Number) {
            return;
        }
        if (o instanceof Boolean) {
            return;
        }
        throw new IllegalArgumentException();
    }

}
