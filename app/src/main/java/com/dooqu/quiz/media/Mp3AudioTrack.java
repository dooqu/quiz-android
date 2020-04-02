package com.dooqu.quiz.media;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;


import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;


import static com.dooqu.quiz.service.AudioChannelRecord.SAMPLE_RATE_IN_HZ;

public class Mp3AudioTrack {
    static String TAG = Mp3AudioTrack.class.getSimpleName();
    protected long mp3StreamSize;
    protected Mp3BufferedInputStream mp3BufferedInputStream;
    protected Thread playThread;
    protected AudioTrack audioTrack;
    protected int recoderSessionId;
    protected MpegAudioFileReader mpegAudioFileReader;

    protected volatile boolean isPlaying;

    public Mp3AudioTrack(long mp3StreamSize, int recorderSessionId) {
        this.mp3StreamSize = mp3StreamSize;
        this.recoderSessionId = recorderSessionId;
    }

    class DecodeAndPlayerThread extends Thread {
        protected Mp3BufferedInputStream mp3BufferedInputStream;

        public DecodeAndPlayerThread(Mp3BufferedInputStream mp3BufferedInputStream) {
            this.mp3BufferedInputStream = mp3BufferedInputStream;
        }

        @Override
        public void run() {
            super.run();
            mpegAudioFileReader = new MpegAudioFileReader();
            AudioInputStream mpegInputStream = null;
            AudioInputStream pcmStream = null;
            try {
                mpegInputStream = mpegAudioFileReader.getAudioInputStream(mp3BufferedInputStream);
                AudioInputStream mpegStream = mpegAudioFileReader.getAudioInputStream(mpegInputStream);
                javax.sound.sampled.AudioFormat targetFormat = new javax.sound.sampled.AudioFormat(javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 1, 2, 16000, false);
                pcmStream = AudioSystem.getAudioInputStream(targetFormat, mpegStream);
            }
            catch (javax.sound.sampled.UnsupportedAudioFileException ex) {
                Log.d(TAG, ex.toString());
            }
            catch (IOException ex) {
                Log.d(TAG, ex.toString());
            }

            int bytesReaded = -1;
            do {
                byte[] pcmBufferFrame = new byte[1280];
                try {
                    bytesReaded = pcmStream.read(pcmBufferFrame, 0, pcmBufferFrame.length);
                    Log.d(TAG, "mp3AudioTrack:pcmStream.read读取了:" + bytesReaded);
                    if (bytesReaded > 0) {
                        audioTrack.write(pcmBufferFrame, 0, bytesReaded);
                    }
                }
                catch (IOException ex) {
                    Log.e(TAG, ex.toString());
                    break;
                }
            }
            while (isPlaying && bytesReaded != -1);
            Log.d(TAG, "playThread over.");
        }
    }

    public void play() {
        stop();
        if (initAudioTrackInner() == true) {
            isPlaying = true;
            mp3BufferedInputStream = new Mp3BufferedInputStream(mp3StreamSize);
            playThread = new DecodeAndPlayerThread(mp3BufferedInputStream);
            playThread.start();
        }
        audioTrack.play();
    }

    public void write(byte[] buffer, int offset, int length) {
        if (isPlaying) {
            mp3BufferedInputStream.writeMp3Data(buffer, offset, length);
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
            Log.d(TAG, "播放设备初始化成功");
            return true;

        }
        else {
            Log.d(TAG, "播放设备初始化失败");
            return false;
        }
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
    }

    public void release() {
        if (isPlaying) {
            stop();
        }
        if (audioTrack != null) {
            audioTrack.release();
        }
    }
}
