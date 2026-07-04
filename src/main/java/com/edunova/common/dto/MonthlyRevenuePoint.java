package com.edunova.common.dto;

import java.math.BigDecimal;

public record MonthlyRevenuePoint(
        String     month,
        int        year,
        BigDecimal revenue,
        long       activeSchools
) {}
