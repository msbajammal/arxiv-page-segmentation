package com.github.msbajammal.segmentation;

import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.core.Mat;
import org.opencv.core.Core;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class Util {
    public static Mat readImageBuffer(byte[] buffer) {
        return Imgcodecs.imdecode(new MatOfByte(buffer), Imgcodecs.IMREAD_UNCHANGED);
    }

    public static String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded);
    }

    public static String readFile(File file) throws IOException {
        byte[] encoded = Files.readAllBytes(file.toPath());
        return new String(encoded);
    }

    public static BufferedImage convertMatToBufferedImage(Mat matrix) {
        int type;
        switch (matrix.channels()) {
            case 3:
                type = BufferedImage.TYPE_3BYTE_BGR;
                break;

            case 4:
                List<Mat> src = new ArrayList<>();
                List<Mat> dest = new ArrayList<>();
                src.add(matrix);
                dest.add(matrix.clone());
                MatOfInt mix = new MatOfInt();
                int[] order = {0,1 , 1,2 , 2,3 , 3,0};
                mix.fromArray(order);
                Core.mixChannels(src, dest, mix);
                matrix = dest.get(0);
                type = BufferedImage.TYPE_4BYTE_ABGR;
                break;

            default:
                type = BufferedImage.TYPE_BYTE_GRAY;
        }
        int bufferSize = matrix.channels()*matrix.cols()*matrix.rows();
        byte[] b = new byte[bufferSize];
        matrix.get(0, 0, b);
        BufferedImage image = new BufferedImage(matrix.cols(), matrix.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;
    }

    public static void displayMat(Mat matrix) {
        BufferedImage image = convertMatToBufferedImage(matrix);
        ImageIcon icon = new ImageIcon(image);
        JLabel lbl = new JLabel();
        lbl.setIcon(icon);

        JPanel container = new JPanel();
        container.add(lbl);
        JScrollPane scrollPane = new JScrollPane(container);
        scrollPane.getVerticalScrollBar().setUnitIncrement(150);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(150);

        JFrame frame = new JFrame();
        frame.setSize(image.getWidth(null)+50, image.getHeight(null)+50);
        frame.add(scrollPane);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
