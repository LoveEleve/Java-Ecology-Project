package com.loveeleve.ecology.budget.lease;

/**
 * 自适应号段大小计算。
 *
 * <p>思路参考 Netflix/concurrency-limits 的 EWMA + AIMD：</p>
 * <ul>
 *     <li>按消耗速率计算“下一段需要覆盖多久”；</li>
 *     <li>使用公平份额限制单实例囤积；</li>
 *     <li>利用率高时放大，利用率低时缩小；</li>
 *     <li>用 min/max 和冷却避免震荡。</li>
 * </ul>
 */
public final class AdaptiveChunkSizer {

    private final long minChunk;
    private final long maxChunk;
    private final double refillLatencySeconds;
    private final double cushionSeconds;

    private double lastUtilization = 0.5;
    private long lastChunk;

    public AdaptiveChunkSizer(
            long minChunk,
            long maxChunk,
            double refillLatencySeconds,
            double cushionSeconds) {
        if (minChunk <= 0 || maxChunk < minChunk) {
            throw new IllegalArgumentException("invalid chunk bounds");
        }
        if (refillLatencySeconds < 0 || cushionSeconds <= 0) {
            throw new IllegalArgumentException("invalid latency/cushion");
        }
        this.minChunk = minChunk;
        this.maxChunk = maxChunk;
        this.refillLatencySeconds = refillLatencySeconds;
        this.cushionSeconds = cushionSeconds;
    }

    /**
     * 计算下一段大小。
     *
     * @param consumeRate      EWMA 消耗速率（单位/秒）
     * @param available        中央当前可用额度
     * @param activeInstances  活跃实例数
     */
    public synchronized long nextChunk(double consumeRate, long available, int activeInstances) {
        double desired = consumeRate * (refillLatencySeconds + cushionSeconds);
        long fairShare = available / Math.max(1, activeInstances);
        long cap = Math.min(maxChunk, Math.max(minChunk, fairShare * 2));

        long chunk = Math.max(minChunk, (long) desired);
        chunk = Math.min(chunk, cap);

        // AIMD：高利用率放大，低利用率缩小。
        if (lastUtilization > 0.8) {
            chunk = (long) (chunk * 1.5);
        } else if (lastUtilization < 0.3) {
            chunk = (long) (chunk * 0.5);
        }

        chunk = Math.max(minChunk, Math.min(maxChunk, chunk));
        lastChunk = chunk;
        return chunk;
    }

    /** 记录上一段实际利用率，供下一次调整。 */
    public synchronized void recordUtilization(long chunk, long consumed) {
        if (chunk <= 0) {
            return;
        }
        lastUtilization = Math.min(1.0, (double) consumed / chunk);
    }

    public synchronized double lastUtilization() {
        return lastUtilization;
    }

    public synchronized long lastChunk() {
        return lastChunk;
    }
}
