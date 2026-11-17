package br.com.learningscore.usuario;

import br.com.learningscore.common.RegraNegocioException;
import br.com.learningscore.privacidade.ConsentimentoService;
import br.com.learningscore.privacidade.TipoConsentimento;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final ConsentimentoService consentimentos;

    @Transactional
    public Usuario registrar(UsuarioDtos.RegistroRequest req) {
        String email = req.email().trim().toLowerCase();
        if (repo.existsByEmailIgnoreCase(email)) {
            throw new RegraNegocioException("Não foi possível concluir o cadastro com estes dados.");
        }
        Usuario u = new Usuario();
        u.setNome(req.nome().trim());
        u.setEmail(email);
        u.setSenhaHash(encoder.encode(req.senha()));   // BCrypt (nunca guardamos a senha em texto)
        u = repo.save(u);
        consentimentos.registrar(u.getId(), TipoConsentimento.TERMOS_USO, true);
        consentimentos.registrar(u.getId(), TipoConsentimento.INTEGRACOES, req.consentiuIntegracoes());
        consentimentos.registrar(u.getId(), TipoConsentimento.USO_DADOS_EXTERNOS, req.consentiuUsoDadosExternos());
        return u;
    }
}
