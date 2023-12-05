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

package org.mbari.vars.annotation

import org.mbari.vars.annotation.dao.DAOFactory
import org.mbari.vars.annotation.model.{CachedVideoReferenceInfo, _}

/**
  *
  *  Controllers abstract away the messier details of the DAO objects. Controllers are used by
  *  the api classes. You should be able to plug in any implementation of DAO's that you like.
  *  Although currently, I've only created a JPA/SQL version.
  *
  * @author Brian Schlining
  * @since 2016-06-25T17:45:00
  */
package object controllers {

  type BasicDAOFactory = DAOFactory[
    ImagedMoment,
    Observation,
    Association,
    ImageReference,
    CachedAncillaryDatum,
    CachedVideoReferenceInfo,
    ImagedMoment
  ]
}
