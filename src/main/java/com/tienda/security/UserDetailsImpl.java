package com.tienda.security;

import com.tienda.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Data
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {

    private Usuario usuario;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (usuario.getRol() != null) {
            return Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getCodigo())
            );
        }
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return usuario.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return usuario.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !usuario.isBloqueado();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return usuario.isActivo();
    }

    public Long getId() {
        return usuario.getId();
    }

    public String getEmail() {
        return usuario.getEmail();
    }

    public String getNombreCompleto() {
        return usuario.getNombreCompleto();
    }
}