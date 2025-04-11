import java.awt.Color;
import java.awt.image.BufferedImage;

public class VarErrorCalc implements ErrorCalc {
    @Override
    public double computeError(BufferedImage image, int x, int y, int width, int height, Color avgColor) {
        double sumVariance = 0;
        int count = 0;
        
        // Pastikan tidak melewati ukuran gambar
        int endX = Math.min(x + width, image.getWidth());
        int endY = Math.min(y + height, image.getHeight());
        x = Math.max(0, x);
        y = Math.max(0, y);
        
        for (int j = y; j < endY; j++) {
            for (int i = x; i < endX; i++) {
                Color c = new Color(image.getRGB(i, j));
                double diffR = c.getRed() - avgColor.getRed();
                double diffG = c.getGreen() - avgColor.getGreen();
                double diffB = c.getBlue() - avgColor.getBlue();
                sumVariance += (diffR * diffR + diffG * diffG + diffB * diffB);
                count++;
            }
        }
        
        if (count == 0) return 0.0; // Jika dibagi 0
        
        // Rata-rata variansi tiap kanal (dibagi 3)
        return (sumVariance / count) / 3.0;
    }
}
