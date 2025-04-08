import java.awt.Color;
import java.awt.image.BufferedImage;

public class MADErrorCalc implements ErrorCalc {
    @Override
    public double computeError(BufferedImage image, int x, int y, int width, int height, Color avgColor) {
        double sumR=0, sumG=0, sumB=0;
        int count = 0;
        for (int j = y; j < y + height; j++) {
            for (int i = x; i < x + width; i++) {
                Color c = new Color(image.getRGB(i, j));
                sumR += Math.abs(c.getRed() - avgColor.getRed());
                sumG += Math.abs(c.getGreen() - avgColor.getGreen());
                sumB += Math.abs(c.getBlue() - avgColor.getBlue());
                count++;
            }
        }
        if (count == 0) count = 1;
        double madR = sumR/count; double madG = sumG/count; double madB = sumB/count;
        // Rata-rata MAD tiap kanal (dibagi 3)
        return (madR + madG + madB) / 3.0;
    }
}
