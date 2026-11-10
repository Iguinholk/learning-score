package br.com.learningscore.recomendacao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecomendacaoRepository extends JpaRepository<Recomendacao, Long> {
    List<Recomendacao> findByUsuarioIdOrderByCompatibilidadeDesc(Long usuarioId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Recomendacao r where r.disciplina.id = :disciplinaId")
    void apagarDaDisciplina(@Param("disciplinaId") Long disciplinaId);
}
