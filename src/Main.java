import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import javax.imageio.ImageIO;
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
        System.out.println("2. Mean Absolute Deviation (tidak diimplementasikan, gunakan Variance)");
        System.out.println("3. Max Pixel Difference (tidak diimplementasikan, gunakan Variance)");
        System.out.println("4. Entropy (tidak diimplementasikan, gunakan Variance)");
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
            inputImage = ImageIO.read(new File(config.inputImagePath));
        } catch (IOException e) {
            System.err.println("Gagal membaca gambar input: " + e.getMessage());
            System.exit(1);
        }
        
        long startTime = System.nanoTime();
        
        // Pilih kalkulator error sesuai pilihan
        ErrorCalc errorCalc;
        if (config.errorMethod == 5) {
            errorCalc = new SSIMErrorCalc();
        } else {
            errorCalc = new VarErrorCalc();
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
            ImageIO.write(outputImage, "png", new File(config.outputImagePath));
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
                ImageOutputStream output = ImageIO.createImageOutputStream(new File(config.gifOutputPath));
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
