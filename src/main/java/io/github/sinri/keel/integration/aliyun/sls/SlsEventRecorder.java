package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.logger.api.adapter.Adapter;
import io.github.sinri.keel.logger.api.adapter.LogWriter;
import io.github.sinri.keel.logger.api.adapter.Render;
import io.github.sinri.keel.logger.api.event.EventRecord;
import io.github.sinri.keel.logger.api.record.LoggingRecord;
import io.github.sinri.keel.logger.impl.event.AbstractEventRecorder;
import io.github.sinri.keel.logger.impl.record.QueuedLogWriter;

import javax.annotation.Nonnull;

public class SlsEventRecorder extends AbstractEventRecorder<LoggingRecord> {
    @Nonnull
    private final Adapter<EventRecord, LoggingRecord> adapter;

    public SlsEventRecorder(@Nonnull String topic, @Nonnull QueuedLogWriter<LoggingRecord> writer) {
        super(topic);
        this.adapter = new AdapterImpl(writer);
    }

    @Nonnull
    @Override
    public Adapter<EventRecord, LoggingRecord> adapter() {
        return adapter;
    }

    private static class AdapterImpl implements Adapter<EventRecord, LoggingRecord> {

        private final LogWriter<LoggingRecord> writer;
        private final Render<EventRecord, LoggingRecord> render;

        public AdapterImpl(@Nonnull QueuedLogWriter<LoggingRecord> writer) {
            this.writer = writer;
            this.render = Event2LogRenderImpl.getInstance();
        }

        @Nonnull
        @Override
        public Render<EventRecord, LoggingRecord> render() {
            return render;
        }

        @Nonnull
        @Override
        public LogWriter<LoggingRecord> writer() {
            return writer;
        }
    }
}
