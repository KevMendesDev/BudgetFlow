package br.com.budgetflow.features.auth.service;

import java.time.OffsetDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.budgetflow.features.auth.repository.RefreshTokenRepository;

@Service
public class RefreshTokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenCleanupService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(fixedDelayString = "${app.security.refresh-token-cleanup-ms:3600000}")
    @Transactional
    public void removeExpiredAndRevokedTokens() {
        refreshTokenRepository.deleteExpiredOrRevoked(OffsetDateTime.now());
    }
}
