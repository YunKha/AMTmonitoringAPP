package com.amtmonitoring.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amtmonitoring.app.R;
import com.amtmonitoring.app.api.ApiClient;
import com.amtmonitoring.app.api.ApiService;
import com.amtmonitoring.app.models.ApiResponse;
import com.amtmonitoring.app.models.DeliveryPhoto;
import com.amtmonitoring.app.models.Ticket;
import com.bumptech.glide.Glide;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity untuk melihat detail tiket
 */
public class TicketDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TICKET_ID = "ticket_id";

    private TextView tvNomorLo, tvNomorSpbu, tvNomorShipTo;
    private TextView tvJumlahKl, tvJenisProduk, tvJarakTempuh;
    private TextView tvStatus, tvNamaSopir, tvNomorKarnet;
    private TextView tvTakenAt, tvCompletedAt;
    private LinearLayout layoutPhotos, layoutSopirInfo;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_detail);

        int ticketId = getIntent().getIntExtra(EXTRA_TICKET_ID, -1);

        initViews();
        loadTicketDetail(ticketId);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void initViews() {
        tvNomorLo = findViewById(R.id.tv_nomor_lo);
        tvNomorSpbu = findViewById(R.id.tv_nomor_spbu);
        tvNomorShipTo = findViewById(R.id.tv_nomor_ship_to);
        tvJumlahKl = findViewById(R.id.tv_jumlah_kl);
        tvJenisProduk = findViewById(R.id.tv_jenis_produk);
        tvJarakTempuh = findViewById(R.id.tv_jarak_tempuh);
        tvStatus = findViewById(R.id.tv_status);
        tvNamaSopir = findViewById(R.id.tv_nama_sopir);
        tvNomorKarnet = findViewById(R.id.tv_nomor_karnet);
        tvTakenAt = findViewById(R.id.tv_taken_at);
        tvCompletedAt = findViewById(R.id.tv_completed_at);
        layoutPhotos = findViewById(R.id.layout_photos);
        layoutSopirInfo = findViewById(R.id.layout_sopir_info);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void loadTicketDetail(int ticketId) {
        progressBar.setVisibility(View.VISIBLE);

        ApiService apiService = ApiClient.getApiService(this);
        apiService.getTicketDetail(ticketId).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(Call<ApiResponse<Ticket>> call, Response<ApiResponse<Ticket>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    displayTicket(response.body().getData());
                } else {
                    Toast.makeText(TicketDetailActivity.this, "Gagal memuat detail tiket", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(TicketDetailActivity.this, "Koneksi gagal", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayTicket(Ticket ticket) {
        tvNomorLo.setText(ticket.getNomorLo());
        tvNomorSpbu.setText(ticket.getNomorSpbu());
        tvNomorShipTo.setText(ticket.getNomorShipTo());
        tvJumlahKl.setText(ticket.getJumlahKl() + " Liter");
        tvJenisProduk.setText(ticket.getJenisProduk());
        tvJarakTempuh.setText(ticket.getJarakTempuh() + " KM");
        tvStatus.setText(ticket.getStatusDisplay());

        // Sopir info
        if (ticket.getNamaSopir() != null && !ticket.getNamaSopir().isEmpty()) {
            layoutSopirInfo.setVisibility(View.VISIBLE);
            tvNamaSopir.setText(ticket.getNamaSopir());
            tvNomorKarnet.setText(ticket.getNomorKarnet());
        }

        if (ticket.getTakenAt() != null) {
            tvTakenAt.setText(ticket.getTakenAt());
            tvTakenAt.setVisibility(View.VISIBLE);
        }

        if (ticket.getCompletedAt() != null) {
            tvCompletedAt.setText(ticket.getCompletedAt());
            tvCompletedAt.setVisibility(View.VISIBLE);
        }

        // Display photos
        if (ticket.getPhotos() != null && !ticket.getPhotos().isEmpty()) {
            layoutPhotos.setVisibility(View.VISIBLE);
            for (DeliveryPhoto photo : ticket.getPhotos()) {
                addPhotoView(photo);
            }
        }
    }

    private void addPhotoView(DeliveryPhoto photo) {
        View photoItem = getLayoutInflater().inflate(R.layout.item_photo_detail, layoutPhotos, false);

        ImageView ivPhoto = photoItem.findViewById(R.id.iv_photo);
        TextView tvType = photoItem.findViewById(R.id.tv_photo_type);
        TextView tvCoords = photoItem.findViewById(R.id.tv_photo_coords);

        tvType.setText(photo.getType().equals("check_in") ? "📍 Foto Check-In" : "📍 Foto Delivery");
        tvCoords.setText(String.format("Lat: %.6f | Lng: %.6f", photo.getLatitude(), photo.getLongitude()));

        // Load photo
        String photoUrl = photo.getPhotoUrl();
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this).load(photoUrl).into(ivPhoto);
        }

        layoutPhotos.addView(photoItem);
    }
}
