package com.example.kiddeskpet;

import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Base64;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
    private ValueAnimator motionAnimator;

    private final String[] stateAssets = {
            "pet_idle.b64",
            "pet_wave.b64",
            "pet_walk.b64",
            "pet_jump.b64",
            "pet_read.b64",
            "pet_think.b64",
            "pet_yawn.b64",
            "pet_sleep.b64"
    };

    private final Bitmap[] stateBitmaps = new Bitmap[stateAssets.length];

    private final String[] lines = {
            "嗨～今天也要开心呀！",
            "挥挥手～你好呀！",
            "一起出去走走吧～",
            "耶！跳一下！",
            "要不要一起看书？",
            "让我想一想～",
            "哈欠～有一点困啦",
            "我要睡觉啦～Zzz"
    };

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int petHeightDp() {
        return Math.round(petSize * 1.15f);
    }

    private Bitmap loadAssetBitmap(String assetName) {
        try (InputStream in = getAssets().open(assetName);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
            byte[] decoded = Base64.decode(out.toByteArray(), Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap bitmapForState(int index) {
        if (stateBitmaps[index] == null) {
            stateBitmaps[index] = loadAssetBitmap(stateAssets[index]);
        }
        return stateBitmaps[index];
    }

    private void stopMotion() {
        if (motionAnimator != null) {
            motionAnimator.cancel();
            motionAnimator.removeAllUpdateListeners();
            motionAnimator = null;
        }
        if (pet != null) {
            pet.animate().cancel();
            pet.setTranslationX(0f);
            pet.setTranslationY(0f);
            pet.setRotation(0f);
            pet.setScaleX(1f);
            pet.setScaleY(1f);
            pet.setAlpha(1f);
        }
    }

    private long durationForState(int state) {
        switch (state) {
            case 0: return 1800L; // 呼吸
            case 1: return 900L;  // 挥手摇摆
            case 2: return 1050L; // 走路
            case 3: return 800L;  // 跳跃
            case 4: return 2000L; // 看书
            case 5: return 2200L; // 思考
            case 6: return 1500L; // 哈欠
            case 7: return 2500L; // 睡觉
            default: return 1600L;
        }
    }

    private void startContinuousMotion(final int state) {
        stopMotion();
        if (pet == null || currentState != state) return;

        motionAnimator = ValueAnimator.ofFloat(0f, 1f);
        motionAnimator.setDuration(durationForState(state));
        motionAnimator.setRepeatCount(ValueAnimator.INFINITE);
        motionAnimator.setRepeatMode(ValueAnimator.RESTART);
        motionAnimator.setInterpolator(state == 2 ? new LinearInterpolator() : new AccelerateDecelerateInterpolator());

        motionAnimator.addUpdateListener(animation -> {
            if (pet == null || currentState != state) return;

            float f = (float) animation.getAnimatedValue();
            double phase = f * Math.PI * 2.0;
            float s = (float) Math.sin(phase);
            float c = (float) Math.cos(phase);
            float a = Math.abs(s);

            switch (state) {
                case 0: // 待机：明显但柔和的呼吸 + 轻微上下浮动
                    pet.setScaleX(1f + 0.028f * s);
                    pet.setScaleY(1f + 0.035f * s);
                    pet.setTranslationY(-dp(4) * s);
                    pet.setRotation(0.8f * s);
                    break;

                case 1: // 挥手：整个人轻快左右摆，持续循环
                    pet.setRotation(7.5f * s);
                    pet.setTranslationX(dp(5) * s);
                    pet.setTranslationY(-dp(4) * a);
                    pet.setScaleX(1f + 0.012f * a);
                    pet.setScaleY(1f + 0.012f * a);
                    break;

                case 2: // 走路：水平位移 + 步伐弹跳
                    pet.setTranslationX(dp(22) * s);
                    pet.setTranslationY(-dp(8) * Math.abs((float)Math.sin(phase * 2.0)));
                    pet.setRotation(2.8f * (float)Math.sin(phase * 2.0));
                    pet.setScaleX(1f + 0.015f * a);
                    pet.setScaleY(1f - 0.012f * a);
                    break;

                case 3: // 跳跃：连续弹跳，幅度明显
                    float jump = Math.abs((float)Math.sin(phase));
                    pet.setTranslationY(-dp(34) * jump);
                    pet.setScaleX(1f + 0.04f * jump);
                    pet.setScaleY(1f - 0.045f * jump);
                    pet.setRotation(1.8f * s);
                    break;

                case 4: // 看书：轻微呼吸、点头
                    pet.setTranslationY(-dp(3) * s);
                    pet.setRotation(1.4f * s);
                    pet.setScaleX(1f + 0.012f * s);
                    pet.setScaleY(1f + 0.012f * s);
                    break;

                case 5: // 托腮：慢慢晃脑袋
                    pet.setTranslationY(-dp(4) * s);
                    pet.setRotation(2.8f * s);
                    pet.setScaleX(1f + 0.010f * c);
                    pet.setScaleY(1f + 0.010f * c);
                    break;

                case 6: // 哈欠：身体明显伸展再恢复
                    float yawn = (1f - c) * 0.5f;
                    pet.setScaleX(1f + 0.045f * yawn);
                    pet.setScaleY(1f + 0.065f * yawn);
                    pet.setTranslationY(-dp(8) * yawn);
                    pet.setRotation(-2.2f * s);
                    break;

                case 7: // 睡觉：很慢的呼吸起伏
                    pet.setScaleX(1f + 0.015f * s);
                    pet.setScaleY(1f + 0.020f * s);
                    pet.setTranslationY(-dp(2) * s);
                    pet.setRotation(0.6f * s);
                    break;
            }
        });
        motionAnimator.start();
    }

    private void setState(int index, boolean saySomething) {
        if (pet == null) return;
        currentState = (index + stateAssets.length) % stateAssets.length;
        stopMotion();

        Bitmap bitmap = bitmapForState(currentState);
        if (bitmap != null) {
            pet.animate()
                    .alpha(0.15f)
                    .scaleX(0.94f)
                    .scaleY(0.94f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        if (pet == null) return;
                        pet.setImageBitmap(bitmap);
                        pet.setTranslationX(0f);
                        pet.setTranslationY(0f);
                        pet.setRotation(0f);
                        pet.animate()
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(150)
                                .withEndAction(() -> startContinuousMotion(currentState))
                                .start();
                    })
                    .start();
        } else {
            startContinuousMotion(currentState);
        }

        if (saySomething && bubble != null) {
            bubble.setText(lines[currentState]);
            bubble.animate().cancel();
            bubble.setScaleX(0.92f);
            bubble.setScaleY(0.92f);
            bubble.setAlpha(0.5f);
            bubble.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(180).start();
        }
    }

    private final Runnable autoBehavior = new Runnable() {
        @Override
        public void run() {
            long idle = System.currentTimeMillis() - lastInteraction;
            if (pet != null) {
                if (idle > 75000 && currentState != 7) {
                    setState(7, false);
                    if (bubble != null) bubble.setText("Zzz…");
                } else if (idle > 10000 && idle <= 75000) {
                    int next = random.nextInt(7);
                    if (next != currentState) setState(next, false);
                }
            }
            handler.postDelayed(this, 7000);
        }
    };

    private void rebuildSize() {
        if (pet == null || root == null || lp == null || wm == null) return;
        pet.setLayoutParams(new LinearLayout.LayoutParams(dp(petSize), dp(petHeightDp())));
        lp.width = dp(petSize + 54);
        lp.height = dp(petHeightDp() + 70);
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
                .setContentText("桌宠动画已开启")
                .setSmallIcon(android.R.drawable.btn_star)
                .build());

        lastInteraction = System.currentTimeMillis();
        showPet();
        handler.postDelayed(autoBehavior, 7000);
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
        bubble.setText("点点我，看我动起来～");
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
        Bitmap first = bitmapForState(0);
        if (first != null) pet.setImageBitmap(first);
        pet.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        pet.setLayerType(ImageView.LAYER_TYPE_HARDWARE, null);

        root.addView(bubble, new LinearLayout.LayoutParams(dp(205), dp(44)));
        root.addView(pet, new LinearLayout.LayoutParams(dp(petSize), dp(petHeightDp())));

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        lp = new WindowManager.LayoutParams(
                dp(petSize + 54),
                dp(petHeightDp() + 70),
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );

        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = dp(45);
        lp.y = dp(165);

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
                    if (dx < dp(12) && dy < dp(12)) {
                        setState((currentState + 1) % stateAssets.length, true);
                    }
                    return true;

                default:
                    return false;
            }
        });

        try {
            wm.addView(root, lp);
            handler.postDelayed(() -> startContinuousMotion(0), 250);
        } catch (Exception e) {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        stopMotion();
        if (wm != null && root != null) {
            try { wm.removeView(root); } catch (Exception ignored) {}
        }
        for (int i = 0; i < stateBitmaps.length; i++) {
            if (stateBitmaps[i] != null && !stateBitmaps[i].isRecycled()) {
                stateBitmaps[i].recycle();
            }
            stateBitmaps[i] = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
