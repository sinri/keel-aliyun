module io.github.sinri.keel.integration.aliyun {
    requires com.google.protobuf;
    requires io.github.sinri.keel.base;
    requires io.github.sinri.keel.logger;
    requires io.github.sinri.keel.logger.api;
    requires io.vertx.core;
    requires io.vertx.web.client;
    requires org.jetbrains.annotations;
    requires org.lz4.java;

    // Public API packages
    exports io.github.sinri.keel.integration.aliyun.sls;
    exports io.github.sinri.keel.integration.aliyun.sls.entity;
    exports io.github.sinri.keel.integration.aliyun.sls.protocol;

    // Open packages for reflection (if used by frameworks at runtime)
    opens io.github.sinri.keel.integration.aliyun.sls;
    opens io.github.sinri.keel.integration.aliyun.sls.entity;
    opens io.github.sinri.keel.integration.aliyun.sls.protocol;
}