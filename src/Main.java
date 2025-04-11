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
        try (Scanner sc = new Scanner(System.in)) {
            CompressConfig config = new CompressConfig();
            // Input path gambar
            System.out.print("Masukkan lokasi absolut gambar input: ");
            config.inputImagePath = sc.nextLine();
            // Input metode perhitungan error
            System.out.println("Pilih metode perhitungan error:");
            System.out.println("1. Variance");
            System.out.println("2. Mean Absolute Deviation");
            System.out.println("3. Max Pixel Difference");
            System.out.println("4. Entropy");
            System.out.println("5. Structural Similarity Index (SSIM)");
            System.out.print("Masukkan pilihan (angka): ");
            config.errorMethod = sc.nextInt();
            // Input threshold awal
            System.out.print("Masukkan nilai threshold awal: ");
            config.threshold = sc.nextDouble();
            // Input minBlockSize
            System.out.print("Masukkan ukuran blok minimum: ");
            config.minBlockSize = sc.nextInt();
            // Input target compression
            System.out.print("Masukkan target persentase kompresi (floating number, 1,0 = 100%, 0 utk nonaktif): ");
            config.targetCompression = sc.nextDouble();
            sc.nextLine(); // Buang newline
            // Output path
            System.out.print("Masukkan lokasi absolut untuk gambar keluaran: ");
            config.outputImagePath = sc.nextLine();
            
            // GIF options
            System.out.print("Buat GIF visualisasi? (y/n): ");
            config.createGif = sc.nextLine().trim().equalsIgnoreCase("y");
            
            if (config.createGif) {
                // GIF path
                System.out.print("Masukkan lokasi absolut untuk GIF visualisasi: ");
                config.gifOutputPath = sc.nextLine();
                // Delay frame GIF
                System.out.print("Masukkan delay antar frame (ms): ");
                config.gifDelay = sc.nextInt();
                // Max frames
                System.out.print("Maksimum jumlah frame (batasi untuk menghindari OutOfMemory, rekomendasi 50-200): ");
                config.maxGifFrames = sc.nextInt();
                // Frame skip
                System.out.print("Rekam setiap berapa frame (1=semua, 2=setiap 2 frame, dll): ");
                config.gifFrameSkip = sc.nextInt();
                sc.nextLine(); // Buang newline
            }

            // Baca inputImage
            BufferedImage inputImage;
            try {
                File f = new File(config.inputImagePath);
                if (!f.exists()) {
                    System.err.println("File tidak ditemukan: " + config.inputImagePath);
                    return;
                }
                inputImage = ImageIO.read(f);
                if (inputImage == null) {
                    System.err.println("Format gambar tidak didukung!");
                    return;
                }
                
                // Berikan peringatan untuk gambar besar
                int megapixels = (inputImage.getWidth() * inputImage.getHeight()) / 1000000;
                if (megapixels > 2) {
                    System.out.println("PERINGATAN: Gambar berukuran besar (" + megapixels + " megapixel).");
                    if (config.createGif) {
                        System.out.println("Pembuatan GIF mungkin akan memakan banyak memori. Menyesuaikan parameter...");
                        config.maxGifFrames = Math.min(config.maxGifFrames, 50);
                        config.gifFrameSkip = Math.max(config.gifFrameSkip, 2);
                        System.out.println("Disesuaikan: maxFrames=" + config.maxGifFrames + ", frameSkip=" + config.gifFrameSkip);
                    }
                }
                
            } catch (IOException e) {
                System.err.println("Gagal membaca gambar: " + e.getMessage());
                return;
            }
            // Tentukan ErrorCalc
            ErrorCalc errorCalc;
            switch(config.errorMethod) {
                case 1 -> errorCalc = new VarErrorCalc();
                case 2 -> errorCalc = new MADErrorCalc();
                case 3 -> errorCalc = new PixelDiffErrorCalc();
                case 4 -> errorCalc = new EntropyErrorCalc();
                case 5 -> errorCalc = new SSIMErrorCalc();
                default -> {
                    System.err.println("Metode error tidak diimplementasikan, default ke Variance!");
                    errorCalc = new VarErrorCalc();
                }
            }
            // Mulai timer
            long startTime = System.nanoTime();
            // Jika targetCompression > 0, tuning threshold
            if (config.targetCompression > 0) {
                double desiredRatio = config.targetCompression; // 0..1
                double low = 0.0;
                double high = (config.errorMethod == 1) ? 65025 : 1.0;
                // (Atur rentang high sesuai metode, misal SSIM=1.0, Var=65025, dsb.)
                
                double bestThreshold = config.threshold;
                double tolerance = 0.01;
                int maxIter = 20;
                for (int i = 0; i < maxIter; i++) {
                    double mid = (low + high) / 2.0;
                    // Bangun Quadtree sementara - tanpa GIF recording untuk tuning
                    QuadTree qt = new QuadTree(inputImage, mid, config.minBlockSize, errorCalc, false, 0, 1);
                    qt.buildTree();
                    int leaves = qt.countLeaves();
                    int totalPixels = inputImage.getWidth() * inputImage.getHeight();
                    double currentCompressionRatio = 1.0 - ( (double) leaves / totalPixels );
                    
                    if (Math.abs(currentCompressionRatio - desiredRatio) <= tolerance) {
                        bestThreshold = mid;
                        break;
                    }
                    if (currentCompressionRatio < desiredRatio) {
                        // kompresinya masih kurang (terlalu banyak leaf) => turunkan threshold
                        high = mid;
                    } else {
                        // kompresinya kebanyakan => naikin threshold
                        low = mid;
                    }
                    bestThreshold = mid;
                }
                config.threshold = bestThreshold;
                System.out.println("Optimal threshold ditemukan: " + config.threshold);
            }
            
            // Bangun Quadtree final dengan pengaturan GIF yang sesuai
            QuadTree quadtree = new QuadTree(
                inputImage, 
                config.threshold, 
                config.minBlockSize, 
                errorCalc,
                config.createGif,
                config.maxGifFrames,
                config.gifFrameSkip
            );
            
            System.out.println("Membangun quadtree...");
            try {
                quadtree.buildTree();
            } catch (OutOfMemoryError e) {
                System.err.println("ERROR: Out of memory saat memproses gambar!");
                System.err.println("Coba lagi dengan parameter berikut:");
                System.err.println("- Nonaktifkan pembuatan GIF");
                System.err.println("- Kurangi ukuran gambar input");
                System.err.println("- Tingkatkan nilai threshold");
                System.err.println("- Tingkatkan ukuran blok minimum");
                return;
            }
            
            // Hasil kompresi
            System.out.println("Membuat gambar hasil kompresi...");
            BufferedImage outputImage = quadtree.generateCompressedImage();
            long endTime = System.nanoTime();
            double elapsedSec = (endTime - startTime) / 1e9;
            
            // Simpan
            try {
                File outputFile = new File(config.outputImagePath);
                // Buat direktori jika belum ada
                File parentDir = outputFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                
                // Pilih format output berdasarkan ekstensi file
                String extension = config.outputImagePath.substring(config.outputImagePath.lastIndexOf('.') + 1);
                if (extension.isEmpty()) extension = "jpg";
                
                ImageIO.write(outputImage, extension, outputFile);
                System.out.println("Gambar hasil kompresi berhasil disimpan di: " + config.outputImagePath);
            } catch(IOException e) {
                System.err.println("Gagal menulis gambar: " + e.getMessage());
            }
            
            // Hitung rasio kompresi
            File inFile = new File(config.inputImagePath);
            File outFile = new File(config.outputImagePath);
            long sizeBefore = inFile.length();
            long sizeAfter = outFile.length();
            double percent = (1.0 - ((double)sizeAfter / sizeBefore)) * 100.0;
            int depth = quadtree.getTreeDepth();
            int leaves = quadtree.countLeaves();
            
            System.out.println("\n===== Hasil Kompresi =====");
            System.out.printf("Waktu eksekusi       : %.3f detik%n", elapsedSec);
            System.out.printf("Ukuran gambar sebelum: %d bytes%n", sizeBefore);
            System.out.printf("Ukuran gambar sesudah: %d bytes%n", sizeAfter);
            System.out.printf("Persentase kompresi  : %.2f%%%n", percent);
            System.out.println("Kedalaman pohon      : " + depth);
            System.out.println("Banyak simpul (leaf) : " + leaves);
            
            // Buat GIF animasi jika diaktifkan
            if (config.createGif) {
                List<BufferedImage> frames = quadtree.getGifFrames();
                System.out.println("Terekam " + frames.size() + " frame untuk animasi GIF.");
                
                if (!frames.isEmpty()) {
                    try {
                        File gifFile = new File(config.gifOutputPath);
                        File gifParentDir = gifFile.getParentFile();
                        if (gifParentDir != null && !gifParentDir.exists()) gifParentDir.mkdirs();
                        
                        try (ImageOutputStream ios = new FileImageOutputStream(gifFile)) {
                            GIFSeqWriter gifWriter = new GIFSeqWriter(ios, BufferedImage.TYPE_INT_RGB, config.gifDelay, true);
                            for (BufferedImage frame : frames) {
                                gifWriter.writeToSequence(frame);
                            }
                            gifWriter.close();
                        }
                        System.out.println("GIF proses kompresi berhasil disimpan di: " + config.gifOutputPath);
                    } catch (IOException e) {
                        System.err.println("Gagal menulis GIF: " + e.getMessage());
                    }
                } else {
                    System.out.println("Tidak ada frame GIF yang direkam.");
                }
            }
        }
    }
}
