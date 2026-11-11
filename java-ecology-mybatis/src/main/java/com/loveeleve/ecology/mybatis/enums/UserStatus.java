package com.loveeleve.ecology.mybatis.enums;

public enum UserStatus {

    DISABLED(0),
    ACTIVE(1);

    private final int code;

    UserStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static UserStatus fromCode(int code) {
        for (UserStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("unknown user status code: " + code);
    }
}
