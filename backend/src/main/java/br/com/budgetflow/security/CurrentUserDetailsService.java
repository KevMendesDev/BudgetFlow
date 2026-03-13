package br.com.budgetflow.security;

import br.com.budgetflow.features.users.domain.User;
import br.com.budgetflow.features.users.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class CurrentUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CurrentUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String cpf) throws UsernameNotFoundException {
        User user = userRepository.findByCpf(cpf)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + cpf));

        List<SimpleGrantedAuthority> authorities = Arrays.stream(
                user.getRoles().split("\\s*,\\s*"))
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();

        return new CurrentUserDetails(
                String.valueOf(user.getId()),
                user.getCpf(),
                user.getSenha(),
                authorities);
    }
}
