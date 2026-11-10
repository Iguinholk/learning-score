package br.com.learningscore.integracao;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PlataformaExternaRepository extends JpaRepository<PlataformaExterna, Long> {
    List<PlataformaExterna> findByUsuarioIdOrderByNomeAsc(Long usuarioId);
    Optional<PlataformaExterna> findByIdAndUsuarioId(Long id, Long usuarioId);
    List<PlataformaExterna> findByAtivaTrue();
    boolean existsByUsuarioIdAndNomeIgnoreCase(Long usuarioId, String nome);
}
