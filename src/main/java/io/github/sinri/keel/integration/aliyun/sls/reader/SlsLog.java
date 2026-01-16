package io.github.sinri.keel.integration.aliyun.sls.reader;

import io.github.sinri.keel.base.json.UnmodifiableJsonifiableEntityImpl;
import io.github.sinri.keel.logger.api.LateObject;
import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.NullMarked;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@NullMarked
public class SlsLog extends UnmodifiableJsonifiableEntityImpl {

    private final LateObject<Integer> lateTime = new LateObject<>();
    private final LateObject<String> lateSource = new LateObject<>();
    private final LateObject<String> lateTopic = new LateObject<>();
    private final Map<String, String> tagMap = new HashMap<>();

    public SlsLog(JsonObject jsonObject) {
        super(jsonObject);
        /*
         {
         "level":"INFO",
         "event":"{\"a\":\"b\"}",
         "message":"To call Colony Api",
         "context":"{}",
         "__topic__":"LALALA",
         "__source__":"127.0.0.1",
         "__tag__:__receive_time__":"1765714538",
         "__time__":"1765714537"
         }
         */
        extractAndPurify(jsonObject);
    }

    private void extractAndPurify(JsonObject raw) {
        Set<String> removeKeys = new HashSet<>();
        raw.forEach(entry -> {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.startsWith("__") && key.endsWith("__")) {
                if (key.contains(":")) {
                    String[] split = key.split(":", 2);
                    String rawKey = key.replace("__", "");
                    if (rawKey.equals("tag")) {
                        String rawItem = split[1].replace("__", "");
                        this.tagMap.put(rawItem, value.toString());
                    }
                } else {
                    String rawKey = key.replace("__", "");
                    switch (rawKey) {
                        case "topic" -> lateTopic.set(value.toString());
                        case "source" -> lateSource.set(value.toString());
                        case "time" -> lateTime.set(Integer.parseInt(value.toString()));
                    }
                }
                removeKeys.add(key);
            }
        });

        removeKeys.forEach(raw::remove);
    }

    public int getTime() {
        return lateTime.get();
    }

    public String getSource() {
        return lateSource.get();
    }

    public String getTopic() {
        return lateTopic.get();
    }

    public Map<String, String> getTagMap() {
        return tagMap;
    }

    public JsonObject toJsonObject() {
        var x = new JsonObject()
                .put("topic", getTopic())
                .put("time", getTime())
                .put("source", getSource());
        var y = new JsonObject();
        this.forEach(entry -> {
            y.put(entry.getKey(), entry.getValue());
        });
        x.put("properties", y);

        var z = new JsonObject();
        this.getTagMap().forEach(z::put);
        x.put("tags", z);

        return x;
    }
}
