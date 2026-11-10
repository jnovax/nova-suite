package vn.familytv.parentalcontrol.ui.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class QrCodeRendererTest {

    @Test
    public void testRenderQrMatrixProducesSquare() {
        boolean[][] matrix = QrCodeRenderer.renderQrMatrix("otpauth://totp/Test?secret=JBSWY3DPEHPK3PXP", 100);
        assertEquals(100, matrix.length);
        assertEquals(100, matrix[0].length);
        int blackCount = 0;
        for (boolean[] row : matrix) {
            for (boolean pixel : row) {
                if (pixel) blackCount++;
            }
        }
        assertTrue(blackCount > 0);
    }
}
