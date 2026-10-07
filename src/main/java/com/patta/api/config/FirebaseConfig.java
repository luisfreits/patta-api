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
import java.nio.file.Path;

@Configuration // Diz pro Spring que essa classe cria objetos (beans) que o projeto vai usar
public class FirebaseConfig {

    // O caminho vem do ambiente, para que a chave privada não faça parte do código-fonte.
    // Se não existir firebase.service-account-path no application.properties, usa o valor depois dos ":"
    @Value("${firebase.service-account-path:src/main/resources/firebase-service-account.json}")
    private String serviceAccountPath;

    // Bean 1: a conexão principal com o Firebase
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // Se o Firebase já foi iniciado, reaproveita (evita erro de iniciar duas vezes)
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        // A chave é carregada de um arquivo externo e nunca empacotada dentro do JAR.
        // try-with-resources: fecha o arquivo sozinho quando terminar de ler
        try (InputStream serviceAccount = Files.newInputStream(Path.of(serviceAccountPath))) {
            FirebaseOptions options = FirebaseOptions.builder()
                    // Usa o JSON da conta de serviço como credencial (a "senha" do servidor)
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            // Inicia o Firebase com essas configurações
            return FirebaseApp.initializeApp(options);
        }
    }

    // Bean 2: o banco de dados (Firestore), usado pelos services pra ler e salvar dados
    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) { // O Spring injeta o bean acima aqui
        return FirestoreClient.getFirestore(firebaseApp);
    }

    // Usa a mesma aplicação Firebase para autenticar tokens e gerenciar usuários.
    // Bean 3: o que valida o token e devolve o uid
    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }
}