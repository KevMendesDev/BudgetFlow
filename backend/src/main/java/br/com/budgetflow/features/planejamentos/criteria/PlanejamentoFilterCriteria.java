package br.com.budgetflow.features.planejamentos.criteria;

import br.com.budgetflow.common.enums.ClassificacaoCategoria;
import br.com.budgetflow.common.enums.NaturezaFinanceira;
import jakarta.validation.constraints.NotNull;

public class PlanejamentoFilterCriteria {

    @NotNull(message = "O período é obrigatório")
    private Long periodoId;

    private String query;
    private NaturezaFinanceira tipoMovimentacao;
    private ClassificacaoCategoria classificacaoCategoria;

    public Long getPeriodoId() {
        return periodoId;
    }

    public void setPeriodoId(Long periodoId) {
        this.periodoId = periodoId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public NaturezaFinanceira getTipoMovimentacao() {
        return tipoMovimentacao;
    }

    public void setTipoMovimentacao(NaturezaFinanceira tipoMovimentacao) {
        this.tipoMovimentacao = tipoMovimentacao;
    }

    public ClassificacaoCategoria getClassificacaoCategoria() {
        return classificacaoCategoria;
    }

    public void setClassificacaoCategoria(ClassificacaoCategoria classificacaoCategoria) {
        this.classificacaoCategoria = classificacaoCategoria;
    }
}
