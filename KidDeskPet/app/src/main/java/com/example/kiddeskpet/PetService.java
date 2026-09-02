package com.example.kiddeskpet;

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
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Random;

public class PetService extends Service {

    private WindowManager wm;
    private LinearLayout root;
    private ImageView pet;
    private TextView bubble;
    private WindowManager.LayoutParams lp;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private float downX, downY;
    private int originX, originY;
    private long lastInteraction = 0L;
    private int currentState = 0;
    private int petSize = 225;

    private final int[] states = {
            R.drawable.pet_idle,
            R.drawable.pet_wave,
            R.drawable.pet_walk,
            R.drawable.pet_jump,
            R.drawable.pet_read,
            R.drawable.pet_think,
            R.drawable.pet_yawn,
            R.drawable.pet_sleep
    };

    private final String[] lines = {
            "嗨～今天也要开心呀！",
            "要不要一起看书？",
            "我在这里陪你～",
            "休息一下吧～",
            "嘿嘿，点到我啦！",
            "今天也很棒！",
            "我想伸个懒腰～",
            "困困了，先眯一会儿～"
    };

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int petHeightDp() {
        return Math.round(petSize * 1.15f);
    }

    private void setState(int index, boolean saySomething) {
        if (pet == null) return;
        currentState = (index + states.length) % states.length;

        pet.animate().cancel();
        pet.setTranslationX(0f);
        pet.setTranslationY(0f);
        pet.setRotation(0f);
        pet.setScaleX(1f);
        pet.setScaleY(1f);
        pet.setAlpha(0.2f);
        pet.setImageResource(states[currentState]);
        pet.animate().alpha(1f).setDuration(180).start();

        if (saySomething && bubble != null) {
            bubble.setText(lines[currentState]);
        }
        playMotion(currentState);
    }

    private void playMotion(int state) {
        if (pet == null) return;

        switch (state) {
            case 0:
                pet.animate()
                        .scaleX(1.02f).scaleY(1.02f)
                        .translationY(-dp(3))
                        .setDuration(900)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .withEndAction(() -> pet.animate()
                                .scaleX(1f).scaleY(1f).translationY(0)
                                .setDuration(900).start())
                        .start();
                break;

            case 1:
                pet.animate().rotation(-4f).translationY(-dp(2)).setDuration(180)
                        .withEndAction(() -> pet.animate().rotation(5f).setDuration(220)
                                .withEndAction(() -> pet.animate().rotation(0f).setDuration(180).start())
                                .start())
                        .start();
                break;

            case 2:
                pet.animate().translationX(dp(24)).setDuration(420)
                        .withEndAction(() -> pet.animate().translationX(-dp(10)).setDuration(420)
                                .withEndAction(() -> pet.animate().translationX(0).setDuration(280).start())
                                .start())
                        .start();
                break;

            case 3:
                pet.animate().translationY(-dp(28)).scaleX(1.03f).scaleY(0.97f).setDuration(180)
                        .withEndAction(() -> pet.animate().translationY(0).scaleX(1f).scaleY(1f)
                                .setDuration(260).start())
                        .start();
                break;

            case 4:
            case 5:
                pet.animate().translationY(-dp(3)).setDuration(700)
                        .withEndAction(() -> pet.animate().translationY(0).setDuration(700).start())
                        .start();
                break;

            case 6:
                pet.animate().scaleX(1.04f).scaleY(1.04f).setDuration(320)
                        .withEndAction(() -> pet.animate().scaleX(1f).scaleY(1f).setDuration(420).start())
                        .start();
                break;

            case 7:
                pet.animate().scaleX(1.015f).scaleY(1.015f).setDuration(1200)
                        .withEndAction(() -> pet.animate().scaleX(1f).scaleY(1f).setDuration(1200).start())
                        .start();
                break;
        }
    }

    private final Runnable autoBehavior = new Runnable() {
        @Override
        public void run() {
            long idle = System.currentTimeMillis() - lastInteraction;
            if (pet != null) {
                if (idle > 75000) {
                    setState(7, false);
                    bubble.setText("Zzz…");
                } else if (idle > 12000) {
                    int next = random.nextInt(7);
                    setState(next, false);
                }
            }
            handler.postDelayed(this, 9000);
        }
    };

    private void rebuildSize() {
        if (pet == null || root == null || lp == null || wm == null) return;
        pet.setLayoutParams(new LinearLayout.LayoutParams(dp(petSize), dp(petHeightDp())));
        lp.width = dp(petSize + 42);
        lp.height = dp(petHeightDp() + 62);
        try { wm.updateViewLayout(root, lp); } catch (Exception ignored) {}
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
                    "kidpet", "可爱桌宠服务", NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, "kidpet")
                : new Notification.Builder(this);

        startForeground(11, builder
                .setContentTitle("可爱桌宠正在陪你")
                .setContentText("点击应用可以调整桌宠")
                .setSmallIcon(android.R.drawable.btn_star)
                .build());

        lastInteraction = System.currentTimeMillis();
        showPet();
        handler.postDelayed(autoBehavior, 9000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "SIZE".equals(intent.getAction())) {
            petSize = intent.getIntExtra("size", 225);
            rebuildSize();
        }
        return START_STICKY;
    }

    private void showPet() {
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);

        bubble = new TextView(this);
        bubble.setText("点点我～");
        bubble.setTextSize(13);
        bubble.setTextColor(Color.rgb(82, 62, 73));
        bubble.setGravity(Gravity.CENTER);
        bubble.setPadding(dp(10), dp(6), dp(10), dp(6));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF5FFF9FC);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), 0x22C76A8A);
        bubble.setBackground(bg);

        pet = new ImageView(this);
        pet.setImageResource(states[0]);
        pet.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        root.addView(bubble, new LinearLayout.LayoutParams(dp(190), dp(44)));
        root.addView(pet, new LinearLayout.LayoutParams(dp(petSize), dp(petHeightDp())));

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        lp = new WindowManager.LayoutParams(
                dp(petSize + 42),
                dp(petHeightDp() + 62),
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );

        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = dp(55);
        lp.y = dp(180);

        root.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getRawX();
                    downY = event.getRawY();
                    originX = lp.x;
                    originY = lp.y;
                    lastInteraction = System.currentTimeMillis();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    lp.x = originX + (int)(event.getRawX() - downX);
                    lp.y = originY + (int)(event.getRawY() - downY);
                    try { wm.updateViewLayout(root, lp); } catch (Exception ignored) {}
                    return true;

                case MotionEvent.ACTION_UP:
                    lastInteraction = System.currentTimeMillis();
                    float dx = Math.abs(event.getRawX() - downX);
                    float dy = Math.abs(event.getRawY() - downY);

                    if (dx < dp(10) && dy < dp(10)) {
                        setState((currentState + 1) % states.length, true);
                    }
                    return true;

                default:
                    return false;
            }
        });

        try {
            wm.addView(root, lp);
            playMotion(0);
        } catch (Exception e) {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (pet != null) pet.animate().cancel();
        if (wm != null && root != null) {
            try { wm.removeView(root); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
