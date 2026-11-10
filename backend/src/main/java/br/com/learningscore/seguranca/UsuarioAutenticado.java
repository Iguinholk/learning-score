package br.com.learningscore.seguranca;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Acesso ao estudante logado. Os controllers passam este id aos services (isolamento entre estudantes). */
public final class UsuarioAutenticado {
    private UsuarioAutenticado() {}

    public static UsuarioPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioPrincipal p)) {
            throw new IllegalStateException("Nenhum usuário autenticado");
        }
        return p;
    }

    public static Long id() { return principal().getId(); }
}
