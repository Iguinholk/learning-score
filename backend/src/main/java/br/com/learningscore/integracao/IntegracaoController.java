package br.com.learningscore.integracao;

import br.com.learningscore.integracao.IntegracaoDtos.*;
import br.com.learningscore.seguranca.UsuarioAutenticado;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/integracoes")
@RequiredArgsConstructor
public class IntegracaoController {
    private final IntegracaoService service;

    @GetMapping
    public List<PlataformaResponse> listar() { return service.listar(UsuarioAutenticado.id()); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlataformaResponse associar(@Valid @RequestBody PlataformaRequest r) { return service.associar(UsuarioAutenticado.id(), r); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) { service.remover(UsuarioAutenticado.id(), id); }

    @PostMapping("/{id}/sincronizar")
    public HistoricoResponse sincronizar(@PathVariable Long id) { return service.sincronizarManual(UsuarioAutenticado.id(), id); }

    @GetMapping("/historico")
    public List<HistoricoResponse> historico() { return service.historico(UsuarioAutenticado.id()); }
}
