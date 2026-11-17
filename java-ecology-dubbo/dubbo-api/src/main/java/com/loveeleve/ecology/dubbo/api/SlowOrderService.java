package com.loveeleve.ecology.dubbo.api;

/**
 * 超时/重试示例契约。
 *
 * <p>生产代码不应提供可任意指定延迟的接口；这里只用于演示 consumer 侧
 * timeout + failfast 的边界。</p>
 */
public interface SlowOrderService {

    String slowPing(String message, long delayMs);
}
