public class CompressConfig {
    public String inputImagePath;
    public String outputImagePath;
    public String gifOutputPath;
    public int errorMethod;
    public double threshold;
    public int minBlockSize;
    public double targetCompression; // 0 berarti nonaktif, kalo aktif: nilai antara 0 dan 1 (1.0 = 100%)
    public int gifDelay; // delay per frame GIF (ms)
}
