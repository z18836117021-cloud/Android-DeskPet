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
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
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
    private long lastInteraction;
    private int currentState = 0;
    private int petSize = 225;
    private ValueAnimator motionAnimator;

    private Bitmap spriteSheet;
    private final Bitmap[] frames = new Bitmap[8];

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
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int petHeightDp() {
        return Math.round(petSize * 1.15f);
    }

    private boolean loadFrames() {
        if (frames[0] != null) return true;
        spriteSheet = BitmapFactory.decodeResource(getResources(), R.drawable.kid_sprite);
        if (spriteSheet == null || spriteSheet.getWidth() < 4 || spriteSheet.getHeight() < 2) {
            return false;
        }
        int cellW = spriteSheet.getWidth() / 4;
        int cellH = spriteSheet.getHeight() / 2;
        for (int i = 0; i < frames.length; i++) {
            int col = i % 4;
            int row = i / 4;
            frames[i] = Bitmap.createBitmap(spriteSheet, col * cellW, row * cellH, cellW, cellH);
        }
        return true;
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
            case 0: return 1800L;
            case 1: return 900L;
            case 2: return 1050L;
            case 3: return 800L;
            case 4: return 2000L;
            case 5: return 2200L;
            case 6: return 1500L;
            case 7: return 2500L;
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
        motionAnimator.setInterpolator(state == 2
                ? new LinearInterpolator()
                : new AccelerateDecelerateInterpolator());

        motionAnimator.addUpdateListener(animation -> {
            if (pet == null || currentState != state) return;
            float f = (float) animation.getAnimatedValue();
            double phase = f * Math.PI * 2.0;
            float s = (float) Math.sin(phase);
            float c = (float) Math.cos(phase);
            float a = Math.abs(s);

            switch (state) {
                case 0:
                    pet.setScaleX(1f + 0.025f * s);
                    pet.setScaleY(1f + 0.032f * s);
                    pet.setTranslationY(-dp(4) * s);
                    pet.setRotation(0.7f * s);
                    break;
                case 1:
                    pet.setRotation(7f * s);
                    pet.setTranslationX(dp(5) * s);
                    pet.setTranslationY(-dp(4) * a);
                    break;
                case 2:
                    pet.setTranslationX(dp(22) * s);
                    pet.setTranslationY(-dp(8) * Math.abs((float) Math.sin(phase * 2.0)));
                    pet.setRotation(2.5f * (float) Math.sin(phase * 2.0));
                    break;
                case 3:
                    float jump = Math.abs((float) Math.sin(phase));
                    pet.setTranslationY(-dp(34) * jump);
                    pet.setScaleX(1f + 0.04f * jump);
                    pet.setScaleY(1f - 0.04f * jump);
                    break;
                case 4:
                    pet.setTranslationY(-dp(3) * s);
                    pet.setRotation(1.2f * s);
                    break;
                case 5:
                    pet.setTranslationY(-dp(4) * s);
                    pet.setRotation(2.5f * s);
                    break;
                case 6:
                    float yawn = (1f - c) * 0.5f;
                    pet.setScaleX(1f + 0.04f * yawn);
                    pet.setScaleY(1f + 0.06f * yawn);
                    pet.setTranslationY(-dp(8) * yawn);
                    break;
                case 7:
                    pet.setScaleX(1f + 0.014f * s);
                    pet.setScaleY(1f + 0.020f * s);
                    pet.setTranslationY(-dp(2) * s);
                    break;
            }
        });
        motionAnimator.start();
    }

    private void setState(int index, boolean saySomething) {
        if (pet == null || !loadFrames()) return;
        currentState = (index + frames.length) % frames.length;
        stopMotion();
        final Bitmap frame = frames[currentState];

        pet.animate()
                .alpha(0.25f)
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(90)
                .withEndAction(() -> {
                    if (pet == null) return;
                    pet.setImageBitmap(frame);
                    pet.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(140)
                            .withEndAction(() -> startContinuousMotion(currentState))
                            .start();
                })
                .start();

        if (saySomething && bubble != null) {
            bubble.setText(lines[currentState]);
            bubble.animate().cancel();
            bubble.setScaleX(0.92f);
            bubble.setScaleY(0.92f);
            bubble.setAlpha(0.55f);
            bubble.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(170).start();
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
                } else if (idle > 12000 && idle <= 75000) {
                    int next = random.nextInt(7);
                    if (next != currentState) setState(next, false);
                }
            }
            handler.postDelayed(this, 8000);
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
                    "kidpet", "可爱桌宠服务", NotificationManager.IMPORTANCE_LOW);
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
        handler.postDelayed(autoBehavior, 8000);
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
        if (!loadFrames()) {
            stopSelf();
            return;
        }
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

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
        pet.setImageBitmap(frames[0]);
        pet.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        pet.setLayerType(ImageView.LAYER_TYPE_HARDWARE, null);

        root.addView(bubble, new LinearLayout.LayoutParams(dp(205), dp(44)));
        root.addView(pet, new LinearLayout.LayoutParams(dp(petSize), dp(petHeightDp())));

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        lp = new WindowManager.LayoutParams(
                dp(petSize + 54), dp(petHeightDp() + 70), type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
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
                    lp.x = originX + (int) (event.getRawX() - downX);
                    lp.y = originY + (int) (event.getRawY() - downY);
                    try { wm.updateViewLayout(root, lp); } catch (Exception ignored) {}
                    return true;
                case MotionEvent.ACTION_UP:
                    lastInteraction = System.currentTimeMillis();
                    float dx = Math.abs(event.getRawX() - downX);
                    float dy = Math.abs(event.getRawY() - downY);
                    if (dx < dp(12) && dy < dp(12)) {
                        setState((currentState + 1) % frames.length, true);
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
        for (int i = 0; i < frames.length; i++) {
            if (frames[i] != null && !frames[i].isRecycled()) frames[i].recycle();
            frames[i] = null;
        }
        if (spriteSheet != null && !spriteSheet.isRecycled()) spriteSheet.recycle();
        spriteSheet = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
