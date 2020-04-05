package com.dooqu.quiz.sound;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import com.dooqu.quiz.utils.StreamUtil;

import java.io.IOException;
import java.lang.ref.WeakReference;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;


import static com.dooqu.quiz.service.AudioChannelRecord.SAMPLE_RATE_IN_HZ;

public class Mp3AudioTrack {
    static String TAG = Mp3AudioTrack.class.getSimpleName();
    protected PipedBufferStream pipedBufferStream;
    protected Thread playThread;
    protected AudioTrack audioTrack;
    protected int recoderSessionId;
    protected MpegAudioFileReader mpegAudioFileReader;

    protected volatile boolean isPlaying;

    public Mp3AudioTrack(int recorderSessionId) {
        //this.mp3StreamSize = mp3StreamSize;
        this.recoderSessionId = recorderSessionId;
    }

    class DecodeAndPlayThread extends Thread {
        protected WeakReference<PipedBufferStream> pipedBufferStreamWeakReference;

        public DecodeAndPlayThread(PipedBufferStream pipedBufferStream) {
            this.pipedBufferStreamWeakReference = new WeakReference<>(pipedBufferStream);
        }

        @Override
        public void run() {
            super.run();
            mpegAudioFileReader = new MpegAudioFileReader();
            AudioInputStream mpegInputStream = null;
            AudioInputStream pcmInputStram = null;
            try {
                mpegInputStream = mpegAudioFileReader.getAudioInputStream(pipedBufferStreamWeakReference.get());
                javax.sound.sampled.AudioFormat targetFormat = new javax.sound.sampled.AudioFormat(javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 1, 2, 16000, false);
                pcmInputStram = AudioSystem.getAudioInputStream(targetFormat, mpegInputStream);
                int bytesReaded = -1;

                do {
                    byte[] pcmBufferFrame = new byte[1280];
                    try {
                        bytesReaded = pcmInputStram.read(pcmBufferFrame, 0, pcmBufferFrame.length);
                        Log.d(TAG, "Mp3AudioTrack:pcmBytesStream.read = " + bytesReaded);
                        if (bytesReaded > 0 && initialized()) {
                            audioTrack.write(pcmBufferFrame, 0, bytesReaded);
                        }
                    }
                    catch (IOException ex) {
                        Log.e(TAG, ex.toString());
                        break;
                    }
                }
                while (isPlaying && bytesReaded != -1);
                Log.d(TAG, "Mp3AudioTrack.play() over.");
            }
            catch (javax.sound.sampled.UnsupportedAudioFileException ex) {
                Log.d(TAG, ex.toString());
            }
            catch (IOException ex) {
                Log.d(TAG, ex.toString());
            }
            finally {
                StreamUtil.safeClose(pcmInputStram);
                StreamUtil.safeClose(mpegInputStream);
            }
        }
    }

    public void play() {
        if (isPlaying == false && initAudioTrackInner() == true) {
            isPlaying = true;
            pipedBufferStream = new PipedBufferStream();
            playThread = new DecodeAndPlayThread(pipedBufferStream);
            playThread.start();
            audioTrack.play();
        }
    }

    public void write(byte[] buffer, int offset, int length) {
        if (isPlaying) {
            pipedBufferStream.write(buffer, offset, length);
        }
    }


    protected boolean initAudioTrackInner() {
        this.audioTrack = new AudioTrack(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                new AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE_IN_HZ)
                        .setChannelMask(AudioFormat.CHANNEL_CONFIGURATION_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build(),
                AudioTrack.getMinBufferSize(
                        SAMPLE_RATE_IN_HZ,
                        AudioFormat.CHANNEL_CONFIGURATION_MONO,
                        AudioFormat.ENCODING_PCM_16BIT),
                AudioTrack.MODE_STREAM, recoderSessionId);
        //channelRecordService.isAecEnabling() ? channelRecordService.getSessionId() : AudioManager.AUDIO_SESSION_ID_GENERATE);
        if (this.audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            Log.d(TAG, "Mp3AudioTrack init sucess.");
            return true;
        }
        else {
            Log.d(TAG, "Mp3AudioTrack init failed.");
            return false;
        }
    }

    public boolean initialized() {
        return this.audioTrack != null && this.audioTrack.getState() == AudioTrack.STATE_INITIALIZED;
    }


    public void stop() {
        if (isPlaying == false) {
            return;
        }
        isPlaying = false;
        if (playThread != null) {
            try {
                playThread.join();
            }
            catch (InterruptedException ex) {
            }
        }
        if (audioTrack != null) {
            audioTrack.stop();
        }
        StreamUtil.safeClose(pipedBufferStream);
    }

    public void release() {
        if (isPlaying) {
            stop();
        }
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }
        pipedBufferStream = null;
    }
}
