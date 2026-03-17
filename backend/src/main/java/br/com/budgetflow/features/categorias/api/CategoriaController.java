package br.com.budgetflow.features.categorias.api;

import br.com.budgetflow.features.categorias.dto.CategoriaRequestDTO;
import br.com.budgetflow.features.categorias.dto.CategoriaResponseDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.create(categoriaDTO));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<Page<CategoriaResponseDTO>> findAll(Pageable pageable) {
        return ResponseEntity.ok(categoriaService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CategoriaResponseDTO> findById(@PathVariable Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O id da categoria é obrigatório");
        }
        return ResponseEntity.ok(categoriaService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CategoriaResponseDTO> update(
        @PathVariable Long id, 
        @Valid @RequestBody CategoriaRequestDTO categoriaDTO
    ) {
        if (id == null) {
            throw new IllegalArgumentException("O id da categoria é obrigatório");
        }
        return ResponseEntity.ok(categoriaService.update(id, categoriaDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O id da categoria é obrigatório");
        }
        categoriaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
