package br.com.learningscore.recomendacao;

import br.com.learningscore.recomendacao.RecomendacaoDtos.PorDisciplina;
import br.com.learningscore.seguranca.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recomendacoes")
@RequiredArgsConstructor
public class RecomendacaoController {
    private final RecomendacaoService service;

    @GetMapping
    public List<PorDisciplina> listar() { return service.listar(UsuarioAutenticado.id()); }
}
