package com.example.androiddeskpet;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

public class MainActivity extends Activity {

    private boolean waitingForOverlayPermission = false;

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

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }

        findViewById(R.id.startButton).setOnClickListener(v -> requestOverlayAndStart());

        findViewById(R.id.stopButton).setOnClickListener(v ->
                stopService(new Intent(this, PetService.class)));

        findViewById(R.id.smallButton).setOnClickListener(v -> sendSize(180));
        findViewById(R.id.normalButton).setOnClickListener(v -> sendSize(240));
        findViewById(R.id.largeButton).setOnClickListener(v -> sendSize(310));
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
