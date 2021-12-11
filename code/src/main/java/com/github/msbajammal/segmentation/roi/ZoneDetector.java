package com.github.msbajammal.segmentation.roi;


import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Core;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ZoneDetector {
    public static Zone[] getZones(Mat ROIImage) throws InterruptedException {
        long startTime = System.nanoTime();

        Mat ROIsingleChannel = new Mat(ROIImage.height(), ROIImage.width(), CvType.CV_8UC1);
        Imgproc.cvtColor(ROIImage, ROIsingleChannel, Imgproc.COLOR_BGRA2GRAY);

        int numberOfCPUCores = Runtime.getRuntime().availableProcessors();
        numberOfCPUCores = (numberOfCPUCores % 2 == 0) ? numberOfCPUCores : numberOfCPUCores-1;

        numberOfCPUCores = 1;

        int height = ROIImage.height();
        int stopHeight = (height % 2 == 0) ? height : height-1;
        int blockHeight = Math.round(stopHeight/numberOfCPUCores);
        List<Integer> dividers = Collections.synchronizedList(new ArrayList<>());
        ExecutorService threadPool = Executors.newFixedThreadPool(numberOfCPUCores);
        CountDownLatch latch = new CountDownLatch(numberOfCPUCores);
        for (int i=0; i<numberOfCPUCores; i++) {
            int startCoordinate = i * blockHeight;
            int endCoordinate = ((i+1)*blockHeight) - 1;
            threadPool.execute(() -> {
                for (int y=startCoordinate; y<=endCoordinate; y++) {
                    if (Core.countNonZero(ROIsingleChannel.row(y))==0) {
                        synchronized (dividers) {
                            dividers.add(y);
                        }
                    }
                }
                synchronized (latch) {
                    latch.countDown();
                }
            });
        }

//        System.out.println("Waiting for threads to finish ...");
        latch.await();
        long endTime = System.nanoTime();
        long timeElapsed = endTime - startTime;
        System.out.println("============= All threads finished ("+timeElapsed/1000000+" milliseconds total) ===============");



        Zone[] detectedZones;

        return new Zone[10];
    }

    public static Mat overlayZones(Mat ROIImage, Zone[] zones) {
        Mat overlay = ROIImage.clone();
        int width = ROIImage.width();

        for (Zone zone : zones) {
            Imgproc.line(overlay, new Point(0, zone.y1), new Point(width, zone.y1), new Scalar(0,0,255,255), 10);
            Imgproc.line(overlay, new Point(0, zone.y2), new Point(width, zone.y2), new Scalar(0,0,255,255), 10);
        }

        return overlay;
    }

    public static Zone[] getZonesST(Mat ROIImage) throws InterruptedException {
        long startTime = System.nanoTime();

        Mat ROISingleChannel = new Mat(ROIImage.height(), ROIImage.width(), CvType.CV_8UC1);
        Imgproc.cvtColor(ROIImage, ROISingleChannel, Imgproc.COLOR_BGRA2GRAY);

        int height = ROIImage.height();
        List<Integer> dividers = new ArrayList<>();

        for (int y=0; y<height; y++) {
            if (Core.countNonZero(ROISingleChannel.row(y))==0) {
                    dividers.add(y);
            }
        }

//        Zone[] detectedZones = _mergeToZones(dividers);
        Zone[] detectedZones = _clusterToZones(dividers);
        long endTime = System.nanoTime();
        long timeElapsed = endTime - startTime;
        System.out.println("===== getZonesST Finished ("+timeElapsed/1000000+" milliseconds total) =====");

        return detectedZones;
    }

    private static Zone[] _mergeToZones(List<Integer> dividers) {
        List<Integer> filteredDividers = new ArrayList<>();
        List<Integer> subgroup = new ArrayList<>();
        List<List<Integer>> subgroups = new ArrayList<>();
        ArrayList<Zone> zones = new ArrayList<>();
        int offset = 0;
        int size = dividers.size();
        boolean finished = false;
        for (int i=0; i<size; ) {
            subgroup.add(dividers.get(i));
            while (true) {
                offset++;
                if (i+offset >= size) {
                    finished = true;
                    break;
                }
                if (dividers.get(i+offset) == subgroup.get(0)+offset) {
                    subgroup.add(dividers.get(i+offset));
                } else {
                    if (subgroup.size()>=10) {
                        int index = Math.round(subgroup.size()/2);
                        filteredDividers.add(subgroup.get(index));
                        subgroups.add(subgroup);
                    }
                    i = i + offset;
                    offset = 0;
                    subgroup.clear();
                    break;
                }
            }
            if (finished) {
                break;
            }
        }

        for (int d=0; d<filteredDividers.size()-1; d++) {
            zones.add(new Zone(filteredDividers.get(d), filteredDividers.get(d+1)));
        }

        Zone[] finalArray = new Zone[zones.size()];
        zones.toArray(finalArray);
        return finalArray;
    }

    private static Zone[] _clusterToZones(List<Integer> dividers) {
        List<Integer> filteredDividers = new ArrayList<>();
        List<Integer> subgroup = new ArrayList<>();
        List<List<Integer>> subgroups = new ArrayList<>();
        ArrayList<Zone> zones = new ArrayList<>();
        int offset = 0;
        int size = dividers.size();
        boolean finished = false;
        for (int i=0; i<size; ) {
            subgroup.add(dividers.get(i));
            while (true) {
                offset++;
                if (i+offset >= size) {
                    finished = true;
                    break;
                }
                if (dividers.get(i+offset) == subgroup.get(0)+offset) {
                    subgroup.add(dividers.get(i+offset));
                } else {
                    if (subgroup.size()>=10) {
                        int index = Math.round(subgroup.size()/2);
                        filteredDividers.add(subgroup.get(index));
                        subgroups.add(new ArrayList<>(subgroup));
                    }
                    i = i + offset;
                    offset = 0;
                    subgroup.clear();
                    break;
                }
            }
            if (finished) {
                break;
            }
        }

        List<Integer> thicknessList = new ArrayList<>();
        for (int g=0; g<subgroups.size(); g++) {
            thicknessList.add(subgroups.get(g).size());
        }


        List<Long> scores = new ArrayList<>();
        for (int T : thicknessList) {
            long dividersCount = subgroups.stream()
                                .filter(group -> group.size()>=T)
                                .count();
            long zoneCount = dividersCount + 1;
            long score = T * zoneCount;
            scores.add(score);
        }
//        List<Integer> potentialZoneCount = new ArrayList<>();
//        for (int T=0; T<thicknessList.size(); T++) {
//            int dividersCount = subgroups.stream().filter(group -> group.size())
//        }

        return new Zone[10];
    }
}
