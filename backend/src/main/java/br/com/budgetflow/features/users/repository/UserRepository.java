package br.com.budgetflow.features.users.repository;

import br.com.budgetflow.features.users.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);
}
