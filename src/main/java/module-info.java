module io.github.sinri.keel.integration.aliyun {
    requires com.google.protobuf;
    requires io.github.sinri.keel.base;
    requires io.github.sinri.keel.logger.api;
    requires io.vertx.core;
    requires io.vertx.web.client;
    requires org.jetbrains.annotations;
    requires org.lz4.java;
    requires io.github.sinri.keel.core;

    // Public API packages
    exports io.github.sinri.keel.integration.aliyun.sls;
}