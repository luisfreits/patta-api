package com.patta.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;

/**
 * Dados enviados para criar uma conta de usuário.
 */
public record CadastroRequestDTO(

        // Nome completo: obrigatório, até 100 caracteres
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
        String nome,

        // E-mail: obrigatório, formato válido, até 254 caracteres (limite padrão do RFC)
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.")
        String email,

        // Telefone: obrigatório, até 30 caracteres
        @NotBlank(message = "O telefone é obrigatório.")
        @Size(max = 30, message = "O telefone deve ter no máximo 30 caracteres.")
        String telefone,

        // Senha: entre 6 e 128 caracteres (6 é o mínimo exigido pelo Firebase)
        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 6, max = 128, message = "A senha deve ter entre 6 e 128 caracteres.")
        String senha,

        // Confirmação: só verifica presença; a comparação com "senha" está abaixo
        @NotBlank(message = "A confirmação da senha é obrigatória.")
        String confirmaSenha
) {

    /**
     * Validação customizada: a senha e a confirmação precisam ser iguais.
     * O Bean Validation chama este método porque ele começa com "is" e
     * retorna boolean. Se retornar false, a requisição é rejeitada com 400
     * (desde que o controller use @Valid).
     */
    @AssertTrue(message = "A confirmação da senha não confere com a senha.")
    @JsonIgnore // Impede que o Jackson o inclua no JSON como campo "senhasIguais"
    public boolean isSenhasIguais() {
        return senha != null && senha.equals(confirmaSenha);
    }
}
