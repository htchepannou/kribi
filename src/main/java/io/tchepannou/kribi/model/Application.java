package io.tchepannou.kribi.model;

import io.tchepannou.kribi.model.aws.ApplicationTemplate;
import io.swagger.annotations.ApiModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApiModel
public class Application {

    private String name;
    private String description;
    private String runAs;
    private String jvmOptions = "";
    private OS operatingSystem = OS.LINUX;
    private ApplicationTemplate template = ApplicationTemplate.none;
    private List<Instance> instances = new ArrayList<>();
    private Services services = new Services();
    private String healthCheckPath;

    public Optional<Instance> getInstance(final String region) {
        return instances.stream()
                .filter(i -> i.getRegion().equals(region))
                .findFirst();
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public List<Instance> getInstances() {
        return instances;
    }

    public void setInstances(final List<Instance> instances) {
        this.instances = instances;
    }

    public Services getServices() {
        return services;
    }

    public void setServices(final Services services) {
        this.services = services;
    }

    public String getHealthCheckPath() {
        return healthCheckPath;
    }

    public void setHealthCheckPath(final String healthCheckPath) {
        this.healthCheckPath = healthCheckPath;
    }

    public ApplicationTemplate getTemplate() {
        return template;
    }

    public void setTemplate(final ApplicationTemplate template) {
        this.template = template;
    }

    public OS getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(final OS operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public String getRunAs() {
        return runAs;
    }

    public void setRunAs(final String runAs) {
        this.runAs = runAs;
    }

    public String getJvmOptions() {
        return jvmOptions;
    }

    public void setJvmOptions(final String jvmOptions) {
        this.jvmOptions = jvmOptions;
    }
}
