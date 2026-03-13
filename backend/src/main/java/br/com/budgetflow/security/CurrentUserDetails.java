package br.com.budgetflow.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class CurrentUserDetails implements UserDetails {

    private final String userId;
    private final String cpf;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public CurrentUserDetails(String userId, String cpf, String password,
                               Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.cpf = cpf;
        this.password = password;
        this.authorities = authorities;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return cpf;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
