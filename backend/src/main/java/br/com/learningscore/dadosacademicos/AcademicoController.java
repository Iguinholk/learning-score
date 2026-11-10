package br.com.learningscore.dadosacademicos;

import br.com.learningscore.dadosacademicos.AcademicoDtos.*;
import br.com.learningscore.seguranca.UsuarioAutenticado;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** RF04 — disciplinas, notas e informações financeiras. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AcademicoController {
    private final DisciplinaService disciplinas;
    private final FinanceiroService financeiro;

    @GetMapping("/disciplinas")
    public List<DisciplinaResponse> listar() { return disciplinas.listar(UsuarioAutenticado.id()); }

    @PostMapping("/disciplinas")
    @ResponseStatus(HttpStatus.CREATED)
    public DisciplinaResponse criar(@Valid @RequestBody DisciplinaRequest r) { return disciplinas.criar(UsuarioAutenticado.id(), r); }

    @PutMapping("/disciplinas/{id}")
    public DisciplinaResponse atualizar(@PathVariable Long id, @Valid @RequestBody DisciplinaRequest r) {
        return disciplinas.atualizar(UsuarioAutenticado.id(), id, r);
    }

    @DeleteMapping("/disciplinas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) { disciplinas.excluir(UsuarioAutenticado.id(), id); }

    @PostMapping("/disciplinas/{id}/notas")
    @ResponseStatus(HttpStatus.CREATED)
    public NotaResponse lancarNota(@PathVariable Long id, @Valid @RequestBody NotaRequest r) {
        return disciplinas.lancarNota(UsuarioAutenticado.id(), id, r);
    }

    @PutMapping("/notas/{id}")
    public NotaResponse atualizarNota(@PathVariable Long id, @Valid @RequestBody NotaRequest r) {
        return disciplinas.atualizarNota(UsuarioAutenticado.id(), id, r);
    }

    @DeleteMapping("/notas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirNota(@PathVariable Long id) { disciplinas.excluirNota(UsuarioAutenticado.id(), id); }

    @GetMapping("/financeiro")
    public FinanceiroListagem listarFinanceiro() { return financeiro.listar(UsuarioAutenticado.id()); }

    @PostMapping("/financeiro")
    @ResponseStatus(HttpStatus.CREATED)
    public FinanceiroResponse criarFinanceiro(@Valid @RequestBody FinanceiroRequest r) { return financeiro.criar(UsuarioAutenticado.id(), r); }

    @PutMapping("/financeiro/{id}")
    public FinanceiroResponse atualizarFinanceiro(@PathVariable Long id, @Valid @RequestBody FinanceiroRequest r) {
        return financeiro.atualizar(UsuarioAutenticado.id(), id, r);
    }

    @DeleteMapping("/financeiro/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirFinanceiro(@PathVariable Long id) { financeiro.excluir(UsuarioAutenticado.id(), id); }
}
