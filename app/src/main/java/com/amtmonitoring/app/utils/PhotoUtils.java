package com.amtmonitoring.app.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility untuk menambahkan metadata GPS overlay pada foto
 * Mirip dengan aplikasi Geo Camera
 */
public class PhotoUtils {

    /**
     * Menambahkan overlay informasi GPS, tanggal, dan waktu pada foto
     */
    public static Bitmap addGpsOverlay(Bitmap original, double latitude, double longitude) {
        Bitmap result = original.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);

        int width = result.getWidth();
        int height = result.getHeight();

        // Background semi-transparent hitam di bawah
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#99000000"));
        bgPaint.setStyle(Paint.Style.FILL);

        float overlayHeight = height * 0.18f;
        float overlayTop = height - overlayHeight;
        canvas.drawRect(0, overlayTop, width, height, bgPaint);

        // Text paint
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(width * 0.035f);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        Paint smallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        smallPaint.setColor(Color.parseColor("#FFCC00"));
        smallPaint.setTextSize(width * 0.028f);
        smallPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));

        // Format tanggal dan waktu
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", new Locale("id", "ID"));
        Date now = new Date();
        String dateStr = dateFormat.format(now);
        String timeStr = timeFormat.format(now);

        // Padding
        float padding = width * 0.04f;
        float lineSpacing = width * 0.045f;

        float y = overlayTop + lineSpacing;

        // Header
        Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(Color.parseColor("#00E676"));
        headerPaint.setTextSize(width * 0.032f);
        headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.drawText("📍 AMT MONITORING GPS", padding, y, headerPaint);
        y += lineSpacing;

        // Tanggal
        canvas.drawText("Tanggal : " + dateStr, padding, y, textPaint);
        y += lineSpacing;

        // Waktu
        canvas.drawText("Waktu   : " + timeStr + " WIB", padding, y, textPaint);
        y += lineSpacing;

        // Koordinat
        String latStr = String.format(Locale.US, "%.6f", latitude);
        String lngStr = String.format(Locale.US, "%.6f", longitude);
        canvas.drawText("Lat: " + latStr + "  |  Lng: " + lngStr, padding, y, smallPaint);

        return result;
    }

    /**
     * Format tanggal untuk dikirim ke API
     */
    public static String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        return sdf.format(new Date());
    }
}
