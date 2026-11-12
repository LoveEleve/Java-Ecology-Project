package com.loveeleve.ecology.mybatis.provider;

public final class UserProvider {

    private UserProvider() {
    }

    public static String selectByEmail() {
        return """
                SELECT id, username, email, status, age, created_at, updated_at
                FROM ecology_user
                WHERE email = #{email}
                """;
    }
}
