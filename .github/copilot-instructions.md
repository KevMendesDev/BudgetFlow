---
name: budgetflow-development
description: "Communication style and code principles for BudgetFlow development. Keep responses brief in Brazilian Portuguese, direct to the point, minimal formality."
---

# BudgetFlow Development Guidelines

## Communication Style

- Fala pouco, só o essencial
- Sem regras cultas de português
- Direto ao ponto
- Resume o que fez em 1-2 linhas
- Faz perguntas claras, sem rodeios

## Code Principles

### Separation of Concerns
- Lógica de negócio separada da API
- Controllers só orquestram
- Services cuidam da regra
- Repositories só acessam dados

### Cohesion & Coupling
- Métodos focados, uma responsabilidade por método
- Classes coesas, alta coesão interna
- Acoplamento mínimo entre módulos
- DTOs/Mappers para isolar domínio

### Clean Code
- Nomes claros e autoexplicativos
- Métodos curtos (< 20 linhas idealmente)
- Validações centralizadas (beans separadas)
- Sem código duplicado (DRY)

### Java (Backend)
- Annotations pra validações (@Valid, customizadas)
- Beans separadas pra validações complexas
- Mappers pra converter Domain ↔ DTO
- Services com métodos atômicos

### Angular (Frontend)
- Components pequenos, reutilizáveis
- Services pra lógica, components pra UI
- RxJS observables com unsub limpo
- Validadores customizados quando necessário
- Design Responsivo e acessível

### Testing
- Testa regra de negócio, não getter/setter
- Mock externo (API, BD)
- Arrange-Act-Assert pattern
