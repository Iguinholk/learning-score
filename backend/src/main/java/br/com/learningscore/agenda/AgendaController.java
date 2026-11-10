package br.com.learningscore.agenda;

import br.com.learningscore.agenda.AgendaDtos.*;
import br.com.learningscore.seguranca.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class AgendaController {
    private final AgendaService service;

    @GetMapping("/calendario")
    public List<ItemCalendario> calendario(@RequestParam @Min(2000) @Max(2100) int ano,
                                           @RequestParam @Min(1) @Max(12) int mes) {
        return service.mes(UsuarioAutenticado.id(), ano, mes);
    }

    @GetMapping("/eventos/{id}")
    public EventoResponse buscar(@PathVariable Long id) { return service.buscar(UsuarioAutenticado.id(), id); }

    @PostMapping("/eventos")
    @ResponseStatus(HttpStatus.CREATED)
    public EventoResponse criar(@Valid @RequestBody EventoRequest r) { return service.criar(UsuarioAutenticado.id(), r); }

    @PutMapping("/eventos/{id}")
    public EventoResponse atualizar(@PathVariable Long id, @Valid @RequestBody EventoRequest r) {
        return service.atualizar(UsuarioAutenticado.id(), id, r);
    }

    @DeleteMapping("/eventos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) { service.excluir(UsuarioAutenticado.id(), id); }
}
