package br.com.learningscore.conteudo;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface ConteudoRepository extends JpaRepository<Conteudo, Long> {
    List<Conteudo> findByAprovadoTrueOrderByTituloAsc();
    List<Conteudo> findBySugeridoPorOrderByCriadoEmDesc(Long usuarioId);
    Optional<Conteudo> findByTokenHash(String tokenHash);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conteudo c where c.tokenHash = :hash")
    Optional<Conteudo> bloquearPorToken(@Param("hash") String hash);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conteudo c where c.id = :id and c.sugeridoPor = :uid")
    Optional<Conteudo> bloquearMinhaSugestao(@Param("id") Long id, @Param("uid") Long uid);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conteudo c where c.id = :id")
    Optional<Conteudo> bloquearPorId(@Param("id") Long id);
    @Query("select c.id from Conteudo c where c.respondidoEm is not null and c.emailResultadoEnviado = false and c.sugeridoPor is not null order by c.respondidoEm")
    List<Long> resultadosSemEmail(Pageable pagina);
}
