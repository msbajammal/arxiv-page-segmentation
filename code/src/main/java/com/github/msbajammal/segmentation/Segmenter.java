package com.github.msbajammal.segmentation;

import com.github.msbajammal.segmentation.roi.ROI;
import com.github.msbajammal.segmentation.roi.ROIExtractor;
import com.github.msbajammal.segmentation.roi.Cluster;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Segmenter {

    public static List<Rectangle> apply(Browser browser) throws IOException {
        List<ROI> ROIs = ROIExtractor.generateROIs(browser);
        Cluster root = new Cluster(ROIs);
        /*
        // breakdown of cluster stages
        Map<String, List<Double>> stats = root.getAdjacencyStats();
        start = System.nanoTime();
        Object map = root.buildAdjacencyNeighborhood();
        finish = System.nanoTime();
        duration1 = (finish-start)/1000000.0;
        System.out.println(duration1);

        start = System.nanoTime();
        Map<String, Object> mapStats = root.getStats();
        finish = System.nanoTime();
        duration2 = (finish-start)/1000000.0;
        System.out.println(duration2);

        start = System.nanoTime();
        Map<String, List<Double>> mapGeomStats = root.getGeomStats();
        List<Double> alignmentStats = root.getAlignmentStats();
        finish = System.nanoTime();
        duration3 = (finish-start)/1000000.0;
        System.out.println(duration3);

        start = System.nanoTime();
        Map<String, List<Double>> adjStats = root.getAdjacencyStats();
        finish = System.nanoTime();
        duration4 = (finish-start)/1000000.0;
        System.out.println(duration4);

        List<Double> data = adjStats.get("distances");
        List<Double> y =  data;
        Collections.sort(y);
        List<Integer> x = IntStream.rangeClosed(1, y.size()).boxed().collect(Collectors.toList());
//        Histogram histDist = new Histogram(data, 100);
//        CategoryChart chart = new CategoryChartBuilder().width(800).height(600)
//                                    .title("Adjacency Histogram").xAxisTitle("Distance Delta").yAxisTitle("Frequency")
//                                    .build();
//        chart.addSeries("h", histDist.getxAxisData(), histDist.getyAxisData());
//        new SwingWrapper<CategoryChart>(chart).displayChart();
        XYChart chart = new XYChartBuilder().width(800).height(600).build();
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
        chart.getStyler().setChartTitleVisible(false);
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
        chart.getStyler().setMarkerSize(1);
        chart.addSeries("a", x, y);
        new SwingWrapper<XYChart>(chart).displayChart();
*/
        List<Cluster> clusters = root.populateClusters();
        List<Rectangle> result = new ArrayList<>(clusters.size());
        for (Cluster C : clusters) {
            result.add(C.getMBR(0));
            System.out.println(C.getMBR(0));
        }
        return result;
    }
}
