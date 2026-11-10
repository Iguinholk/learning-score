package br.com.learningscore.notificacao;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    List<Notificacao> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);
    Optional<Notificacao> findByIdAndUsuarioId(Long id, Long usuarioId);
    boolean existsByUsuarioIdAndChaveUnica(Long usuarioId, String chaveUnica);
    long countByUsuarioIdAndLidaFalse(Long usuarioId);
}
