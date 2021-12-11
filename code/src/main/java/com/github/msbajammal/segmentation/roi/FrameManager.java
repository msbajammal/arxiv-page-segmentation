package com.github.msbajammal.segmentation.roi;

import com.github.msbajammal.segmentation.Browser;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.List;

public class FrameManager {

//    public List<Frame> getAllDescendants(Frame frame) {
//        List<Frame> all = new ArrayList<>();
//
//        class Helper { public void getDescendants(Frame node) {
//            for (int i=0; i<node.children.size(); i++) {
//                Frame child = node.children.get(i);
//                getDescendants(child);
//                all.add(child);
//            }
//        }}
//
//        new Helper().getDescendants(frame);
//
//        return all;
//    }

    public static Frame getFrameTree(Browser browser) throws IOException {
        Frame root = new Frame("/html[1]/body[1]", null, null);

        class Helper { public Frame getFrameChildren(Frame parent) throws IOException  {
            WebElement parentFrame = browser.findElementByXPath(parent.xpath);
            browser.switchTo().frame(parentFrame);
            ROIExtractor._loadJavaScriptInstrumentation(browser);
            List<String> childIFramesXPaths = (List<String>) (browser.executeScript("return window.__CORTEXJS._findIFrames();"));

            for (String xpath : childIFramesXPaths) {
                Frame child = new Frame(xpath, parent, null);
                getFrameChildren(child);
                parent.addChild(child);
            }
            return parent;
        }}

        return new Helper().getFrameChildren(root);
    }
}