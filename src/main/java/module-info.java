module io.github.sinri.keel.integration.aliyun {
    requires com.google.protobuf;
    requires transitive io.github.sinri.keel.base;
    requires transitive io.github.sinri.keel.logger.api;
    requires transitive io.vertx.core;
    requires transitive io.vertx.web.client;
    requires org.lz4.java;
    requires transitive io.github.sinri.keel.core;
    requires static org.jspecify;

    // Public API packages
    exports io.github.sinri.keel.integration.aliyun.sls;
    exports io.github.sinri.keel.integration.aliyun.sae;
}