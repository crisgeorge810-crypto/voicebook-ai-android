package com.voicebookai.app;

import android.app.*;
import android.content.*;
import android.media.MediaRecorder;
import android.os.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Keeps microphone capture alive independently of the Activity and speech recognition. */
public class RecordingService extends Service {
    public static final String ACTION_START = "com.voicebookai.app.RECORD_START";
    public static final String ACTION_STOP = "com.voicebookai.app.RECORD_STOP";
    private static final String CHANNEL = "voicebook_recording";
    private MediaRecorder recorder;
    private File output;
    private boolean started;

    @Override public void onCreate() { super.onCreate(); createChannel(); }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_STOP : intent.getAction();
        if (ACTION_START.equals(action)) startCapture(intent.getStringExtra("topic"));
        else if (ACTION_STOP.equals(action)) stopCapture();
        return START_NOT_STICKY;
    }

    private void startCapture(String topic) {
        if (started) return;
        try {
            String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
            String safeTopic = (topic == null || topic.trim().isEmpty()) ? "General" : topic.trim().replaceAll("[^A-Za-z0-9_-]", "_");
            File dir = new File(getFilesDir(), "recordings");
            if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Cannot create recordings folder");
            output = new File(dir, safeTopic + "-" + stamp + ".m4a");
            startForeground(41, notification("Recording " + safeTopic));
            if (Build.VERSION.SDK_INT >= 31) recorder = new MediaRecorder(this); else recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(96000);
            recorder.setAudioSamplingRate(44100);
            recorder.setOutputFile(output.getAbsolutePath());
            recorder.prepare();
            recorder.start();
            started = true;
            getSharedPreferences("voicebook", MODE_PRIVATE).edit().putString("recording_status", "recording").apply();
        } catch (Exception e) {
            releaseRecorder();
            stopForeground(true);
            stopSelf();
        }
    }

    private void stopCapture() {
        if (recorder != null && started) {
            try {
                recorder.stop();
                getSharedPreferences("voicebook", MODE_PRIVATE).edit()
                    .putString("last_audio", output.getAbsolutePath())
                    .putString("recording_status", "saved")
                    .apply();
            } catch (Exception ignored) {
                getSharedPreferences("voicebook", MODE_PRIVATE).edit().putString("recording_status", "failed").apply();
            }
        }
        started = false;
        releaseRecorder();
        stopForeground(true);
        stopSelf();
    }

    private void releaseRecorder() {
        if (recorder != null) { try { recorder.reset(); } catch (Exception ignored) {} try { recorder.release(); } catch (Exception ignored) {} recorder = null; }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "VoiceBook AI recording", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    private Notification notification(String text) {
        Intent stop = new Intent(this, RecordingService.class).setAction(ACTION_STOP);
        PendingIntent pi = PendingIntent.getService(this, 42, stop, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("VoiceBook AI")
            .setContentText(text).setOngoing(true).addAction(android.R.drawable.ic_media_pause, "Stop", pi).build();
    }

    @Override public void onDestroy() { if (started) stopCapture(); super.onDestroy(); }
    @Override public android.os.IBinder onBind(Intent intent) { return null; }
}
