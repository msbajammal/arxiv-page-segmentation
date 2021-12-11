package com.github.msbajammal.segmentation;


import org.bytedeco.opencv.opencv_java;
import org.bytedeco.javacpp.Loader;


public class App {
    public static void main( String[] args ) throws Exception {
        Loader.load(opencv_java.class);

        try {
            // Begin binary data transfer debugging
//            URL imgURL = new URL("https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png");
//            ByteArrayOutputStream output = new ByteArrayOutputStream();
//            try (InputStream inputStream = imgURL.openStream()) {
//                int n = 0;
//                byte [] buffer = new byte[ 1024 ];
//                while (-1 != (n = inputStream.read(buffer))) {
//                    output.write(buffer, 0, n);
//                }
//            }
            // End transfer debugging

            Browser chrome = new Browser();
            System.out.println("Loading page ...");
            chrome.goTo("https://www.google.ca/");
            System.out.println("Finished loading.");

            Segmenter.apply(chrome);

//            List<ROI> all = ROIExtractor.getAllROIs(chrome);
//            List<ROI> a = ROIExtractor.getROIs(chrome);
//            List<ROI> b = ROIExtractor.getROIs_V2(chrome);
//            ZoneDetector.getZones(ROIsScreenshot);
//            ZoneDetector.getZonesST(ROIsScreenshot);
//            ZoneDetector.getZones(ROIsScreenshot);
//            ZoneDetector.getZonesST(ROIsScreenshot);
//            ZoneDetector.getZones(ROIsScreenshot);
//            ZoneDetector.getZonesST(ROIsScreenshot);
//            ZoneDetector.getZones(ROIsScreenshot);
//            Zone[] zones = ZoneDetector.getZonesST(ROIsScreenshot);
//            zones = ZoneDetector.getZonesST(ROIsScreenshot);
//            zones = ZoneDetector.getZonesST(ROIsScreenshot);
//            zones = ZoneDetector.getZonesST(ROIsScreenshot);
        } catch (Exception e) {
            throw e;
        }
        System.exit(0);
    }
}
