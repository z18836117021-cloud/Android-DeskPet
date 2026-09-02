package com.example.kiddeskpet;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {
    private boolean waitingForOverlayPermission = false;

    private Bitmap loadAssetBitmap(String assetName) {
        try (InputStream in = getAssets().open(assetName);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
            byte[] encoded = out.toByteArray();
            byte[] decoded = Base64.decode(encoded, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
        } catch (Exception e) {
            return null;
        }
    }

    private void startPetService() {
        Intent i = new Intent(this, PetService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);
    }

    private void requestOverlayAndStart() {
        if (Settings.canDrawOverlays(this)) {
            startPetService();
        } else {
            waitingForOverlayPermission = true;
            startActivity(new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            ));
        }
    }

    private void sendSize(int sizeDp) {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayAndStart();
            return;
        }
        Intent i = new Intent(this, PetService.class);
        i.setAction("SIZE");
        i.putExtra("size", sizeDp);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bitmap preview = loadAssetBitmap("pet_idle.b64");
        if (preview != null) ((ImageView)findViewById(R.id.previewPet)).setImageBitmap(preview);

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }

        findViewById(R.id.startButton).setOnClickListener(v -> requestOverlayAndStart());
        findViewById(R.id.stopButton).setOnClickListener(v ->
                stopService(new Intent(this, PetService.class)));

        findViewById(R.id.smallButton).setOnClickListener(v -> sendSize(170));
        findViewById(R.id.normalButton).setOnClickListener(v -> sendSize(225));
        findViewById(R.id.largeButton).setOnClickListener(v -> sendSize(290));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (waitingForOverlayPermission && Settings.canDrawOverlays(this)) {
            waitingForOverlayPermission = false;
            startPetService();
        }
    }
}
