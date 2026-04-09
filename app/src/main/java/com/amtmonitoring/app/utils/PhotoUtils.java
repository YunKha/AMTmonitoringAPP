package com.amtmonitoring.app.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.graphics.Matrix;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhotoUtils {

    /**
     * Decode bitmap dan perbaiki orientasi EXIF
     */
    public static Bitmap decodeAndRotateBitmap(String filePath) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);

            ExifInterface exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:  matrix.postRotate(90);  break;
                case ExifInterface.ORIENTATION_ROTATE_180: matrix.postRotate(180); break;
                case ExifInterface.ORIENTATION_ROTATE_270: matrix.postRotate(270); break;
                default: break;
            }

            return Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Tambahkan overlay GPS + alamat ke foto
     * Versi baru dengan parameter address
     */
    public static Bitmap addGpsOverlay(Bitmap original, double latitude, double longitude, String address) {
        int width  = original.getWidth();
        int height = original.getHeight();

        Bitmap result = original.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);

        // Ukuran font proporsional dengan resolusi foto
        float scale    = width / 1080f;
        float fontSize = 28f * scale;
        float smallFontSize = 24f * scale;
        float padding  = 20f * scale;
        float lineGap  = 8f * scale;

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTypeface(android.graphics.Typeface.MONOSPACE);
        textPaint.setTextSize(fontSize);

        Paint smallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        smallPaint.setTypeface(android.graphics.Typeface.MONOSPACE);
        smallPaint.setTextSize(smallFontSize);

        // Hitung tinggi overlay — ada 4 baris + alamat
        float lineHeight      = fontSize + lineGap;
        float smallLineHeight = smallFontSize + lineGap;

        // Wrap alamat jika terlalu panjang
        String addressLine1 = "";
        String addressLine2 = "";
        if (address != null && !address.isEmpty()) {
            if (address.length() > 45) {
                int cutAt = address.lastIndexOf(",", 45);
                if (cutAt < 0) cutAt = 45;
                addressLine1 = address.substring(0, cutAt).trim();
                addressLine2 = address.substring(cutAt).replaceFirst("^,\\s*", "").trim();
            } else {
                addressLine1 = address;
            }
        }

        int addressLines  = addressLine2.isEmpty() ? 1 : 2;
        float overlayHeight = padding
                + lineHeight         // header AMT MONITORING GPS
                + lineHeight         // tanggal
                + lineHeight         // waktu
                + lineHeight         // koordinat
                + (smallLineHeight * addressLines) // alamat
                + padding;

        // Background semi-transparan hitam
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.argb(180, 0, 0, 0));
        float top = height - overlayHeight;
        canvas.drawRect(0, top, width, height, bgPaint);

        // Garis aksen biru di atas overlay
        Paint accentPaint = new Paint();
        accentPaint.setColor(Color.argb(220, 30, 58, 110));
        canvas.drawRect(0, top, width, top + (4f * scale), accentPaint);

        float y = top + padding + lineHeight;

        // Baris 1 — Header (hijau)
        textPaint.setColor(Color.rgb(0, 230, 118));
        textPaint.setFakeBoldText(true);
        canvas.drawText("📍 AMT MONITORING GPS", padding, y, textPaint);
        y += lineHeight;

        // Baris 2 — Tanggal (putih)
        textPaint.setColor(Color.WHITE);
        textPaint.setFakeBoldText(false);
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
        canvas.drawText("Tanggal : " + dateFmt.format(new Date()), padding, y, textPaint);
        y += lineHeight;

        // Baris 3 — Waktu (putih)
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss 'WIB'", new Locale("id", "ID"));
        canvas.drawText("Waktu   : " + timeFmt.format(new Date()), padding, y, textPaint);
        y += lineHeight;

        // Baris 4 — Koordinat (kuning)
        textPaint.setColor(Color.rgb(255, 214, 0));
        canvas.drawText(String.format(Locale.US,
                "Lat: %.6f  |  Lng: %.6f", latitude, longitude), padding, y, textPaint);
        y += lineHeight;

        // Baris 5+ — Alamat (cyan)
        if (!addressLine1.isEmpty()) {
            smallPaint.setColor(Color.rgb(0, 229, 255));
            canvas.drawText("🏠 " + addressLine1, padding, y, smallPaint);
            y += smallLineHeight;
            if (!addressLine2.isEmpty()) {
                canvas.drawText("    " + addressLine2, padding, y, smallPaint);
            }
        }

        return result;
    }

    /**
     * Overload lama — untuk backward compatibility jika ada kode lain yang panggil tanpa address
     */
    public static Bitmap addGpsOverlay(Bitmap original, double latitude, double longitude) {
        return addGpsOverlay(original, latitude, longitude, "");
    }

    /**
     * Ambil datetime sekarang sebagai string
     */
    public static String getCurrentDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }
}