package br.com.budgetflow.features.movimentacoes.criteria;

import br.com.budgetflow.common.enums.ClassificacaoCategoria;
import br.com.budgetflow.common.enums.NaturezaFinanceira;
import br.com.budgetflow.common.enums.TipoPagamento;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransacaoFilterCriteria {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFim;

    private Long periodoId;
    private Boolean recorrente;
    private BigDecimal valorMin;
    private BigDecimal valorMax;
    private String nomeCategoria;
    private ClassificacaoCategoria classificacaoCategoria;
    private NaturezaFinanceira tipoMovimentacao;
    private TipoPagamento tipoPagamento;
    private String query;

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public Long getPeriodoId() {
        return periodoId;
    }

    public void setPeriodoId(Long periodoId) {
        this.periodoId = periodoId;
    }

    public Boolean getRecorrente() {
        return recorrente;
    }

    public void setRecorrente(Boolean recorrente) {
        this.recorrente = recorrente;
    }

    public BigDecimal getValorMin() {
        return valorMin;
    }

    public void setValorMin(BigDecimal valorMin) {
        this.valorMin = valorMin;
    }

    public BigDecimal getValorMax() {
        return valorMax;
    }

    public void setValorMax(BigDecimal valorMax) {
        this.valorMax = valorMax;
    }

    public String getNomeCategoria() {
        return nomeCategoria;
    }

    public void setNomeCategoria(String nomeCategoria) {
        this.nomeCategoria = nomeCategoria;
    }

    public ClassificacaoCategoria getClassificacaoCategoria() {
        return classificacaoCategoria;
    }

    public void setClassificacaoCategoria(ClassificacaoCategoria classificacaoCategoria) {
        this.classificacaoCategoria = classificacaoCategoria;
    }

    public NaturezaFinanceira getTipoMovimentacao() {
        return tipoMovimentacao;
    }

    public void setTipoMovimentacao(NaturezaFinanceira tipoMovimentacao) {
        this.tipoMovimentacao = tipoMovimentacao;
    }

    public TipoPagamento getTipoPagamento() {
        return tipoPagamento;
    }

    public void setTipoPagamento(TipoPagamento tipoPagamento) {
        this.tipoPagamento = tipoPagamento;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
