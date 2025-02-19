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

package org.mbari.annosaurus.etc.tapir

import org.mbari.annosaurus.etc.jdk.Instants
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.{Codec, CodecFormat, DecodeResult}

import java.net.{URI, URL}
import java.time.Instant
import java.util.HexFormat
import scala.util.{Failure, Success, Try}

object TapirCodecs:

    private val hexFormat = HexFormat.of()

    private def decodeUrl(s: String): DecodeResult[URL] =
        decodeUri(s).map(_.toURL)
    private def encodeUrl(u: URL): String               = u.toExternalForm
    given urlCodec: Codec[String, URL, TextPlain]       = Codec.string.mapDecode(decodeUrl)(encodeUrl)

    private def decodeUri(s: String): DecodeResult[URI] =
        Try(URI.create(s)) match
            case Success(uri) => DecodeResult.Value(uri)
            case Failure(e)   => DecodeResult.Error(s"Failed to decode $s to a URI", e)
    private def encodeUri(u: URI): String               = u.toString
    given uriCodec: Codec[String, URI, TextPlain]       = Codec.string.mapDecode(decodeUri)(encodeUri)

    private def decodeInstant(s: String): DecodeResult[Instant] =
        Instants.parseIso8601(s) match
            case Right(i) => DecodeResult.Value(i)
            case Left(e)  => DecodeResult.Error("Failed to decode $s to an Instant", e)
    private def encodeInstant(i: Instant): String               = i.toString
    given instantCodec: Codec[String, Instant, TextPlain]       =
        Codec.string.mapDecode(decodeInstant)(encodeInstant)

    private def decodeByteArray(s: String): DecodeResult[Array[Byte]] =
        Try(hexFormat.parseHex(s)) match
            case Success(bytes) => DecodeResult.Value(bytes)
            case Failure(e)     => DecodeResult.Error(s"Failed to decode $s to a byte array", e)
    private def encodeByteArray(bytes: Array[Byte]): String           = hexFormat.formatHex(bytes)
    given byteArrayCodec: Codec[String, Array[Byte], TextPlain]       =
        Codec.string.mapDecode(decodeByteArray)(encodeByteArray)
