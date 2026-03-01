package com.amtmonitoring.app.api;

import com.amtmonitoring.app.models.ApiResponse;
import com.amtmonitoring.app.models.LoginResponse;
import com.amtmonitoring.app.models.Ticket;
import com.amtmonitoring.app.models.User;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

/**
 * Retrofit API service interface
 *
 * PENTING - Nama field multipart harus cocok dengan backend Laravel:
 *   takeTicket  → driver_name, karnet_number, latitude, longitude, taken_at, photo
 *   completeTicket → latitude, longitude, taken_at, photo
 */
public interface ApiService {

    // ==================== AUTH ====================

    @FormUrlEncoded
    @POST("login")
    Call<LoginResponse> login(
            @Field("email") String email,
            @Field("password") String password);

    @POST("logout")
    Call<ApiResponse<Void>> logout();

    @GET("me")
    Call<ApiResponse<User>> getMe();

    // ==================== TICKETS ====================

    @GET("tickets/available")
    Call<ApiResponse<List<Ticket>>> getAvailableTickets();

    @GET("tickets/my-jobs")
    Call<ApiResponse<List<Ticket>>> getMyJobs();

    @GET("tickets/history")
    Call<ApiResponse<List<Ticket>>> getHistory();

    @GET("tickets/{id}")
    Call<ApiResponse<Ticket>> getTicketDetail(@Path("id") int id);

    @Multipart
    @POST("tickets/{id}/take")
    Call<ApiResponse<Ticket>> takeTicket(
            @Path("id") int id,
            // Backend validasi: 'driver_name' (bukan nama_sopir)
            @Part("driver_name") RequestBody driverName,
            // Backend validasi: 'karnet_number' (bukan nomor_karnet)
            @Part("karnet_number") RequestBody karnetNumber,
            @Part("latitude") RequestBody latitude,
            @Part("longitude") RequestBody longitude,
            @Part("taken_at") RequestBody takenAt,
            @Part MultipartBody.Part photo);

    @Multipart
    @POST("tickets/{id}/complete")
    Call<ApiResponse<Ticket>> completeTicket(
            @Path("id") int id,
            @Part("latitude") RequestBody latitude,
            @Part("longitude") RequestBody longitude,
            @Part("taken_at") RequestBody takenAt,
            @Part MultipartBody.Part photo);

    // ==================== PROFILE ====================

    @GET("profile")
    Call<ApiResponse<User>> getProfile();

    @FormUrlEncoded
    @PUT("profile")
    Call<ApiResponse<User>> updateProfile(
            @Field("name") String name,
            @Field("phone") String phone,
            @Field("employee_id") String employeeId);
}