---
name: debug-refactor-workflow
description: "Workflow para debug sistemático e refatoração com clean code. Use quando: investigar erro, refatorar feature, ou melhorar código existente."
---

# Debug & Refactor Workflow

## When to Use

- Erro pra investigar
- Refatoração de feature existente  
- Melhorar código violando principles (coesão, acoplamento)

## Debug Process

### 1. Identify Error
- Lê log/stack trace
- Identifica classe e método afetado
- Anota linha e contexto

### 2. Investigate Root Cause
- Breakpoint nebo read código relevante
- Valida assunções (valores, estado)
- Mapeia fluxo de execução
- Pergunta: "por que isso tá errado?"

### 3. Isolate Problem
- Reproduz erro em contexto mínimo
- Separa sintoma de causa raiz
- Valida se é lógica ou dados ruins

### 4. Propose Fix
- Solução mínima, só o necessário
- Explica por que func agora
- Checa se abre outro problema

### 5. Verify & Test
- Testa fix localmente
- Valida casos edge
- Confirma que não quebrou nada

---

## Refactor Process

### 1. Code Review
- Mapeia responsabilidades
- Identifica violations:
  - Método muito grande?
  - Classe faz muita coisa?
  - Code duplicado?
  - Acoplamento alto?

### 2. Plan Refactor
- Define new structure
- Separação de métodos/classes
- Mappers/DTOs necessárias?

### 3. Extract & Reorganize
- Extrai métodos pequenos e focados
- Move lógica de negócio pra service
- Controllers só orquestram
- Um DTO/Mapper por bounded context

### 4. Preserve Behavior
- Não muda regra de negócio
- Tests passam igual
- Mesmo output, código melhor

### 5. Validate Quality
- [ ] Uma responsabilidade por método
- [ ] Sem code duplicado (DRY)
- [ ] Nomes claros e autoexplicativos
- [ ] High cohesion, low coupling
- [ ] Tests continuam passando

---

## Decision Points

**Stack trace grande?** → Start pelo erro mais recente, ignore os antigos.

**Múltiplos problemas no código?** → Refatora um de cada vez, commita separado.

**Não acha a causa?** → Add logs/breakpoints incrementalmente, não assume.

**Test falhu depois do refactor?** → Rollback, valida se regra mudou, refatora de novo.

---

## Quality Checklist

- [ ] Erro corrigido ou refator completo
- [ ] Tests rodando (unit + integration se aplicável)
- [ ] Sem código duplicado
- [ ] Métodos < 20 linhas (ish)
- [ ] Responsabilidade clara por classe/método
- [ ] Commits atômicos e descritivos
