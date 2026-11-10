package br.com.learningscore.privacidade;

import br.com.learningscore.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ConsentimentoService {
    private final ConsentimentoRepository repo;
    private final UsuarioRepository usuarios;

    @Transactional
    public Consentimento registrar(Long usuarioId, TipoConsentimento tipo, boolean concedido) {
        Consentimento c = repo.findByUsuarioIdAndTipo(usuarioId, tipo).orElseGet(() -> {
            Consentimento novo = new Consentimento();
            novo.setUsuario(usuarios.getReferenceById(usuarioId));
            novo.setTipo(tipo);
            return novo;
        });
        c.setConcedido(concedido);
        c.setRegistradoEm(LocalDateTime.now());
        return repo.save(c);
    }

    @Transactional(readOnly = true)
    public boolean possui(Long usuarioId, TipoConsentimento tipo) {
        return repo.findByUsuarioIdAndTipo(usuarioId, tipo).map(Consentimento::isConcedido).orElse(false);
    }
}
