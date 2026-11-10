package br.com.learningscore.dadosacademicos;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DisciplinaRepository extends JpaRepository<Disciplina, Long> {
    List<Disciplina> findByUsuarioIdOrderByNomeAsc(Long usuarioId);
    Optional<Disciplina> findByIdAndUsuarioId(Long id, Long usuarioId);
}
