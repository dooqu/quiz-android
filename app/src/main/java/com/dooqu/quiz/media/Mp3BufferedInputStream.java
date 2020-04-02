package com.dooqu.quiz.media;

import android.util.Log;

import com.dooqu.quiz.utils.StreamUtil;
import com.dooqu.quiz.utils.ThreadUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import static com.dooqu.quiz.media.Mp3AudioTrack.TAG;

public class Mp3BufferedInputStream extends InputStream {

    protected long bytesTotal;
    protected long bytesReadedTotal;
    protected volatile boolean isOpen;
    PipedInputStream consumerStream;
    PipedOutputStream producerStream;

    public Mp3BufferedInputStream(long bytesTotal) {
        this.bytesTotal = bytesTotal;
        consumerStream = new PipedInputStream(1280 * 32);
        try {
            producerStream = new PipedOutputStream(consumerStream);
            isOpen = true;
        }
        catch (IOException ex) {
        }
    }


    public void writeMp3Data(byte[] buffer, int offset, int length) {
        try {
            producerStream.write(buffer, 0, length);
        }
        catch (IOException ex) {
        }
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        int ret = read(b, 0, 1);

        if(ret > 0) {
            return (int)b[0];
        }
        return 0;
    }


    @Override
    public int read(byte[] b) {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) {
        if(isOpen == false) {
            Log.d(TAG, "read 返回,isopen = false");
            return -1;
        }
        else if (bytesReadedTotal >= bytesTotal) {
            Log.d(TAG, "read 返回，没有可读了");
            return -1;
        }
        try {
            int bytesRead = consumerStream.read(b, off, len);
            if (bytesRead > 0) {
                bytesReadedTotal += bytesRead;
                Log.d(TAG, "bytesRead=" + bytesRead + ", bytesReadedTotal=" + bytesReadedTotal + ", bytesTotal=" + bytesTotal);
            }
            return bytesRead;
        }
        catch (IOException ex) {
            Log.e(TAG, ex.toString());
        }
        return -1;
    }


    /*    @Override
    public int read(byte[] b, int off, int len) {
        if(isOpen == false) {
            Log.d(TAG, "read 返回,isopen = false");
            return -1;
        }
        else if (bytesReadedTotal >= bytesTotal) {
            Log.d(TAG, "read 返回，没有可读了");
            return -1;
        }
        try {
            int bytesAvailable = 0;
            while (isOpen && (bytesAvailable = consumerStream.available()) <= 0) {
                ThreadUtil.safeSleep(5);
                Log.d(TAG, "read waiting");
                continue;
            }
            if(isOpen == false) {
                return -1;
            }
            int bytesRead = consumerStream.read(b, off, len);
            if (bytesRead > 0) {
                bytesReadedTotal += bytesRead;
                Log.d(TAG, "bytesRead=" + bytesRead + ", bytesReadedTotal=" + bytesReadedTotal + ", bytesTotal=" + bytesTotal);
                return bytesRead;
            }
            else {
                Log.d(TAG, "read 返回 0");
            }
        }
        catch (IOException ex) {
            Log.e(TAG, ex.toString());
        }
        return -1;
    }*/

    @Override
    public void close() throws IOException {
        super.close();
        isOpen = false;
        bytesTotal = 0;
        StreamUtil.safeClose(producerStream);
        StreamUtil.safeClose(consumerStream);
    }

    @Override
    public synchronized void reset() throws IOException {

    }

    @Override
    public int available() throws IOException {
        if(isOpen == false) {
            return 0;
        }
        int bytesAvailable = consumerStream.available();
        Log.d(TAG, "Mp3BufferdInputStram:availabe( " + bytesAvailable + ")");
        return bytesAvailable;
    }
}
