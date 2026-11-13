package com.loveeleve.ecology.budget.lease;

/**
 * 时间衰减 EWMA 消耗速率。
 *
 * <p>为什么按时间衰减，而不是按请求次数：</p>
 * <ul>
 *     <li>流量稀疏时，按次数衰减会让旧速率长期不更新；</li>
 *     <li>按时间衰减能自然反映“最近一段时间”的消耗速度。</li>
 * </ul>
 *
 * <p>这是自适应号段大小的输入，不是核心扣减逻辑。</p>
 */
public final class EwmaRate {

    private final double tauSeconds;
    private double ratePerSecond;
    private long lastNanos;
    private boolean initialized;

    public EwmaRate(double tauSeconds) {
        if (tauSeconds <= 0) {
            throw new IllegalArgumentException("tauSeconds must be > 0");
        }
        this.tauSeconds = tauSeconds;
    }

    /**
     * 记录一次消耗。
     *
     * @param amount    本次消耗额度
     * @param nowNanos  单调时钟，避免系统时间回拨
     */
    public synchronized void record(long amount, long nowNanos) {
        if (!initialized) {
            initialized = true;
            lastNanos = nowNanos;
            ratePerSecond = 0;
            return;
        }

        double elapsedSeconds = Math.max(1e-6, (nowNanos - lastNanos) / 1_000_000_000.0);
        double instantRate = amount / elapsedSeconds;
        double alpha = 1 - Math.exp(-elapsedSeconds / tauSeconds);
        ratePerSecond = alpha * instantRate + (1 - alpha) * ratePerSecond;
        lastNanos = nowNanos;
    }

    /** 当前平滑消耗速率（单位/秒）。 */
    public synchronized double ratePerSecond() {
        return ratePerSecond;
    }
}
