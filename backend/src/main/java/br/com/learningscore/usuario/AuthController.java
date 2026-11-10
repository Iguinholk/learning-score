package br.com.learningscore.usuario;

import br.com.learningscore.seguranca.UsuarioPrincipal;
import br.com.learningscore.usuario.UsuarioDtos.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

/** RF10: cadastro, login e consulta do usuário logado. O logout é tratado pelo Spring Security (POST /api/auth/logout). */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UsuarioService usuarioService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@Valid @RequestBody RegistroRequest req) {
        return UsuarioResponse.de(usuarioService.registrar(req));
    }

    @PostMapping("/login")
    public UsuarioResponse login(@Valid @RequestBody LoginRequest req,
                                 HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(req.email().trim().toLowerCase(), req.senha()));
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
        if (request.getSession(false) != null) request.changeSessionId();   // evita session fixation
        contextRepository.saveContext(ctx, request, response);
        UsuarioPrincipal p = (UsuarioPrincipal) auth.getPrincipal();
        return new UsuarioResponse(p.getId(), p.getNome(), p.getUsername(),
                p.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""));
    }

    @GetMapping("/me")
    public UsuarioResponse me() {
        UsuarioPrincipal p = br.com.learningscore.seguranca.UsuarioAutenticado.principal();
        return new UsuarioResponse(p.getId(), p.getNome(), p.getUsername(),
                p.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""));
    }
}
