package com.loveeleve.ecology.dubbo.api;

import java.io.Serializable;

public class OrderView implements Serializable {

    private long orderId;
    private long userId;
    private long amount;
    private String status;

    public OrderView() {
    }

    public OrderView(long orderId, long userId, long amount, String status) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
