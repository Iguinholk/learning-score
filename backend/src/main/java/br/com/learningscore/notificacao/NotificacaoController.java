package br.com.learningscore.notificacao;

import br.com.learningscore.notificacao.NotificacaoDtos.*;
import br.com.learningscore.seguranca.UsuarioAutenticado;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificacaoController {
    private final NotificacaoService service;

    @GetMapping("/notificacoes")
    public Listagem listar() { return service.listar(UsuarioAutenticado.id()); }

    @PatchMapping("/notificacoes/{id}/lida")
    public Response marcar(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean lida) {
        return service.marcarLida(UsuarioAutenticado.id(), id, lida);
    }

    @PostMapping("/notificacoes/marcar-todas-lidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarTodas() { service.marcarTodasLidas(UsuarioAutenticado.id()); }

    @DeleteMapping("/notificacoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) { service.excluir(UsuarioAutenticado.id(), id); }

    @GetMapping("/preferencias-notificacao")
    public List<PreferenciaItem> preferencias() { return service.listarPreferencias(UsuarioAutenticado.id()); }

    @PutMapping("/preferencias-notificacao")
    public List<PreferenciaItem> salvar(@Valid @RequestBody PreferenciasRequest r) {
        return service.salvarPreferencias(UsuarioAutenticado.id(), r);
    }
}
