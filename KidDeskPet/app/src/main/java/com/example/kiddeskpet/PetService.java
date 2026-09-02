package com.example.kiddeskpet;

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
            "耶！今天也很棒！",
            "要不要一起看书？",
            "让我想一想～",
            "有一点困困了～",
            "我要睡觉啦～"
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
        if (stateBitmaps[index] == null) stateBitmaps[index] = loadAssetBitmap(stateAssets[index]);
        return stateBitmaps[index];
    }

    private void resetMotion() {
        if (pet == null) return;
        pet.animate().cancel();
        pet.setTranslationX(0f);
        pet.setTranslationY(0f);
        pet.setRotation(0f);
        pet.setScaleX(1f);
        pet.setScaleY(1f);
        pet.setAlpha(1f);
    }

    private void setState(int index, boolean saySomething) {
        if (pet == null) return;
        currentState = (index + stateAssets.length) % stateAssets.length;
        resetMotion();

        Bitmap bitmap = bitmapForState(currentState);
        if (bitmap != null) pet.setImageBitmap(bitmap);

        pet.setAlpha(0.25f);
        pet.animate().alpha(1f).setDuration(180).start();

        if (saySomething && bubble != null) bubble.setText(lines[currentState]);
        handler.postDelayed(() -> playMotion(currentState), 190);
    }

    private void breatheLoop(final int state, final float scale, final int liftDp, final long duration) {
        if (pet == null || currentState != state) return;
        pet.animate().cancel();
        pet.animate()
                .scaleX(scale).scaleY(scale)
                .translationY(-dp(liftDp))
                .setDuration(duration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    if (pet == null || currentState != state) return;
                    pet.animate()
                            .scaleX(1f).scaleY(1f).translationY(0f)
                            .setDuration(duration)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .withEndAction(() -> breatheLoop(state, scale, liftDp, duration))
                            .start();
                })
                .start();
    }

    private void playMotion(int state) {
        if (pet == null || currentState != state) return;
        switch (state) {
            case 0:
                breatheLoop(0, 1.018f, 3, 950);
                break;
            case 1:
                pet.animate().rotation(-5f).translationY(-dp(3)).setDuration(170)
                        .withEndAction(() -> {
                            if (currentState != 1) return;
                            pet.animate().rotation(6f).setDuration(210)
                                    .withEndAction(() -> {
                                        if (currentState != 1) return;
                                        pet.animate().rotation(0f).translationY(0f).setDuration(180).start();
                                    }).start();
                        }).start();
                break;
            case 2:
                pet.animate().translationX(dp(26)).setDuration(380)
                        .withEndAction(() -> {
                            if (currentState != 2) return;
                            pet.animate().translationX(-dp(12)).setDuration(400)
                                    .withEndAction(() -> pet.animate().translationX(0f).setDuration(250).start())
                                    .start();
                        }).start();
                break;
            case 3:
                pet.animate().translationY(-dp(30)).scaleX(1.04f).scaleY(0.96f).setDuration(180)
                        .withEndAction(() -> {
                            if (currentState != 3) return;
                            pet.animate().translationY(0f).scaleX(1f).scaleY(1f).setDuration(260).start();
                        }).start();
                break;
            case 4:
            case 5:
                breatheLoop(state, 1.01f, 2, 1100);
                break;
            case 6:
                pet.animate().scaleX(1.045f).scaleY(1.045f).setDuration(320)
                        .withEndAction(() -> {
                            if (currentState != 6) return;
                            pet.animate().scaleX(1f).scaleY(1f).setDuration(430).start();
                        }).start();
                break;
            case 7:
                breatheLoop(7, 1.012f, 1, 1350);
                break;
        }
    }

    private final Runnable autoBehavior = new Runnable() {
        @Override
        public void run() {
            long idle = System.currentTimeMillis() - lastInteraction;
            if (pet != null) {
                if (idle > 75000 && currentState != 7) {
                    setState(7, false);
                    bubble.setText("Zzz…");
                } else if (idle > 14000 && idle <= 75000 && random.nextBoolean()) {
                    int next = random.nextInt(7);
                    if (next != currentState) setState(next, false);
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
        Bitmap first = bitmapForState(0);
        if (first != null) pet.setImageBitmap(first);
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
                        setState((currentState + 1) % stateAssets.length, true);
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
        resetMotion();
        if (wm != null && root != null) {
            try { wm.removeView(root); } catch (Exception ignored) {}
        }
        for (int i = 0; i < stateBitmaps.length; i++) {
            if (stateBitmaps[i] != null && !stateBitmaps[i].isRecycled()) stateBitmaps[i].recycle();
            stateBitmaps[i] = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
