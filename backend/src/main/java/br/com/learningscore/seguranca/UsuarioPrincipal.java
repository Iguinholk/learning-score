package br.com.learningscore.seguranca;

import br.com.learningscore.usuario.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Usuário autenticado (guardado na sessão). Contém o id usado em TODAS as consultas para isolar dados. */
public class UsuarioPrincipal implements UserDetails {
    private final Long id;
    private final String nome;
    private final String email;
    private final String senhaHash;
    private final String papel;
    private final boolean ativo;

    public UsuarioPrincipal(Usuario u) {
        this.id = u.getId();
        this.nome = u.getNome();
        this.email = u.getEmail();
        this.senhaHash = u.getSenhaHash();
        this.papel = u.getPapel().name();
        this.ativo = u.isAtivo();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_" + papel)); }
    @Override public String getPassword() { return senhaHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return ativo; }
}
