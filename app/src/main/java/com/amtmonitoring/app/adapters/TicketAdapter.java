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
 * Adapter untuk daftar tiket di Home (New Job / My Job)
 */
public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.ViewHolder> {

    private List<Ticket> tickets;
    private OnTicketClickListener listener;

    public interface OnTicketClickListener {
        void onTicketClick(Ticket ticket);

        void onTicketLongClick(Ticket ticket);
    }

    public TicketAdapter(List<Ticket> tickets, OnTicketClickListener listener) {
        this.tickets = tickets;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ticket ticket = tickets.get(position);

        holder.tvNomorLo.setText(ticket.getNomorLo());
        holder.tvNomorSpbu.setText("SPBU " + ticket.getNomorSpbu());
        holder.tvJenisProduk.setText(ticket.getJenisProduk());
        holder.tvJumlahKl.setText(ticket.getJumlahKl() + " L");
        holder.tvJarakTempuh.setText(ticket.getJarakTempuh() + " KM");
        holder.tvStatus.setText(ticket.getStatusDisplay());

        // Status colors
        switch (ticket.getStatus()) {
            case "available":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_available);
                break;
            case "in_progress":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_progress);
                break;
            case "completed":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
                break;
        }

        holder.itemView.setOnClickListener(v -> listener.onTicketClick(ticket));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onTicketLongClick(ticket);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNomorLo, tvNomorSpbu, tvJenisProduk;
        TextView tvJumlahKl, tvJarakTempuh, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNomorLo = itemView.findViewById(R.id.tv_nomor_lo);
            tvNomorSpbu = itemView.findViewById(R.id.tv_nomor_spbu);
            tvJenisProduk = itemView.findViewById(R.id.tv_jenis_produk);
            tvJumlahKl = itemView.findViewById(R.id.tv_jumlah_kl);
            tvJarakTempuh = itemView.findViewById(R.id.tv_jarak_tempuh);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }
}
