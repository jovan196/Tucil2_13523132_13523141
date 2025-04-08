import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class QuadTree {
    private BufferedImage image;
    private int minBlockSize;
    private double threshold;
    private ErrorCalc errorCalculator;
    private QuadTreeNode root;
    private List<BufferedImage> gifFrames; // Untuk menyimpan frame GIF (bonus)

    public QuadTree(BufferedImage image, double threshold, int minBlockSize, ErrorCalc errorCalculator) {
        this.image = image;
        this.threshold = threshold;
        this.minBlockSize = minBlockSize;
        this.errorCalculator = errorCalculator;
        this.gifFrames = new ArrayList<>();
    }

    public QuadTreeNode getRoot() {
        return root;
    }

    public List<BufferedImage> getGifFrames() {
        return gifFrames;
    }

    public void buildTree() {
        root = buildTreeRecursive(0, 0, image.getWidth(), image.getHeight());
    }

    private QuadTreeNode buildTreeRecursive(int x, int y, int width, int height) {
        // Ensure we don't go outside the image boundaries
        if (x < 0 || y < 0 || x + width > image.getWidth() || y + height > image.getHeight()) {
            x = Math.max(0, x);
            y = Math.max(0, y);
            width = Math.min(width, image.getWidth() - x);
            height = Math.min(height, image.getHeight() - y);
        }
        
        // Check for zero or negative dimensions
        if (width <= 0 || height <= 0) {
            return null;
        }

        Color avgColor = computeAverageColor(x, y, width, height);
        double error = errorCalculator.computeError(image, x, y, width, height, avgColor);

        // Simplified stopping condition
        boolean tooSmall = width <= minBlockSize || height <= minBlockSize;
        boolean cannotSplitFurther = width <= 1 || height <= 1;
        boolean errorAcceptable = error <= threshold;
        
        if (errorAcceptable || tooSmall || cannotSplitFurther) {
            return new QuadTreeNode(x, y, width, height, avgColor, error);
        }

        // Create internal node and divide into 4 sub-blocks
        QuadTreeNode node = new QuadTreeNode(x, y, width, height, avgColor, error);
        node.children = new QuadTreeNode[4];

        int halfWidth = width / 2;
        int halfHeight = height / 2;
        int width2 = width - halfWidth;  // Handles odd widths correctly
        int height2 = height - halfHeight; // Handles odd heights correctly

        node.children[0] = buildTreeRecursive(x, y, halfWidth, halfHeight);
        node.children[1] = buildTreeRecursive(x + halfWidth, y, width2, halfHeight);
        node.children[2] = buildTreeRecursive(x, y + halfHeight, halfWidth, height2);
        node.children[3] = buildTreeRecursive(x + halfWidth, y + halfHeight, width2, height2);

        // Only add a frame periodically to avoid memory issues with large images
        // Add every 20 nodes or so (adjust as needed)
        if (gifFrames.size() % 20 == 0) {
            BufferedImage frame = renderTreeState();
            gifFrames.add(frame);
        }

        return node;
    }

    private Color computeAverageColor(int x, int y, int width, int height) {
        long sumR = 0, sumG = 0, sumB = 0;
        int count = 0;
        
        // Make sure we stay within image boundaries
        int endX = Math.min(x + width, image.getWidth());
        int endY = Math.min(y + height, image.getHeight());
        x = Math.max(0, x);
        y = Math.max(0, y);
        
        for (int j = y; j < endY; j++) {
            for (int i = x; i < endX; i++) {
                Color c = new Color(image.getRGB(i, j));
                sumR += c.getRed();
                sumG += c.getGreen();
                sumB += c.getBlue();
                count++;
            }
        }
        
        if (count == 0) return Color.BLACK; // Avoid division by zero
        
        int avgR = (int) (sumR / count);
        int avgG = (int) (sumG / count);
        int avgB = (int) (sumB / count);
        
        return new Color(avgR, avgG, avgB);
    }

    /**
     * Menghasilkan frame gambar yang menggambarkan kondisi Quadtree saat ini.
     * Frame ini digambar dengan menampilkan batas setiap blok (daun) menggunakan garis merah.
     */
    public BufferedImage renderTreeState() {
        BufferedImage frame = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics g = frame.getGraphics();
        // Gunakan gambar asli sebagai latar
        g.drawImage(image, 0, 0, null);
        drawBoundaries(g, root);
        g.dispose();
        return frame;
    }

    private void drawBoundaries(Graphics g, QuadTreeNode node) {
        if (node == null) return;
        if (node.isLeaf()) {
            g.setColor(Color.RED);
            g.drawRect(node.x, node.y, node.width, node.height);
        } else {
            for (QuadTreeNode child : node.children) {
                drawBoundaries(g, child);
            }
        }
    }

    /**
     * Menghasilkan gambar akhir hasil kompresi, dengan setiap blok daun diisi dengan warna rata–ratanya.
     */
    public BufferedImage generateCompressedImage() {
        BufferedImage output = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics g = output.getGraphics();
        fillCompressedImage(g, root);
        g.dispose();
        return output;
    }

    private void fillCompressedImage(Graphics g, QuadTreeNode node) {
        if (node == null) return;
        
        if (node.isLeaf()) {
            g.setColor(node.averageColor);
            g.fillRect(node.x, node.y, node.width, node.height);
        } else {
            for (QuadTreeNode child : node.children) {
                fillCompressedImage(g, child);
            }
        }
    }

    public int countLeaves() {
        return countLeavesRecursive(root);
    }

    private int countLeavesRecursive(QuadTreeNode node) {
        if (node == null) return 0;
        if (node.isLeaf()) return 1;
        int count = 0;
        for (QuadTreeNode child : node.children) {
            count += countLeavesRecursive(child);
        }
        return count;
    }

    public int getTreeDepth() {
        return computeDepth(root);
    }

    private int computeDepth(QuadTreeNode node) {
        if (node == null) return 0;
        if (node.isLeaf()) return 1;
        int max = 0;
        for (QuadTreeNode child : node.children) {
            int d = computeDepth(child);
            if (d > max) max = d;
        }
        return max + 1;
    }
}
