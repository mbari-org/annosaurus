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

package org.mbari.vars.annotation.messaging

import io.reactivex.rxjava3.subjects.{PublishSubject, Subject}
import org.slf4j.LoggerFactory

/**
  * This is the shared message bus. All publishers whould listen to this bus and
  * publish the appropriate events to their subscribers.
  *
  * MessageBus.RxSubject: Subject[Any]
  *      ^
  *      |
  * AnnotationPublisher.publish(msg)
 **/
object MessageBus {

  private lazy val log = LoggerFactory.getLogger(getClass)

  val RxSubject: Subject[Any] =
    PublishSubject.create[Any]().toSerialized

  if (log.isDebugEnabled()) {
    RxSubject.subscribe(m => log.debug(m.toString))
  }

}
