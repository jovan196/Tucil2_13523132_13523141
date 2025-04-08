import java.awt.Color;
import java.awt.image.BufferedImage;

public interface ErrorCalc {
    /**
     * Menghitung error untuk blok gambar [x,y,width,height] berdasarkan nilai rata-rata.
     */
    double computeError(BufferedImage image, int x, int y, int width, int height, Color avgColor);
}
