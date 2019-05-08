package org.mbari.vars.annotation.util;

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
}
