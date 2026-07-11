package com.edunova.module.fee.util;


import com.edunova.config.SchoolConfig;
import com.edunova.config.SchoolConfigService;
import com.edunova.module.fee.repository.FeePaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReceiptNumberGenerator {

    private final FeePaymentRepository feePaymentRepository;
    private final SchoolConfigService configService;

    // Format: {PREFIX}{YEAR}{SEQUENCE}
    // Example: REC20240001, FEE20240002
    public String generate(UUID schoolId) {

        String prefix  = configService.getConfigOrDefault(
                schoolId, SchoolConfig.RECEIPT_PREFIX, "REC");
        String yearStr = String.valueOf(Year.now().getValue());

        String latest = feePaymentRepository
                .findLatestReceiptNo(schoolId)
                .orElse(null);

        int nextSeq = 1;

        if (latest != null && latest.contains(yearStr)) {
            try {
                String seqPart = latest.substring(
                        latest.indexOf(yearStr) + yearStr.length());
                nextSeq = Integer.parseInt(seqPart) + 1;
            } catch (NumberFormatException e) {
                log.warn("Could not parse sequence from receipt no: {}", latest);
            }
        }

        return String.format("%s%s%04d", prefix, yearStr, nextSeq);
    }
}