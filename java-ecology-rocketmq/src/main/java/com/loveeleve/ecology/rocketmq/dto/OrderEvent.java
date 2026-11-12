package com.loveeleve.ecology.rocketmq.dto;

public class OrderEvent {

    private String orderId;
    private String userId;
    private long amount;
    private boolean fail;

    public OrderEvent() {
    }

    public OrderEvent(String orderId, String userId, long amount, boolean fail) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.fail = fail;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public boolean isFail() {
        return fail;
    }

    public void setFail(boolean fail) {
        this.fail = fail;
    }
}
