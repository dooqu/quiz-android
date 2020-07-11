package com.dooqu.quiz.sound;

import android.util.Log;

import com.dooqu.quiz.utils.StreamUtil;

import java.io.Closeable;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;


import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;

public class Mp3StreamDecoder implements Closeable {
    static String TAG = Mp3StreamDecoder.class.getSimpleName();

    protected MpegAudioFileReader mpegAudioFileReader;
    protected PipedBufferStream pipedBufferStream;
    protected AudioInputStream mpegInputStream;
    protected AudioInputStream pcmInputStream;
    protected volatile boolean isOpen;
    protected boolean initialized;
    protected Thread decoderThread;

    public Mp3StreamDecoder() {
        this.pipedBufferStream = new PipedBufferStream();
        this.mpegAudioFileReader = new MpegAudioFileReader();
    }


    public void write(byte[] buffer, int offset, int length) {
        this.pipedBufferStream.write(buffer, offset, length);
    }

    public void prepare() {
        this.isOpen = true;
        decoderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mpegInputStream = mpegAudioFileReader.getAudioInputStream(pipedBufferStream);
                    javax.sound.sampled.AudioFormat targetFormat = new javax.sound.sampled.AudioFormat(javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 1, 2, 16000, false);
                    pcmInputStream = AudioSystem.getAudioInputStream(targetFormat, mpegInputStream);
                    initialized = true;
                    int bytesReaded = -1;
                    byte[] buffer = new byte[1280];
                    do {
                        bytesReaded = pcmInputStream.read(buffer, 0, buffer.length);
                        if (bytesReaded > 0) {
                            onDecodedData(buffer, 0, bytesReaded);
                        }
                    }
                    while (isOpen && bytesReaded != -1);
                }
                catch (javax.sound.sampled.UnsupportedAudioFileException ex) {
                    Log.d(TAG, ex.toString());
                }
                catch (IOException ex) {
                    Log.d(TAG, ex.toString());
                }
            }
        });
        decoderThread.start();
    }

    protected void onDecodedData(byte[] buffer, int offset, int length) {
    }


    @Override
    public void close() {
        isOpen = false;
        if(decoderThread != null) {
            try {
                decoderThread.join();
            }
            catch (InterruptedException ex) {}
        }
        StreamUtil.safeClose(mpegInputStream);
        StreamUtil.safeClose(pcmInputStream);
        StreamUtil.safeClose(pipedBufferStream);
    }
}
