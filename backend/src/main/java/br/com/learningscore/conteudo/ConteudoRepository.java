package br.com.learningscore.conteudo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConteudoRepository extends JpaRepository<Conteudo, Long> {
    List<Conteudo> findByAprovadoTrueOrderByTituloAsc();
    List<Conteudo> findBySugeridoPorAndAprovadoFalse(Long usuarioId);
    List<Conteudo> findByAprovadoFalseOrderByCriadoEmDesc();
}
