package br.com.learningscore.privacidade;

import br.com.learningscore.privacidade.PrivacidadeDtos.*;
import br.com.learningscore.seguranca.UsuarioAutenticado;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/privacidade")
@RequiredArgsConstructor
public class PrivacidadeController {
    private final PrivacidadeService service;

    @GetMapping("/meus-dados")
    public MeusDados meusDados() { return service.meusDados(UsuarioAutenticado.id()); }

    @GetMapping("/consentimentos")
    public List<ConsentimentoResponse> consentimentos() { return service.listarConsentimentos(UsuarioAutenticado.id()); }

    @PutMapping("/consentimentos")
    public List<ConsentimentoResponse> definir(@Valid @RequestBody ConsentimentoRequest r) {
        return service.definirConsentimento(UsuarioAutenticado.id(), r);
    }

    @PostMapping("/exclusao")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExclusaoResponse solicitar(@Valid @RequestBody ExclusaoRequest r) {
        return service.solicitarExclusao(UsuarioAutenticado.id(), r.senha());
    }

    @DeleteMapping("/exclusao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar() { service.cancelarExclusao(UsuarioAutenticado.id()); }
}
