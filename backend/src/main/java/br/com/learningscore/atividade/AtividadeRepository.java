package br.com.learningscore.atividade;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AtividadeRepository extends JpaRepository<Atividade, Long>, JpaSpecificationExecutor<Atividade> {
    Optional<Atividade> findByIdAndUsuarioId(Long id, Long usuarioId);
    List<Atividade> findByUsuarioId(Long usuarioId);
    List<Atividade> findByUsuarioIdAndStatusIn(Long usuarioId, Collection<StatusAtividade> status);
    List<Atividade> findByUsuarioIdAndPrazoBetween(Long usuarioId, LocalDateTime de, LocalDateTime ate);
    List<Atividade> findByUsuarioIdAndStatusInAndPrazoBetween(Long usuarioId, Collection<StatusAtividade> status, LocalDateTime de, LocalDateTime ate);
    long countByUsuarioIdAndStatus(Long usuarioId, StatusAtividade status);

    @Modifying(clearAutomatically = true)
    @Query("update Atividade a set a.status = :novo where a.usuario.id = :uid and a.prazo < :agora and a.status in :abertos")
    int marcarAtrasadas(@Param("uid") Long uid, @Param("agora") LocalDateTime agora,
                        @Param("abertos") Collection<StatusAtividade> abertos, @Param("novo") StatusAtividade novo);

    @Modifying(clearAutomatically = true)
    @Query("update Atividade a set a.status = :novo where a.prazo < :agora and a.status in :abertos")
    int marcarTodasAtrasadas(@Param("agora") LocalDateTime agora,
                             @Param("abertos") Collection<StatusAtividade> abertos, @Param("novo") StatusAtividade novo);
}
