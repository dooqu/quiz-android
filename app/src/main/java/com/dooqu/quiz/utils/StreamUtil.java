package com.dooqu.quiz.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

public class StreamUtil {
    public static boolean safeClose(Closeable stream) {
        try {
            if( stream != null) {
                stream.close();
                return true;
            }
        }
        catch (IOException ex) {

        }
        return false;
    }
}
