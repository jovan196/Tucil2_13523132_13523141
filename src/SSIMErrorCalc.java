import java.awt.Color;
import java.awt.image.BufferedImage;

public class SSIMErrorCalc implements ErrorCalc {
    // Konstanta untuk SSIM gambar 8-bit (K1=0.01, K2=0.03, L=255)
    private final double C1 = 6.5025;
    private final double C2 = 58.5225;

    @Override
    public double computeError(BufferedImage image, int x, int y, int width, int height, Color avgColor) {
        // Hitung SSIm per channel dengan asumsi blok konstanta
        double ssimR = computeSSIM(image, x, y, width, height, 'R', avgColor.getRed());
        double ssimG = computeSSIM(image, x, y, width, height, 'G', avgColor.getGreen());
        double ssimB = computeSSIM(image, x, y, width, height, 'B', avgColor.getBlue());
        double avgSSIM = (ssimR + ssimG + ssimB) / 3.0;
        
        // Konversi ke error (SSIM makin tinggi berarti error making rendah)
        return 1.0 - avgSSIM;
    }

    private double computeSSIM(BufferedImage image, int x, int y, int width, int height, 
                              char channel, int avgVal) {
        double sum = 0, sumSq = 0, covar = 0;
        int count = 0;
        
        // Pastikan tidak lewat ukuran gambar
        int endX = Math.min(x + width, image.getWidth());
        int endY = Math.min(y + height, image.getHeight());
        x = Math.max(0, x);
        y = Math.max(0, y);
        
        // Rata-rata dan variansi original block
        for (int j = y; j < endY; j++) {
            for (int i = x; i < endX; i++) {
                Color c = new Color(image.getRGB(i, j));
                int val = 0;
                switch (channel) {
                    case 'R' -> val = c.getRed();
                    case 'G' -> val = c.getGreen();
                    case 'B' -> val = c.getBlue();
                    default -> {
                    }
                }
                sum += val;
                sumSq += val * val;
                covar += val * avgVal; // Kovariansi dengan blok konstan
                count++;
            }
        }
        
        if (count == 0) return 1.0; // Return perfect match, cegah dibagi 0
        
        double mean = sum / count;
        double variance = sumSq / count - mean * mean;
        double covariance = covar / count - mean * avgVal;
        if (variance < 1e-3) variance = 1e-3;
        
        // Hitung SSIM index
        double numerator = (2 * mean * avgVal + C1) * (2 * covariance + C2);
        double denominator = (mean * mean + avgVal * avgVal + C1) * (variance + C2);
        
        return numerator / denominator;
    }
}
