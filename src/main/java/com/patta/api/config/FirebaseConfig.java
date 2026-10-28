package com.patta.api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Cria os beans do Firebase: a aplicação, o Firestore e o Firebase Auth.
 * Todos usam a mesma aplicação, iniciada com a conta de serviço.
 */
@Configuration
public class FirebaseConfig {

    private final String caminhoContaServico;

    // O caminho vem da configuração e é obrigatório. A chave privada fica fora do código-fonte
    // e do JAR, então não há valor padrão apontando para dentro do projeto.
    public FirebaseConfig(@Value("${firebase.service-account-path}") String caminhoContaServico) {
        this.caminhoContaServico = caminhoContaServico;
    }

    /** Inicia o app Firebase e encerra seus serviços quando o contexto Spring termina. */
    @Bean(destroyMethod = "delete")
    public FirebaseApp firebaseApp() throws IOException {
        synchronized (FirebaseApp.class) {
            Optional<FirebaseApp> appPadrao = FirebaseApp.getApps().stream()
                    .filter(app -> FirebaseApp.DEFAULT_APP_NAME.equals(app.getName()))
                    .findFirst();
            if (appPadrao.isPresent()) {
                return appPadrao.get();
            }

            // O try-with-resources fecha somente o arquivo após carregar as credenciais.
            try (InputStream conta = abrirContaServico()) {
                return FirebaseApp.initializeApp(FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(conta))
                        .build());
            }
        }
    }

    /**
     * O ciclo de vida do cliente Firestore pertence ao FirebaseApp; o SDK o fecha
     * e remove do cache quando FirebaseApp.delete() é chamado.
     */
    @Bean(destroyMethod = "")
    public Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }

    /** Cliente do Firebase Auth, usado para validar tokens e gerenciar usuários. */
    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    // Abre o arquivo da conta de serviço com uma mensagem clara se ele não existir
    private InputStream abrirContaServico() throws IOException {
        try {
            return Files.newInputStream(Path.of(caminhoContaServico));
        } catch (NoSuchFileException exception) {
            throw new IllegalStateException(
                    "Arquivo da conta de serviço não encontrado. Confira firebase.service-account-path.", exception);
        }
    }
}
