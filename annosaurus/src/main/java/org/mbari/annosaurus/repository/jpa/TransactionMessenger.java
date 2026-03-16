package org.mbari.annosaurus.repository.jpa;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import org.mbari.annosaurus.repository.jpa.entity.AssociationEntity;
import org.mbari.annosaurus.repository.jpa.entity.ObservationEntity;

import java.util.Optional;
import java.util.UUID;

public class TransactionMessenger {

    public enum Action {
        CREATE,
        UPDATE,
        REMOVE
    }

    public record Message(Action action, Class<?> clazz, UUID uuid) {}

    private static final @NonNull Subject<Object> rxSubject = PublishSubject.create().toSerialized();


    public static Subject<?> getRxSubject() {
        return rxSubject;
    }

    public void notifyPersist(Object o) {
        extractUuid(o).ifPresent(uuid -> getRxSubject().onNext(new Message(Action.CREATE, o.getClass(), uuid)));
    }


    private Optional<UUID> extractUuid(Object o) {
        return switch(o) {
            case ObservationEntity observation -> Optional.of(observation.getUuid());
            case AssociationEntity association -> Optional.of(association.getUuid());
            default -> Optional.empty();
        }
    }
}
