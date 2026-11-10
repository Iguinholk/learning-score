package br.com.learningscore.usuario;

import br.com.learningscore.common.EntidadeBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Identidade única do estudante no Learning Score (RN01). */
@Entity
@Table(name = "usuario")
@Getter
@Setter
public class Usuario extends EntidadeBase {
    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Papel papel = Papel.ESTUDANTE;

    @Column(nullable = false)
    private boolean ativo = true;
}
