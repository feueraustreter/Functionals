package feueraustreter.utils;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Pair<K, V> {

    public static <K, V> Pair<K, V> of(K k, V v) {
        return new Pair<>(k, v);
    }

    public static <K, V> Pair<K, V> of(Map.Entry<K, V> entry) {
        return new Pair<>(entry.getKey(), entry.getValue());
    }

    private final K k;
    private final V v;

    public Map<K, V> toMap() {
        Map<K, V> map = new HashMap<>();
        map.put(k, v);
        return map;
    }

    public Map.Entry<K, V> toMapEntry() {
        return toMap().entrySet().iterator().next();
    }
}
