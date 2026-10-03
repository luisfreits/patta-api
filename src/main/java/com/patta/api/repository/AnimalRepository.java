import com.google.cloud.firestore.DocumentSnapshot;
import com.patta.api.model.AnimalModel;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
@RequiredArgsConstructor
public class AnimalRepository {

    private static final String COLLECTION = "animais";

    // Mesmo padrão do UserRepository: o Spring injeta o Firestore.
    private final Firestore firestore;

    public AnimalModel save(AnimalModel animal) throws ExecutionException, InterruptedException {
        DocumentReference ref;

        if (animal.getId() == null || animal.getId().isBlank()) {
            ref = firestore.collection(COLLECTION).document();
            animal.setId(ref.getId());
        } else {
            ref = firestore.collection(COLLECTION).document(animal.getId());
        }

        ref.set(animal).get();
        return animal;
    }

    // Lista os animais de um usuário: aqui o "1 usuário -> N animais" acontece.
    public List<AnimalModel> findByUserId(String userId) throws ExecutionException, InterruptedException {
        return firestore.collection(COLLECTION)
                .whereEqualTo("userId", userId)
                .get().get()
                .toObjects(AnimalModel.class);
    }

    public Optional<AnimalModel> findById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = firestore.collection(COLLECTION).document(id).get().get();
        if (!doc.exists()) return Optional.empty();

        AnimalModel animal = doc.toObject(AnimalModel.class);
        if (animal != null) {
            animal.setId(doc.getId()).id().userId().nome().sexo().idade().animal().castrado().build();
        }
        return Optional.ofNullable(animal);
    }

    public void deleteById(String id) throws ExecutionException, InterruptedException {
        firestore.collection(COLLECTION).document(id).delete().get();
    }
}