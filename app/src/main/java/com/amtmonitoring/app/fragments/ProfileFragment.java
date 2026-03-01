package com.amtmonitoring.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.amtmonitoring.app.R;
import com.amtmonitoring.app.activities.LoginActivity;
import com.amtmonitoring.app.api.ApiClient;
import com.amtmonitoring.app.api.ApiService;
import com.amtmonitoring.app.api.TokenManager;
import com.amtmonitoring.app.models.ApiResponse;
import com.amtmonitoring.app.models.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Profile Fragment - menampilkan data profil sopir
 */
public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvPhone, tvEmployeeId, tvTotalDeliveries;
    private Button btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initViews(view);
        loadProfile();

        return view;
    }

    private void initViews(View view) {
        tvName = view.findViewById(R.id.tv_name);
        tvEmail = view.findViewById(R.id.tv_email);
        tvPhone = view.findViewById(R.id.tv_phone);
        tvEmployeeId = view.findViewById(R.id.tv_employee_id);
        tvTotalDeliveries = view.findViewById(R.id.tv_total_deliveries);
        btnLogout = view.findViewById(R.id.btn_logout);

        btnLogout.setOnClickListener(v -> logout());
    }

    private void loadProfile() {
        ApiService apiService = ApiClient.getApiService(requireContext());
        apiService.getProfile().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    User user = response.body().getData();
                    displayProfile(user);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Gagal memuat profil", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void displayProfile(User user) {
        tvName.setText(user.getName());
        tvEmail.setText(user.getEmail());
        tvPhone.setText(user.getPhone() != null ? user.getPhone() : "-");
        tvEmployeeId.setText(user.getEmployeeId() != null ? user.getEmployeeId() : "-");
        tvTotalDeliveries.setText(String.valueOf(user.getTotalDeliveries()));
    }

    private void logout() {
        ApiService apiService = ApiClient.getApiService(requireContext());
        apiService.logout().enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                performLogout();
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                performLogout();
            }
        });
    }

    private void performLogout() {
        TokenManager tokenManager = new TokenManager(requireContext());
        tokenManager.clear();
        ApiClient.reset();

        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
