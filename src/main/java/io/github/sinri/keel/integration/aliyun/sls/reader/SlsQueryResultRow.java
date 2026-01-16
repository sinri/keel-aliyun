package io.github.sinri.keel.integration.aliyun.sls.reader;

import io.github.sinri.keel.base.json.UnmodifiableJsonifiableEntityImpl;
import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@NullMarked
public class SlsQueryResultRow extends UnmodifiableJsonifiableEntityImpl {

    public SlsQueryResultRow(JsonObject jsonObject) {
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
    }


    public @Nullable Integer getTime() {
        return readInteger("__time__");
    }

    public @Nullable String getSource() {
        return readString("__source__");
    }

    public @Nullable String getTopic() {
        return readString("__topic__");
    }

    public Map<String, String> getTagMap() {
        Map<String, String> tagMap = new HashMap<>();
        this.forEach(entry -> {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.startsWith("__") && key.endsWith("__")) {
                if (key.contains(":")) {
                    String[] split = key.split(":", 2);
                    String rawKey = split[0].replace("__", "");
                    if (rawKey.equals("tag")) {
                        String rawItem = split[1].replace("__", "");
                        tagMap.put(rawItem, value.toString());
                    }
                }
            }
        });
        return tagMap;
    }

    public JsonObject normalize() {
        var x = new JsonObject();
        String topic = getTopic();
        if (topic != null) {
            x.put("topic", topic);
        }
        Integer time = getTime();
        if (time != null) {
            x.put("time", getTime());
        }
        String source = getSource();
        if (source != null) {
            x.put("source", getSource());
        }

        var z = new JsonObject();
        this.getTagMap().forEach(z::put);
        x.put("tags", z);

        var y = new JsonObject();
        this.forEach(entry -> {
            String key = entry.getKey();
            if (key.startsWith("__") && key.endsWith("__")) {
                return;
            }
            y.put(key, entry.getValue());
        });
        x.put("properties", y);
        return x;
    }
}
