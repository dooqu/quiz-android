package com.dooqu.quiz.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.dooqu.quiz.R;
import com.dooqu.quiz.sound.Mp3AudioTrack;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class QuizService extends Service {
    public class QuizServiceBinder extends Binder {
        public QuizService getService() {
            return QuizService.this;
        }
    }

    static String TAG = QuizService.class.getSimpleName();
    public static String BROAD_CAST_NAME = "com.dooqu.quiz";
    OkHttpClient client;
    Request request;
    WebSocket socket;
    AudioChannelRecord audioChannelRecord;
    AudioChannelRecord.ChannelBinder channelBinder;
    volatile boolean attachRecordingData;
    boolean isSkillFirstFrameData;
    IBinder binder = new QuizServiceBinder();
    int frameCount = 0;
    Mp3AudioTrack mp3AudioTrack;
    volatile boolean isSocketConnected;
    NotificationHelper<QuizService> notificationHelper;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationHelper = new NotificationHelper<>(this, R.mipmap.icon_notif);
        notificationHelper.retainForeground();
        audioChannelRecord = new AudioChannelRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        channelBinder = audioChannelRecord.new ChannelBinder() {
            @Override
            protected void onRecordData(byte[] data, int size) {
                if (socket != null) {
                    if (attachRecordingData) {
                        socket.send(ByteString.of(data, 0, size));
                    }
                }
            }
        };
        audioChannelRecord.start();
        mp3AudioTrack = new Mp3AudioTrack(audioChannelRecord.getSessionId());
        mp3AudioTrack.play();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (socket != null) {
            socket.close(1001, "server closed.");
        }
        notificationHelper.cancelForeground();

        audioChannelRecord.stop();
        audioChannelRecord.release();

        mp3AudioTrack.stop();
        mp3AudioTrack.release();

    }

    public void connectService() {
        client = new OkHttpClient.Builder()
                .writeTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build();
        request = new Request.Builder().url("ws://dooqu.com:8000/service/quiz").build();
        //request = new Request.Builder().url("ws://192.168.31.38:8080/service/quiz").build();
        client.newWebSocket(request, webSocketListener);
    }

    public void requestNewGame() {
        if (isSocketConnected == true) {
            socket.send("STT");
        }
    }

    WebSocketListener webSocketListener = new WebSocketListener() {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            super.onOpen(webSocket, response);
            socket = webSocket;
            isSocketConnected = true;
            channelBinder.attach();
        }


        @Override
        public void onMessage(WebSocket webSocket, String text) {
            super.onMessage(webSocket, text);
            Log.d(TAG, "onTextMessage:" + text);
            Command command = Command.parse(text);
            Intent intent = new Intent();
            intent.setAction(BROAD_CAST_NAME);
            if (command == null) {
                return;
            }
            switch (command.getName()) {
                case "ASR":
                    if ("1".equals(command.getArgumentAt(0))) {
                        attachRecordingData = true;
                        intent.putExtra("event", "asr_start");
                    }
                    else if ("0".equals(command.getArgumentAt(0))) {
                        attachRecordingData = false;
                        intent.putExtra("event", "asr_stop");
                    }
                    sendBroadcast(intent);
                    break;
                case "JRM":
                    intent.putExtra("event", "join_game");
                    sendBroadcast(intent);
                    break;

                case "SKL":
                    intent.putExtra("event", "skill_start");
                    sendBroadcast(intent);
                    break;

                case "TIT":
                    intent.putExtra("event", "subject_title");
                    intent.putExtra("title", command.getArgumentAt(0));
                    sendBroadcast(intent);
                    break;
                case "OPT":
                    intent.putExtra("event", "subject_option");
                    intent.putExtra("index", Integer.parseInt(command.getArgumentAt(0)));
                    intent.putExtra("option", command.getArgumentAt(1));
                    sendBroadcast(intent);
                    break;
                case "URS":
                    intent.putExtra("event", "asr_result");
                    intent.putExtra("result_index", Integer.parseInt(command.getArgumentAt(0)));
                    intent.putExtra("result_string", command.getArgumentAt(1));
                    sendBroadcast(intent);
                    break;
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            super.onMessage(webSocket, bytes);
            Log.d(TAG, "onByteMessage:" + bytes.size() + "framecount=" + bytes.size());
            mp3AudioTrack.write(bytes.toByteArray(), 0, bytes.size());
        }


        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            super.onClosing(webSocket, code, reason);
            attachRecordingData = false;
            isSkillFirstFrameData = false;
            isSocketConnected = false;
            Log.d(TAG, "onClosing");
        }


        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            super.onFailure(webSocket, t, response);
            Log.e(TAG, "onFailure:" + t.toString());
        }
    };
}
