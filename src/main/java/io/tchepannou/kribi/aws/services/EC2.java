package io.tchepannou.kribi.aws.services;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.ShutdownBehavior;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.Role;
import com.jcabi.ssh.SSH;
import com.jcabi.ssh.Shell;
import io.tchepannou.kribi.KribiException;
import io.tchepannou.kribi.aws.AwsContext;
import io.tchepannou.kribi.aws.support.AwsSupport;
import io.tchepannou.kribi.client.DeployRequest;
import io.tchepannou.kribi.client.KribiRequest;
import io.tchepannou.kribi.client.UndeployRequest;
import io.tchepannou.kribi.model.Application;
import io.tchepannou.kribi.model.Cluster;
import io.tchepannou.kribi.model.Environment;
import io.tchepannou.kribi.model.Host;
import io.tchepannou.kribi.model.Instance;
import io.tchepannou.kribi.model.aws.ApplicationTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class EC2 {
    private static final Logger LOGGER = LoggerFactory.getLogger(EC2.class);
    private static final String SSH_USERNAME = "ec2-user";
    private static final int DELAY_SECONDS = 30;
    private static final int MAX_RETRIES = 10;
    private static final String TAG_NAME = "Name";
    private static final String TAG_APP_NAME = "AppName";
    private static final String TAG_ENVIRONMENT = "Environment";
    private static final String TAG_VERSION = "Version";
    private static final String TAG_TRANSACTION_ID = "TransactionId";
    private static final String TAG_TEMPLATE = "Template";

    private final AmazonEC2 ec2;
    private final AmazonIdentityManagement iam;
    private final AwsContext context;

    public EC2(final AwsContext context) {
        ec2 = context.getEc2();
        iam = context.getIam();

        this.context = context;
    }

    //-- Public
    public Reservation create(final DeployRequest deployRequest) {
        ensureNotAlreadyDeployed(deployRequest);
        ensureNotTooManyRequests(deployRequest);

        final Reservation reservation = createInstances(deployRequest);
        tag(deployRequest, reservation);

        return waitForReady(reservation);
    }

    public void delete(final UndeployRequest undeployRequest) {
        deleteInstances(undeployRequest);
    }

    public void install(final DeployRequest deployRequest, final Cluster cluster) {
        for (final Host host : cluster.getHosts()) {
            install(deployRequest, host);
        }
    }

    public Collection<String> getVersions(final String name, final Environment env) {
        final List<String> versions = new ArrayList<>();
        for (final Reservation reservation : ec2.describeInstances().getReservations()) {
            for (final com.amazonaws.services.ec2.model.Instance instance : reservation.getInstances()) {
                if (isTerminated(instance)) {
                    continue;
                }

                if (hasTag(TAG_APP_NAME, name, instance) && hasTag(TAG_ENVIRONMENT, env.name(), instance)) {
                    final Optional<Tag> version = getTag(TAG_VERSION, instance);
                    if (version.isPresent()) {
                        versions.add(version.get().getValue());
                    }
                }
                break;
            }
        }
        return versions;
    }

    public Collection<Reservation> getReservations() {
        return ec2.describeInstances().getReservations();
    }

    public Collection<Reservation> getReservationsByTemplate(final ApplicationTemplate template) {
        final List<Reservation> reservations = new ArrayList<>();
        for (final Reservation reservation : ec2.describeInstances().getReservations()) {
            for (final com.amazonaws.services.ec2.model.Instance instance : reservation.getInstances()) {
                if (isTerminated(instance)) {
                    continue;
                }

                if (hasTag(TAG_TEMPLATE, template.name(), instance)) {
                    reservations.add(reservation);
                }
                break;
            }
        }
        return reservations;
    }

    public void delete(final Reservation reservation) {
        final List<String> instanceIds = getInstanceIds(reservation);

        LOGGER.info("Terminating {} instance(s) {} from reservation {}", instanceIds.size(), instanceIds, reservation.getReservationId());
        final TerminateInstancesRequest request = new TerminateInstancesRequest()
                .withInstanceIds(instanceIds);
        ec2.terminateInstances(request);
    }

    public void vaccum() {
        final Collection<Reservation> reservations = getReservations();
        LOGGER.info("{} reservation found", reservations.size());

        final long now = System.currentTimeMillis();
        final long fiveMinutes = 5 * 60 * 1000;
        for (final Reservation reservation : reservations) {
            for (final com.amazonaws.services.ec2.model.Instance instance : reservation.getInstances()) {
                final Optional<Tag> tag = getTag(TAG_TEMPLATE, instance);

                if (isValidTemplate(tag)) {
                    final long diffMillis = now - reservation.getInstances().get(0).getLaunchTime().getTime();
                    if (diffMillis > fiveMinutes) {
                        LOGGER.error("{} is a phantom reservation", reservation.getReservationId());
                        try {
                            delete(reservation);
                            break;
                        } catch (final Exception e) {
                            LOGGER.error("Unable to delete Reservation: {}", reservation.getReservationId(), e);
                        }
                    }
                }
            }
        }
    }

    private boolean isValidTemplate(Optional<Tag> tag){
        if (tag.isPresent()){
            try {
                return ApplicationTemplate.valueOf(tag.get().getValue().toLowerCase()) == null;
            } catch (Exception e){
                return false;
            }
        }
        return false;
    }

    //-- Private
    private void ensureNotAlreadyDeployed(final DeployRequest deployRequest) {
        final Optional<Reservation> prev = findReservation(deployRequest);
        if (prev.isPresent()) {
            LOGGER.error("This version of the application has already been deployed");
            throw new KribiException(KribiException.APPLICATION_ALREADY_DEPLOYED, "This version of the application has already been deployed");
        }
    }

    private void ensureNotTooManyRequests(final DeployRequest deployRequest) {
        final Collection<String> versions = getVersions(deployRequest.getApplicationName(), deployRequest.getEnvironment());
        if (versions.size() >= 2) {
            throw new KribiException(KribiException.TOO_MANY_INSTANCES_DEPLOYED, "The application has too many version deployed: " + versions);
        }
    }

    private Reservation createInstances(final DeployRequest deployRequest) {
        final Application app = deployRequest.getApplication();
        final String region = deployRequest.getRegion();
        final Optional<Instance> instance = app.getInstance(region);

        final String roleName = app.getRunAs();
        final Optional<Role> role = getRole(roleName);
        if (!role.isPresent()) {
            throw new KribiException(KribiException.ROLE_NOT_FOUND, "Role not found: " + roleName);
        }

        LOGGER.info("Create EC2 {} Instance(s)", instance.get().getMaxCount());
        final RunInstancesRequest request = new RunInstancesRequest();
        final IamInstanceProfileSpecification profile = new IamInstanceProfileSpecification();
        profile.setName(roleName);

        request.withImageId(context.getAMI(app).getImageId())
                .withInstanceType(instance.get().getType())
                .withMinCount(instance.get().getMinCount())
                .withMaxCount(instance.get().getMaxCount())
                .withKeyName(context.getKeyPair().getName())
                .withSecurityGroupIds(getSecurityGroups(app))
                .withIamInstanceProfile(profile)
        ;
        shutdownBehavior(deployRequest, request);

        LOGGER.info("Creating instances");
        final RunInstancesResult result = ec2.runInstances(request);
        final Reservation reservation = result.getReservation();
        return reservation;
    }

    private RunInstancesRequest shutdownBehavior(final DeployRequest deployRequest, final RunInstancesRequest request) {
        final Application app = deployRequest.getApplication();
        if (ApplicationTemplate.javaapp.equals(app.getTemplate())) {
            request.setInstanceInitiatedShutdownBehavior(ShutdownBehavior.Terminate);
        }
        return request;
    }

    private Optional<Role> getRole(final String roleName) {
        LOGGER.info("Resolving Service Role {}", roleName);
        return iam.listRoles().getRoles().stream()
                .filter(r -> r.getRoleName().equals(roleName))
                .findFirst();
    }

    private void deleteInstances(final UndeployRequest undeployRequest) {
        final String instanceName = getInstanceName(undeployRequest);
        final Optional<Reservation> reservation = findReservation(undeployRequest);
        if (!reservation.isPresent()) {
            LOGGER.info("Instances {} not found. Nothing to delete", instanceName);
            return;
        }

        delete(reservation.get());
    }

    private List<String> getSecurityGroups(final Application app) {
        final Set<String> securityGroups = new HashSet<>();
        if (app.getServices().isSsh()) {
            securityGroups.add(context.getSecurityGroups().getSsh());
        }
        if (app.getServices().isHttp()) {
            securityGroups.add(context.getSecurityGroups().getHttp());
        }
        if (app.getServices().isHttps()) {
            securityGroups.add(context.getSecurityGroups().getHttps());
        }
        return securityGroups.stream()
                .filter(sg -> sg != null)
                .collect(Collectors.toList());
    }

    private List<String> getInstanceIds(final Reservation reservation) {
        return reservation.getInstances().stream()
                .map(i -> i.getInstanceId())
                .collect(Collectors.toList());
    }

    private Optional<Reservation> findReservation(final KribiRequest request) {
        LOGGER.info("Resolving Instance(s) {}", getInstanceName(request));
        final DescribeInstancesResult result = ec2.describeInstances();
        return result.getReservations().stream()
                .filter(r -> isReservationForApp(request, r))
                .findFirst();
    }

    private boolean isReservationForApp(final KribiRequest request, final Reservation reservation) {
        final String instanceName = getInstanceName(request);

        final List<com.amazonaws.services.ec2.model.Instance> instances = reservation.getInstances();
        if (instances.isEmpty()) {
            return false;
        }

        for (final com.amazonaws.services.ec2.model.Instance instance : instances) {
            if (isTerminated(instance)) {
                continue;
            }

            final Optional<Tag> tagName = getTag(TAG_NAME, instance);
            final Optional<Tag> tagVersion = getTag(TAG_VERSION, instance);

            if (tagName.isPresent() && tagName.get().getValue().equals(instanceName)
                    && tagVersion.isPresent() && tagVersion.get().getValue().equals(request.getVersion())) {
                return true;
            }
        }
        return false;
    }

    private boolean isTerminated(final com.amazonaws.services.ec2.model.Instance instance) {
        return "terminated".equals(instance.getState().getName());
    }

    private String getInstanceName(final KribiRequest request) {
        final Application app = request.getApplication();
        final Environment env = request.getEnvironment();
        final String name = String.format("%s_%s_%s", app.getName(), env, AwsSupport.shortenVersion(request.getVersion()));
        return AwsSupport.nomalizeName(name);
    }

    private Reservation waitForReady(final Reservation reservation) {
        LOGGER.info("Waiting for the cluster to be ready");

        final List<String> instanceIds = getInstanceIds(reservation);
        final long delayMillis = DELAY_SECONDS * 1000;
        for (int i = 0; i < MAX_RETRIES; i++) {
            if (ready(instanceIds)) {

                final DescribeInstancesRequest request = new DescribeInstancesRequest()
                        .withInstanceIds(instanceIds);
                return ec2.describeInstances(request).getReservations().get(0);

            } else {
                wait(delayMillis, "... Cluster not ready yet. waiting for " + DELAY_SECONDS + " seconds");
            }
        }

        throw new KribiException(KribiException.DEPLOYMENT_TIMEOUT, "EC2 instance not ready after " + (MAX_RETRIES + DELAY_SECONDS) + " secs");
    }

    private boolean ready(final List<String> instanceIds) {
        final DescribeInstancesRequest request = new DescribeInstancesRequest()
                .withInstanceIds(instanceIds);
        final DescribeInstancesResult result = ec2.describeInstances(request);
        for (final com.amazonaws.services.ec2.model.Instance instance : result.getReservations().get(0).getInstances()) {
            final String state = instance.getState().getName();
            LOGGER.info("... Instance {} is {}", instance.getInstanceId(), state);
            if (!"running".equalsIgnoreCase(state)) {
                return false;
            }
        }
        return true;
    }

    //-- Tags
    private void tag(final DeployRequest deployRequest, final Reservation reservation) {
        final Application app = deployRequest.getApplication();
        final Environment env = deployRequest.getEnvironment();

        final List<String> instanceIds = getInstanceIds(reservation);
        LOGGER.info("Tagging {}", instanceIds);

        final List<Tag> tags = new ArrayList<>();
        addTag(TAG_NAME, getInstanceName(deployRequest), tags);
        addTag(TAG_APP_NAME, app.getName(), tags);
        addTag(TAG_VERSION, deployRequest.getVersion(), tags);
        addTag(TAG_ENVIRONMENT, env.name(), tags);
        addTag(TAG_TRANSACTION_ID, deployRequest.getTransactionId(), tags);
        addTag(TAG_TEMPLATE, app.getTemplate().name(), tags);

        final CreateTagsRequest request = new CreateTagsRequest();
        request.withResources(instanceIds)
                .withTags(tags);
        ec2.createTags(request);
    }

    private void addTag(final String name, final String value, final List<Tag> tags) {
        if (value == null || value.length() == 0) {
            return;
        }

        LOGGER.info("Tag: {}={}", name, value);
        tags.add(new Tag(name, value));
    }

    private Optional<Tag> getTag(final String name, final com.amazonaws.services.ec2.model.Instance instance) {
        return instance.getTags().stream()
                .filter(t -> t.getKey().equals(name))
                .findFirst();
    }

    boolean hasTag(final String name, final String value, final com.amazonaws.services.ec2.model.Instance instance) {
        final Optional<Tag> tag = getTag(name, instance);
        return tag.isPresent() ? value.equalsIgnoreCase(tag.get().getValue()) : false;
    }

    //-- Install
    private void install(final DeployRequest deployRequest, final Host host) {
        try {
            waitForSSH(host, deployRequest.getApplication());

            final Shell shell = createShell(host, deployRequest.getApplication());

            copyProfile(deployRequest, shell, host);
            copyScripts(deployRequest, shell, host);
            runInstaller(shell, host);
        } catch (final UnknownHostException e) {
            throw new KribiException(KribiException.UNEXPECTED_ERROR, host.getPublicIp(), e);
        }
    }

    private void copyProfile(final DeployRequest deployRequest, final Shell shell, final Host host) {
        final Application app = deployRequest.getApplication();
        final Environment env = deployRequest.getEnvironment();

        LOGGER.info("Creating file: {}@{}:~/service-profile", SSH_USERNAME, host.getPublicIp());

        final List<String> cmds = new ArrayList<>();
        cmds.add("echo SERVICE_ENVIRONMENT=" + env.name().toLowerCase() + " > service-profile");
        cmds.add("echo SERVICE_NAME=" + app.getName() + " >> service-profile");
        cmds.add("echo SERVICE_USER=" + app.getName() + " >> service-profile");
        cmds.add("echo SERVICE_VERSION=" + deployRequest.getVersion() + " >> service-profile");
        cmds.add("echo JVM_OPTS=\\\"" + app.getJvmOptions() + "\\\" >> service-profile");

        exec(String.join(";", cmds), shell);
    }

    private void copyScripts(final DeployRequest deployRequest, final Shell shell, final Host host) {
        final Application app = deployRequest.getApplication();

        LOGGER.info("Copying install scripts to {}@{}", SSH_USERNAME, host.getPublicIp());
        exec("aws s3 sync s3://io.tchepannou.kribi/installer/" + app.getTemplate() + " .", shell);
    }

    private void runInstaller(final Shell shell, final Host host) {
        LOGGER.info("Running installer {}@{}:~/install.sh", SSH_USERNAME, host.getPublicIp());

        exec("chmod +x install.sh", shell);
        exec("sudo ./install.sh", shell);
    }

    private void exec(final String cmd, final Shell shell) {
        try {
            new Shell.Plain(shell).exec(cmd);
        } catch (final IOException e) {
            throw new KribiException(KribiException.SSH_ERROR, "Unable to execute: " + cmd, e);
        }
    }

    private Shell createShell(final Host host, final Application app) throws UnknownHostException {
        final int port = app.getServices().getSshPort();
        return new SSH(host.getPublicIp(), port, SSH_USERNAME, context.getKeyPair().getPrivateKey());
    }

    private void waitForSSH(final Host host, final Application app) throws UnknownHostException {
        LOGGER.info("Waiting SSH port to be available on {}", host.getPublicIp());

        final long delayMillis = DELAY_SECONDS * 1000;
        final String waitMessage = "... SSH port not ready. waiting for " + DELAY_SECONDS + " seconds";
        final Shell shell = createShell(host, app);
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {

                new Shell.Plain(shell).exec("echo $HOSTNAME");
                return;

            } catch (final IOException e) {
                wait(delayMillis, waitMessage);
            }
        }

        throw new KribiException(KribiException.SSH_ERROR, "Unable to establish SSH connection");
    }

    private void wait(final long delayMillis, final String msg) {
        LOGGER.info(msg);
        try {
            Thread.sleep(delayMillis);
        } catch (final InterruptedException ex) {
        }
    }
}
