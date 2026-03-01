package com.amtmonitoring.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.amtmonitoring.app.R;
import com.amtmonitoring.app.activities.TicketDetailActivity;
import com.amtmonitoring.app.adapters.HistoryAdapter;
import com.amtmonitoring.app.api.ApiClient;
import com.amtmonitoring.app.api.ApiService;
import com.amtmonitoring.app.models.ApiResponse;
import com.amtmonitoring.app.models.Ticket;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * History Fragment - riwayat pengantaran yang sudah selesai
 */
public class HistoryFragment extends Fragment {

    private RecyclerView rvHistory;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmpty;
    private HistoryAdapter adapter;
    private List<Ticket> historyList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        initViews(view);
        setupRecyclerView();
        loadHistory();

        return view;
    }

    private void initViews(View view) {
        rvHistory = view.findViewById(R.id.rv_history);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        tvEmpty = view.findViewById(R.id.tv_empty);

        swipeRefresh.setOnRefreshListener(this::loadHistory);
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter(historyList, ticket -> {
            Intent intent = new Intent(requireContext(), TicketDetailActivity.class);
            intent.putExtra(TicketDetailActivity.EXTRA_TICKET_ID, ticket.getId());
            startActivity(intent);
        });

        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistory.setAdapter(adapter);
    }

    private void loadHistory() {
        swipeRefresh.setRefreshing(true);

        ApiService apiService = ApiClient.getApiService(requireContext());
        apiService.getHistory().enqueue(new Callback<ApiResponse<List<Ticket>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Ticket>>> call, Response<ApiResponse<List<Ticket>>> response) {
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    historyList.clear();
                    historyList.addAll(response.body().getData());
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(historyList.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Ticket>>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Gagal memuat riwayat", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
