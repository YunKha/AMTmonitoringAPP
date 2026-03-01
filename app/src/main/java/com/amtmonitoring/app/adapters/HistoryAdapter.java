package com.amtmonitoring.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amtmonitoring.app.R;
import com.amtmonitoring.app.models.Ticket;

import java.util.List;

/**
 * Adapter untuk riwayat pengantaran
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<Ticket> tickets;
    private OnHistoryClickListener listener;

    public interface OnHistoryClickListener {
        void onHistoryClick(Ticket ticket);
    }

    public HistoryAdapter(List<Ticket> tickets, OnHistoryClickListener listener) {
        this.tickets = tickets;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ticket ticket = tickets.get(position);

        holder.tvNomorLo.setText(ticket.getNomorLo());
        holder.tvNomorSpbu.setText("SPBU " + ticket.getNomorSpbu());
        holder.tvJenisProduk.setText(ticket.getJenisProduk());
        holder.tvJumlahKl.setText(ticket.getJumlahKl() + " L");
        holder.tvCompletedAt.setText("Selesai: " + (ticket.getCompletedAt() != null ? ticket.getCompletedAt() : "-"));
        holder.tvNamaSopir.setText("Sopir: " + (ticket.getNamaSopir() != null ? ticket.getNamaSopir() : "-"));

        holder.itemView.setOnClickListener(v -> listener.onHistoryClick(ticket));
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNomorLo, tvNomorSpbu, tvJenisProduk;
        TextView tvJumlahKl, tvCompletedAt, tvNamaSopir;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNomorLo = itemView.findViewById(R.id.tv_nomor_lo);
            tvNomorSpbu = itemView.findViewById(R.id.tv_nomor_spbu);
            tvJenisProduk = itemView.findViewById(R.id.tv_jenis_produk);
            tvJumlahKl = itemView.findViewById(R.id.tv_jumlah_kl);
            tvCompletedAt = itemView.findViewById(R.id.tv_completed_at);
            tvNamaSopir = itemView.findViewById(R.id.tv_nama_sopir);
        }
    }
}
