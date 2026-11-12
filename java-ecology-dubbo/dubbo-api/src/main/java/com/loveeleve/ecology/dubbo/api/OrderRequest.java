package com.loveeleve.ecology.dubbo.api;

import jakarta.validation.constraints.Min;
import java.io.Serializable;

public class OrderRequest implements Serializable {

    @Min(1)
    private long orderId;
    @Min(1)
    private long userId;
    @Min(1)
    private long amount;

    public OrderRequest() {
    }

    public OrderRequest(long orderId, long userId, long amount) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
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
}
