package com.github.msbajammal.segmentation.roi;

public class ROI {
    public final int x1, y1, x2, y2, width, height, fontSize;
    public final String type, xpath, content;
    public final int[] foregroundColor, backgroundColor;
    public final int documentWidth, documentHeight;

    public final long area;
//    public double normalizedArea = 0.0; // computed on 2nd pass

    public ROI(int x1, int y1, int x2, int y2, int width, int height,
               String type, String xpath, String content,
               int fontSize, int[] foregroundColor, int[] backgroundColor,
               int documentWidth, int documentHeight) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.width = width;
        this.height = height;
        this.area = width*height;
        this.type = type;
        this.xpath = xpath;
        this.content = content;
        this.fontSize = fontSize;
        this.foregroundColor = foregroundColor;
        this.backgroundColor = backgroundColor;
        this.documentWidth = documentWidth;
        this.documentHeight = documentHeight;
    }

    @Override public String toString() {
        return type+"["+x1+" - "+x2+", "+y1+" - "+y2+"]";
    }
}
