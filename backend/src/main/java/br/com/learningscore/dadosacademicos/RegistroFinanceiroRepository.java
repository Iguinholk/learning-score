package br.com.learningscore.dadosacademicos;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RegistroFinanceiroRepository extends JpaRepository<RegistroFinanceiro, Long> {
    List<RegistroFinanceiro> findByUsuarioIdOrderByVencimentoDesc(Long usuarioId);
    Optional<RegistroFinanceiro> findByIdAndUsuarioId(Long id, Long usuarioId);
}
