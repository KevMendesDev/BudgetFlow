package br.com.budgetflow.features.planejamentos.service.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;

public final class PlanejamentoChaveSincronizacaoUtils {

    private PlanejamentoChaveSincronizacaoUtils() {
    }

    public static String build(Long userId, Long recorrenteId, LocalDate data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String source = userId + ":" + recorrenteId + ":" + data;
            return HexFormat.of().formatHex(digest.digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível", ex);
        }
    }
}
