package com.dooqu.quiz.service;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.util.Log;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;

/*
负责接管mic服务，并提供多路转发的recorder对象，可以让多个外部实例以并行的方式拿到mic数据
 */
public class AudioChannelRecord {

    private static volatile AudioChannelRecord instance;
    private static String TAG = AudioChannelRecord.class.getSimpleName();
    public static int RECORD_BUFFER_SIZE = 1280;
    public static int MAX_BUFFER_BLOCK_COUNT = 32;
    public final static int MAX_SHARED_RECORD_CHANNEL = 32;
    public final static int SAMPLE_RATE_IN_HZ = 16000;
    private AudioRecord audioRecord;
    private volatile boolean isRecording;
    private volatile boolean isPaused;
    boolean aecEnabled;
    private Thread recordReadThread;
    AcousticEchoCanceler acousticEchoCanceler;
    ArrayList<PipedOutputStream> outputStreamArrayList  = new ArrayList<PipedOutputStream>();
    static {
        RECORD_BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
    }

    public AudioChannelRecord(int audioSource) {
        audioRecord = new AudioRecord(audioSource, SAMPLE_RATE_IN_HZ,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, RECORD_BUFFER_SIZE);
        aecEnabled = initAEC();
    }


    public int getSessionId() {
        return this.audioRecord.getAudioSessionId();
    }

    private boolean initAEC() {
        if (AcousticEchoCanceler.isAvailable()) {
            if (acousticEchoCanceler == null) {
                acousticEchoCanceler = AcousticEchoCanceler.create(getSessionId());
                if (acousticEchoCanceler != null) {
                    acousticEchoCanceler.setEnabled(true);
                    return acousticEchoCanceler.getEnabled();
                }
            }
        }
        return false;
    }


    public boolean start() {
        //如果初始化错误，返回false
        if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            return false;
        }
        //如果已经在录制了，返回true
        if (isRecording && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            return true;
        }

        audioRecord.startRecording();
        //如果没有进入录制状态，返回falseS
        if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            return false;
        }
        isPaused = false;
        isRecording = true;
        Log.d(TAG, "audioRecord.getState=" + audioRecord.getState());
        recordReadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int bytesReaded = 0;
                byte[] recordData = new byte[RECORD_BUFFER_SIZE];
                do {
                    bytesReaded = audioRecord.read(recordData, 0, recordData.length);
                    if (bytesReaded > 0 && isPaused == false) {
                       //Log.d(TAG, "audio_record_receive:" + bytesReaded);
                        synchronized (outputStreamArrayList) {
                            //Log.d(TAG, "目前监听数:" + ChannelRecorder.outputStreamArrayList.size());
                            for (int i = 0, size = outputStreamArrayList.size(); i < size; i++) {
                                try {
                                    outputStreamArrayList.get(i).write(recordData, 0, bytesReaded);
                                    outputStreamArrayList.get(i).flush();
                                }
                                catch (IOException ex) {
                                    Log.d(TAG, ex.toString());
                                }
                            }
                        }
                    }
                } while (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING);
                isPaused = false;
                isRecording = false;
            }
        }, "thread_name:recording");
        recordReadThread.start();
        return true;
    }


    public int getRecordingState() {
        return this.audioRecord.getRecordingState();
    }


    public void stop() {
        isRecording = false;
        audioRecord.stop();
    }


    public void release() {
        stop();
        if (recordReadThread != null) {
            try {
                recordReadThread.join(100);
            }
            catch (InterruptedException ex) {
                Log.d(TAG, ex.toString());
            }
        }
        if (acousticEchoCanceler != null) {
            acousticEchoCanceler.release();
        }
        audioRecord.release();
    }



    public abstract class ChannelBinder {
        PipedOutputStream streamProducer;
        PipedInputStream streamConsumer;

        Thread dataReceiverThread;
        volatile boolean isAttaching;

        public ChannelBinder() {
        }

        protected abstract void onRecordData(final byte[] data, int size);

        public synchronized boolean attach() {
            if (isAttaching) {
                detach();
            }
            synchronized (outputStreamArrayList) {
                if (outputStreamArrayList.size() >= MAX_SHARED_RECORD_CHANNEL) {
                    return false;
                }
                streamProducer = new PipedOutputStream();
                streamConsumer = new PipedInputStream(RECORD_BUFFER_SIZE * MAX_BUFFER_BLOCK_COUNT);
                try {
                    streamProducer.connect(streamConsumer);
                }
                catch (IOException ex) {
                    return false;
                }
                outputStreamArrayList.add(streamProducer);
                isAttaching = true;
                dataReceiverThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        byte[] bufferRead = new byte[RECORD_BUFFER_SIZE];
                        int bytesReaded = 0;
                        while (isAttaching) {
                            try {
                                if (streamConsumer.available() < RECORD_BUFFER_SIZE) {
                                    try {
                                        //Log.d(TAG, "未等到数据" + streamConsumer.available());
                                        Thread.sleep(20);
                                    }
                                    catch (Exception ex) {
                                    }
                                    continue;
                                }
                                bytesReaded = streamConsumer.read(bufferRead, 0, RECORD_BUFFER_SIZE);
                                //Log.d(TAG, "onRecordData" + bytesReaded);
                                if (bytesReaded > 0) {
                                    //Log.d(TAG, "onRecordData" + bytesReaded + ",thread_id" + Thread.currentThread().getName());
                                    onRecordData(bufferRead, bytesReaded);
                                }
                            }
                            catch (IOException ex) {
                                Log.e(TAG, ex.toString());
                            }
                        }
                        Log.d(TAG, "RecordChannel complete:");
                    }
                }, "thread_name:channel_recorder_read" + ChannelBinder.this.toString());
                dataReceiverThread.start();
            }
            return isAttaching;
        }

        public int getSampleRateInHz() {
            return SAMPLE_RATE_IN_HZ;
        }

        public int getAudioSource() {
            return MediaRecorder.AudioSource.MIC;
        }

        public int getAudioFormat() {
            return AudioFormat.ENCODING_PCM_16BIT;
        }

        public int getBufferSize() {
            return RECORD_BUFFER_SIZE;
        }


        public synchronized boolean detach() {
            isAttaching = false;
            if (streamProducer != null) {
                synchronized (outputStreamArrayList) {
                    if (outputStreamArrayList.contains(streamProducer)) {
                        outputStreamArrayList.remove(streamProducer);
                        Log.d(TAG, "remote streamProducer");
                    }
                }
                try {
                    streamProducer.close();
                }
                catch (IOException ex) {
                }
            }

            if (streamConsumer != null) {
                try {
                    streamConsumer.close();
                }
                catch (IOException ex) {
                }
            }

            if (dataReceiverThread != null) {
                try {
                    dataReceiverThread.join(100);
                }
                catch (InterruptedException ex) {
                }
            }
            Log.d(TAG, "RecordChannel.detach()");
            return true;
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            Log.d(TAG, "ChannerRecorer.finalize()");
        }
    }

}