package com.loveeleve.ecology.mybatisplus.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum UserStatus {

    DISABLED(0),
    ACTIVE(1);

    @EnumValue
    private final int code;

    UserStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
