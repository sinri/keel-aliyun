package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.logger.api.logger.Logger;
import io.github.sinri.keel.tesuto.KeelInstantRunner;
import io.vertx.core.Future;
import org.jetbrains.annotations.NotNull;

public class AliyunSLSLogWriteTest extends KeelInstantRunner {
    @Override
    protected @NotNull Future<Void> run() throws Exception {
        SlsRecorderFactory slsRecorderFactory = new SlsRecorderFactory(this.getKeel());
        Logger logger = slsRecorderFactory.createLogger(getClass().getSimpleName());

        return asyncCallStepwise(10, i -> {
            logger.info("Step " + i + " testing");
            return asyncSleep(1000);
        })
                .compose(v -> {
                    return getKeel().asyncSleep(2000);
                });
    }
}
