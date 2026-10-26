package com.patta.api.config;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FirebaseConfigLifecycleTest {

    @Test
    void springDelegatesResourceCleanupToFirebaseApp() throws NoSuchMethodException {
        Bean firebaseAppBean = FirebaseConfig.class.getMethod("firebaseApp").getAnnotation(Bean.class);
        Bean firestoreBean = FirebaseConfig.class
                .getMethod("firestore", FirebaseApp.class).getAnnotation(Bean.class);

        assertEquals("delete", firebaseAppBean.destroyMethod());
        assertEquals("", firestoreBean.destroyMethod());
    }

    @Test
    void deletingFirebaseAppRemovesItsFirestoreClientFromSdkCache() {
        String appName = "lifecycle-test-" + UUID.randomUUID();
        FirebaseApp firstApp = createApp(appName);
        Firestore firstFirestore = FirestoreClient.getFirestore(firstApp);

        firstApp.delete();

        assertThrows(IllegalStateException.class, () -> FirestoreClient.getFirestore(firstApp));

        FirebaseApp secondApp = createApp(appName);
        try {
            Firestore secondFirestore = FirestoreClient.getFirestore(secondApp);
            assertNotSame(firstFirestore, secondFirestore);
        } finally {
            secondApp.delete();
        }
    }

    private FirebaseApp createApp(String appName) {
        GoogleCredentials testCredentials = GoogleCredentials.create(
                new AccessToken("test-token-not-used", Date.from(Instant.now().plusSeconds(3600))));
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(testCredentials)
                .setProjectId("patta-lifecycle-test")
                .build();
        return FirebaseApp.initializeApp(options, appName);
    }
}
