package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.base.logger.adapter.QueuedLogWriterAdapter;
import io.github.sinri.keel.base.logger.adapter.StdoutLogWriter;
import io.github.sinri.keel.logger.api.log.Log;
import io.github.sinri.keel.logger.api.log.SpecificLog;
import io.github.sinri.keel.logger.api.logger.Logger;
import io.vertx.core.Future;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NullMarked
class FallbackQueuedLogWriter extends QueuedLogWriterAdapter {
    private final Map<String, Logger> logRecordMap = new ConcurrentHashMap<>();

    public FallbackQueuedLogWriter() {
        super();
    }

    private static Log formatLog(SpecificLog<?> specificLog) {
        if (specificLog instanceof Log log) {
            return log;
        } else {
            return new Log(specificLog);
        }
    }

    @Override
    protected Future<Void> processLogRecords(String topic, List<SpecificLog<?>> batch) {
        //        System.out.println("FallbackQueuedLogWriter.processLogRecords -> " + topic + " " + batch.size());
        return getKeel().executeBlocking(() -> {
            batch.forEach(item -> {
                StdoutLogWriter.getInstance().accept(topic, item);
            });
            return (Void) null;
        });
    }

    @Override
    protected Future<Void> prepareForLoop() {
        return Future.succeededFuture();
    }

    @Override
    public void accept(String topic, SpecificLog<?> log) {
        //        System.out.println("FallbackQueuedLogWriter.accept -> " + topic + " " + log.message());
        //        RuntimeException testException = new RuntimeException("test exception");
        //        JsonifiedThrowable jsonifiedThrowable = JsonifiedThrowable.wrap(testException);
        //        System.out.println(jsonifiedThrowable.toFormattedJsonExpression());


        super.accept(topic, log);
    }
}
