package br.com.learningscore.conteudo;
import br.com.learningscore.conteudo.ConteudoDtos.*;
import br.com.learningscore.seguranca.UsuarioAutenticado;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/conteudos")
@RequiredArgsConstructor
public class ConteudoController {
    private final ConteudoService service;
    @GetMapping
    public List<Response> aprovados() { return service.aprovados(); }
    @GetMapping({"/minhas-sugestoes", "/pendentes"})
    public List<SugestaoResponse> minhas() { return service.minhas(UsuarioAutenticado.id()); }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SugestaoResponse sugerir(@Valid @RequestBody Request r) { return service.sugerir(UsuarioAutenticado.id(), r); }
    @PostMapping("/{id}/reenviar-convite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reenviar(@PathVariable Long id) { service.reenviar(UsuarioAutenticado.id(), id); }
}
