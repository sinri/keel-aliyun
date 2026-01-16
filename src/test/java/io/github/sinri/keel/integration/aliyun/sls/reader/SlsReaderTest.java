package io.github.sinri.keel.integration.aliyun.sls.reader;

import io.github.sinri.keel.base.configuration.ConfigElement;
import io.github.sinri.keel.integration.aliyun.sls.AliyunSlsConfigElement;
import io.github.sinri.keel.tesuto.KeelJUnit5Test;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

class SlsReaderTest extends KeelJUnit5Test {

    /**
     * 构造方法。
     * <p>本方法在 {@code @BeforeAll} 注解的静态方法运行后运行。
     * <p>注意，本构造方法会注册 {@code JsonifiableSerializer} 所载 JSON 序列化能力。
     *
     */
    public SlsReaderTest() {
        super();
    }

    @Test
    void callGetLogsV2(VertxTestContext testContext) {
        AliyunSlsConfigElement aliyunSlsConfigElement = AliyunSlsConfigElement.forSls(ConfigElement.root());
        testContext.verify(() -> {
            if (aliyunSlsConfigElement == null) {
                testContext.failNow("No Aliyun SLS Config");
            }
        });
        SlsReader slsReader = new SlsReader(getVertx(), Objects.requireNonNull(aliyunSlsConfigElement));
        GetLogsV2Request request = GetLogsV2Request.builder()
                                                   .from(Math.toIntExact(System.currentTimeMillis() / 1000 - 60 * 60 * 24 * 30))
                                                   .to(Math.toIntExact(System.currentTimeMillis() / 1000))
                                                   .build();
        slsReader.callGetLogsV2(request)
                 .onSuccess(resp -> {
                     getUnitTestLogger().info("process " + resp.getMeta().getProgress() + " in " + resp.getMeta()
                                                                                                       .getElapsedMillisecond() + " for " + resp.getMeta()
                                                                                                                                                .getProcessedRows() + " rows");
                     getUnitTestLogger().info("data:");
                     resp.getData().forEach(j -> {
                         getUnitTestLogger().info("log:\n" + j);
                     });
                 })
                 .andThen(testContext.succeedingThenComplete());
    }
}