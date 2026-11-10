package br.com.learningscore.conteudo;

import br.com.learningscore.conteudo.ConteudoDtos.*;
import br.com.learningscore.seguranca.UsuarioAutenticado;
import br.com.learningscore.seguranca.UsuarioPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conteudos")
@RequiredArgsConstructor
public class ConteudoController {
    private final ConteudoService service;

    /** Somente conteúdos aprovados por professores. */
    @GetMapping
    public List<Response> aprovados() { return service.aprovados(); }

    /** Estudante: suas sugestões em análise. Professor: todas as pendentes. */
    @GetMapping("/pendentes")
    public List<Response> pendentes() {
        UsuarioPrincipal p = UsuarioAutenticado.principal();
        boolean professor = p.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PROFESSOR"));
        return service.pendentes(p.getId(), professor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response sugerir(@Valid @RequestBody Request r) { return service.sugerir(UsuarioAutenticado.id(), r); }

    @PatchMapping("/{id}/aprovar")
    @PreAuthorize("hasRole('PROFESSOR')")
    public Response aprovar(@PathVariable Long id) {
        return service.aprovar(id, UsuarioAutenticado.principal().getNome());
    }
}
