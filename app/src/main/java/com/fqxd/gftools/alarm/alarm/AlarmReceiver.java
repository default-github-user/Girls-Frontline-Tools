package com.fqxd.gftools.alarm.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.application.isradeleon.notify.Notify;
import com.fqxd.gftools.R;

import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        int count = bundle.getInt("count", 0x01);

        SharedPreferences sharedPreferences = context.getSharedPreferences(Integer.toString(count), Context.MODE_PRIVATE);
        int H = sharedPreferences.getInt("H", -1);
        int M = sharedPreferences.getInt("M", -1);
        String packagename = sharedPreferences.getString("package", "");
        Intent run = context.getPackageManager().getLaunchIntentForPackage(packagename);

        Notify.create(context)
                .setTitle("군수지원 완료!")
                .setContent("군수지원 " + Integer.toString(H) + "-" + Integer.toString(M) + " 완료!")
                .setLargeIcon(R.drawable.gf_icon)
                .circleLargeIcon()
                .setImportance(Notify.NotificationImportance.MAX)
                .setSmallIcon(R.drawable.start_xd)
                .setAction(run)
                .enableVibration(true)
                .setAutoCancel(true)
                .show();

        Calendar next = Calendar.getInstance();

        Log.d("AlarmReceiver.java", "chk point1");
        if (!(H == -1 || M == -1)) {
            if (sharedPreferences.getBoolean("isCal", false)) {
                while (!isRunning(context, packagename)) {
                    continue;
                }
            }
            AlarmUtills alarmUtills = new AlarmUtills();
            next = alarmUtills.calculate(H, M, context);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putLong("nextAlarm", next.getTimeInMillis());
            editor.apply();
            alarmUtills.repeat(sharedPreferences, context, count);
        }
        Log.d("AlarmReceiver.java", "chk point2");
    }

    boolean isRunning(Context context, String PackageName) {
        return true;
    }
}