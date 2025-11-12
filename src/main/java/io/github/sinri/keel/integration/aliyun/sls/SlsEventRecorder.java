package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.logger.base.event.BaseEventRecorder;
import io.github.sinri.keel.logger.consumer.QueuedTopicRecordConsumer;

import javax.annotation.Nonnull;

public class SlsEventRecorder extends BaseEventRecorder {

    public SlsEventRecorder(@Nonnull String topic, @Nonnull QueuedTopicRecordConsumer consumer) {
        super(topic, consumer);
    }

}
