package com.example.androiddeskpet;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.*;
import android.view.*;
import android.widget.*;

public class PetService extends Service {
    WindowManager wm; LinearLayout root; WindowManager.LayoutParams p;
    float sx,sy; int px,py;

    int dp(int x){ return (int)(x*getResources().getDisplayMetrics().density+.5f); }

    @Override public void onCreate() {
        super.onCreate();
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel c=new NotificationChannel("pet","桌宠服务",NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
        Notification.Builder nb = Build.VERSION.SDK_INT>=26 ?
            new Notification.Builder(this,"pet") : new Notification.Builder(this);
        startForeground(7,nb.setContentTitle("桌宠正在运行")
            .setSmallIcon(android.R.drawable.btn_star).build());
        showPet();
    }

    void showPet(){
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER);

        TextView bubble=new TextView(this);
        bubble.setText("点点我～"); bubble.setTextSize(14); bubble.setTextColor(Color.DKGRAY);
        bubble.setGravity(Gravity.CENTER); bubble.setBackgroundColor(0xDDFFFFFF);

        ImageView pet=new ImageView(this); pet.setImageResource(R.drawable.pet);
        pet.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        root.addView(bubble,new LinearLayout.LayoutParams(dp(150),dp(42)));
        root.addView(pet,new LinearLayout.LayoutParams(dp(190),dp(270)));

        int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
        p=new WindowManager.LayoutParams(dp(210),dp(325),type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.START; p.x=dp(70); p.y=dp(220);

        root.setOnTouchListener((v,e)->{
            if(e.getAction()==MotionEvent.ACTION_DOWN){sx=e.getRawX();sy=e.getRawY();px=p.x;py=p.y;return true;}
            if(e.getAction()==MotionEvent.ACTION_MOVE){p.x=px+(int)(e.getRawX()-sx);p.y=py+(int)(e.getRawY()-sy);wm.updateViewLayout(root,p);return true;}
            if(e.getAction()==MotionEvent.ACTION_UP){
                if(Math.abs(e.getRawX()-sx)<dp(10)&&Math.abs(e.getRawY()-sy)<dp(10))
                    bubble.setText(new String[]{"在干嘛呢？","陪你一会儿～","今天也要加油！","该喝水啦～"}[(int)(Math.random()*4)]);
                return true;
            }
            return false;
        });
        wm.addView(root,p);
    }

    @Override public void onDestroy(){if(wm!=null&&root!=null)try{wm.removeView(root);}catch(Exception ignored){} super.onDestroy();}
    @Override public android.os.IBinder onBind(Intent i){return null;}
}
