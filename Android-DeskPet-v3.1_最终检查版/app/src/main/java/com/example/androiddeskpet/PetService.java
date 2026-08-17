package com.example.androiddeskpet;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Random;

public class PetService extends Service {

    private WindowManager windowManager;
    private LinearLayout root;
    private ImageView pet;
    private TextView bubble;
    private WindowManager.LayoutParams params;

    private float startTouchX, startTouchY;
    private int startX, startY;
    private long lastInteractionTime = 0L;
    private int currentState = 0;
    private int petSize = 240;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private final int[] states = new int[] {
            R.drawable.pet_idle,
            R.drawable.pet_phone,
            R.drawable.pet_think,
            R.drawable.pet_read,
            R.drawable.pet_lie_phone,
            R.drawable.pet_rest,
            R.drawable.pet_walk
    };

    private final String[] lines = new String[] {
            "在干嘛呢？",
            "今天也要加油呀！",
            "我在这儿，陪你～",
            "休息一下吧，别太累了～",
            "记得喝水哦~",
            "摸到我啦 ♡",
            "先坐一会儿",
            "今天看点书吧",
            "要不要休息一下？"
    };

    private final Runnable autoSleepRunnable = new Runnable() {
        @Override
        public void run() {
            long delta = System.currentTimeMillis() - lastInteractionTime;
            if (delta > 60000 && pet != null) {
                pet.setImageResource(R.drawable.pet_sleep);
                bubble.setText("Zzz…");
            }
            handler.postDelayed(this, 15000);
        }
    };

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int petHeightDp() {
        return Math.round(petSize * 1.25f);
    }

    private void rebuildSize() {
        if (root == null || pet == null || params == null || windowManager == null) return;

        pet.setLayoutParams(new LinearLayout.LayoutParams(dp(petSize), dp(petHeightDp())));
        params.width = dp(petSize + 36);
        params.height = dp(petHeightDp() + 64);

        try {
            windowManager.updateViewLayout(root, params);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    "pet", "桌宠服务", NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, "pet")
                : new Notification.Builder(this);

        startForeground(7, builder
                .setContentTitle("桌宠正在陪你")
                .setContentText("点击应用可调整桌宠")
                .setSmallIcon(android.R.drawable.btn_star)
                .build());

        lastInteractionTime = System.currentTimeMillis();
        showPet();
        handler.postDelayed(autoSleepRunnable, 15000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "SIZE".equals(intent.getAction())) {
            petSize = intent.getIntExtra("size", 240);
            rebuildSize();
        }
        return START_STICKY;
    }

    private void showPet() {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);

        bubble = new TextView(this);
        bubble.setText("点点我～");
        bubble.setTextSize(13);
        bubble.setTextColor(Color.rgb(42, 46, 56));
        bubble.setGravity(Gravity.CENTER);
        bubble.setPadding(dp(10), dp(6), dp(10), dp(6));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF2FFFFFF);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), 0x22000000);
        bubble.setBackground(bg);

        pet = new ImageView(this);
        pet.setImageResource(states[0]);
        pet.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        root.addView(bubble, new LinearLayout.LayoutParams(dp(180), dp(44)));
        root.addView(pet, new LinearLayout.LayoutParams(dp(petSize), dp(petHeightDp())));

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                dp(petSize + 36),
                dp(petHeightDp() + 64),
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dp(60);
        params.y = dp(180);

        root.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startTouchX = event.getRawX();
                    startTouchY = event.getRawY();
                    startX = params.x;
                    startY = params.y;
                    lastInteractionTime = System.currentTimeMillis();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    params.x = startX + (int) (event.getRawX() - startTouchX);
                    params.y = startY + (int) (event.getRawY() - startTouchY);
                    try {
                        windowManager.updateViewLayout(root, params);
                    } catch (Exception ignored) {
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    lastInteractionTime = System.currentTimeMillis();
                    float dx = Math.abs(event.getRawX() - startTouchX);
                    float dy = Math.abs(event.getRawY() - startTouchY);

                    if (dx < dp(10) && dy < dp(10)) {
                        currentState = (currentState + 1) % states.length;
                        pet.setImageResource(states[currentState]);
                        bubble.setText(lines[random.nextInt(lines.length)]);

                        pet.animate()
                                .scaleX(1.06f)
                                .scaleY(1.06f)
                                .setDuration(120)
                                .withEndAction(() ->
                                        pet.animate()
                                                .scaleX(1f)
                                                .scaleY(1f)
                                                .setDuration(150)
                                                .start()
                                )
                                .start();
                    }
                    return true;

                default:
                    return false;
            }
        });

        try {
            windowManager.addView(root, params);
        } catch (Exception e) {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);

        if (windowManager != null && root != null) {
            try {
                windowManager.removeView(root);
            } catch (Exception ignored) {
            }
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
