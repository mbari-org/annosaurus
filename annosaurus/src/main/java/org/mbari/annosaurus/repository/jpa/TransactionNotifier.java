/*
 * Copyright 2017 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbari.annosaurus.repository.jpa;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.mbari.annosaurus.repository.jpa.entity.AssociationEntity;
import org.mbari.annosaurus.repository.jpa.entity.ObservationEntity;

import java.util.Optional;
import java.util.UUID;

public class TransactionNotifier {

    public enum Action {
        CREATE,
        UPDATE,
        REMOVE
    }

    public record Message<T>(Action action, Class<T> clazz, UUID uuid) {}

    private static final @NonNull Subject<Object> rxSubject = PublishSubject.create().toSerialized();


    public static Subject<Object> getRxSubject() {
        return rxSubject;
    }

    @PostPersist
    public void notifyPersist(Object o) {
        notify(Action.CREATE, o);
    }

    @PostUpdate
    public void notifyUpdate(Object o) {
        notify(Action.UPDATE, o);
    }

    @PostRemove
    public void notifyRemove(Object o) {
        notify(Action.REMOVE, o);
    }

    private void notify(Action action, Object obj) {
        extractUuid(obj).ifPresent(uuid -> getRxSubject().onNext(new Message<>(action, obj.getClass(), uuid)));
    }

    private Optional<UUID> extractUuid(Object o) {
        return switch(o) {
            case ObservationEntity observation -> Optional.of(observation.getUuid());
            case AssociationEntity association -> Optional.of(association.getUuid());
            default -> Optional.empty();
        };
    }
}
