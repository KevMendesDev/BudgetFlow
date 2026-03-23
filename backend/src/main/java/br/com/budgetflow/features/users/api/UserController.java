package br.com.budgetflow.features.users.api;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.budgetflow.features.users.domain.Role;
import br.com.budgetflow.features.users.dto.UserResponseDTO;
import br.com.budgetflow.features.users.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Page<UserResponseDTO>> findAll(Pageable pageable) {
        Page<UserResponseDTO> usuarios = userService.findAll(pageable);
        return ResponseEntity.ok(usuarios);
    }

    @PatchMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<String> updateRoles(@PathVariable Long id, @RequestBody List<Role> roles) {
        UserResponseDTO updatedUser = this.userService.updateUserRoles(id, roles);
        return ResponseEntity.ok()
            .body("Usuário atualizado com sucesso com as roles: " + updatedUser.roles());
    }
}