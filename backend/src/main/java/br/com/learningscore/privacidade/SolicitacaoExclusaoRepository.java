package br.com.learningscore.privacidade;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SolicitacaoExclusaoRepository extends JpaRepository<SolicitacaoExclusao, Long> {
    Optional<SolicitacaoExclusao> findFirstByUsuarioIdAndStatus(Long usuarioId, SolicitacaoExclusao.Status status);
    List<SolicitacaoExclusao> findByStatusAndSolicitadaEmBefore(SolicitacaoExclusao.Status status, LocalDateTime limite);
}
