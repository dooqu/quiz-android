package com.dooqu.quiz.sound;

import android.util.Log;

import com.dooqu.quiz.utils.StreamUtil;

import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;

public class Mp3StreamDecoder extends InputStream {
    static String TAG = Mp3StreamDecoder.class.getSimpleName();
    protected MpegAudioFileReader mpegAudioFileReader;
    protected PipedBufferStream pipedBufferStream;
    protected AudioInputStream mpegInputStream;
    protected AudioInputStream pcmInputStream;
    protected volatile boolean isClosed;
    protected boolean initialized;

    public void open() throws IOException, javax.sound.sampled.UnsupportedAudioFileException {
        this.pipedBufferStream = new PipedBufferStream();
        this.mpegAudioFileReader = new MpegAudioFileReader();
        try {
            mpegInputStream = mpegAudioFileReader.getAudioInputStream(pipedBufferStream);
            javax.sound.sampled.AudioFormat targetFormat = new javax.sound.sampled.AudioFormat(javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 1, 2, 16000, false);
            pcmInputStream = AudioSystem.getAudioInputStream(targetFormat, mpegInputStream);
            initialized = true;
        }
        catch (javax.sound.sampled.UnsupportedAudioFileException ex) {
            Log.d(TAG, ex.toString());
            throw ex;
        }
        catch (IOException ex) {
            Log.d(TAG, ex.toString());
            throw ex;
        }
    }

    public Mp3StreamDecoder()  {
    }

    @Override
    public int read() throws IOException {
        return 0;
    }


    public void write(byte[] buffer, int offset, int length) {
        if (initialized && isClosed == false && pipedBufferStream != null) {
            pipedBufferStream.write(buffer, offset, length);
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (initialized && isClosed == false && pcmInputStream != null) {
            return pcmInputStream.read(buffer, offset, length);
        }
        return -1;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int available() throws IOException{
        if (initialized && isClosed == false && pcmInputStream != null) {
            return pipedBufferStream.available();
        }
        return 0;
    }

    @Override
    public void close() {
        StreamUtil.safeClose(mpegInputStream);
        StreamUtil.safeClose(pcmInputStream);
        StreamUtil.safeClose(pipedBufferStream);
    }
}
