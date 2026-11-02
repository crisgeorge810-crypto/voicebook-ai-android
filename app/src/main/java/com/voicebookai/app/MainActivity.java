package com.voicebookai.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.widget.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout root, savedList;
    EditText titleInput, noteInput;
    TextView status, audioStatus;
    SharedPreferences prefs;
    SpeechRecognizer recognizer;
    MediaRecorder recorder;
    MediaPlayer player;
    boolean listening = false;
    String mode = "GENERAL";
    String activeAudioPath = "";

    int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density); }

    TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(0xff202124);
        t.setPadding(dp(5), dp(7), dp(5), dp(7));
        return t;
    }

    Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        return b;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("voicebook", MODE_PRIVATE);
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(20));
        root.setBackgroundColor(0xfffff5df);
        scroll.addView(root);
        setContentView(scroll);

        TextView heading = text("📒 VoiceBook AI", 28);
        heading.setGravity(Gravity.CENTER);
        heading.setTextColor(0xff6039a5);
        root.addView(heading);
        root.addView(text("Your speaking notebook", 16));

        root.addView(text("CHOOSE YOUR MODE", 14));
        LinearLayout modes = new LinearLayout(this);
        Button church = button("⛪ Church Mode");
        Button general = button("📝 General Notes");
        modes.addView(church, new LinearLayout.LayoutParams(0, -2, 1));
        modes.addView(general, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(modes);
        status = text("General Notes mode selected.", 14);
        root.addView(status);
        church.setOnClickListener(v -> setMode("CHURCH"));
        general.setOnClickListener(v -> setMode("GENERAL"));

        root.addView(text("TOPIC / SERMON / MEETING TITLE", 14));
        titleInput = new EditText(this);
        titleInput.setSingleLine(true);
        titleInput.setHint("e.g. Sunday sermon or Team meeting");
        root.addView(titleInput);

        root.addView(text("YOUR NOTES / TRANSCRIPT", 14));
        noteInput = new EditText(this);
        noteInput.setMinLines(5);
        noteInput.setGravity(Gravity.TOP);
        noteInput.setHint("Speak or type your notes here");
        root.addView(noteInput);

        Button listen = button("🎙 Start listening");
        root.addView(listen);
        listen.setOnClickListener(v -> {
            if (listening) stopListening(listen);
            else startListening(listen);
        });

        Button save = button("💾 Save notes");
        root.addView(save);
        save.setOnClickListener(v -> saveNotes());

        root.addView(text("AUDIO RECORDING", 17));
        audioStatus = text("No recording in progress.", 14);
        root.addView(audioStatus);
        Button record = button("⏺ Start recording");
        root.addView(record);
        record.setOnClickListener(v -> {
            if (recorder == null) startRecording(record);
            else stopRecording(record);
        });

        root.addView(text("SAVED NOTEBOOK ENTRIES", 19));
        savedList = new LinearLayout(this);
        savedList.setOrientation(LinearLayout.VERTICAL);
        root.addView(savedList);
        refreshSaved();

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 9);
        }
    }

    void setMode(String selected) {
        mode = selected;
        status.setText(selected.equals("CHURCH")
                ? "Church Mode: ready for a sermon or church meeting."
                : "General Notes Mode: ready for meetings, lectures and everyday notes.");
    }

    void startListening(Button b) {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.setText("Speech recognition is not available on this phone.");
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 9);
            return;
        }
        if (recognizer != null) recognizer.destroy();
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(Bundle x) { status.setText("Listening… speak now."); }
            public void onBeginningOfSpeech() {}
            public void onRmsChanged(float x) {}
            public void onBufferReceived(byte[] x) {}
            public void onEndOfSpeech() { status.setText("Processing speech…"); }
            public void onError(int e) {
                listening = false;
                b.setText("🎙 Start listening");
                status.setText("Listening stopped. Tap Start listening to try again.");
            }
            public void onResults(Bundle x) {
                listening = false;
                b.setText("🎙 Start listening");
                ArrayList<String> results = x.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (results == null || results.isEmpty()) return;
                String spoken = results.get(0).trim();
                String lower = spoken.toLowerCase(Locale.ROOT);
                if (lower.equals("new topic") || lower.startsWith("new topic ")) {
                    String newTitle = spoken.length() > 9 ? spoken.substring(10).trim() : "";
                    titleInput.setText(newTitle.isEmpty() ? "New topic" : newTitle);
                    status.setText("New topic selected.");
                } else {
                    String old = noteInput.getText().toString();
                    noteInput.setText(old.isEmpty() ? spoken : old + "\n" + spoken);
                    noteInput.setSelection(noteInput.length());
                    status.setText("Speech added to your notes.");
                }
            }
            public void onPartialResults(Bundle x) {}
            public void onEvent(int event, Bundle x) {}
        });
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        listening = true;
        b.setText("■ Stop listening");
        recognizer.startListening(intent);
    }

    void stopListening(Button b) {
        if (recognizer != null) recognizer.cancel();
        listening = false;
        b.setText("🎙 Start listening");
        status.setText("Listening stopped.");
    }

    String timestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
    }

    void saveNotes() {
        String body = noteInput.getText().toString().trim();
        String title = titleInput.getText().toString().trim();
        if (body.isEmpty()) {
            status.setText("Write or speak some notes before saving.");
            return;
        }
        if (title.isEmpty()) title = mode.equals("CHURCH") ? "Church entry" : "General note";
        String id = String.valueOf(System.currentTimeMillis());
        String audioPath = activeAudioPath;
        String value = mode + "\n" + title + "\n" + timestamp() + "\n" + audioPath + "\n" + body;
        prefs.edit().putString("entry_" + id, value).apply();
        noteInput.setText("");
        activeAudioPath = "";
        status.setText("Saved: " + title);
        refreshSaved();
    }

    File createAudioFile() {
        File folder = new File(getFilesDir(), "recordings");
        if (!folder.exists()) folder.mkdirs();
        return new File(folder, "voicebook_" + System.currentTimeMillis() + ".3gp");
    }

    void startRecording(Button b) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 9);
            return;
        }
        try {
            File file = createAudioFile();
            activeAudioPath = file.getAbsolutePath();
            recorder = Build.VERSION.SDK_INT >= 31 ? new MediaRecorder(this) : new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(activeAudioPath);
            recorder.prepare();
            recorder.start();
            b.setText("■ Stop recording");
            audioStatus.setText("Recording this session…");
        } catch (Exception e) {
            audioStatus.setText("Could not start recording. Check microphone permission.");
            releaseRecorder();
        }
    }

    void stopRecording(Button b) {
        try {
            recorder.stop();
            audioStatus.setText("Recording saved. Save your notes to link it to this entry.");
        } catch (Exception e) {
            audioStatus.setText("Recording was too short. Please try again.");
            if (!activeAudioPath.isEmpty()) new File(activeAudioPath).delete();
            activeAudioPath = "";
        } finally {
            releaseRecorder();
            b.setText("⏺ Start recording");
        }
    }

    void releaseRecorder() {
        if (recorder != null) {
            try { recorder.release(); } catch (Exception ignored) {}
            recorder = null;
        }
    }

    void refreshSaved() {
        savedList.removeAllViews();
        ArrayList<String> keys = new ArrayList<>();
        for (String key : prefs.getAll().keySet()) if (key.startsWith("entry_") || key.startsWith("note_")) keys.add(key);
        Collections.sort(keys, Collections.reverseOrder());
        if (keys.isEmpty()) {
            savedList.addView(text("Your saved entries will appear here.", 14));
            return;
        }
        for (String key : keys) {
            String value = prefs.getString(key, "");
            String[] parts = value.split("\n", 5);
            String display;
            if (key.startsWith("entry_") && parts.length >= 5) {
                display = (parts[0].equals("CHURCH") ? "⛪ CHURCH" : "📝 GENERAL") +
                        "\n" + parts[1] + "\n" + parts[2] + "\n" + parts[4];
            } else {
                display = "📌 " + value.replaceFirst("\n", " • ");
            }
            TextView item = text(display, 14);
            item.setBackgroundColor(0xffffffff);
            savedList.addView(item);
            if (key.startsWith("entry_") && parts.length >= 4 && !parts[3].isEmpty()) {
                Button play = button("▶ Play this recording");
                savedList.addView(play);
                play.setOnClickListener(v -> playFile(parts[3]));
            }
        }
    }

    void playFile(String path) {
        try {
            if (player != null) player.release();
            player = new MediaPlayer();
            player.setDataSource(path);
            player.setOnCompletionListener(p -> {
                p.release();
                player = null;
                audioStatus.setText("Playback finished.");
            });
            player.prepare();
            player.start();
            audioStatus.setText("Playing saved recording…");
        } catch (Exception e) {
            audioStatus.setText("Could not play this recording.");
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (recognizer != null) recognizer.destroy();
        if (player != null) player.release();
        if (recorder != null) {
            try { recorder.stop(); } catch (Exception ignored) {}
            releaseRecorder();
        }
    }
}
