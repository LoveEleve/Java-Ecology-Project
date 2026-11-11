package com.loveeleve.ecology.multids.model;

public record Account(
        long id,
        String owner,
        long balance
) {
}
