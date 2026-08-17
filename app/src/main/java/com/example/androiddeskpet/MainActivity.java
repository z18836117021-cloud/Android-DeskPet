package com.example.androiddeskpet;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= 33)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        ((Button)findViewById(R.id.startButton)).setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
            } else {
                Intent i = new Intent(this, PetService.class);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
            }
        });
        ((Button)findViewById(R.id.stopButton)).setOnClickListener(v ->
            stopService(new Intent(this, PetService.class)));
    }
}
