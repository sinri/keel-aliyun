package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.logger.api.issue.IssueRecord;
import io.github.sinri.keel.logger.base.issue.BaseIssueRecorder;
import io.github.sinri.keel.logger.consumer.QueuedTopicRecordConsumer;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class SlsIssueRecorder<T extends IssueRecord<T>> extends BaseIssueRecorder<T> {

    public SlsIssueRecorder(@Nonnull String topic, @Nonnull Supplier<T> issueRecordSupplier, @Nonnull QueuedTopicRecordConsumer consumer) {
        super(topic, issueRecordSupplier, consumer);
    }

}
