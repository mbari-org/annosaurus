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

package org.mbari.annosaurus.etc.nats

/**
 * Configuration for NATS from application.conf/reference.conf
 * @param url e.g. nats://localhost:4222
 * @param enable true if NATS should be used
 * @param topic the NATS topic to publish to
 */
final case class NatsConfig(
    url: String,
    enable: Boolean,
    topic: String
)
