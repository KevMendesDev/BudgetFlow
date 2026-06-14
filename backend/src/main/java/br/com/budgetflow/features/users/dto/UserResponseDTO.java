package br.com.budgetflow.features.users.dto;

import java.util.Set;

import br.com.budgetflow.features.users.domain.Role;
import br.com.budgetflow.features.users.domain.User;

public record UserResponseDTO(
    Long id,
    String nome,
    String email,
    String telefone,
    Set<Role> roles
) {
    public UserResponseDTO(User user) {
        this(
            user.getId(),
            user.getNome(),
            user.getEmail(),
            user.getTelefone(),
            user.getRoles()
        );
    }
}
