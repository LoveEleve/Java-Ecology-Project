package com.loveeleve.ecology.mybatisplus.dto;

import java.util.List;

public record PageResult<T>(
        long total,
        long pages,
        long page,
        long size,
        List<T> records
) {
}
