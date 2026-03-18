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

import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

/**
 *
 * @author brian
 */
public class TransactionLogger {

    private final System.Logger log = System.getLogger(getClass().getName());

    public TransactionLogger() {
    }

    @PostLoad
    public void logLoad(Object object) {
        if (log.isLoggable(System.Logger.Level.DEBUG)) {
            log.log(System.Logger.Level.DEBUG, "Loaded '" + object +"' into persistent context");
        }
    }

    @PrePersist
    public void logPersist(Object object) {
        logTransaction(object, TransactionType.CREATE);
    }

    @PreRemove
    public void logRemove(Object object) {
        logTransaction(object, TransactionType.REMOVE);
    }

    @PreUpdate
    public void logUpdate(Object object) {
        logTransaction(object, TransactionType.MERGE);
    }

    private void logTransaction(Object object, TransactionType transactionType) {
        if (log.isLoggable(System.Logger.Level.DEBUG)) {
            log.log(System.Logger.Level.DEBUG, "Performing '" + transactionType + "' on " + object);
        }
    }
}