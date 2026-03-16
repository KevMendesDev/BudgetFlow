package br.com.budgetflow.common.enums;

public enum TipoMovimentacao {
    RECEITA("Receita"),
    DESPESA("Despesa");

    public final String value;

    TipoMovimentacao(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
