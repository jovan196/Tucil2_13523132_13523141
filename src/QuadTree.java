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
    private List<BufferedImage> gifFrames; // Menyimpan frame GIF (bonus)
    
    // Options for controlling GIF creation
    private boolean recordGif = true;
    private int maxFrames = 100; // Maximum number of frames to record
    private int frameSkip = 1;   // Record every Nth frame (1 = record all)
    private int frameCount = 0;  // Counter to track frames for skipping
    private int totalFramesPossible = 0; // Counter for all potential frames
    
    public QuadTree(BufferedImage image, double threshold, int minBlockSize, ErrorCalc errorCalculator) {
        this(image, threshold, minBlockSize, errorCalculator, true, 100, 1);
    }
    
    public QuadTree(BufferedImage image, double threshold, int minBlockSize, ErrorCalc errorCalculator,
                   boolean recordGif, int maxFrames, int frameSkip) {
        this.image = image;
        this.threshold = threshold;
        this.minBlockSize = minBlockSize;
        this.errorCalculator = errorCalculator;
        this.gifFrames = new ArrayList<>();
        this.recordGif = recordGif;
        this.maxFrames = maxFrames;
        this.frameSkip = frameSkip;
        
        // Disable GIF recording for large images automatically to prevent memory issues
        if (recordGif && (image.getWidth() * image.getHeight() > 2000000)) { // > ~2 megapixels
            System.out.println("Peringatan: Gambar besar terdeteksi. Perekaman GIF dibatasi hingga " + maxFrames + " frame.");
            this.frameSkip = Math.max(2, frameSkip); // Skip more frames for large images
        }
    }

    public QuadTreeNode getRoot() {
        return root;
    }

    public List<BufferedImage> getGifFrames() {
        return gifFrames;
    }

    /** 
     * Membangun Quadtree; panggil ini di Main.
     */
    public void buildTree() {
        root = buildTreeRecursive(0, 0, image.getWidth(), image.getHeight(), 0);
        // Rekam frame final jika diperlukan
        recordFrame();
    }

    /**
     * Fungsi rekursif untuk membangun Quadtree.
     * Jika error > threshold dan blok masih lebih besar dari minBlockSize, blok di-split.
     */
    private QuadTreeNode buildTreeRecursive(int x, int y, int width, int height, int depth) {
        // Pastikan tidak keluar dari batas
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x + width > image.getWidth()) {
            width = image.getWidth() - x;
        }
        if (y + height > image.getHeight()) {
            height = image.getHeight() - y;
        }
        if (width <= 0 || height <= 0) {
            return null;
        }

        // Hitung rata-rata warna
        Color avgColor = computeAverageColor(x, y, width, height);
        // Hitung error
        double error = errorCalculator.computeError(image, x, y, width, height, avgColor);

        boolean isBlockTooSmall = (width <= minBlockSize || height <= minBlockSize);
        boolean errorAcceptable = (error <= threshold);

        // Jika error sudah OK atau blok sudah kecil, jadikan daun
        if (isBlockTooSmall || errorAcceptable) {
            // Rekam frame ketika kita memutuskan daun
            recordFrame();
            return new QuadTreeNode(x, y, width, height, avgColor, error);
        }

        // Kalau masih butuh di-split
        QuadTreeNode node = new QuadTreeNode(x, y, width, height, avgColor, error);
        node.children = new QuadTreeNode[4];

        // Rekam frame sebelum pembagian
        recordFrame();

        int halfWidth = width / 2;
        int halfHeight = height / 2;
        int width2 = width - halfWidth;     // Menangani ganjil
        int height2 = height - halfHeight;  // Menangani ganjil

        // Split 4 sub-blok
        node.children[0] = buildTreeRecursive(x,          y,          halfWidth,  halfHeight, depth+1);
        recordFrame();
        node.children[1] = buildTreeRecursive(x + halfWidth, y,       width2,     halfHeight, depth+1);
        recordFrame();
        node.children[2] = buildTreeRecursive(x,          y + halfHeight, halfWidth,  height2, depth+1);
        recordFrame();
        node.children[3] = buildTreeRecursive(x + halfWidth, y + halfHeight, width2, height2, depth+1);
        recordFrame();

        return node;
    }

    /**
     * Merekam satu frame ke gifFrames, menampilkan "batas" di sekitar blok daun
     * agar terlihat proses splitting.
     * Dengan parameter depth untuk memungkinkan rekaman yang lebih selektif.
     */
    private void recordFrame() {
        // Skip jika GIF recording dimatikan
        if (!recordGif) return;
        
        // Hitung total kemungkinan frame
        totalFramesPossible++;
        
        // Skip berdasarkan frameSkip
        frameCount++;
        if ((frameCount % frameSkip) != 0) return;
        
        // Stop jika sudah mencapai batas frame
        if (gifFrames.size() >= maxFrames) return;
        
        // Untuk mendapatkan distribusi frame yang lebih merata, kita bisa coba
        // skema sampling berdasarkan prediksi total frame
        if (totalFramesPossible > maxFrames * 4 && gifFrames.size() > 10) {
            // Semakin banyak frame yang dihasilkan, semakin selektif kita merekam
            if (Math.random() > 0.2) return; // 80% chance to skip additional frames
        }
        
        BufferedImage frame = renderTreeState();
        gifFrames.add(frame);
    }

    /** 
     * Hitung rata-rata warna pada blok.
     */
    private Color computeAverageColor(int x, int y, int width, int height) {
        long sumR = 0, sumG = 0, sumB = 0;
        int count = 0;

        int endX = x + width;
        int endY = y + height;
        if (endX > image.getWidth()) endX = image.getWidth();
        if (endY > image.getHeight()) endY = image.getHeight();

        for (int j = y; j < endY; j++) {
            for (int i = x; i < endX; i++) {
                Color c = new Color(image.getRGB(i, j));
                sumR += c.getRed();
                sumG += c.getGreen();
                sumB += c.getBlue();
                count++;
            }
        }
        if (count == 0) return Color.BLACK;

        int avgR = (int)(sumR / count);
        int avgG = (int)(sumG / count);
        int avgB = (int)(sumB / count);
        return new Color(avgR, avgG, avgB);
    }

    /**
     * Membuat gambar snapshot yang menampilkan boundary node daun
     * di atas background gambar aslinya.
     */
    public BufferedImage renderTreeState() {
        // Jika gambarnya sangat besar, buat versi kecilnya untuk GIF
        BufferedImage frame;
        int maxDimension = 800; // Batasi ukuran frame
        
        if (image.getWidth() > maxDimension || image.getHeight() > maxDimension) {
            // Scale down untuk frame GIF
            double scale = (double) maxDimension / Math.max(image.getWidth(), image.getHeight());
            int newWidth = (int) (image.getWidth() * scale);
            int newHeight = (int) (image.getHeight() * scale);
            
            frame = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics g = frame.getGraphics();
            g.drawImage(image, 0, 0, newWidth, newHeight, null);
            
            // Skala juga boundary
            drawScaledBoundaries(g, root, scale);
            g.dispose();
        } else {
            // Untuk gambar yang cukup kecil, gunakan ukuran asli
            frame = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics g = frame.getGraphics();
            g.drawImage(image, 0, 0, null);
            drawBoundaries(g, root);
            g.dispose();
        }
        
        return frame;
    }
    
    /**
     * Menggambar kotak merah di setiap node daun dengan skala.
     */
    private void drawScaledBoundaries(Graphics g, QuadTreeNode node, double scale) {
        if (node == null) return;
        if (node.isLeaf()) {
            g.setColor(Color.RED);
            int x = (int)(node.x * scale);
            int y = (int)(node.y * scale);
            int width = (int)(node.width * scale);
            int height = (int)(node.height * scale);
            g.drawRect(x, y, width, height);
        } else {
            for (QuadTreeNode child : node.children) {
                drawScaledBoundaries(g, child, scale);
            }
        }
    }

    /**
     * Menggambar kotak merah di setiap node daun.
     */
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
     * Menghasilkan gambar akhir hasil kompresi,
     * setiap node daun diisi warna rata-ratanya.
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
        int sum = 0;
        for (QuadTreeNode child : node.children) {
            sum += countLeavesRecursive(child);
        }
        return sum;
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
