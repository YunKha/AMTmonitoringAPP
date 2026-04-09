package com.amtmonitoring.app.activities;

import android.os.Bundle;
import android.util.Log;
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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TicketDetailActivity extends AppCompatActivity {

    private static final String TAG = "TicketDetailActivity";
    public static final String EXTRA_TICKET_ID = "ticket_id";

    private TextView tvNomorLo, tvNomorSpbu, tvNomorShipTo;
    private TextView tvJumlahKl, tvJenisProduk, tvJarakTempuh;
    private TextView tvStatus, tvNamaSopir, tvNomorKarnet;
    private TextView tvTakenAt, tvCompletedAt;
    private LinearLayout layoutPhotos, layoutSopirInfo;
    private ImageView ivFotoSebelum, ivFotoSesudah;
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
        tvNomorLo       = findViewById(R.id.tv_nomor_lo);
        tvNomorSpbu     = findViewById(R.id.tv_nomor_spbu);
        tvNomorShipTo   = findViewById(R.id.tv_nomor_ship_to);
        tvJumlahKl      = findViewById(R.id.tv_jumlah_kl);
        tvJenisProduk   = findViewById(R.id.tv_jenis_produk);
        tvJarakTempuh   = findViewById(R.id.tv_jarak_tempuh);
        tvStatus        = findViewById(R.id.tv_status);
        tvNamaSopir     = findViewById(R.id.tv_nama_sopir);
        tvNomorKarnet   = findViewById(R.id.tv_nomor_karnet);
        tvTakenAt       = findViewById(R.id.tv_taken_at);
        tvCompletedAt   = findViewById(R.id.tv_completed_at);
        layoutPhotos    = findViewById(R.id.layout_photos);
        layoutSopirInfo = findViewById(R.id.layout_sopir_info);
        ivFotoSebelum   = findViewById(R.id.iv_foto_sebelum_1);
        ivFotoSesudah   = findViewById(R.id.iv_foto_sesudah_1);
        progressBar     = findViewById(R.id.progress_bar);
    }

    private void loadTicketDetail(int ticketId) {
        if (ticketId == -1) {
            Toast.makeText(this, "ID tiket tidak valid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        ApiService apiService = ApiClient.getApiService(this);
        apiService.getTicketDetail(ticketId).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(Call<ApiResponse<Ticket>> call,
                                   Response<ApiResponse<Ticket>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()) {
                    displayTicket(response.body().getData());
                } else {
                    Toast.makeText(TicketDetailActivity.this,
                            "Gagal memuat detail tiket", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "onFailure: " + t.getMessage());
                Toast.makeText(TicketDetailActivity.this,
                        "Koneksi gagal", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayTicket(Ticket ticket) {
        // Info tiket
        tvNomorLo.setText(ticket.getNomorLo() != null ? ticket.getNomorLo() : "-");
        tvNomorSpbu.setText(ticket.getNomorSpbu() != null ? ticket.getNomorSpbu() : "-");
        tvNomorShipTo.setText(ticket.getNomorShipTo() != null ? ticket.getNomorShipTo() : "-");
        tvJumlahKl.setText(ticket.getJumlahKl() != null ? ticket.getJumlahKl() + " KL" : "-");
        tvJenisProduk.setText(ticket.getJenisProduk() != null ? ticket.getJenisProduk() : "-");
        tvJarakTempuh.setText(ticket.getJarakTempuh() != null ? ticket.getJarakTempuh() + " KM" : "-");
        tvStatus.setText(ticket.getStatusDisplay());

        // Info sopir
        if (ticket.getNamaSopir() != null && !ticket.getNamaSopir().isEmpty()) {
            layoutSopirInfo.setVisibility(View.VISIBLE);
            tvNamaSopir.setText(ticket.getNamaSopir());
            tvNomorKarnet.setText(ticket.getNomorKarnet() != null ? ticket.getNomorKarnet() : "-");
        }

        if (ticket.getTakenAt() != null) {
            tvTakenAt.setText("Diambil: " + ticket.getTakenAt());
            tvTakenAt.setVisibility(View.VISIBLE);
        }

        if (ticket.getCompletedAt() != null) {
            tvCompletedAt.setText("Selesai: " + ticket.getCompletedAt());
            tvCompletedAt.setVisibility(View.VISIBLE);
        }

        // Foto
        List<DeliveryPhoto> photos = ticket.getPhotos();
        if (photos != null && !photos.isEmpty()) {
            layoutPhotos.setVisibility(View.VISIBLE);

            for (DeliveryPhoto photo : photos) {
                String url = photo.getPhotoUrl();
                if (url == null || url.isEmpty()) continue;

                Log.d(TAG, "Loading photo type=" + photo.getType() + " url=" + url);

                // Gunakan FIT_CENTER agar foto tidak dipaksa crop — orientasi tetap terjaga
                RequestOptions options = new RequestOptions()
                        .fitCenter()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(android.R.drawable.ic_menu_camera)
                        .error(android.R.drawable.ic_menu_close_clear_cancel);

                if ("checkin".equals(photo.getType())) {
                    ivFotoSebelum.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    Glide.with(this).load(url).apply(options).into(ivFotoSebelum);
                } else if ("checkout".equals(photo.getType())) {
                    ivFotoSesudah.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    Glide.with(this).load(url).apply(options).into(ivFotoSesudah);
                }
            }
        }
    }
}