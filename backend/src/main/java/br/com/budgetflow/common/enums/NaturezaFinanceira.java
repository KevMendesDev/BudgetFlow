package br.com.budgetflow.common.enums;

public enum NaturezaFinanceira {
    RECEITA("Receita"),
    DESPESA("Despesa");

    public final String value;

    NaturezaFinanceira(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}