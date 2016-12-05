package io.tchepannou.kribi.model;

public class Instance {
    private String region;
    private String type;
    private int minCount = 1;
    private int maxCount = 1;

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(final String region) {
        this.region = region;
    }

    public int getMinCount() {
        return minCount;
    }

    public void setMinCount(final int minCount) {
        this.minCount = minCount;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(final int maxCount) {
        this.maxCount = maxCount;
    }
}
