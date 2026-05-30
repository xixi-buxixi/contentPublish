package com.example.pulsedistro.service;

import com.example.pulsedistro.dto.session.SessionInitRequest;
import com.example.pulsedistro.dto.session.SessionInitResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SessionService {

    private static final DateTimeFormatter TRACE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final AtomicLong userIdSequence = new AtomicLong(1);

    public SessionInitResponse init(SessionInitRequest request) {
        long userId = userIdSequence.getAndIncrement();
        String slug = request == null || request.nickname() == null || request.nickname().isBlank()
                ? "demo"
                : request.nickname().trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        return new SessionInitResponse(
                userId,
                "ut_" + suffix + "_" + slug,
                "trace_" + LocalDate.now().format(TRACE_DATE) + "_" + suffix
        );
    }
}
