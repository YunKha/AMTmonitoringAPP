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

/**
 * Activity untuk menyelesaikan job - foto delivery di SPBU tujuan
 */
public class CompleteJobActivity extends AppCompatActivity {

    public static final String EXTRA_TICKET_ID = "ticket_id";
    public static final String EXTRA_TICKET_NOLO = "ticket_nolo";
    public static final String EXTRA_TICKET_SPBU = "ticket_spbu";

    private Button btnTakePhoto, btnSubmit;
    private ImageView ivPhotoPreview;
    private TextView tvTicketInfo, tvInstruction;
    private ProgressBar progressBar;

    private int ticketId;
    private String photoPath;
    private double latitude, longitude;
    private String takenAt;

    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_job);

        ticketId = getIntent().getIntExtra(EXTRA_TICKET_ID, -1);
        String nomorLo = getIntent().getStringExtra(EXTRA_TICKET_NOLO);
        String nomorSpbu = getIntent().getStringExtra(EXTRA_TICKET_SPBU);

        initViews();

        tvTicketInfo.setText("Tiket: " + nomorLo + " | SPBU: " + nomorSpbu);

        setupCameraLauncher();
        setupListeners();
    }

    private void initViews() {
        btnTakePhoto = findViewById(R.id.btn_take_photo);
        btnSubmit = findViewById(R.id.btn_submit);
        ivPhotoPreview = findViewById(R.id.iv_photo_preview);
        tvTicketInfo = findViewById(R.id.tv_ticket_info);
        tvInstruction = findViewById(R.id.tv_instruction);
        progressBar = findViewById(R.id.progress_bar);

        btnSubmit.setEnabled(false);
        tvInstruction.setText("Ambil foto di lokasi SPBU tujuan sebagai bukti pengantaran");
    }

    private void setupCameraLauncher() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        photoPath = data.getStringExtra(CameraActivity.EXTRA_PHOTO_PATH);
                        latitude = data.getDoubleExtra(CameraActivity.EXTRA_LATITUDE, 0);
                        longitude = data.getDoubleExtra(CameraActivity.EXTRA_LONGITUDE, 0);
                        takenAt = data.getStringExtra(CameraActivity.EXTRA_TAKEN_AT);

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
        btnTakePhoto.setOnClickListener(v -> {
            Intent intent = new Intent(this, CameraActivity.class);
            cameraLauncher.launch(intent);
        });

        btnSubmit.setOnClickListener(v -> submitCompletion());

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void submitCompletion() {
        if (photoPath == null) {
            Toast.makeText(this, "Ambil foto delivery terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        File photoFile = new File(photoPath);
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), photoFile);
        MultipartBody.Part photoPart = MultipartBody.Part.createFormData("photo", photoFile.getName(), requestFile);

        RequestBody latBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(latitude));
        RequestBody lngBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(longitude));
        RequestBody takenAtBody = RequestBody.create(MediaType.parse("text/plain"), takenAt);

        ApiService apiService = ApiClient.getApiService(this);
        apiService.completeTicket(ticketId, latBody, lngBody, takenAtBody, photoPart)
                .enqueue(new Callback<ApiResponse<Ticket>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Ticket>> call, Response<ApiResponse<Ticket>> response) {
                        showLoading(false);

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Toast.makeText(CompleteJobActivity.this, "Pengantaran selesai!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(CompleteJobActivity.this, "Gagal menyelesaikan pengantaran",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) {
                        showLoading(false);
                        Toast.makeText(CompleteJobActivity.this, "Koneksi gagal: " + t.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!show);
    }
}
