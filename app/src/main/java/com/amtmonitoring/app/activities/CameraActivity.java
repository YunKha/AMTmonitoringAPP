package com.amtmonitoring.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageButton;
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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final String TAG = "CameraActivity";

    public static final String EXTRA_PHOTO_PATH = "photo_path";
    public static final String EXTRA_LATITUDE   = "latitude";
    public static final String EXTRA_LONGITUDE  = "longitude";
    public static final String EXTRA_TAKEN_AT   = "taken_at";

    private PreviewView previewView;
    private ImageButton btnCapture, btnBack, btnFlipCamera;
    private TextView tvLocation, tvDateTime, tvAddress;
    private ImageCapture imageCapture;
    private GpsUtils gpsUtils;

    private double currentLatitude  = 0;
    private double currentLongitude = 0;
    private String currentAddress   = "";

    // Kamera aktif: belakang atau depan
    private int currentCameraFacing = CameraSelector.LENS_FACING_BACK;

    private final Executor geocodeExecutor = Executors.newSingleThreadExecutor();

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
        previewView   = findViewById(R.id.preview_view);
        btnCapture    = findViewById(R.id.btn_capture);
        btnBack       = findViewById(R.id.btn_back);
        btnFlipCamera = findViewById(R.id.btn_flip_camera);
        tvLocation    = findViewById(R.id.tv_location);
        tvDateTime    = findViewById(R.id.tv_date_time);
        tvAddress     = findViewById(R.id.tv_address);
    }

    private void setupListeners() {
        btnCapture.setOnClickListener(v -> capturePhoto());
        btnBack.setOnClickListener(v -> finish());
        btnFlipCamera.setOnClickListener(v -> flipCamera());
    }

    // ==================== Kamera ====================

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = future.get();
                bindCamera(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCamera(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(currentCameraFacing)
                .build();

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    /**
     * Toggle antara kamera depan dan belakang
     */
    private void flipCamera() {
        currentCameraFacing = (currentCameraFacing == CameraSelector.LENS_FACING_BACK)
                ? CameraSelector.LENS_FACING_FRONT
                : CameraSelector.LENS_FACING_BACK;

        // Update icon tombol
        btnFlipCamera.setImageResource(
                currentCameraFacing == CameraSelector.LENS_FACING_BACK
                        ? R.drawable.ic_camera_flip
                        : R.drawable.ic_camera_front
        );

        // Restart kamera dengan facing baru
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = future.get();
                bindCamera(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error flipping camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // ==================== GPS & Geocoding ====================

    private void startLocationUpdates() {
        gpsUtils.getCurrentLocation(new GpsUtils.LocationListener() {
            @Override
            public void onLocationReceived(double latitude, double longitude) {
                currentLatitude  = latitude;
                currentLongitude = longitude;
                updateLocationUI();
                fetchAddress(latitude, longitude);
                // Terus update lokasi
                gpsUtils.getCurrentLocation(this);
            }

            @Override
            public void onLocationError(String error) {
                tvLocation.setText("📍 Mendapatkan lokasi...");
                tvAddress.setText("🏠 Mencari alamat...");
            }
        });
    }

    /**
     * Reverse geocoding — ubah koordinat jadi nama jalan/alamat
     */
    private void fetchAddress(double latitude, double longitude) {
        geocodeExecutor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, new Locale("id", "ID"));
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

                String addressText = "";
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);

                    // Ambil komponen alamat yang tersedia
                    StringBuilder sb = new StringBuilder();
                    if (addr.getThoroughfare() != null) {
                        sb.append(addr.getThoroughfare()); // nama jalan
                    }
                    if (addr.getSubLocality() != null) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(addr.getSubLocality()); // kelurahan
                    }
                    if (addr.getSubAdminArea() != null) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(addr.getSubAdminArea()); // kecamatan/kota
                    }
                    if (addr.getAdminArea() != null) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(addr.getAdminArea()); // provinsi
                    }

                    addressText = sb.length() > 0 ? sb.toString() : "Alamat tidak ditemukan";
                } else {
                    addressText = "Alamat tidak ditemukan";
                }

                currentAddress = addressText;
                final String finalAddress = addressText;

                runOnUiThread(() -> tvAddress.setText("🏠 " + finalAddress));

            } catch (Exception e) {
                Log.e(TAG, "Geocoding error: " + e.getMessage());
                runOnUiThread(() -> tvAddress.setText("🏠 Alamat tidak tersedia"));
            }
        });
    }

    private void updateLocationUI() {
        runOnUiThread(() -> {
            tvLocation.setText(String.format(Locale.US,
                    "📍 Lat: %.6f | Lng: %.6f", currentLatitude, currentLongitude));
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy | HH:mm:ss", new Locale("id", "ID"));
            tvDateTime.setText("📅 " + sdf.format(new Date()));
        });
    }

    // ==================== Capture Photo ====================

    private void capturePhoto() {
        if (imageCapture == null) return;

        if (currentLatitude == 0 && currentLongitude == 0) {
            Toast.makeText(this, "Menunggu lokasi GPS...", Toast.LENGTH_SHORT).show();
            return;
        }

        File photoDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (photoDir != null && !photoDir.exists()) photoDir.mkdirs();

        String fileName = "AMT_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".jpg";
        File photoFile  = new File(photoDir, fileName);

        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        btnCapture.setEnabled(false);

        imageCapture.takePicture(options, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        String filePath = photoFile.getAbsolutePath();

                        Bitmap original = PhotoUtils.decodeAndRotateBitmap(filePath);

                        if (original != null) {
                            // Kirim alamat ke overlay foto
                            Bitmap overlayed = PhotoUtils.addGpsOverlay(
                                    original, currentLatitude, currentLongitude, currentAddress);

                            try (FileOutputStream fos = new FileOutputStream(photoFile)) {
                                overlayed.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                                fos.flush();
                            } catch (Exception e) {
                                Log.e(TAG, "Error saving overlayed photo", e);
                            } finally {
                                original.recycle();
                                overlayed.recycle();
                            }
                        }

                        Intent result = new Intent();
                        result.putExtra(EXTRA_PHOTO_PATH, filePath);
                        result.putExtra(EXTRA_LATITUDE,   currentLatitude);
                        result.putExtra(EXTRA_LONGITUDE,  currentLongitude);
                        result.putExtra(EXTRA_TAKEN_AT,   PhotoUtils.getCurrentDateTime());
                        setResult(RESULT_OK, result);
                        finish();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        btnCapture.setEnabled(true);
                        Toast.makeText(CameraActivity.this,
                                "Gagal mengambil foto: " + exception.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Photo capture error", exception);
                    }
                });
    }

    // ==================== Permissions ====================

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{ Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION },
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gpsUtils != null) gpsUtils.stopLocationUpdates();
    }
}