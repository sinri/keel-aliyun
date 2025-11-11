package io.github.sinri.keel.integration.aliyun.sls.writer;

public final class AliyunSLSDisabled extends Exception {
    public AliyunSLSDisabled() {
        super("Aliyun SLS logging is disabled");
    }
}
