package com.dooqu.quiz;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Toast;

import com.dooqu.quiz.service.AudioChannelRecord;
import com.dooqu.quiz.service.QuizService;
import com.dooqu.quiz.service.ServiceProxy;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okio.ByteString;

public class MainActivity extends AppCompatActivity {
    static String TAG = MainActivity.class.getSimpleName();
    Handler handler;
    ServiceProxy serviceProxy;
    WeakReference<QuizService> quizServiceWeakReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);
        handler = new Handler();
        serviceProxy = new ServiceProxy(this, "com.dooqu.quiz.service.QuizService", "com.dooqu.quiz") {
            @Override
            protected void onConnected(boolean connected, IBinder serviceBinder) {
                if (quizServiceWeakReference != null) {
                    quizServiceWeakReference.clear();
                }
                QuizService.QuizServiceBinder binder = ((QuizService.QuizServiceBinder) serviceBinder);
                quizServiceWeakReference = new WeakReference<>(binder.getService());
                quizServiceWeakReference.get().connectService();
            }
        };
        requestPermissions();
    }

    private void enabledButton(final boolean enabled) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.button).setEnabled(enabled);
            }
        });
    }

    private void toastMessage(String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "已经连接到服务器", Toast.LENGTH_SHORT).show();
            }
        });
    }


    protected void startQuizService() {
        if(serviceProxy.isConnected() == false) {
            serviceProxy.bind();
        }
    }

    protected void stopQuizService() {
        serviceProxy.unbind();
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissionsNotGranted = new ArrayList<String>();
            String permissions[] = {
                    Manifest.permission.RECORD_AUDIO,
            };

            for (String permission : permissions) {
                if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, permission)) {
                    permissionsNotGranted.add(permission);
                }
            }

            if (permissionsNotGranted.size() > 0) {
                String[] permissionsToApply = new String[permissionsNotGranted.size()];
                ActivityCompat.requestPermissions(this, permissionsNotGranted.toArray(permissionsToApply), 99);
                return;
            }
        }
        //基础权限ok，校验浮窗权限
        startQuizService();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        boolean grantAll = true;
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] == -1) {
                grantAll = false;
                break;
            }
        }
        if (grantAll == true) {
            startQuizService();
        }
        else {
            Toast.makeText(this, "没有授权无法进行", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopQuizService();
    }

}
