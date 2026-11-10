package vn.familytv.parentalcontrol.ui.auth;

import android.graphics.Bitmap;
import android.graphics.Color;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public final class QrCodeRenderer {
    private QrCodeRenderer() {}

    public static boolean[][] renderQrMatrix(String content, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size);
            boolean[][] result = new boolean[size][size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    result[y][x] = bitMatrix.get(x, y);
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to render QR matrix", e);
        }
    }

    public static boolean[][] renderQrMatrix(String content) {
        return renderQrMatrix(content, 300);
    }

    public static Bitmap renderQrBitmap(String content, int size) {
        boolean[][] matrix = renderQrMatrix(content, size);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                bitmap.setPixel(x, y, matrix[y][x] ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }

    public static Bitmap renderQrBitmap(String content) {
        return renderQrBitmap(content, 300);
    }
}
