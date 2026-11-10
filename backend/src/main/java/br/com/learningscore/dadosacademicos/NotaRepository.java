package br.com.learningscore.dadosacademicos;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NotaRepository extends JpaRepository<Nota, Long> {
    /** Garante que a nota pertence a uma disciplina do estudante logado. */
    Optional<Nota> findByIdAndDisciplinaUsuarioId(Long id, Long usuarioId);
    List<Nota> findByDisciplinaUsuarioId(Long usuarioId);
}
