package com.example.trave_app;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.io.FileDescriptor;
import java.io.IOException;

public class PicnicActivity extends AppCompatActivity {

    private static final int REQ_CAMERA = 201;
    private static final int REQ_AUDIO = 202;
    private static final int REQ_IMAGE_CAPTURE = 301;
    private static final int REQ_VIDEO_CAPTURE = 302;

    private TextView txtResult;
    private MediaRecorder recorder;
    private boolean isRecording = false;

    private Uri currentPhotoUri = null;
    private Uri currentVideoUri = null;
    private Uri currentAudioUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_picnic);

        MaterialButton btnPhoto = findViewById(R.id.btnPhoto);
        MaterialButton btnVideo = findViewById(R.id.btnVideo);
        MaterialButton btnVoice = findViewById(R.id.btnVoice);
        MaterialButton btnOpenDirectory = findViewById(R.id.btnOpenDirectory);
        txtResult = findViewById(R.id.txtResult);

        btnPhoto.setOnClickListener(v -> capturePhoto());
        btnVideo.setOnClickListener(v -> captureVideo());
        btnVoice.setOnClickListener(v -> toggleVoiceRecording());
        btnOpenDirectory.setOnClickListener(v -> {
            Intent intent = new Intent(PicnicActivity.this, PlaceDirectoryActivity.class);
            startActivity(intent);
        });
    }

    private void capturePhoto() {
        if (ensurePermission(Manifest.permission.CAMERA, REQ_CAMERA)) {
            currentPhotoUri = createImageUri();
            if (currentPhotoUri == null) {
                txtResult.setText("Unable to create image location");
                return;
            }
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, REQ_IMAGE_CAPTURE);
            } else {
                txtResult.setText("No camera app available");
            }
        }
    }

    private void captureVideo() {
        if (ensurePermission(Manifest.permission.CAMERA, REQ_CAMERA)) {
            currentVideoUri = createVideoUri();
            if (currentVideoUri == null) {
                txtResult.setText("Unable to create video location");
                return;
            }
            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, currentVideoUri);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, REQ_VIDEO_CAPTURE);
            } else {
                txtResult.setText("No camera app available");
            }
        }
    }

    private void toggleVoiceRecording() {
        if (!isRecording) {
            if (ensurePermission(Manifest.permission.RECORD_AUDIO, REQ_AUDIO)) {
                startRecording();
            }
        } else {
            stopRecording();
        }
    }

    private boolean ensurePermission(String permission, int reqCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, reqCode);
            return false;
        }
        return true;
    }

    private Uri createImageUri() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TravelApp");
        }
        ContentResolver resolver = getContentResolver();
        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private Uri createVideoUri() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, "VID_" + System.currentTimeMillis() + ".mp4");
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/TravelApp");
        }
        ContentResolver resolver = getContentResolver();
        return resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
    }

    private Uri createAudioUri() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, "AUD_" + System.currentTimeMillis() + ".m4a");
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/TravelApp");
            values.put(MediaStore.Audio.Media.IS_MUSIC, 1);
        }
        ContentResolver resolver = getContentResolver();
        return resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
    }

    private void startRecording() {
        try {
            currentAudioUri = createAudioUri();
            if (currentAudioUri == null) {
                txtResult.setText("Unable to create audio location");
                return;
            }
            ContentResolver resolver = getContentResolver();
            try (android.os.ParcelFileDescriptor pfd = resolver.openFileDescriptor(currentAudioUri, "w")) {
                if (pfd == null) {
                    txtResult.setText("Unable to open audio file");
                    return;
                }
                FileDescriptor fd = pfd.getFileDescriptor();
                recorder = new MediaRecorder();
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.setOutputFile(fd);
                recorder.prepare();
                recorder.start();
                isRecording = true;
                txtResult.setText("Recording... Tap again to stop.");
            }
        } catch (IOException e) {
            txtResult.setText("Recording failed: " + e.getMessage());
        }
    }

    private void stopRecording() {
        try {
            if (recorder != null) {
                recorder.stop();
                recorder.reset();
                recorder.release();
                recorder = null;
            }
            isRecording = false;
            if (currentAudioUri != null) {
                txtResult.setText("Audio saved to Gallery: " + currentAudioUri.toString());
            } else {
                txtResult.setText("Recording stopped");
            }
        } catch (RuntimeException ex) {
            txtResult.setText("Stop failed: " + ex.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQ_IMAGE_CAPTURE) {
                txtResult.setText("Photo saved to Gallery: " + (currentPhotoUri != null ? currentPhotoUri.toString() : ""));
            } else if (requestCode == REQ_VIDEO_CAPTURE) {
                // Some camera apps may ignore EXTRA_OUTPUT and return their own Uri.
                Uri saved = (data != null && data.getData() != null) ? data.getData() : currentVideoUri;
                txtResult.setText("Video saved to Gallery: " + (saved != null ? saved.toString() : ""));
            }
        } else {
            // Clean up pre-created URIs if capture was cancelled
            if (requestCode == REQ_IMAGE_CAPTURE && currentPhotoUri != null) {
                getContentResolver().delete(currentPhotoUri, null, null);
                currentPhotoUri = null;
            }
            if (requestCode == REQ_VIDEO_CAPTURE && currentVideoUri != null) {
                getContentResolver().delete(currentVideoUri, null, null);
                currentVideoUri = null;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == REQ_CAMERA) {
                txtResult.setText("Camera permission granted. Tap again to proceed.");
            } else if (requestCode == REQ_AUDIO) {
                startRecording();
            }
        } else {
            txtResult.setText("Permission denied.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recorder != null) {
            try {
                recorder.release();
            } catch (Exception ignored) {}
        }
    }
}
