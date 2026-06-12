package br.com.budgetflow.features.movimentacoes.api;

import br.com.budgetflow.features.movimentacoes.criteria.TransacaoFilterCriteria;
import br.com.budgetflow.features.movimentacoes.dto.SincronizacaoRecorrentesResponseDTO;
import br.com.budgetflow.features.movimentacoes.dto.TransacaoRequestDTO;
import br.com.budgetflow.features.movimentacoes.dto.TransacaoResponseDTO;
import br.com.budgetflow.features.movimentacoes.service.TransacaoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/transacoes")
public class TransacaoController {

    private final TransacaoService transacaoService;

    public TransacaoController(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<TransacaoResponseDTO> create(@Valid @RequestBody TransacaoRequestDTO requestDTO) {
        TransacaoResponseDTO response = transacaoService.create(requestDTO);
        return ResponseEntity
                .created(URI.create("/api/transacoes/" + response.id()))
                .body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<Page<TransacaoResponseDTO>> findAll(
            @Valid TransacaoFilterCriteria criteria,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<TransacaoResponseDTO> transacoes = transacaoService.findAll(criteria, pageable);
        return ResponseEntity.ok(transacoes);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<TransacaoResponseDTO> findById(@PathVariable Long id) {
        TransacaoResponseDTO transacao = transacaoService.findById(id);
        return ResponseEntity.ok(transacao);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<TransacaoResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody TransacaoRequestDTO requestDTO
    ) {
        TransacaoResponseDTO updatedTransacao = transacaoService.update(id, requestDTO);
        return ResponseEntity.ok(updatedTransacao);
    }

    @PostMapping("/sincronizar-recorrentes")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<SincronizacaoRecorrentesResponseDTO> sincronizarRecorrentes(@RequestParam Long periodoId) {
        return ResponseEntity.ok(transacaoService.sincronizarRecorrentes(periodoId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transacaoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
