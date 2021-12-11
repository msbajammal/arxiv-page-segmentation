package com.github.msbajammal.segmentation.roi;


import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Scalar;
import org.opencv.core.Point;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.GeometryItemDistance;
import org.locationtech.jts.index.strtree.STRtree;
import jsat.DataSet;
import jsat.SimpleDataSet;
import jsat.classifiers.CategoricalData;
import jsat.classifiers.DataPoint;
import jsat.clustering.Clusterer;
import jsat.clustering.HDBSCAN;
import jsat.linear.DenseVector;
import jsat.linear.Vec;
import jsat.linear.distancemetrics.DistanceMetric;

import java.awt.Rectangle;
import java.util.*;
import java.util.concurrent.ExecutorService;


public class Cluster {
    public List<ROI> ROIs;
    public Cluster parent;
    public List<Cluster> children;
    public GeometryFactory geometryFactory;

    public Cluster(List<ROI> ROIs) {
        this.ROIs = ROIs;
        this.parent = null;
        this.children = new ArrayList<>();
        this.geometryFactory = new GeometryFactory(new PrecisionModel(1));
    }

    public void populateDefaultClusters() {
        if (ROIs.size()>1) {
            this.children = new ArrayList<>(ROIs.size());
            for (ROI R : ROIs) {
                List<ROI> list = new ArrayList<>(1);
                list.add(R);
                this.children.add(new Cluster(list));
            }
        } else {
            this.children = null;
        }
    }

    public List<Geometry> castROIsToGeoList() {
        List<Geometry> result = new ArrayList<>(ROIs.size());
        for (ROI R : ROIs) {
            result.add(geometryFactory.createPolygon(new Coordinate[]{new Coordinate(R.x1, R.y1),
                    new Coordinate(R.x2, R.y1),
                    new Coordinate(R.x2, R.y2),
                    new Coordinate(R.x1, R.y2),
                    new Coordinate(R.x1, R.y1)}));
        }
        return result;
    }

    public Map<String, Object> getStats() {
        List<Integer> yDiff = new ArrayList<>(ROIs.size()*ROIs.size()*4);
        List<Integer> xDiff = new ArrayList<>(ROIs.size()*ROIs.size()*4);
        for (ROI A : ROIs) {
            for (ROI B : ROIs) {
                if (A.equals(B)) {
                    continue;
                } else {
                    List<Integer> ydiffs = Arrays.asList(Math.abs(A.y1-B.y1), Math.abs(A.y1-B.y2),
                                                         Math.abs(A.y2-B.y1), Math.abs(A.y2-B.y2));
                    List<Integer> xdiffs = Arrays.asList(Math.abs(A.x1-B.x1), Math.abs(A.x1-B.x2),
                                                         Math.abs(A.x2-B.x1), Math.abs(A.x2-B.x2));
                    yDiff.addAll(ydiffs);
                    xDiff.addAll(xdiffs);
                }
            }
        }
        Map<String, Object> result = new HashMap<>(2);
        result.put("yDiff", yDiff);
        result.put("xDiff", xDiff);
        return result;
    }

    public List<Double> getAlignmentStats() {
        List<Geometry> objects = castROIsToGeoList();
        List<Double> deltas = new ArrayList<>(ROIs.size()*ROIs.size());
        for (Geometry A : objects) {
            for (Geometry B : objects) {
                if (A.equals(B)) {
                    continue;
                } else {
                    List<Double> deltasAB = Arrays.asList(
                            Math.abs(A.getEnvelopeInternal().getMinX()-B.getEnvelopeInternal().getMinX()), // left align
                            Math.abs(A.getEnvelopeInternal().getMaxX()-B.getEnvelopeInternal().getMaxX()), // right align
                            Math.abs(A.getEnvelopeInternal().getMinY()-B.getEnvelopeInternal().getMinY()), // top align
                            Math.abs(A.getEnvelopeInternal().getMaxY()-B.getEnvelopeInternal().getMaxY()), // bottom align
                            Math.abs(A.getCentroid().getX() - B.getCentroid().getX()), // vertical center align
                            Math.abs(A.getCentroid().getY() - B.getCentroid().getY())  // horizontal center align
                            );
                    double delta = Collections.min(deltasAB);
                    if (delta>=1) deltas.add(delta);
                }
            }
        }
        return deltas;
    }

    public Map<String, List<Double>> getGeomStats() {
        List<Geometry> objects = castROIsToGeoList();
        List<Double> distances = new ArrayList<>(ROIs.size()*ROIs.size());
        for (Geometry A : objects) {
            for (Geometry B : objects) {
                if (A.equals(B)) {
                    continue;
                } else {
                    distances.add(A.distance(B));
                }
            }
        }
        Map<String, List<Double>> result = new HashMap<>(2);
        result.put("diff", distances);
        return result;
    }

    public Map<String, Object> buildAdjacencyNeighborhood() {
        STRtree tree = new STRtree();
        Map<Polygon, ROI> treeMapping = new HashMap<>(ROIs.size());
        for (ROI R : ROIs) {
            Polygon P = geometryFactory.createPolygon(new Coordinate[]{new Coordinate(R.x1, R.y1),
                    new Coordinate(R.x2, R.y1), new Coordinate(R.x2, R.y2), new Coordinate(R.x1, R.y2),
                    new Coordinate(R.x1, R.y1)});
            treeMapping.put(P, R);
            tree.insert(P.getEnvelopeInternal(), P);
        }

        Map<Polygon, List<Polygon>> adjacency = new HashMap<>(treeMapping.keySet().size());
        for (Polygon P : treeMapping.keySet()) {
            List<Polygon> adjacents = new ArrayList<>(10);
            Object[] _NN = tree.nearestNeighbour(P.getEnvelopeInternal(), P, new GeometryItemDistance(), 10);
            List<Polygon> NN = new ArrayList<>(_NN.length);
            for (Object _N : _NN) { NN.add((Polygon)_N); }

            for (Polygon neighbor : NN) {
                if (neighbor.equals(P)) {
                    continue;
                } else {
                    LineString scanLine = geometryFactory.createLineString(new Coordinate[]{
                            P.getCentroid().getCoordinate(), neighbor.getCentroid().getCoordinate()
                    });
                    List<Polygon> scanQuery = tree.query(scanLine.getEnvelopeInternal());
                    if (scanQuery.size()==2) {
                        adjacents.add(neighbor);
                    } else {
                        continue;
                    }
                }
            }
            adjacency.put(P, adjacents);
        }

        Map<ROI, List<ROI>> ROIsAdjacency = new HashMap<>(adjacency.size());
        for (Polygon P : adjacency.keySet()) {
            ROI R = treeMapping.get(P);
            List<ROI> list = new ArrayList<>(adjacency.get(P).size());
            for (Polygon l : adjacency.get(P)) {
                list.add(treeMapping.get(l));
            }
            ROIsAdjacency.put(R, list);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("ROIsAdjacency", ROIsAdjacency);
        result.put("polygonAdjacency", adjacency);
        return result;
    }

    public Map<String, List<Double>> getAdjacencyStats() {
        List<Double> distances = new ArrayList<>(ROIs.size()*ROIs.size());
        List<Double> deltas = new ArrayList<>(ROIs.size()*ROIs.size());
        Map<String, Object> raw = buildAdjacencyNeighborhood();
        Map<Polygon, List<Polygon>> adjacency = (Map<Polygon, List<Polygon>>)(raw.get("polygonAdjacency"));
        for (Polygon A : adjacency.keySet()) {
            for (Polygon B : adjacency.get(A)) {
                List<Double> deltasAB = Arrays.asList(
                        Math.abs(A.getEnvelopeInternal().getMinX()-B.getEnvelopeInternal().getMinX()), // left align
                        Math.abs(A.getEnvelopeInternal().getMaxX()-B.getEnvelopeInternal().getMaxX()), // right align
                        Math.abs(A.getEnvelopeInternal().getMinY()-B.getEnvelopeInternal().getMinY()), // top align
                        Math.abs(A.getEnvelopeInternal().getMaxY()-B.getEnvelopeInternal().getMaxY()), // bottom align
                        Math.abs(A.getCentroid().getX() - B.getCentroid().getX()), // vertical center align
                        Math.abs(A.getCentroid().getY() - B.getCentroid().getY())  // horizontal center align
                );
                deltas.add(Collections.min(deltasAB));
                distances.add(A.distance(B));
            }
        }
        Map<String, List<Double>> result = new HashMap<>(2);
        result.put("distances", distances);
        result.put("deltas", deltas);
        return result;
    }


    public List<String> getXPaths() {
        List<String> xpaths = new ArrayList<>();
        for (ROI R : ROIs) {
            xpaths.add(R.xpath);
        }
        return xpaths;
    }

    public int getSize() {
        return ROIs.size();
    }

    public void add(ROI R) {
        ROIs.add(R);
    }

    public void remove(ROI R) {
        ROIs.remove(R);
    }

    public Rectangle getMBR(int offset) {
        List<Integer> x = new ArrayList<>(ROIs.size()*2);
        List<Integer> y = new ArrayList<>(ROIs.size()*2);
        for (ROI R : ROIs) {
            x.add(R.x1); x.add(R.x2);
            y.add(R.y1); y.add(R.y2);
        }
        int x1 = Collections.min(x) - offset, x2 = Collections.max(x) + offset;
        int y1 = Collections.min(y) - offset, y2 = Collections.max(y) + offset;
        return new Rectangle(x1, y1, (x2-x1), (y2-y1));
    }

    public Mat renderOn(Mat overlay) {
        Scalar borderROI = new Scalar(2, 166, 249, 255);
        Scalar borderCluster = new Scalar(87, 139, 46, 255);
        for (ROI R : ROIs) {
            Imgproc.rectangle(overlay, new Point(R.x1, R.y1), new Point(R.x2, R.y2), borderROI, 1);
        }
        Rectangle mbr = getMBR(2);
        Imgproc.rectangle(overlay, new Point(mbr.x, mbr.y), new Point(mbr.x+mbr.width, mbr.y+mbr.height), borderCluster, 3);
        return overlay;
    }

//    public DataSet getDataSet() {
//        // Exact numerical sequence (16 fields in total):
//        // ROI index, x1, y1, x2, y2, width, height
//        // fontSize, foregroundColor[0,1,2,3], backgroundColor[0,1,2,3]
//        // Followed by one category: xpath
//        List<DataPoint> dataPoints = new ArrayList<>();
//
//        // Setup the categorical fields: xpath, ROI type
////        List<String> xpaths = getXPaths();
////        int numberOfCategories = xpaths.size();
////        CategoricalData xpathField = new CategoricalData(numberOfCategories);
////        xpathField.setCategoryName("xpath");
////        for (int i=0; i<xpaths.size(); i++) {
////            xpathField.setOptionName(xpaths.get(i), i);
////        }
////        CategoricalData ROITypeField = new CategoricalData(3);
////        ROITypeField.setCategoryName("type");
////        ROITypeField.setOptionName("text", 1);
////        ROITypeField.setOptionName("image", 2);
////        ROITypeField.setOptionName("input", 3);
//
//
//        // Build the data point
////        DenseVector numericalValues = new DenseVector(17);
//        DenseVector numericalValues = new DenseVector(1);
////        int[] categoricalValues = new int[1];
////        CategoricalData[] categoricalData = {xpathField, ROITypeField};
//        int[] categoricalValues = new int[0];
//        CategoricalData[] categoricalData = {};
//
//        for (int R=0; R<ROIs.size(); R++) {
//            numericalValues.set(0, (double)R);
////            numericalValues.set(1, (double)(ROIs.get(R).x1));
////            numericalValues.set(2, (double)(ROIs.get(R).y1));
////            numericalValues.set(3, (double)(ROIs.get(R).x2));
////            numericalValues.set(4, (double)(ROIs.get(R).y2));
////            numericalValues.set(5, (double)(ROIs.get(R).width));
////            numericalValues.set(6, (double)(ROIs.get(R).height));
////            numericalValues.set(7, (double)(ROIs.get(R).fontSize));
////            numericalValues.set(8, (double)(ROIs.get(R).foregroundColor[0]));
////            numericalValues.set(9, (double)(ROIs.get(R).foregroundColor[1]));
////            numericalValues.set(10, (double)(ROIs.get(R).foregroundColor[2]));
////            numericalValues.set(11, (double)(ROIs.get(R).foregroundColor[3]));
////            numericalValues.set(12, (double)(ROIs.get(R).backgroundColor[0]));
////            numericalValues.set(13, (double)(ROIs.get(R).backgroundColor[1]));
////            numericalValues.set(14, (double)(ROIs.get(R).backgroundColor[2]));
////            numericalValues.set(15, (double)(ROIs.get(R).backgroundColor[3]));
////            switch (ROIs.get(R).type) {
////                case "text":
////                    numericalValues.set(16, 1.0);
////                    break;
////                case "image":
////                    numericalValues.set(16, 2.0);
////                    break;
////                case "input":
////                    numericalValues.set(16, 3.0);
////                    break;
////            }
//            dataPoints.add(new DataPoint(numericalValues, categoricalValues, categoricalData));
//        }
//
//        return new SimpleDataSet(dataPoints);
//    }

    public List<Cluster> populateClusters() {
        class DPrime implements DistanceMetric {
//            @Override public boolean isSubadditive() { return false; }
            @Override public boolean isSubadditive() { return true; }
            @Override public boolean isIndiscemible() { return true; }
            @Override public boolean isSymmetric() { return true; }
            @Override public double metricBound() { return Double.POSITIVE_INFINITY; }
            @Override public String toString() { return "DPrime"; }
            @Override public DPrime clone() { return new DPrime(); }
            @Override public boolean supportsAcceleration() { return false; }
            @Override public List<Double> getAccelerationCache(List<? extends Vec> vecs) { return null; }
            @Override public List<Double> getAccelerationCache(List<? extends Vec> list, ExecutorService executorService) { return null; }
            @Override public List<Double> getQueryInfo(Vec vec) { return null; }
            @Override public double dist(int a, int b, List<? extends Vec> vecs, List<Double> cache) { return dist(vecs.get(a), vecs.get(b)); }
            @Override public double dist(int a, Vec b, List<? extends Vec> vecs, List<Double> cache) { return dist(a, b, getQueryInfo(b), vecs, cache); }
            @Override public double dist(int a, Vec b, List<Double> qi, List<? extends Vec> vecs, List<Double> cache) { return dist(vecs.get(a), b); }

            private final GeometryFactory geometryFactory;
            public DPrime() {
                super();
                geometryFactory = new GeometryFactory(new PrecisionModel(1));
            }

            @Override public double dist(Vec a, Vec b) {
//                int  a_x1 = (int)(a.get(1)), a_y1 = (int)(a.get(2)),
//                        a_x2 = (int)(a.get(3)), a_y2 = (int)(a.get(4)),
//                        a_width = (int)(a.get(5)), a_height = (int)(a.get(6)),
//                        a_fontSize = (int)(a.get(7));
//                int[] a_foregroundColor = {(int)(a.get(8)), (int)(a.get(9)),
//                        (int)(a.get(10)), (int)(a.get(11))},
//                        a_backgroundColor = {(int)(a.get(12)), (int)(a.get(13)),
//                                (int)(a.get(14)), (int)(a.get(15))};
//                String a_xpath = xpaths.get((int)(a.get(0)));
//                String a_type = "";
//                switch ((int)(a.get(16))) {
//                    case 1:
//                        a_type = "text";
//                        break;
//                    case 2:
//                        a_type = "image";
//                        break;
//                    case 3:
//                        a_type = "input";
//                        break;
//                }
//
//                int  b_x1 = (int)(b.get(1)), b_y1 = (int)(b.get(2)),
//                        b_x2 = (int)(b.get(3)), b_y2 = (int)(b.get(4)),
//                        b_width = (int)(b.get(5)), b_height = (int)(b.get(6)),
//                        b_fontSize = (int)(b.get(7));
//                int[] b_foregroundColor = {(int)(b.get(8)), (int)(b.get(9)),
//                        (int)(b.get(10)), (int)(b.get(11))},
//                        b_backgroundColor = {(int)(b.get(12)), (int)(b.get(13)),
//                                (int)(b.get(14)), (int)(b.get(15))};
//                String b_xpath = xpaths.get((int)(b.get(0)));
//                String b_type = "";
//                switch ((int)(b.get(16))) {
//                    case 1:
//                        b_type = "text";
//                        break;
//                    case 2:
//                        b_type = "image";
//                        break;
//                    case 3:
//                        b_type = "input";
//                        break;
//                }
                ROI A = ROIs.get((int)(a.get(0)));
                ROI B = ROIs.get((int)(b.get(0)));
//                double score = 0.0;
//                score += formattingMetric(A, B);
//                score += alignmentMetric(A, B);
//                score += distanceMetric(A, B);
                return distanceMetric(A, B)+pathMetric(A, B);
                // Ideas for Zone detection:
                /*
                1. A "rectangle ballooning" algorithm,
                  where a rectangle is incrementally enlarged to fit the
                  area, then scored using: score = area * width
                  P.S. it is multipleid by width to penalize
                  large and thin vertically elongated rectangles

                2. Use a normalized area as a "gravity factor".
                Once all ROIs are created, do another pass
                to assign a normalized area (w.r.t max area) to each ROI.
                Then, use this normalized area/gravity factor in the
                distance functions in order to guide the clustering
                towards "more massive" ROIs instead of smaller ROIs.
                 */
            }

            public double distanceMetric(ROI A, ROI B) {
                Coordinate[] A_shell = {new Coordinate(A.x1, A.y1), new Coordinate(A.x2, A.y1),
                                         new Coordinate(A.x2, A.y2), new Coordinate(A.x1, A.y2),
                                                                    new Coordinate(A.x1, A.y1)};
                Coordinate[] B_shell = {new Coordinate(B.x1, B.y1), new Coordinate(B.x2, B.y1),
                                         new Coordinate(B.x2, B.y2), new Coordinate(B.x1, B.y2),
                                                                    new Coordinate(B.x1, B.y1)};
                Polygon A_poly = geometryFactory.createPolygon(A_shell);
                Polygon B_poly = geometryFactory.createPolygon(B_shell);
                return A_poly.distance(B_poly);
            }
            public double alignmentMetric(ROI A, ROI B) {
                List<Double> deltas = new ArrayList<>();
                deltas.add(Math.sqrt(Math.pow(A.x1-B.x1, 2)));
                deltas.add(Math.sqrt(Math.pow(A.x2-B.x2, 2)));
                deltas.add(Math.sqrt(Math.pow(A.y1-B.y1, 2)));
                deltas.add(Math.sqrt(Math.pow(A.y2-B.y2, 2)));
                double A_center_x = A.x1 + ((double)(A.width)/2);
                double A_center_y = A.y1 + ((double)(A.height)/2);
                double B_center_x = B.x1 + ((double)(B.width)/2);
                double B_center_y = B.y1 + ((double)(B.height)/2);
                deltas.add(Math.sqrt(Math.pow(A_center_x-B_center_x, 2)));
                deltas.add(Math.sqrt(Math.pow(A_center_y-B_center_y, 2)));
                return Collections.min(deltas);
            }
            public double formattingMetric(ROI A, ROI B) {
                if (!A.type.equals(B.type)) {
                    return 0.0;
                } else {
                    int[] aFG = A.foregroundColor;
                    int[] aBG = A.backgroundColor;
                    int[] bFG = B.foregroundColor;
                    int[] bBG = B.backgroundColor;
                    double fg = Math.sqrt(Math.pow(aFG[0]-bFG[0], 2)+
                                Math.pow(aFG[1]-bFG[1], 2)+
                                Math.pow(aFG[2]-bFG[2], 2)+
                                Math.pow(aFG[3]-bFG[3], 2));
                    double bg = Math.sqrt(Math.pow(aBG[0]-bBG[0], 2)+
                            Math.pow(aBG[1]-bBG[1], 2)+
                            Math.pow(aBG[2]-bBG[2], 2)+
                            Math.pow(aBG[3]-bBG[3], 2));

                    return fg+bg+Math.sqrt(Math.pow(A.fontSize-B.fontSize, 2));
                }
            }
            public double pathMetric(ROI A, ROI B) {
                String A_xpath = A.xpath;
                String B_xpath = B.xpath;
                int diff = A_xpath.length() - B_xpath.length();
                if (diff > 0) {
                    B_xpath = String.format("%-"+(A_xpath.length())+"s", B_xpath);
                }
                if (diff < 0) {
                    A_xpath = String.format("%-"+(B_xpath.length())+"s", A_xpath);
                }
                int score = 0;
                for (int c=0; c<A_xpath.length(); c++) {
                    score += (A_xpath.charAt(c) != B_xpath.charAt(c)) ? 1 : 0;
                }
                return score*1;
            }
        }

        List<DataPoint> dataPoints = new ArrayList<DataPoint>(this.ROIs.size());
        int[] categoricalValues = new int[0];
        CategoricalData[] categoricalData = {};
        for (int r=0; r<this.ROIs.size(); r++) {
            DenseVector numericalValues = new DenseVector(1);
            numericalValues.set(0, (double) r);
            dataPoints.add(new DataPoint(numericalValues, categoricalValues, categoricalData));
        }
        DataSet dataset = new SimpleDataSet(dataPoints);
        Clusterer hdbscan = new HDBSCAN(new DPrime(), 3);
        List<List<DataPoint>> pointClusters = hdbscan.cluster(dataset);
        List<Cluster> clusters = new ArrayList<>(pointClusters.size());

        for (List<DataPoint> points : pointClusters) {
            List<ROI> clusterROIs = new ArrayList<>(points.size());
            for (int p=0; p<points.size(); p++) {
                Vec vector = points.get(p).getNumericalValues();
                int index = (int)(vector.get(0));
                clusterROIs.add(this.ROIs.get(index));
            }
            Cluster x = new Cluster(clusterROIs);
            x.parent = this;
            this.children.add(x);
            clusters.add(x);
        }

        return clusters;
    }
}

