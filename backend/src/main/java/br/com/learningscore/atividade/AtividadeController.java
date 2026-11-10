package br.com.learningscore.atividade;

import br.com.learningscore.atividade.AtividadeDtos.*;
import br.com.learningscore.seguranca.UsuarioAutenticado;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/atividades")
@RequiredArgsConstructor
public class AtividadeController {
    private final AtividadeService service;

    @GetMapping
    public Pagina listar(@RequestParam(required = false) StatusAtividade status,
                         @RequestParam(required = false) Long disciplinaId,
                         @RequestParam(defaultValue = "0") int pagina,
                         @RequestParam(defaultValue = "20") int tamanho) {
        return service.listar(UsuarioAutenticado.id(), status, disciplinaId, pagina, tamanho);
    }

    @GetMapping("/{id}")
    public Response buscar(@PathVariable Long id) { return service.buscar(UsuarioAutenticado.id(), id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response criar(@Valid @RequestBody Request r) { return service.criar(UsuarioAutenticado.id(), r); }

    @PutMapping("/{id}")
    public Response atualizar(@PathVariable Long id, @Valid @RequestBody Request r) {
        return service.atualizar(UsuarioAutenticado.id(), id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) { service.excluir(UsuarioAutenticado.id(), id); }
}
