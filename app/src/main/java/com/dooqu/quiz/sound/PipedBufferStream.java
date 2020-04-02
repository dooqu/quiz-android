package com.dooqu.quiz.sound;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import android.util.Log;

import com.dooqu.quiz.utils.StreamUtil;


public class PipedBufferStream extends InputStream {
    static String TAG = PipedOutputStream.class.getSimpleName();
    protected long bytesReadedTotal;
    protected volatile boolean isOpen;
    PipedInputStream consumerStream;
    PipedOutputStream producerStream;

    public PipedBufferStream() {
        //this.bytesTotal = bytesTotal;
        consumerStream = new PipedInputStream(1280 * 32);
        try {
            producerStream = new PipedOutputStream(consumerStream);
            isOpen = true;
        }
        catch (IOException ex) {
            Log.d(TAG, ex.toString());
        }
    }


    public void write(byte[] buffer, int offset, int length) {
        try {
            producerStream.write(buffer, offset, length);
            producerStream.flush();
        }
        catch (IOException ex) {
            Log.d(TAG, ex.toString());
        }
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        int ret = read(b, 0, 1);

        if (ret > 0) {
            return (int) b[0];
        }
        return 0;
    }


    @Override
    public int read(byte[] b) {
        return read(b, 0, b.length);
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) {
        if (isOpen == false) {
            Log.d(TAG, "read 返回,isopen = false");
            return -1;
        }
        try {
            int bytesRead = consumerStream.read(b, off, len);
            if (bytesRead > 0) {
                bytesReadedTotal += bytesRead;
                Log.d(TAG, "bytesRead=" + bytesRead + ", bytesReadedTotal=" + bytesReadedTotal);
            }
            return bytesRead;
        }
        catch (IOException ex) {
            Log.e(TAG, ex.toString());
        }
        return -1;
    }


    @Override
    public void close() throws IOException {
        super.close();
        isOpen = false;
        //bytesTotal = 0;
        StreamUtil.safeClose(producerStream);
        StreamUtil.safeClose(consumerStream);
    }

    @Override
    public synchronized void reset() throws IOException {
        int bytesAvailable = consumerStream.available();
        if (bytesAvailable > 0) {
            int consumed = 0, bytesRead = -1;
            byte[] buffer = new byte[Math.min(1024, bytesAvailable)];
            do {
                bytesRead = consumerStream.read(buffer, 0, buffer.length);
                consumed += bytesRead;
            }
            while (bytesRead != -1 || consumed >= bytesAvailable);
        }
    }

    @Override
    public int available() throws IOException {
        if (isOpen == false) {
            return 0;
        }
        int bytesAvailable = consumerStream.available();
        Log.d(TAG, "availabe( " + bytesAvailable + ")");
        return bytesAvailable;
    }
}
