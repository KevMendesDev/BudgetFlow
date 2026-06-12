package br.com.budgetflow.common.enums;

public enum ClassificacaoCategoria {
    ESSENCIAL ("Essencial"),
    NAO_ESSENCIAL ("Não Essencial"),
    INVESTIMENTO ("Investimento");

    public final String value;

    ClassificacaoCategoria(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
