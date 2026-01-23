package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.base.configuration.ConfigElement;
import io.github.sinri.keel.base.configuration.NotConfiguredException;
import io.github.sinri.keel.tesuto.KeelJUnit5Test;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;

public class SlsLoggerFactoryTest extends KeelJUnit5Test {

    public SlsLoggerFactoryTest() {
        super();
    }

    @Test
    void test1(VertxTestContext testContext) throws NotConfiguredException {
        AliyunSlsConfigElement aliyunSlsConfigElement = AliyunSlsConfigElement.forSls(ConfigElement.root());
        SlsLoggerFactory slsLoggerFactory = new SlsLoggerFactory(aliyunSlsConfigElement);
        slsLoggerFactory.deployMe(getVertx(), new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER))
                        .compose(s -> {
                            getUnitTestLogger().info("deployed: " + s);
                            return Future.succeededFuture();
                        })
                        .compose(v -> {
                            slsLoggerFactory.createLogger("test1").info("!!!");
                            //                            System.out.println("!!!");
                            return asyncSleep(1000);
                        })
                        .eventually(() -> {
                            getUnitTestLogger().info("test1 done");
                            return slsLoggerFactory.undeployMe();
                        })
                        .compose(v -> {
                            getUnitTestLogger().info("undeployed");
                            return Future.succeededFuture();
                        })
                        .onComplete(ar -> {
                            getUnitTestLogger().info("test1 ending");
                            if (ar.failed()) {
                                // ar.cause().printStackTrace();
                                testContext.failNow(ar.cause());
                            } else {
                                testContext.completeNow();
                            }
                            getUnitTestLogger().info("test1 after ending");
                        });
    }
}
