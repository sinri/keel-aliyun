package io.github.sinri.keel.integration.aliyun.sls;

public final class AliyunSLSDisabled extends Exception {
    public AliyunSLSDisabled() {
        super("Aliyun SLS logging is disabled");
    }
}
