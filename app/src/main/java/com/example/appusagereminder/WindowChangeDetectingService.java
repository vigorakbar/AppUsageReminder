package com.example.appusagereminder;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.Calendar;

import twitter4j.DirectMessage;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import static android.content.ContentValues.TAG;

public class WindowChangeDetectingService extends AccessibilityService {

    protected class CustomTimer {
        String name;
        int second;

        CustomTimer()
        {
            name = "";
            second = 0;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setSecond(int second) {
            this.second = second;
        }
    }

    CustomTimer customTimer = new CustomTimer();

    long startTime = 0;
    long millis = 0;
    int lastSubmit = 0;
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            Calendar calendar = Calendar.getInstance();
            int day = calendar.get(Calendar.DAY_OF_WEEK);
            customTimer.setSecond(seconds);
            Log.i("DAY", Boolean.toString(day == Calendar.SATURDAY));
            if (day != Calendar.SATURDAY &&  day != Calendar.SUNDAY && seconds % 60*15 == 0 && seconds != lastSubmit && seconds != 0) {
                lastSubmit = seconds;
                tweet();
            }

            timerHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        //Configure these here for compatibility with API 13 and below.
        AccessibilityServiceInfo config = new AccessibilityServiceInfo();
        config.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        config.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;

        if (Build.VERSION.SDK_INT >= 16)
            //Just in case this helps
            config.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;

        setServiceInfo(config);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() != null && event.getClassName() != null) {
                ComponentName componentName = new ComponentName(
                        event.getPackageName().toString(),
                        event.getClassName().toString()
                );

                ActivityInfo activityInfo = tryGetActivity(componentName);
                boolean isActivity = activityInfo != null;
                if (isActivity) {
                    String activeAppPackage = componentName.getPackageName();
                    Log.i("CurrentActivity name", activeAppPackage);
                    if (!customTimer.name.equals("com.android.systemui")) {
                        if ((activeAppPackage.equals("com.miui.home")) || (!activeAppPackage.equals(customTimer.name) && !isRegisteredPackage(activeAppPackage))) {
                            stopTimer();
                        } else if (activeAppPackage != customTimer.name && isRegisteredPackage(activeAppPackage)) {
                            startTimer(activeAppPackage);
                        }
                    }
                }
            }
        }
    }

    private ActivityInfo tryGetActivity(ComponentName componentName) {
        try {
            return getPackageManager().getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private void startTimer(String name) {
        if (millis == 0) {
            startTime = System.currentTimeMillis();
            timerHandler.postDelayed(timerRunnable, 0);
            customTimer.setName(name);
        }
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
        millis = 0;
        lastSubmit = 0;
        customTimer.setName("");
    }

    private boolean isRegisteredPackage(String name) {
        return (name.equals("com.twitter.android") || name.equals("com.facebook.katana") ||
                name.equals("com.ninegag.android.app") || name.equals("com.google.android.youtube") ||
                name.equals("com.instagram.android"));
    }

    private void tweet() {
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    ConfigurationBuilder cb = new ConfigurationBuilder();
                    cb.setDebugEnabled(true)
                            .setOAuthConsumerKey("UqalRB4LQYRfV53VYPEHdqmjS")
                            .setOAuthConsumerSecret("jLQqtLxeXpG3CjQvToVegOTt1siFnQrHVuT4I9uOoAkPH3RcW8")
                            .setOAuthAccessToken("1648837434-nUboAc3xzYAoByamAvp3UuTEStDclVmiWnGIXjQ")
                            .setOAuthAccessTokenSecret("wKM2l8tQJOH0myRuPU1Cw6hG7KS5xKAUk4FdcIBgQDRjs");
                    TwitterFactory tf = new TwitterFactory(cb.build());
                    Twitter twitter = tf.getInstance();
                    Status status = twitter.updateStatus(getStatusMessage());
                    DirectMessage message1 = twitter.sendDirectMessage("radiyyadwi", getStatusMessage());
                    Log.i("CurrentActivity TWEET","Successfully updated the status to [" + status.getText() + "].");
                    Log.i("CurrentActivity DM","Successfully sent DM: [" + message1.getText() + "].");
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        });
        thread.start();
    }

    private String getStatusMessage() {
        String appName = getAppName();
        String minutes = Integer.toString(customTimer.second / 60);

        String message = "[Bot] Aku udah buka "+appName+" selama "+minutes+" menit sekarang." +
                "Yang ingin berbaik hati buat ngingetin mahasiswa tingkat akhir ini untuk ngerjain TA dan tugas2nya," +
                "boleh bangent reply ini atau dm/chat langsung\r\n"+
                "Have a productive day everyone!";
        return message;
    }

    private String getAppName() {
        if (customTimer.name.equals("com.twitter.android")) {
            return "Twitter";
        } else if (customTimer.name.equals("com.facebook.katana")) {
            return "Facebook";
        } else if (customTimer.name.equals("com.ninegag.android.app")) {
            return "9Gag";
        } else if (customTimer.name.equals("com.google.android.youtube")) {
            return "Youtube";
        } else if (customTimer.name.equals("com.instagram.android")) {
            return "Instagram";
        } else {
            return "Whoops, something is wrong.";
        }
    }

    @Override
    public void onInterrupt() {}
}
