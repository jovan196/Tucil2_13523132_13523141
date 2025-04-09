import java.awt.Color;
import java.awt.image.BufferedImage;

public class PixelDiffErrorCalc implements ErrorCalc {
    @Override
    public double computeError(BufferedImage image, int x, int y, int width, int height, Color avgColor) {
        int maxR = 0, maxG = 0, maxB = 0;
        int minR = 255, minG = 255, minB = 255;
        
        for (int i=x; i<x+width; i++) {
            for (int j=y; j<y+height; j++) {
                Color c = new Color(image.getRGB(i, j));
                int r = c.getRed();
                int g = c.getGreen();
                int b = c.getBlue();
                maxR = Math.max(maxR, r);
                maxG = Math.max(maxG, g);
                maxB = Math.max(maxB, b);
                minR = Math.min(minR, r);
                minG = Math.min(minG, g);
                minB = Math.min(minB, b);
            }
        }

        double dR = maxR - minR;
        double dG = maxG - minG;
        double dB = maxB - minB;
        return (dR+dG+dB) / 3;
    }
}
