package com.amtmonitoring.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.amtmonitoring.app.R;
import com.amtmonitoring.app.activities.CompleteJobActivity;
import com.amtmonitoring.app.activities.TakeTicketActivity;
import com.amtmonitoring.app.activities.TicketDetailActivity;
import com.amtmonitoring.app.adapters.TicketAdapter;
import com.amtmonitoring.app.api.ApiClient;
import com.amtmonitoring.app.api.ApiService;
import com.amtmonitoring.app.api.TokenManager;
import com.amtmonitoring.app.models.ApiResponse;
import com.amtmonitoring.app.models.Ticket;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Home Fragment - berisi New Job dan My Job
 */
public class HomeFragment extends Fragment {

    private LinearLayout boxNewJob, boxMyJob;
    private RecyclerView rvTickets;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvSectionTitle, tvEmpty, tvWelcome;
    private View viewNewJobIndicator, viewMyJobIndicator;

    private TicketAdapter adapter;
    private List<Ticket> ticketList = new ArrayList<>();
    private boolean isShowingNewJob = true;

    private ActivityResultLauncher<Intent> activityLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Refresh data setelah kembali dari activity
                    loadData();
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        setupListeners();
        setupRecyclerView();

        // Load new jobs by default
        loadNewJobs();

        return view;
    }

    private void initViews(View view) {
        boxNewJob = view.findViewById(R.id.box_new_job);
        boxMyJob = view.findViewById(R.id.box_my_job);
        rvTickets = view.findViewById(R.id.rv_tickets);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        tvSectionTitle = view.findViewById(R.id.tv_section_title);
        tvEmpty = view.findViewById(R.id.tv_empty);
        tvWelcome = view.findViewById(R.id.tv_welcome);
        viewNewJobIndicator = view.findViewById(R.id.view_new_job_indicator);
        viewMyJobIndicator = view.findViewById(R.id.view_my_job_indicator);

        TokenManager tokenManager = new TokenManager(requireContext());
        tvWelcome.setText("Halo, " + tokenManager.getUserName() + " 👋");
    }

    private void setupListeners() {
        boxNewJob.setOnClickListener(v -> {
            isShowingNewJob = true;
            updateTabUI();
            loadNewJobs();
        });

        boxMyJob.setOnClickListener(v -> {
            isShowingNewJob = false;
            updateTabUI();
            loadMyJobs();
        });

        swipeRefresh.setOnRefreshListener(this::loadData);
    }

    private void updateTabUI() {
        if (isShowingNewJob) {
            viewNewJobIndicator.setBackgroundColor(getResources().getColor(R.color.primary));
            viewMyJobIndicator.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            tvSectionTitle.setText("📦 Tiket Tersedia");
        } else {
            viewNewJobIndicator.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            viewMyJobIndicator.setBackgroundColor(getResources().getColor(R.color.primary));
            tvSectionTitle.setText("🚛 Tiket Aktif");
        }
    }

    private void setupRecyclerView() {
        adapter = new TicketAdapter(ticketList, new TicketAdapter.OnTicketClickListener() {
            @Override
            public void onTicketClick(Ticket ticket) {
                if (isShowingNewJob) {
                    // Ambil tiket baru
                    Intent intent = new Intent(requireContext(), TakeTicketActivity.class);
                    intent.putExtra(TakeTicketActivity.EXTRA_TICKET_ID, ticket.getId());
                    intent.putExtra(TakeTicketActivity.EXTRA_TICKET_NOLO, ticket.getNomorLo());
                    intent.putExtra(TakeTicketActivity.EXTRA_TICKET_SPBU, ticket.getNomorSpbu());
                    intent.putExtra(TakeTicketActivity.EXTRA_DRIVER_NAME, ticket.getNamaSopir());
                    intent.putExtra(TakeTicketActivity.EXTRA_KARNET_NUMBER, ticket.getNomorKarnet());
                    activityLauncher.launch(intent);
                } else {
                    // Selesaikan job
                    Intent intent = new Intent(requireContext(), CompleteJobActivity.class);
                    intent.putExtra(CompleteJobActivity.EXTRA_TICKET_ID, ticket.getId());
                    intent.putExtra(CompleteJobActivity.EXTRA_TICKET_NOLO, ticket.getNomorLo());
                    intent.putExtra(CompleteJobActivity.EXTRA_TICKET_SPBU, ticket.getNomorSpbu());
                    intent.putExtra(TakeTicketActivity.EXTRA_DRIVER_NAME, ticket.getNamaSopir());
                    intent.putExtra(TakeTicketActivity.EXTRA_KARNET_NUMBER, ticket.getNomorKarnet());
                    activityLauncher.launch(intent);
                }
            }

            @Override
            public void onTicketLongClick(Ticket ticket) {
                // Detail view
                Intent intent = new Intent(requireContext(), TicketDetailActivity.class);
                intent.putExtra(TicketDetailActivity.EXTRA_TICKET_ID, ticket.getId());
                startActivity(intent);
            }
        });

        rvTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTickets.setAdapter(adapter);
    }

    private void loadData() {
        if (isShowingNewJob) {
            loadNewJobs();
        } else {
            loadMyJobs();
        }
    }

    private void loadNewJobs() {
        swipeRefresh.setRefreshing(true);
        ApiService apiService = ApiClient.getApiService(requireContext());
        apiService.getAvailableTickets().enqueue(new Callback<ApiResponse<List<Ticket>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Ticket>>> call, Response<ApiResponse<List<Ticket>>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    ticketList.clear();
                    ticketList.addAll(response.body().getData());
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(ticketList.isEmpty() ? View.VISIBLE : View.GONE);
                    if (ticketList.isEmpty()) {
                        tvEmpty.setText("Tidak ada tiket tersedia");
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Ticket>>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadMyJobs() {
        swipeRefresh.setRefreshing(true);
        ApiService apiService = ApiClient.getApiService(requireContext());
        apiService.getMyJobs().enqueue(new Callback<ApiResponse<List<Ticket>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Ticket>>> call, Response<ApiResponse<List<Ticket>>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    ticketList.clear();
                    ticketList.addAll(response.body().getData());
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(ticketList.isEmpty() ? View.VISIBLE : View.GONE);
                    if (ticketList.isEmpty()) {
                        tvEmpty.setText("Tidak ada tiket aktif");
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Ticket>>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
