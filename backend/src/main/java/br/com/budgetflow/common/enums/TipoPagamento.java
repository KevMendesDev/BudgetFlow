package br.com.budgetflow.common.enums;

public enum TipoPagamento {
    DINHEIRO("Dinheiro"),
    CARTAO_CREDITO("Cartão de Crédito"),
    CARTAO_DEBITO("Cartão de Débito"),
    PIX("Pix"),
    TRANSFERENCIA("Transferência"),
    BOLETO("Boleto");

    public final String value;

    TipoPagamento(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
