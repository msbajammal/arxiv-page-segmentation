package com.github.msbajammal.segmentation.roi;

import java.util.*;

public class Frame {
    public final String xpath;
    public final int depth;

    public final Frame parent;
    public List<Frame> children;

    public boolean addChild(Frame frame) {
        return this.children.add(frame);
    }

    public Frame(String xpath, Frame parent, List<Frame> children) {
        this.xpath = xpath;
        this.parent = parent;
        if (children == null) {
            this.children = new ArrayList<>();
        } else {
            this.children = children;
        }

        if (parent==null) {
            this.depth = 0;
        } else {
            this.depth = parent.depth+1;
        }
    }

    public boolean isRoot() {
        return this.parent==null;
    }

    public boolean isLeaf() {
        return (this.children==null) || (this.children.size()==0);
    }

    public Frame getRoot() {
        Frame frame = this;
        while (!frame.isRoot()) {
            frame = frame.parent;
        }
        return frame;
    }

    public List<Frame> getSiblings() {
        if (this.parent == null) {
            // no siblings
            return new ArrayList<>();
        } else {
            List<Frame> siblings = this.parent.children;
            for (Frame sibling : siblings) {
                if (this.equals(sibling)) {
                    siblings.remove(sibling);
                }
            }
            return siblings;
        }
    }

    public boolean isParentOf(Frame frame) {
        return frame.parent.equals(this);
    }

    public boolean isChildOf(Frame frame) {
        boolean isChild = false;
        for (Frame childFrame : frame.children) {
            if (childFrame.equals(this)) {
                isChild = true;
                break;
            }
        }
        return isChild;
    }

    public boolean isGrandParentOf(Frame frame) {
        boolean isGrandParent = false;

        if (this.isParentOf(frame)) {
            isGrandParent = true;

        } else {
            Frame parentFrame = frame.parent;
            while (parentFrame!=null) {
                if (this.equals(parentFrame)) {
                    isGrandParent = true;
                    break;
                } else {
                    parentFrame = parentFrame.parent;
                }
            }
        }

        return isGrandParent;
    }

//    function getAllDescendants(node) {
//        var all = [];
//        getDescendants(node);
//
//        function getDescendants(node) {
//        for (var i = 0; i < node.childNodes.length; i++) {
//            var child = node.childNodes[i];
//            getDescendants(child);
//            all.push(child);
//        }
//          }
//        return all;
//    }

    /*
           a
          /
        b
      /  \
    c    d
          \
          e
     */

    public List<Frame> getAllDescendants(Frame frame) {
        List<Frame> all = new ArrayList<>();

        class Helper { public void getDescendants(Frame node) {
            for (int i=0; i<node.children.size(); i++) {
                Frame child = node.children.get(i);
                getDescendants(child);
                all.add(child);
            }
        }}

        new Helper().getDescendants(frame);

        return all;
    }

    public boolean isGrandChildOf(Frame frame) {
        return frame.isGrandParentOf(this);
    }

    public Stack<Frame> getPath() {
        Stack<Frame> path = new Stack<>();
        Frame parent = this.parent;
        path.push(this);

        while (parent!=null) {
            path.push(parent);
            parent = parent.parent;
        }

        return path;
    }
}
