package com.dooqu.quiz.sound;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import com.dooqu.quiz.utils.StreamUtil;

import static com.dooqu.quiz.service.AudioChannelRecord.SAMPLE_RATE_IN_HZ;

public class Mp3AudioTrack {
    static String TAG = Mp3AudioTrack.class.getSimpleName();

    protected int recoderSessionId;
    protected volatile boolean isPlaying;
    protected AudioTrack audioTrack;
    protected Mp3StreamDecoder mp3StreamDecoder;

    public Mp3AudioTrack(int recorderSessionId) {
        this.recoderSessionId = recorderSessionId;
        mp3StreamDecoder = new Mp3StreamDecoder() {
            @Override
            protected void onDecodedData(byte[] buffer, int offset, int length) {
                if(isPlaying) {
                    audioTrack.write(buffer, offset, length);
                }
            }
        };
    }

    public void play() {
        isPlaying = true;
        if (initAudioTrackInner() == true) {
            audioTrack.play();
        }
    }

    public void write(byte[] buffer, int offset, int length) {
        if (initialized()) {
            mp3StreamDecoder.write(buffer, offset, length);
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

        if (this.audioTrack.getState() == AudioTrack.STATE_INITIALIZED ) {
            Log.d(TAG, "Mp3AudioTrack init sucess.");
            mp3StreamDecoder.prepare();
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
        isPlaying = false;
        if (audioTrack != null) {
            //audioTrack.pause();
            audioTrack.stop();
        }
    }

    public void release() {
        if (isPlaying) {
            stop();
        }
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }
        StreamUtil.safeClose(mp3StreamDecoder);
    }
}
