package com.github.msbajammal.segmentation.roi;

import com.github.msbajammal.segmentation.Browser;
import com.github.msbajammal.segmentation.Util;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ROIExtractor {

    public static List<ROI> generateROIs(Browser browser) throws IOException {
        _loadJavaScriptInstrumentation(browser);
        List<ROI> ROIs = new ArrayList<>();

        Map<String, Object> layout = browser.send("Page.getLayoutMetrics");
        int fullWidth = ((Long)((Map<String, Object>)layout.get("contentSize")).get("width")).intValue();
        int fullHeight = ((Long)((Map<String, Object>)layout.get("contentSize")).get("height")).intValue();
        int windowWidth = ((Long)((Map<String, Object>)layout.get("visualViewport")).get("clientWidth")).intValue();
        int windowHeight = ((Long)((Map<String, Object>)layout.get("visualViewport")).get("clientHeight")).intValue();
//
//        browser.send("Emulation.setDeviceMetricsOverride",
//                ImmutableMap.of("width", fullWidth, "height", fullHeight,
//                        "deviceScaleFactor", Long.valueOf(1), "mobile", Boolean.FALSE,
//                        "fitWindow", Boolean.FALSE)
//        );
//        browser.send("Emulation.setVisibleSize", ImmutableMap.of("width", fullWidth,
//                "height", fullHeight));

        List<Map<String, Object>> mapROIs = (List<Map<String, Object>>)(browser.executeScript("return window.__CORTEXJS.getROIs();"));
        mapROIs.forEach(ROI -> {
            String content = ROI.containsKey("content") ? (String)(ROI.get("content")) : "";
            int fontSize = ((Long)(ROI.get("fontSize"))).intValue();
            List<Long> rawForegroundColor = (List<Long>)(ROI.get("foregroundColor"));
            List<Long> rawBackgroundColor = (List<Long>)(ROI.get("backgroundColor"));
            int[] foregroundColor = {rawForegroundColor.get(0).intValue(),
                    rawForegroundColor.get(1).intValue(),
                    rawForegroundColor.get(2).intValue(),
                    rawForegroundColor.get(3).intValue()};
            int[] backgroundColor = {rawBackgroundColor.get(0).intValue(),
                    rawBackgroundColor.get(1).intValue(),
                    rawBackgroundColor.get(2).intValue(),
                    rawBackgroundColor.get(3).intValue()};

            ROI ROIObject = new ROI(((Long)(ROI.get("x1"))).intValue(), ((Long)(ROI.get("y1"))).intValue(),
                    ((Long)(ROI.get("x2"))).intValue(), ((Long)(ROI.get("y2"))).intValue(),
                    ((Long)(ROI.get("width"))).intValue(), ((Long)(ROI.get("height"))).intValue(),
                    (String)(ROI.get("type")), (String)(ROI.get("xpath")), content,
                    fontSize, foregroundColor, backgroundColor, fullWidth, fullHeight);
            ROIs.add(ROIObject);
        });

//        browser.send("Emulation.setVisibleSize", ImmutableMap.of(
//                "x", Long.valueOf(0), "y", Long.valueOf(0),
//                "width", windowWidth, "height", windowHeight));
//
//        browser.send("Emulation.setDeviceMetricsOverride",
//                ImmutableMap.of("width", 0, "height", 0,
//                        "deviceScaleFactor", 0, "mobile", Boolean.FALSE,
//                        "fitWindow", Boolean.FALSE)
//        );

        return ROIs;
    }

    public static void _loadJavaScriptInstrumentation(Browser browser) throws IOException {
        File instrumentFile = new File(ClassLoader.getSystemClassLoader().getResource("instrumentation.js").getFile());
        String script = Util.readFile(instrumentFile);
        browser.executeScript(script);
        if ((Boolean)(browser.executeScript("return window.__CORTEXJS == undefined"))) {
            throw new RuntimeException("Unable to load instrumentation");
        }
        System.out.println("JavaScript instrumentation loaded successfully.");
    }

//    public static Mat getROIImage(Browser browser) throws IOException {
//        Map<String, Object> layout = browser.send("Page.getLayoutMetrics");
//        int fullWidth = ((Long)((Map<String, Object>)layout.get("contentSize")).get("width")).intValue();
//        int fullHeight = ((Long)((Map<String, Object>)layout.get("contentSize")).get("height")).intValue();
//        List<ROI> ROIs = getROIs(browser);
//        Mat ROIImage = new Mat(fullHeight, fullWidth, CvType.CV_8UC3);
//        Scalar color;
//        for (ROI R : ROIs) {
//            color = R.type.equals("text") ? new Scalar(0,255,0) : new Scalar(255,0,0);
//            Imgproc.rectangle(ROIImage, new Point(R.x1, R.y1), new Point(R.x2, R.y2), color, -1);
//        }
//        return ROIImage;
//    }
//
//    private static List<ROI> _getNestedROIs(Browser browser, WebElement iframe, List<ROI> ROIs) throws IOException {
//        String rootXPath;
//        if (iframe == null) {
//            rootXPath = "";
//            ROIs = new ArrayList<>();
//            browser.switchTo().defaultContent();
//        } else {
//            rootXPath = iframe.getAttribute("xpath");
//            ROIs = ROIs;
//            browser.switchTo().frame(iframe);
//        }
//
//        _loadCortexJS(browser);
//        ROIs.addAll(_getROIsPerFrame(browser, rootXPath));
//        List<WebElement> iframes = (List<WebElement>) (browser.executeScript("return window.__CORTEXJS._findIFrames();"));
//
//        for (WebElement frame : iframes) {
//            ROIs.addAll(_getNestedROIs(browser, frame, ROIs));
//        }
//
//        return ROIs;
//    }
//
//    public static List<ROI> getROIs_V2(Browser browser) throws IOException {
//        return _getNestedROIs(browser, null, null);
//    }
//
//    public static List<ROI> getROIs(Browser browser) throws IOException {
//        _loadCortexJS(browser);
//        String rootXPath = "";
//        List<ROI> ROIs = new ArrayList<>();
//        ROIs.addAll(_getROIsPerFrame(browser, rootXPath));
//
//        List<WebElement> iframes = (List<WebElement>) (browser.executeScript("return window.__CORTEXJS._findIFrames();"));
//        for (WebElement iframe : iframes) {
//            rootXPath = iframe.getAttribute("xpath");
//            browser.switchTo().frame(iframe);
//            ROIs.addAll(_getROIsPerFrame(browser, rootXPath));
//            browser.switchTo().defaultContent();
//        }
//
//        return ROIs;
//    }
//
//    private static List<ROI> _getROIsPerFrame(Browser browser, String rootXPath) throws IOException {
//        Map<String, Object> result = browser.evaluate("window.__CORTEXJS.getROIs();");
//        ArrayList<Map<String,Object>> mapROIs = (ArrayList<Map<String,Object>>)(((Map<String,Object>)(result.get("result"))).get("value"));
//        ArrayList<ROI> ROIs = new ArrayList<ROI>();
//        mapROIs.forEach(ROI -> {
//            String content = ROI.containsKey("content") ? (String)(ROI.get("content")) : "";
//            ROI ROIObject = new ROI(((Long)(ROI.get("x1"))).intValue(), ((Long)(ROI.get("y1"))).intValue(),
//                                    ((Long)(ROI.get("x2"))).intValue(), ((Long)(ROI.get("y2"))).intValue(),
//                                    ((Long)(ROI.get("width"))).intValue(), ((Long)(ROI.get("height"))).intValue(),
//                                    (String)(ROI.get("type")), rootXPath+(String)(ROI.get("xpath")), content);
//            ROIs.add(ROIObject);
//        });
//        return ROIs;
//    }

    public static Mat renderROIs(List<ROI> ROIs) throws IOException {
        Mat ROIImage = new Mat(ROIs.get(0).documentHeight, ROIs.get(0).documentWidth, CvType.CV_8UC3);
        Scalar color;
        Scalar border = new Scalar(0,0,0);

        for (ROI R : ROIs) {
            switch (R.type) {
                case "text":
                    color = new Scalar(0,255,0); // BGR
                    break;
                case "image":
                    color = new Scalar(255,0,0);
                    break;
                case "input":
                    color = new Scalar(0,0,255);
                    break;
                default:
                    color = new Scalar(70,70,70);
            }
            Imgproc.rectangle(ROIImage, new Point(R.x1, R.y1), new Point(R.x2, R.y2), color, -1);
            Imgproc.rectangle(ROIImage, new Point(R.x1, R.y1), new Point(R.x2, R.y2), border, 1);
        }

        return ROIImage;
    }

//    public static List<ROI> getFrameROIs(Browser browser, String xPathPrefix) throws IOException {
//        _loadCortexJS(browser);
//        List<Map<String, Object>> mapROIs = (List<Map<String, Object>>)(browser.executeScript("return window.__CORTEXJS.getROIs();"));
//        List<ROI> ROIs = new ArrayList<>();
//        mapROIs.forEach(ROI -> {
//            String content = ROI.containsKey("content") ? (String)(ROI.get("content")) : "";
//            ROI ROIObject = new ROI(((Long)(ROI.get("x1"))).intValue(), ((Long)(ROI.get("y1"))).intValue(),
//                    ((Long)(ROI.get("x2"))).intValue(), ((Long)(ROI.get("y2"))).intValue(),
//                    ((Long)(ROI.get("width"))).intValue(), ((Long)(ROI.get("height"))).intValue(),
//                    (String)(ROI.get("type")), xPathPrefix+(String)(ROI.get("xpath")), content);
//            ROIs.add(ROIObject);
//        });
//        return ROIs;
//    }
//
//    public static List<ROI> getAllROIs(Browser browser) throws IOException {
//        List<ROI> all = new ArrayList<>();
//
//        class Helper {
//            int removedIFramesCounter = 0;
//            public void getDescendants(String xPathRel, String xPathAbs) throws IOException {
//            if (xPathAbs.equals("") && xPathRel.equals("")) {
////                browser.switchTo().defaultContent();
////                WebElement parentFrame = browser.findElementByXPath("/html[1]/body[1]");
////                browser.switchTo().frame(parentFrame);
//            } else {
//                try {
//                    WebElement parentFrame = browser.findElementByXPath(xPathRel);
//                    browser.switchTo().frame(parentFrame);
//                } catch (Exception e) { // iframe was removed
//                    System.out.print("Couldn't find "+xPathRel);
//                    removedIFramesCounter++;
//                    System.out.println(".   "+removedIFramesCounter+" so far.");
//                    return;
//                }
//            }
//            all.addAll(getFrameROIs(browser, xPathAbs));
//
//            List<String> childIFramesXPaths = (List<String>) (browser.executeScript("return window.__CORTEXJS._findIFrames();"));
//            for (String xpath : childIFramesXPaths) {
//                getDescendants(xpath, xPathAbs+xpath);
//            }
//
//        }}
//
//        new Helper().getDescendants("", "");
//        return all;
//    }
}
