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

package org.mbari.annosaurus.etc.sdk

import scala.reflect.ClassTag
import scala.jdk.CollectionConverters.*

object Reflect:

    /**
     * Create an instance of a class from a Map of parameters. The keys of the map must match the names of the
     * constructor parameters. This works for both case classes and regular classes.
     *
     * @param m
     *   The map of parameters
     * @tparam T
     *   The type of the class to create
     * @return
     *   A new instance of the class
     * @throws IllegalArgumentException
     *   if a required parameter is missing
     */
    def fromMap[T: ClassTag](m: Map[String, ?]): T =
        val classTag        = implicitly[ClassTag[T]]
        val constructor     = classTag.runtimeClass.getDeclaredConstructors.head
        val constructorArgs = constructor
            .getParameters()
            .map { param =>
                val paramName = param.getName
                if param.getType == classOf[Option[?]] then m.get(paramName)
                else
                    m.getOrElse(
                        paramName,
                        throw new IllegalArgumentException(
                            s"Missing required parameter: $paramName"
                        )
                    )
            }
        constructor.newInstance(constructorArgs*).asInstanceOf[T]

    /**
     * Create an instance of a class from a java.util.Map of parameters. The keys of the map must match the names of the
     * constructor parameters. This works for both case classes and regular classes.
     *
     * @param m
     *   The map of parameters
     * @tparam T
     *   The type of the class to create
     * @return
     *   A new instance of the class
     * @throws IllegalArgumentException
     *   if a required parameter is missing
     */
    def fromJavaMap[T: ClassTag](m: java.util.Map[String, ?]): T =
        fromMap(m.asScala.toMap)

    /**
     * Convert a case class to a Map of parameters. The keys of the map are the names of the constructor parameters.
     *
     * @param t
     *   The case class to convert
     * @tparam T
     *   The type of the case class
     * @return
     *   A Map of parameters
     */
    def toMap[T: ClassTag](t: T): Map[String, ?] =
        val classTag = implicitly[ClassTag[T]]
        val fields   = classTag.runtimeClass.getDeclaredFields
        fields.map { field =>
            field.setAccessible(true)
            field.getName -> field.get(t)
        }.toMap

    def toFormBody[T: ClassTag](t: T): String =
        toMap(t)
            .filter { case (_, v) => // Remove nulls and None
                v match
                    case null => false
                    case None => false
                    case _    => true
            }
            .map { // Convert Some(x) to x
                case (k, v) =>
                    val d = v match
                        case Some(x) => x
                        case x       => x
                    k -> d
            }
            .map { case (k, v) => s"$k=$v" }
            .mkString("&")
