import java.awt.Color;
import java.awt.image.BufferedImage;

public class EntropyErrorCalc implements ErrorCalc {
    @Override
    public double computeError(BufferedImage image, int x, int y, int width, int height, Color avgColor) {
        int[] uniquePxCountR = new int[256];
        int[] uniquePxCountG = new int[256];
        int[] uniquePxCountB = new int[256];
        int count = width*height;

        for (int i=x; i<x+width; i++) {
            for (int j=y; j<y+height; j++) {
                Color c = new Color(image.getRGB(i, j));
                uniquePxCountR[c.getRed()]++;
                uniquePxCountG[c.getGreen()]++;
                uniquePxCountB[c.getBlue()]++;
            }
        }

        double hR=0, hG=0, hB=0;

        for (int i=0; i<256; i++) {
            if (uniquePxCountR[i]>0) {
                double pR = uniquePxCountR[i]/count;
                hR -= pR * (Math.log(pR)/Math.log(2));
            }
            if (uniquePxCountG[i]>0) {
                double pG = uniquePxCountG[i]/count;
                hG -= pG * (Math.log(pG)/Math.log(2));
            }
            if (uniquePxCountB[i]>0) {
                double pB = uniquePxCountB[i]/count;
                hB -= pB * (Math.log(pB)/Math.log(2));
            }
        }

        return (hR+hG+hB) / 3;
    }
}
