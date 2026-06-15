package br.com.budgetflow.features.categorias.api;

import br.com.budgetflow.common.enums.ClassificacaoCategoria;
import br.com.budgetflow.common.enums.NaturezaFinanceira;
import br.com.budgetflow.features.categorias.dto.CategoriaRequestDTO;
import br.com.budgetflow.features.categorias.dto.CategoriaResponseDTO;
import jakarta.validation.Valid;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import br.com.budgetflow.features.categorias.service.CategoriaService;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {
    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CategoriaResponseDTO> create(@Valid @RequestBody CategoriaRequestDTO categoriaDTO) {
        CategoriaResponseDTO response = categoriaService.create(categoriaDTO);
        return ResponseEntity
            .created(URI.create("/api/categorias/" + response.id()))
            .body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<Page<CategoriaResponseDTO>> findAll(
        @RequestParam(required = false) ClassificacaoCategoria classificacao,
        @RequestParam(required = false) NaturezaFinanceira tipoCategoria,
        @RequestParam(required = false) String nomeUsuario,
        @RequestParam(required = false, name = "q") String search,
        Pageable pageable
    ) {
        Page<CategoriaResponseDTO> categorias = categoriaService.findAll(classificacao, tipoCategoria, nomeUsuario, search, pageable);
        return ResponseEntity.ok(categorias);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CategoriaResponseDTO> findById(@PathVariable Long id) {
        CategoriaResponseDTO categoria = categoriaService.findById(id);
        return ResponseEntity.ok(categoria);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CategoriaResponseDTO> update(
        @PathVariable Long id, 
        @Valid @RequestBody CategoriaRequestDTO categoriaDTO
    ) {
        CategoriaResponseDTO updatedCategoria = categoriaService.update(id, categoriaDTO);
        return ResponseEntity.ok(updatedCategoria);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoriaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
