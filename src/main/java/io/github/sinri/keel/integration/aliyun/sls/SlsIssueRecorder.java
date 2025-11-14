package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.logger.api.issue.BaseIssueRecorder;
import io.github.sinri.keel.logger.api.issue.IssueRecord;
import io.github.sinri.keel.logger.consumer.QueuedTopicRecordConsumer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class SlsIssueRecorder<T extends IssueRecord<T>> extends BaseIssueRecorder<T> {

    public SlsIssueRecorder(@NotNull String topic, @NotNull Supplier<T> issueRecordSupplier, @NotNull QueuedTopicRecordConsumer consumer) {
        super(topic, issueRecordSupplier, consumer);
    }

}
