import java.awt.Color;

public class QuadTreeNode {
    public int x, y, width, height;
    public Color averageColor;
    public double error;
    public QuadTreeNode[] children; // Jika null = daun

    public QuadTreeNode(int x, int y, int width, int height, Color averageColor, double error) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.averageColor = averageColor;
        this.error = error;
        this.children = null;
    }

    public boolean isLeaf() {
        return children == null;
    }
}
