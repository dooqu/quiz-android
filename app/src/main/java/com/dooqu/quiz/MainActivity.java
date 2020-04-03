package com.dooqu.quiz;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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

import static com.dooqu.quiz.service.QuizService.BROAD_CAST_NAME;

public class MainActivity extends AppCompatActivity {
    static String TAG = MainActivity.class.getSimpleName();
    Handler handler;
    ServiceProxy serviceProxy;
    TextView tvSubjectTitle;
    TextView tvSubjectOption0;
    TextView tvSubjectOption1;
    TextView tvSubjectOption2;
    TextView tvSubjectOption3;
    TextView tvSubjectOption4;
    TextView tvUserASRResult;
    Button btnNewGame;

    WeakReference<QuizService> quizServiceWeakReference;
    QuizEventReceiver quizEventReceiver;
    IntentFilter intentFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);
        tvSubjectTitle = findViewById(R.id.tvSubjectTitle);
        tvSubjectOption0 = findViewById(R.id.tvOption0);
        tvSubjectOption1 = findViewById(R.id.tvOption1);
        tvSubjectOption2 = findViewById(R.id.tvOption2);
        tvSubjectOption3 = findViewById(R.id.tvOption3);
        tvSubjectOption4 = findViewById(R.id.tvOption4);
        btnNewGame = findViewById(R.id.btnNewGame);
        tvUserASRResult = findViewById(R.id.tvUserASRResult);
        handler = new Handler();
        btnNewGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(quizServiceWeakReference.get() != null) {
                    quizServiceWeakReference.get().requestNewGame();
                }
            }
        });
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
        quizEventReceiver = new QuizEventReceiver();
        intentFilter = new IntentFilter(BROAD_CAST_NAME);
        registerReceiver(quizEventReceiver, intentFilter);
        requestPermissions();
    };

    private void enabledButton(final boolean enabled) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.btnNewGame).setEnabled(enabled);
            }
        });
    }

    private void toastMessage(String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
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
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
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

    protected void setTitle(String title) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                tvSubjectTitle.setText(title + "?");
                clearOptionsText();
                clearOptionsColor();
            }
        });
    }

    protected void setOption(String option, int optionIndex) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                switch (optionIndex) {
                    case 0:
                        tvSubjectOption0.setText("1. " + option);
                        break;
                    case 1:
                        tvSubjectOption1.setText("2. " + option);
                        break;
                    case 2:
                        tvSubjectOption2.setText("3. " + option);
                        break;
                    case 3:
                        tvSubjectOption3.setText("4. " + option);
                        break;
                    case 4:
                        tvSubjectOption4.setText("5. " + option);
                        break;
                }
            }
        });
    }

    protected void setUserInputAndResult(int index, String userResult) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                clearOptionsColor();
                tvUserASRResult.setText(userResult);
                switch (index) {
                    case 0:
                        tvSubjectOption0.setBackgroundColor(Color.parseColor("#9c9ace"));
                        break;
                    case 1:
                        tvSubjectOption1.setBackgroundColor(Color.parseColor("#9c9ace"));
                        break;
                    case 2:
                        tvSubjectOption2.setBackgroundColor(Color.parseColor("#9c9ace"));
                        break;
                    case 3:
                        tvSubjectOption3.setBackgroundColor(Color.parseColor("#9c9ace"));
                        break;
                    case 4:
                        tvSubjectOption4.setBackgroundColor(Color.parseColor("#9c9ace"));
                        break;
                }
            }
        });
    }

    protected void clearOptionsText() {
        tvSubjectOption0.setText("");
        tvSubjectOption1.setText("");
        tvSubjectOption2.setText("");
        tvSubjectOption3.setText("");
        tvSubjectOption4.setText("");
    }

    protected void clearOptionsColor() {
        tvSubjectOption0.setBackgroundColor(Color.parseColor("#efefef"));
        tvSubjectOption1.setBackgroundColor(Color.parseColor("#efefef"));
        tvSubjectOption2.setBackgroundColor(Color.parseColor("#efefef"));
        tvSubjectOption3.setBackgroundColor(Color.parseColor("#efefef"));
        tvSubjectOption4.setBackgroundColor(Color.parseColor("#efefef"));
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopQuizService();
        unregisterReceiver(quizEventReceiver);
    }

    public class QuizEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String eventType = intent.getStringExtra("event");
            if(eventType == null) {
                return;
            }

            switch (eventType) {
                case "subject_title":
                    setTitle(intent.getStringExtra("title"));
                    break;

                case "subject_option":
                    setOption(intent.getStringExtra("option"), intent.getIntExtra("index", -1));
                    break;

                case "join_room":
                    enabledButton(true);
                    break;

                case "asr_start":
                    toastMessage("请说答案");
                    break;

                case "asr_result":
                    setUserInputAndResult(intent.getIntExtra("result_index", -1), intent.getStringExtra("result_string"));
                    break;

                case "asr_stop":
                    toastMessage("识别完成");
                    break;
            }
        }
    }

}
