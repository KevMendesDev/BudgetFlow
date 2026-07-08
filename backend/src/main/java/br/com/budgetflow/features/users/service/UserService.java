package br.com.budgetflow.features.users.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.budgetflow.common.exceptions.BusinessRuleException;
import br.com.budgetflow.features.users.domain.Role;
import br.com.budgetflow.features.users.domain.User;
import br.com.budgetflow.features.users.dto.UserResponseDTO;
import br.com.budgetflow.features.users.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDTO> findAll(Pageable pageable) {
        Page<User> users = this.userRepository.findAll(pageable);
        return users.map(UserResponseDTO::new);
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    }

    @Transactional
    public UserResponseDTO updateUserRoles(Long userId, List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new BusinessRuleException("O usuário deve possuir ao menos uma role");
        }
        User user = this.findById(userId);
        user.getRoles().clear();
        user.getRoles().addAll(roles);
        return new UserResponseDTO(this.userRepository.save(user));
    }
}