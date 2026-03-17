package org.mbari.annosaurus.repository.jpa;

import io.reactivex.rxjava3.subjects.Subject;
import jakarta.persistence.PostPersist;
import org.mbari.annosaurus.messaging.Publisher$;

import java.util.concurrent.atomic.AtomicReference;

public class TransactionMessenger {

    private static final AtomicReference<Subject<Object>> subjectRef = new AtomicReference<>();

    public TransactionMessenger() {

    }

    public static void setSubject(Subject<Object> subject) {
        subjectRef.set(subject);
    }

    public static Subject<Object> getSubject() {
        return subjectRef.get();
    }

    @PostPersist
    public void postPersist(Object object) {
        var subject = subjectRef.get();
        if (subject == null) {
            Publisher$.MODULE$.created(object, subject);
        }

    }
}
