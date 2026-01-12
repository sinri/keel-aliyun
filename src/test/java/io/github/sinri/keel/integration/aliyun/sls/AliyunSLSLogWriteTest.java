package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.base.configuration.ConfigElement;
import io.github.sinri.keel.logger.api.logger.Logger;
import io.github.sinri.keel.tesuto.KeelInstantRunner;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class AliyunSLSLogWriteTest extends KeelInstantRunner {
    @Override
    protected Future<Void> run() throws Exception {
        AliyunSlsConfigElement aliyunSlsConfigElement = AliyunSlsConfigElement.forSls(ConfigElement.root());
        SlsLoggerFactory slsLoggerFactory = new SlsLoggerFactory(aliyunSlsConfigElement);
        return slsLoggerFactory.deployMe(getVertx(), new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER))
                               .compose(deploymentId -> {
                                   Logger logger = slsLoggerFactory.createLogger(getClass().getSimpleName());

                                     return asyncCallStepwise(10, i -> {
                                         logger.info("Step " + i + " testing");
                                         return asyncSleep(1000);
                                     })
                                             .compose(v -> {
                                                 return asyncSleep(2000);
                                             });
                                 });
    }
}
