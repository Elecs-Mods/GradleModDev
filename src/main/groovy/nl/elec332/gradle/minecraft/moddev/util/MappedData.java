package nl.elec332.gradle.minecraft.moddev.util;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
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
        MappedData ret = new MappedData(newMapImpl(), false);
        builder.accept(ret);
        return new MappedData(ret.data);
    }

    private static Map<String, Object> newMapImpl() {
        return new LinkedHashMap<>();
    }

    private static <T> Collection<T> newCollectionImpl() {
        return new ArrayList<>();
    }

    public MappedData() {
        this(newMapImpl());
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
        Objects.requireNonNull(key);
        this.data.put(key, value);
    }

    public void put(String key, Object value) {
        Objects.requireNonNull(key);
        validateSingleObject(value);
        this.data.put(key, value);
    }

    public void putStringCollection(String key, Collection<String> value) {
        Objects.requireNonNull(key);
        if (root) {
            throw new UnsupportedOperationException();
        }
        Collection<String> ret = newCollectionImpl();
        ret.addAll(value);
        this.data.put(key, ret);
    }

    public void putMapCollection(String key, Collection<MappedData> value) {
        Objects.requireNonNull(key);
        if (root) {
            throw new UnsupportedOperationException();
        }
        Collection<Map<String, Object>> ret = newCollectionImpl();
        value.stream().map(m -> m.data).forEach(ret::add);
        this.data.put(key, ret);
    }

    public void putMap(String key, MappedData value) {
        Objects.requireNonNull(key);
        if (root) {
            throw new UnsupportedOperationException();
        }
        this.data.put(key, value.data);
    }

    public void putMap(String key, Map<String, ?> value) {
        Objects.requireNonNull(key);
        if (root) {
            throw new UnsupportedOperationException();
        }
        validateMap(value, false);
        Map<String, Object> ret = newMapImpl();
        ret.putAll(value);
        this.data.put(key, ret);
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
        return new MappedData((Map<String, Object>) this.data.computeIfAbsent(key, s -> newMapImpl()));
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
        Map<String, Object> ret = newMapImpl();
        merge(ret, this.data);
        return new MappedData(ret);
    }

    public Map<String, ?> getbackedMap() {
        return Collections.unmodifiableMap(this.data);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void merge(Map origin, Map extra) {
        Objects.requireNonNull(origin);
        if (extra == null || extra.isEmpty()){
            return;
        }
        extra.forEach((key, value) -> {
            if (!(key instanceof String)) {
                throw new UnsupportedOperationException("Invalid key type: " + (key == null ? "null" : key.getClass().getCanonicalName()));
            }
            Object ok = origin.get(key);
            if (ok == null) {
                if (value instanceof Map) {
                    ok = newMapImpl();
                    origin.put(key, ok);
                } else if (value instanceof Collection<?>) {
                    ok = newCollectionImpl();
                    origin.put(key, ok);
                } else {
                    validateSingleObject(value);
                    origin.put(key, value);
                    return;
                }
            }
            if (ok instanceof Map && value instanceof Map) {
                merge((Map) ok, (Map) value);
                return;
            }
            if (value instanceof Collection && ok instanceof Collection) {
                merge((Collection) ok, (Collection) value);
                return;
            }
            throw new UnsupportedOperationException();
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void merge(Collection origin, Collection extra) {
        Objects.requireNonNull(origin);
        if (extra == null || extra.isEmpty()){
            return;
        }
        extra.forEach(value -> {
            if (value instanceof Map) {
                Map ret = newMapImpl();
                merge(ret, (Map) value);
                origin.add(ret);
                return;
            }
            if (value instanceof Collection) {
                Collection ret = newCollectionImpl();
                merge(ret, (Collection) value);
                origin.add(ret);
                return;
            }
            validateSingleObject(value);
            origin.add(value);
        });
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MappedData && ((MappedData) obj).data.equals(this.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.data);
    }

    @Override
    public String toString() {
        return this.data.toString();
    }

    private static void validateMap(Map<?, ?> map, boolean allowSubMaps) {
        map.keySet().forEach(o -> {
            if (o instanceof String) {
                return;
            }
            throw new IllegalArgumentException("Key must be a String!");
        });
        map.values().forEach(o -> MappedData.validateObject(o, allowSubMaps));
    }

    private static void validateObject(Object o, boolean allowMaps) {
        if (o instanceof Collection) {
            ((Collection<?>) o).forEach(co -> MappedData.validateObject(co, allowMaps));
            return;
        }
        if (allowMaps && o instanceof Map) {
            validateMap((Map<?, ?>) o, true);
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
