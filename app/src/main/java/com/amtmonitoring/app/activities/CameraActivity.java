package com.amtmonitoring.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amtmonitoring.app.R;
import com.amtmonitoring.app.utils.GpsUtils;
import com.amtmonitoring.app.utils.PhotoUtils;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * GPS Camera Activity - mengambil foto dengan overlay metadata GPS
 * Mirip dengan aplikasi Geo Camera
 */
public class CameraActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final String TAG = "CameraActivity";

    public static final String EXTRA_PHOTO_PATH = "photo_path";
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_TAKEN_AT = "taken_at";

    private PreviewView previewView;
    private ImageButton btnCapture, btnBack;
    private TextView tvLocation, tvDateTime;
    private ImageCapture imageCapture;
    private GpsUtils gpsUtils;

    private double currentLatitude = 0;
    private double currentLongitude = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        initViews();
        setupListeners();

        gpsUtils = new GpsUtils(this);

        if (checkPermissions()) {
            startCamera();
            startLocationUpdates();
        } else {
            requestPermissions();
        }
    }

    private void initViews() {
        previewView = findViewById(R.id.preview_view);
        btnCapture = findViewById(R.id.btn_capture);
        btnBack = findViewById(R.id.btn_back);
        tvLocation = findViewById(R.id.tv_location);
        tvDateTime = findViewById(R.id.tv_date_time);
    }

    private void setupListeners() {
        btnCapture.setOnClickListener(v -> capturePhoto());
        btnBack.setOnClickListener(v -> finish());
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[] { Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION },
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                startCamera();
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Izin kamera dan lokasi diperlukan", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void startLocationUpdates() {
        gpsUtils.getCurrentLocation(new GpsUtils.LocationListener() {
            @Override
            public void onLocationReceived(double latitude, double longitude) {
                currentLatitude = latitude;
                currentLongitude = longitude;
                updateLocationUI();

                // Terus update lokasi
                gpsUtils.getCurrentLocation(this);
            }

            @Override
            public void onLocationError(String error) {
                tvLocation.setText("📍 Mendapatkan lokasi...");
            }
        });
    }

    private void updateLocationUI() {
        runOnUiThread(() -> {
            String locText = String.format(Locale.US, "📍 Lat: %.6f | Lng: %.6f", currentLatitude, currentLongitude);
            tvLocation.setText(locText);

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy | HH:mm:ss", new Locale("id", "ID"));
            tvDateTime.setText("📅 " + sdf.format(new Date()));
        });
    }

    private void capturePhoto() {
        if (imageCapture == null)
            return;

        if (currentLatitude == 0 && currentLongitude == 0) {
            Toast.makeText(this, "Menunggu lokasi GPS...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Buat file temporary
        File photoDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (photoDir != null && !photoDir.exists()) {
            photoDir.mkdirs();
        }

        String fileName = "AMT_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".jpg";
        File photoFile = new File(photoDir, fileName);

        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        btnCapture.setEnabled(false);

        imageCapture.takePicture(options, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        // Tambahkan overlay GPS pada foto
                        Bitmap original = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                        if (original != null) {
                            Bitmap overlayed = PhotoUtils.addGpsOverlay(original, currentLatitude, currentLongitude);

                            // Simpan foto dengan overlay
                            try (FileOutputStream fos = new FileOutputStream(photoFile)) {
                                overlayed.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                            } catch (Exception e) {
                                Log.e(TAG, "Error saving overlayed photo", e);
                            }

                            original.recycle();
                            overlayed.recycle();
                        }

                        // Return hasil ke activity sebelumnya
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(EXTRA_PHOTO_PATH, photoFile.getAbsolutePath());
                        resultIntent.putExtra(EXTRA_LATITUDE, currentLatitude);
                        resultIntent.putExtra(EXTRA_LONGITUDE, currentLongitude);
                        resultIntent.putExtra(EXTRA_TAKEN_AT, PhotoUtils.getCurrentDateTime());
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        btnCapture.setEnabled(true);
                        Toast.makeText(CameraActivity.this, "Gagal mengambil foto: " + exception.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Photo capture error", exception);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gpsUtils != null) {
            gpsUtils.stopLocationUpdates();
        }
    }
}
