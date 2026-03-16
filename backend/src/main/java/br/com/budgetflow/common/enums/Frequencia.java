package br.com.budgetflow.common.enums;

public enum Frequencia {
    DIARIO("Diário"),
    SEMANAL("Semanal"),
    MENSAL("Mensal"),
    ANUAL("Anual");

    public final String value;

    Frequencia(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
