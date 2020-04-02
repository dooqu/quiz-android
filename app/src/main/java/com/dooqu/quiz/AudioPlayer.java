package com.dooqu.quiz;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import com.dooqu.quiz.service.AudioChannelRecord;

import static com.dooqu.quiz.service.AudioChannelRecord.SAMPLE_RATE_IN_HZ;


public class AudioPlayer {
    static String TAG = AudioPlayer.class.getSimpleName();
    private AudioTrack audioTrack;
    private boolean isShutdown;
    private boolean isStoped;

    public AudioPlayer(AudioChannelRecord recorder) {
                //参考：https://source.android.google.cn/devices/audio/attributes
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
                AudioTrack.MODE_STREAM, recorder.getSessionId());
                //channelRecordService.isAecEnabling() ? channelRecordService.getSessionId() : AudioManager.AUDIO_SESSION_ID_GENERATE);

        if (this.audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            Log.d(TAG, "播放设备初始化成功");

        }
        else {
            Log.d(TAG, "播放设备初始化失败");
        }
    }


    protected boolean initialize() {
        return this.audioTrack.getState() == AudioTrack.STATE_INITIALIZED;
    }


    protected void play(byte[] bytes, int offset, int length) {
        if (this.isShutdown == false) {
            this.audioTrack.write(bytes, offset, length);
        }
    }

    public void start() {
        isShutdown = false;
        isStoped = false;
        this.audioTrack.play();
        Log.d(TAG, "AudioPlayer.start");
    }


    public void stop() {
        Log.d(TAG, "AudioPlayer.stop:" + Thread.currentThread().getName());
        //super.stop()，会有一定的阻塞式的延迟
        isStoped = true;
        this.audioTrack.stop();
    }

    protected synchronized boolean release() {
        Log.d(TAG, "AudioPlayer.release");
        isShutdown = true;
        if (isStoped == false) {
            this.stop();
        }
        this.audioTrack.release();
        return true;
    }
}