package br.com.learningscore.privacidade;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ConsentimentoRepository extends JpaRepository<Consentimento, Long> {
    List<Consentimento> findByUsuarioId(Long usuarioId);
    Optional<Consentimento> findByUsuarioIdAndTipo(Long usuarioId, TipoConsentimento tipo);
}
