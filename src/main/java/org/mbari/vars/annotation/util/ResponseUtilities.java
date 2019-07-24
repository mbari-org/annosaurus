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

package org.mbari.vars.annotation.util;

import io.reactivex.Observable;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Brian Schlining
 * @since 2019-05-08T10:46:00
 */
public class ResponseUtilities {

    private ResponseUtilities() {
        // No instantiation
    }

    public static <T> void sendStreamedResponse(HttpServletResponse response,
                                            Stream<T> items,
                                            Function<T, String> fn)
            throws IOException {


        response.setHeader("Transfer-Encoding", "chunked");
        response.setStatus(200);
        ServletOutputStream out = response.getOutputStream();
        out.write("[\n".getBytes());
        Iterator<T> iterator = items.iterator();
        byte[] comma = ",\n".getBytes();
        while (iterator.hasNext()) {
            T next = iterator.next();
            String s = fn.apply(next);
            out.write(s.getBytes());
            if (iterator.hasNext()) {
                out.write(comma);
            }
        }
        out.write("]".getBytes());

    }

    public static <T> void sendRxResponse(HttpServletResponse response,
                                     Observable<T> observable,
                                     Function<T, String> fn) throws IOException {
        response.setHeader("Transfer-Encoding", "chunked");
        response.setStatus(200);
        ServletOutputStream out = response.getOutputStream();
        out.write("[\n".getBytes());

        observable.subscribe(next -> {
            String s = fn.apply(next);
            out.write(s.getBytes());
        }, e -> {}, () -> out.write("]".getBytes()));
    }
}
