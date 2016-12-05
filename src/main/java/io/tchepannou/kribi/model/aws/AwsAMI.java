package io.tchepannou.kribi.model.aws;

public enum AwsAMI {
    AMAZON_LINUX("ami-b73b63a0"),
    RH_LINUX_v7_3("ami-b73b63a0"),
    SUZE_LINUX_v12_SP2("ami-6f86a478"),
    UBUNTU_v16_04("ami-40d28157"),
    MSWINDOWS_v2016("ami-b06249a7");

    private final String imageId;

    AwsAMI(final String id) {
        imageId = id;
    }

    public String getImageId() {
        return imageId;
    }
}
