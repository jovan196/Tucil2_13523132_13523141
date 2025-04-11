import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        CompressConfig config = new CompressConfig();
        
        // [INPUT] Lokasi gambar yang akan dikompresi
        System.out.print("Masukkan lokasi absolut gambar input: ");
        config.inputImagePath = scanner.nextLine();
        
        // [INPUT] Pilih metode perhitungan error
        System.out.println("Pilih metode perhitungan error:");
        System.out.println("1. Variance");
        System.out.println("2. Mean Absolute Deviation");
        System.out.println("3. Max Pixel Difference");
        System.out.println("4. Entropy");
        System.out.println("5. Structural Similarity Index (SSIM)");
        System.out.print("Masukkan pilihan (angka): ");
        config.errorMethod = scanner.nextInt();
        
        // [INPUT] Nilai threshold awal
        System.out.print("Masukkan nilai threshold awal: ");
        config.threshold = scanner.nextDouble();
        
        // [INPUT] Ukuran blok minimum
        System.out.print("Masukkan ukuran blok minimum: ");
        config.minBlockSize = scanner.nextInt();
        
        // [INPUT] Target persentase kompresi (0 = nonaktif)
        System.out.print("Masukkan target persentase kompresi (floating number, 1.0 = 100%, 0 untuk nonaktif): ");
        config.targetCompression = scanner.nextDouble();
        scanner.nextLine(); // bersihkan newline
        
        // [INPUT] Lokasi file output gambar
        System.out.print("Masukkan lokasi absolut untuk gambar keluaran: ");
        config.outputImagePath = scanner.nextLine();
        
        // [INPUT] Lokasi file output GIF (bonus)
        System.out.print("Masukkan lokasi absolut untuk GIF visualisasi: ");
        config.gifOutputPath = scanner.nextLine();
        
        // Set delay frame GIF (mis. 500 ms)
        config.gifDelay = 500;
        
        // Baca gambar input
        BufferedImage inputImage = null;
        try {
            File inputFile = new File(config.inputImagePath);
            if (!inputFile.exists()) {
                System.err.println("File input tidak ditemukan: " + config.inputImagePath);
                System.exit(1);
            }
            inputImage = ImageIO.read(inputFile);
            if (inputImage == null) {
                System.err.println("Format gambar tidak didukung: " + config.inputImagePath);
                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println("Gagal membaca gambar input: " + e.getMessage());
            System.exit(1);
        }
        
        long startTime = System.nanoTime();
        
        // Pilih kalkulator error sesuai pilihan
        ErrorCalc errorCalc;
        switch (config.errorMethod) {
            case 1 -> errorCalc = new VarErrorCalc();
            case 2 -> errorCalc = new MADErrorCalc();
            case 3 -> errorCalc = new PixelDiffErrorCalc();
            case 4 -> errorCalc = new EntropyErrorCalc();
            case 5 -> errorCalc = new SSIMErrorCalc();
            default -> {
                System.err.println("Metode perhitungan error tidak valid.");
                scanner.close();
                System.exit(1);
                return; // Tambahkan return untuk memastikan kode berhenti
            }
        }
        
        // Jika mode target kompresi aktif, lakukan pencarian threshold optimal secara biner
        if (config.targetCompression > 0) {
            double desiredCompression = config.targetCompression; // nilai antara 0 dan 1
            double low = 0;
            double high = (config.errorMethod == 5) ? 1.0 : 65025; // perkiraan rentang
            double optimalThreshold = config.threshold;
            double tolerance = 0.01;
            int maxIterations = 20;
            for (int iter = 0; iter < maxIterations; iter++) {
                config.threshold = (low + high) / 2;
                // Bangun QuadTree dengan threshold saat ini
                QuadTree qt = new QuadTree(inputImage, config.threshold, config.minBlockSize, errorCalc);
                qt.buildTree();
                int leaves = qt.countLeaves();
                int totalPixels = inputImage.getWidth() * inputImage.getHeight();
                double currentCompression = 1.0 - ((double) leaves / totalPixels);
                if (Math.abs(currentCompression - desiredCompression) < tolerance) {
                    optimalThreshold = config.threshold;
                    break;
                }
                if (currentCompression < desiredCompression) {
                    // Jika kompresi belum mencapai target (daun terlalu banyak), tingkatkan kompresi dengan menurunkan threshold
                    high = config.threshold;
                } else {
                    // Jika terlalu banyak kompresi (daun terlalu sedikit), turunkan kompresi dengan menaikkan threshold
                    low = config.threshold;
                }
                optimalThreshold = config.threshold;
            }
            config.threshold = optimalThreshold;
            System.out.println("Optimal threshold ditemukan: " + config.threshold);
        }
        
        // Bangun QuadTree akhir dengan threshold final
        QuadTree QuadTree = new QuadTree(inputImage, config.threshold, config.minBlockSize, errorCalc);
        QuadTree.buildTree();
        
        // Hasil rekonstruksi gambar terkompresi
        BufferedImage outputImage = QuadTree.generateCompressedImage();
        
        long endTime = System.nanoTime();
        double elapsedTimeSec = (endTime - startTime) / 1_000_000_000.0;
        
        // Simpan gambar keluaran
        try {
            ImageIO.write(outputImage, "jpg", new File(config.outputImagePath));
        } catch (IOException e) {
            System.err.println("Gagal menulis gambar output: " + e.getMessage());
            System.exit(1);
        }
        
        // Hitung ukuran file sebelum dan sesudah
        long sizeBefore = new File(config.inputImagePath).length();
        long sizeAfter = new File(config.outputImagePath).length();
        double compressionPercentage = (1 - ((double) sizeAfter / sizeBefore)) * 100;
        
        int treeDepth = QuadTree.getTreeDepth();
        int totalLeaves = QuadTree.countLeaves();
        
        System.out.println("\n===== Hasil Kompresi =====");
        System.out.printf("Waktu eksekusi       : %.3f detik%n", elapsedTimeSec);
        System.out.printf("Ukuran gambar sebelum : %d bytes%n", sizeBefore);
        System.out.printf("Ukuran gambar sesudah : %d bytes%n", sizeAfter);
        System.out.printf("Persentase kompresi   : %.2f%%%n", compressionPercentage);
        System.out.println("Kedalaman pohon       : " + treeDepth);
        System.out.println("Banyak simpul (leaf)  : " + totalLeaves);
        
        // Buat GIF visualisasi proses kompresi (jika ada frame yang terekam)
        List<BufferedImage> frames = QuadTree.getGifFrames();
        if (!frames.isEmpty()) {
            try {
                File gifFile = new File(config.gifOutputPath);
                // Create parent directories if they don't exist
                File parentDir = gifFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                
                ImageOutputStream output = new FileImageOutputStream(gifFile);
                GIFSeqWriter gifWriter = new GIFSeqWriter(output, inputImage.getType(), config.gifDelay, true);
                for (BufferedImage frame : frames) {
                    gifWriter.writeToSequence(frame);
                }
                gifWriter.close();
                output.close();
                System.out.println("GIF proses kompresi berhasil disimpan di: " + config.gifOutputPath);
            } catch (IOException e) {
                System.err.println("Gagal membuat GIF: " + e.getMessage());
            }
        } else {
            System.out.println("Tidak ada frame GIF yang direkam.");
        }
        
        scanner.close();
    }
}
