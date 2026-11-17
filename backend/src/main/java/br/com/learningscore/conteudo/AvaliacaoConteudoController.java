package br.com.learningscore.conteudo;
import br.com.learningscore.conteudo.ConteudoDtos.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/avaliacoes-conteudo")
@RequiredArgsConstructor
public class AvaliacaoConteudoController {
    private final ConteudoService service;
    @ModelAttribute
    public void privacidade(HttpServletResponse res) {
        res.setHeader("Cache-Control", "no-store");
        res.setHeader("Referrer-Policy", "no-referrer");
        res.setHeader("X-Robots-Tag", "noindex, nofollow");
    }
    @GetMapping
    public AvaliacaoResponse consultar(@RequestHeader("X-Avaliacao-Token") String token) { return service.consultar(token); }
    @PostMapping("/confirmar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmar(@Valid @RequestBody ConfirmacaoRequest r) { service.confirmar(r); }
}
