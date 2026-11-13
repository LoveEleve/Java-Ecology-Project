package com.loveeleve.ecology.budget.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 预算预扣服务生产参数。
 */
@ConfigurationProperties(prefix = "budget")
public class BudgetProperties {

    /** 预占没有 confirm/release 时，多久后允许对账任务回收。 */
    private Duration maxHold = Duration.ofMinutes(30);

    /** 单轮超时回收最多处理多少条，避免一次扫太多。 */
    private int expiryScanLimit = 100;

    /** 对账/回收任务执行间隔。 */
    private Duration expiryScanInterval = Duration.ofSeconds(30);

    /** 本地号段默认 TTL。 */
    private Duration leaseTtl = Duration.ofMinutes(2);

    /** 本地号段最小/最大段大小，单位与预算一致。 */
    private long minChunk = 100;
    private long maxChunk = 100_000;

    public Duration getMaxHold() {
        return maxHold;
    }

    public void setMaxHold(Duration maxHold) {
        this.maxHold = maxHold;
    }

    public int getExpiryScanLimit() {
        return expiryScanLimit;
    }

    public void setExpiryScanLimit(int expiryScanLimit) {
        this.expiryScanLimit = expiryScanLimit;
    }

    public Duration getExpiryScanInterval() {
        return expiryScanInterval;
    }

    public void setExpiryScanInterval(Duration expiryScanInterval) {
        this.expiryScanInterval = expiryScanInterval;
    }

    public Duration getLeaseTtl() {
        return leaseTtl;
    }

    public void setLeaseTtl(Duration leaseTtl) {
        this.leaseTtl = leaseTtl;
    }

    public long getMinChunk() {
        return minChunk;
    }

    public void setMinChunk(long minChunk) {
        this.minChunk = minChunk;
    }

    public long getMaxChunk() {
        return maxChunk;
    }

    public void setMaxChunk(long maxChunk) {
        this.maxChunk = maxChunk;
    }
}
