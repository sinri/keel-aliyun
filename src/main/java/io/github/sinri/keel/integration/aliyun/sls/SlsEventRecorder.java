package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.logger.api.event.BaseEventRecorder;
import io.github.sinri.keel.logger.consumer.QueuedTopicRecordConsumer;
import org.jetbrains.annotations.NotNull;


public class SlsEventRecorder extends BaseEventRecorder {

    public SlsEventRecorder(@NotNull String topic, @NotNull QueuedTopicRecordConsumer consumer) {
        super(topic, consumer);
    }

}
