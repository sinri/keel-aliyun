package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.logger.api.adapter.Adapter;
import io.github.sinri.keel.logger.api.adapter.LogWriter;
import io.github.sinri.keel.logger.api.adapter.Render;
import io.github.sinri.keel.logger.api.event.EventRecord;
import io.github.sinri.keel.logger.api.record.LogRecord;
import io.github.sinri.keel.logger.impl.event.AbstractEventRecorder;
import io.github.sinri.keel.logger.impl.record.QueuedLogWriter;

import javax.annotation.Nonnull;

public class SlsEventRecorder extends AbstractEventRecorder<LogRecord> {
    @Nonnull
    private final Adapter<EventRecord, LogRecord> adapter;

    public SlsEventRecorder(@Nonnull String topic, @Nonnull QueuedLogWriter<LogRecord> writer) {
        super(topic);
        this.adapter = new AdapterImpl(writer);
    }

    @Nonnull
    @Override
    public Adapter<EventRecord, LogRecord> adapter() {
        return adapter;
    }

    private static class AdapterImpl implements Adapter<EventRecord, LogRecord> {

        private final LogWriter<LogRecord> writer;
        private final Render<EventRecord, LogRecord> render;

        public AdapterImpl(@Nonnull QueuedLogWriter<LogRecord> writer) {
            this.writer = writer;
            this.render = Event2LogRenderImpl.getInstance();
        }

        @Nonnull
        @Override
        public Render<EventRecord, LogRecord> render() {
            return render;
        }

        @Nonnull
        @Override
        public LogWriter<LogRecord> writer() {
            return writer;
        }
    }
}
