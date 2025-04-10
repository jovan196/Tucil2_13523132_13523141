import java.awt.Color;
import java.awt.image.BufferedImage;

public class SSIMErrorCalc implements ErrorCalc {
    // Constants for SSIM for 8-bit images (K1=0.01, K2=0.03, L=255)
    private final double C1 = 6.5025;
    private final double C2 = 58.5225;

    @Override
    public double computeError(BufferedImage image, int x, int y, int width, int height, Color avgColor) {
        // Calculate SSIM per channel with constant block assumption
        double ssimR = computeSSIM(image, x, y, width, height, 'R', avgColor.getRed());
        double ssimG = computeSSIM(image, x, y, width, height, 'G', avgColor.getGreen());
        double ssimB = computeSSIM(image, x, y, width, height, 'B', avgColor.getBlue());
        double avgSSIM = (ssimR + ssimG + ssimB) / 3.0;
        
        // Convert to error: higher SSIM means lower error
        return 1.0 - avgSSIM;
    }

    private double computeSSIM(BufferedImage image, int x, int y, int width, int height, 
                              char channel, int avgVal) {
        double sum = 0, sumSq = 0, covar = 0;
        int count = 0;
        
        // Make sure we stay within image boundaries
        int endX = Math.min(x + width, image.getWidth());
        int endY = Math.min(y + height, image.getHeight());
        x = Math.max(0, x);
        y = Math.max(0, y);
        
        // Mean and variance of original block
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
                covar += val * avgVal; // Covariance with constant block
                count++;
            }
        }
        
        if (count == 0) return 1.0; // Avoid division by zero, return perfect match
        
        double mean = sum / count;
        double variance = sumSq / count - mean * mean;
        double covariance = covar / count - mean * avgVal;
        if (variance < 1e-3) variance = 1e-3;
        
        // Calculate SSIM index
        double numerator = (2 * mean * avgVal + C1) * (2 * covariance + C2);
        double denominator = (mean * mean + avgVal * avgVal + C1) * (variance + C2);
        
        return numerator / denominator;
    }
}
