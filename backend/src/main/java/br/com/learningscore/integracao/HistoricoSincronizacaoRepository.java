package br.com.learningscore.integracao;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HistoricoSincronizacaoRepository extends JpaRepository<HistoricoSincronizacao, Long> {
    Optional<HistoricoSincronizacao> findFirstByUsuarioIdAndTipoOrderByIniciadoEmDesc(Long usuarioId, HistoricoSincronizacao.Tipo tipo);
    List<HistoricoSincronizacao> findTop20ByUsuarioIdOrderByIniciadoEmDesc(Long usuarioId);
}
