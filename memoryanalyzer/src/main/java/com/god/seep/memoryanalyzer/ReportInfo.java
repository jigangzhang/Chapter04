package com.god.seep.memoryanalyzer;

import com.squareup.haha.perflib.Instance;

import java.util.ArrayList;
import java.util.List;

public class ReportInfo {
    private int width;
    private int height;
    private int bufferSize;
    private String hash;
    private List<Instance> instances;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public List<Instance> getInstances() {
        return instances;
    }

    public void addInstance(Instance instance) {
        if (this.instances == null)
            this.instances = new ArrayList<>();
        this.instances.add(instance);
    }

    public String dumpStack(Instance bitmapInstance) {
        StringBuilder sb = new StringBuilder();
        Instance instance = bitmapInstance;
        sb.append(instance.getClassObj().getClassName());
        while ((instance = instance.getNextInstanceToGcRoot()) != null) {
            sb.append("\n");
            sb.append(instance.getClassObj().getClassName());
        }
        return sb.toString();
    }

    public String dumpStack(List<Instance> instances) {
        if (instances != null) {
            StringBuilder sb = new StringBuilder();
            for (Instance instance : instances) {
                sb.append(dumpStack(instance));
                sb.append("\n");
            }
            return sb.toString();
        }
        return "";
    }

    @Override
    public String toString() {
        return "ReportInfo{" +
                "width=" + width +
                ", height=" + height +
                ", bufferSize=" + bufferSize +
                ", hash='" + hash + '\'' +
                ",\n stack=" + dumpStack(instances) +
                '}';
    }
}
