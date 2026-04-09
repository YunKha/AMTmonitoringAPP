package com.amtmonitoring.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Ticket {
    @SerializedName("id")
    private int id;

    // Backend kolom: lo_number
    @SerializedName("lo_number")
    private String nomorLo;

    // Backend kolom: spbu_number
    @SerializedName("spbu_number")
    private String nomorSpbu;

    // Backend kolom: ship_to
    @SerializedName("ship_to")
    private String nomorShipTo;

    // Backend kolom: quantity
    @SerializedName("quantity")
    private String jumlahKl;

    // Backend kolom: product_type
    @SerializedName("product_type")
    private String jenisProduk;

    // Backend kolom: distance_km
    @SerializedName("distance_km")
    private String jarakTempuh;

    @SerializedName("status")
    private String status;

    @SerializedName("driver_id")
    private Integer driverId;

    // Backend kolom: driver_name
    @SerializedName("driver_name")
    private String namaSopir;

    // Backend kolom: karnet_number
    @SerializedName("karnet_number")
    private String nomorKarnet;

    @SerializedName("taken_at")
    private String takenAt;

    @SerializedName("completed_at")
    private String completedAt;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("delivery_photos")
    private List<DeliveryPhoto> photos;

    // ==================== Getters & Setters ====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNomorLo() { return nomorLo; }
    public void setNomorLo(String nomorLo) { this.nomorLo = nomorLo; }

    public String getNomorSpbu() { return nomorSpbu; }
    public void setNomorSpbu(String nomorSpbu) { this.nomorSpbu = nomorSpbu; }

    public String getNomorShipTo() { return nomorShipTo; }
    public void setNomorShipTo(String nomorShipTo) { this.nomorShipTo = nomorShipTo; }

    public String getJumlahKl() { return jumlahKl; }
    public void setJumlahKl(String jumlahKl) { this.jumlahKl = jumlahKl; }

    public String getJenisProduk() { return jenisProduk; }
    public void setJenisProduk(String jenisProduk) { this.jenisProduk = jenisProduk; }

    public String getJarakTempuh() { return jarakTempuh; }
    public void setJarakTempuh(String jarakTempuh) { this.jarakTempuh = jarakTempuh; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getDriverId() { return driverId; }
    public void setDriverId(Integer driverId) { this.driverId = driverId; }

    public String getNamaSopir() { return namaSopir; }
    public void setNamaSopir(String namaSopir) { this.namaSopir = namaSopir; }

    public String getNomorKarnet() { return nomorKarnet; }
    public void setNomorKarnet(String nomorKarnet) { this.nomorKarnet = nomorKarnet; }

    public String getTakenAt() { return takenAt; }
    public void setTakenAt(String takenAt) { this.takenAt = takenAt; }

    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public List<DeliveryPhoto> getPhotos() { return photos; }
    public void setPhotos(List<DeliveryPhoto> photos) { this.photos = photos; }

    public String getStatusDisplay() {
        if (status == null) return "-";
        switch (status) {
            case "available":   return "Tersedia";
            case "in_progress": return "Dalam Proses";
            case "completed":   return "Selesai";
            default:            return status;
        }
    }
}