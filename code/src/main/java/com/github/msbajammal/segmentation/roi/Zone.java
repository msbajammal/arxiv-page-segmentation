package com.github.msbajammal.segmentation.roi;

public class Zone {
    public final int y1, y2;

    public Zone(int y1, int y2) {
        this.y1 = y1;
        this.y2 = y2;
    }

    @Override
    public String toString() {
        return String.format("[Zone: "+y1+" -> "+y2+"]");
    }
}
