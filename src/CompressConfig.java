public class CompressConfig {
    // Input/output paths
    public String inputImagePath = "";
    public String outputImagePath = "";
    public String gifOutputPath = "";
    
    // Compression parameters
    public int errorMethod = 1;
    public double threshold = 10.0;
    public int minBlockSize = 4;
    public double targetCompression = 0.0;
    
    // GIF creation parameters
    public boolean createGif = true;
    public int gifDelay = 500;  // delay in ms
    public int maxGifFrames = 100;
    public int gifFrameSkip = 1;
}
