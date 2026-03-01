package com.amtmonitoring.app.activities;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.amtmonitoring.app.R;
import com.amtmonitoring.app.api.ApiClient;
import com.amtmonitoring.app.api.ApiService;
import com.amtmonitoring.app.models.ApiResponse;
import com.amtmonitoring.app.models.Ticket;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TakeTicketActivity extends AppCompatActivity {

    public static final String EXTRA_TICKET_ID     = "ticket_id";
    public static final String EXTRA_TICKET_NOLO   = "ticket_nolo";
    public static final String EXTRA_TICKET_SPBU   = "ticket_spbu";
    // Key baru untuk data sopir dari admin
    public static final String EXTRA_DRIVER_NAME   = "driver_name";
    public static final String EXTRA_KARNET_NUMBER = "karnet_number";

    private Button btnTakePhoto, btnSubmit;
    private ImageView ivPhotoPreview;
    private TextView tvTicketInfo, tvDriverName, tvKarnetNumber, tvNoDriverInfo;
    private ProgressBar progressBar;

    private int ticketId;
    private String driverName;
    private String karnetNumber;
    private String photoPath;
    private double latitude, longitude;
    private String takenAt;

    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_ticket);

        // Ambil semua data dari Intent
        ticketId     = getIntent().getIntExtra(EXTRA_TICKET_ID, -1);
        String nomorLo   = getIntent().getStringExtra(EXTRA_TICKET_NOLO);
        String nomorSpbu = getIntent().getStringExtra(EXTRA_TICKET_SPBU);
        driverName   = getIntent().getStringExtra(EXTRA_DRIVER_NAME);
        karnetNumber = getIntent().getStringExtra(EXTRA_KARNET_NUMBER);

        initViews();
        tvTicketInfo.setText("Tiket: " + nomorLo + " | SPBU: " + nomorSpbu);

        // Tampilkan data sopir yang sudah diisi admin
        displayDriverInfo();

        setupCameraLauncher();
        setupListeners();
    }

    private void initViews() {
        btnTakePhoto    = findViewById(R.id.btn_take_photo);
        btnSubmit       = findViewById(R.id.btn_submit);
        ivPhotoPreview  = findViewById(R.id.iv_photo_preview);
        tvTicketInfo    = findViewById(R.id.tv_ticket_info);
        tvDriverName    = findViewById(R.id.tv_driver_name);
        tvKarnetNumber  = findViewById(R.id.tv_karnet_number);
        tvNoDriverInfo  = findViewById(R.id.tv_no_driver_info);
        progressBar     = findViewById(R.id.progress_bar);

        // Submit disabled sampai foto diambil
        btnSubmit.setEnabled(false);
    }

    /**
     * Tampilkan info sopir dari data tiket admin.
     * Jika admin belum mengisi, tampilkan peringatan dan blokir submit.
     */
    private void displayDriverInfo() {
        boolean hasDriverName   = driverName   != null && !driverName.trim().isEmpty();
        boolean hasKarnetNumber = karnetNumber != null && !karnetNumber.trim().isEmpty();

        if (hasDriverName && hasKarnetNumber) {
            // Data lengkap — tampilkan
            tvDriverName.setText("👤 Sopir   : " + driverName);
            tvKarnetNumber.setText("🪪 Karnet  : " + karnetNumber);
            tvDriverName.setVisibility(View.VISIBLE);
            tvKarnetNumber.setVisibility(View.VISIBLE);
            tvNoDriverInfo.setVisibility(View.GONE);
        } else {
            // Admin belum isi data sopir — blokir pengambilan tiket
            tvDriverName.setVisibility(View.GONE);
            tvKarnetNumber.setVisibility(View.GONE);
            tvNoDriverInfo.setVisibility(View.VISIBLE);
            tvNoDriverInfo.setText("⚠️ Data sopir belum diisi oleh admin.\nTiket ini belum bisa diambil.");

            // Nonaktifkan tombol foto dan submit
            btnTakePhoto.setEnabled(false);
            btnSubmit.setEnabled(false);

            Toast.makeText(this,
                    "Data sopir belum diisi oleh admin",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setupCameraLauncher() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        photoPath  = data.getStringExtra(CameraActivity.EXTRA_PHOTO_PATH);
                        latitude   = data.getDoubleExtra(CameraActivity.EXTRA_LATITUDE, 0);
                        longitude  = data.getDoubleExtra(CameraActivity.EXTRA_LONGITUDE, 0);
                        takenAt    = data.getStringExtra(CameraActivity.EXTRA_TAKEN_AT);

                        if (photoPath != null) {
                            ivPhotoPreview.setImageBitmap(BitmapFactory.decodeFile(photoPath));
                            ivPhotoPreview.setVisibility(View.VISIBLE);
                            btnSubmit.setEnabled(true);
                            btnTakePhoto.setText("📷 Ambil Ulang Foto");
                        }
                    }
                });
    }

    private void setupListeners() {
        btnTakePhoto.setOnClickListener(v ->
                cameraLauncher.launch(new Intent(this, CameraActivity.class)));

        btnSubmit.setOnClickListener(v -> submitTicket());

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void submitTicket() {
        // Validasi foto
        if (photoPath == null) {
            Toast.makeText(this, "Ambil foto check-in terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validasi data sopir (double-check, seharusnya sudah diblokir di displayDriverInfo)
        if (driverName == null || driverName.trim().isEmpty()) {
            Toast.makeText(this, "Data sopir belum diisi oleh admin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (karnetNumber == null || karnetNumber.trim().isEmpty()) {
            Toast.makeText(this, "Nomor karnet belum diisi oleh admin", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // Siapkan multipart request
        File photoFile = new File(photoPath);
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), photoFile);
        MultipartBody.Part photoPart =
                MultipartBody.Part.createFormData("photo", photoFile.getName(), requestFile);

        // Field name harus cocok dengan backend Laravel
        RequestBody driverNameBody  = RequestBody.create(MediaType.parse("text/plain"), driverName.trim());
        RequestBody karnetBody      = RequestBody.create(MediaType.parse("text/plain"), karnetNumber.trim());
        RequestBody latBody         = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(latitude));
        RequestBody lngBody         = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(longitude));
        RequestBody takenAtBody     = RequestBody.create(MediaType.parse("text/plain"),
                takenAt != null ? takenAt : "");

        ApiService apiService = ApiClient.getApiService(this);
        apiService.takeTicket(ticketId, driverNameBody, karnetBody, latBody, lngBody, takenAtBody, photoPart)
                .enqueue(new Callback<ApiResponse<Ticket>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Ticket>> call,
                                           Response<ApiResponse<Ticket>> response) {
                        showLoading(false);

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {
                            Toast.makeText(TakeTicketActivity.this,
                                    "Tiket berhasil diambil!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            String msg = "Gagal mengambil tiket";
                            if (response.body() != null && response.body().getMessage() != null) {
                                msg = response.body().getMessage();
                            }
                            Toast.makeText(TakeTicketActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) {
                        showLoading(false);
                        Toast.makeText(TakeTicketActivity.this,
                                "Koneksi gagal: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!show);
        btnTakePhoto.setEnabled(!show);
    }
}