import java.awt.Color;
import java.awt.image.BufferedImage;

public class SSIMErrorCalc implements ErrorCalc {
    // Konstanta SSIM untuk gambar 8-bit (misalnya, K1=0.01, K2=0.03, L=255)
    private final double C1 = 6.5025;
    private final double C2 = 58.5225;

    @Override
    public double computeError(BufferedImage image, int x, int y, int width, int height, Color avgColor) {
        // Hitung SSIM per kanal dengan asumsi blok rekonstruksi konstan (nilai rata-rata)
        double ssimR = computeSSIM(image, x, y, width, height, 'R');
        double ssimG = computeSSIM(image, x, y, width, height, 'G');
        double ssimB = computeSSIM(image, x, y, width, height, 'B');
        double avgSSIM = (ssimR + ssimG + ssimB) / 3.0;
        // Ubah ke error: semakin tinggi SSIM, error harus rendah.
        return 1.0 - avgSSIM;
    }

    private double computeSSIM(BufferedImage image, int x, int y, int width, int height, char channel) {
        double sum = 0, sumSq = 0;
        int count = 0;
        for (int j = y; j < y + height; j++) {
            for (int i = x; i < x + width; i++) {
                Color c = new Color(image.getRGB(i, j));
                int val = 0;
                if (channel == 'R') {
                    val = c.getRed();
                } else if (channel == 'G') {
                    val = c.getGreen();
                } else if (channel == 'B') {
                    val = c.getBlue();
                }
                sum += val;
                sumSq += val * val;
                count++;
            }
        }
        if (count == 0) count = 1;
        double mean = sum / count;
        double variance = sumSq / count - mean * mean;
        // Karena blok rekonstruksi konstan, rumus SSIM menyederhanakan menjadi:
        double ssim = C2 / (variance + C2);
        return ssim;
    }
}
