package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.logger.api.event.Event2LogRender;
import io.github.sinri.keel.utils.StackUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

class Event2LogRenderImpl implements Event2LogRender {
    private static final Event2LogRenderImpl INSTANCE = new Event2LogRenderImpl();

    private Event2LogRenderImpl() {
    }

    public static Event2LogRenderImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public String renderThrowable(@Nonnull Throwable throwable) {
        return StackUtils.renderThrowableChain(throwable);
    }

    @Override
    public String renderClassification(@Nonnull List<String> classification) {
        return new JsonArray(classification).encode();
    }

    @Override
    public String renderContext(@Nonnull Map<String, Object> context) {
        return new JsonObject(context).encode();
    }
}
