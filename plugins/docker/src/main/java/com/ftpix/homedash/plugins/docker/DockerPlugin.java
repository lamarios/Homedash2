package com.ftpix.homedash.plugins.docker;

import com.ftpix.homedash.Utils.ByteUtils;
import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.Plugin;
import com.ftpix.homedash.plugins.docker.models.DockerInfo;
import com.ftpix.homedash.plugins.docker.models.DockerImageInfo;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.ContainerStats;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class DockerPlugin extends Plugin {

    private static final String ENV_DOCKER_HOST = "DOCKER_HOST", DOCKER_URL = "url", ACTION_REMOVE_IMAGE = "removeImage",
            ACTION_REMOVE_IMAGE_FORCE = "removeImageForce", ACTION_CONTAINER_DETAILS = "details",
            ACTION_RESTART = "restart", ACTION_START = "start", ACTION_STOP = "stop", ACTION_REMOVE = "remove",
            ACTION_KILL = "kill", SUCCESS_MESSAGE_CONTAINER = "Container %sed successfully",
            SUCCESS_MESSAGE_IMAGE = "Image %sed successfully";

    private DockerClient client;


    public DockerPlugin() {
    }

    public DockerPlugin(Module module) {
        super(module);
    }

    @Override
    public String getId() {
        return "docker";
    }

    @Override
    public String getDisplayName() {
        return "Docker";
    }

    @Override
    public String getDescription() {
        return "Control your docker containers with this module";
    }

    @Override
    public String getExternalLink() {
        return null;
    }

    @Override
    protected void init() {

        String url = settings.get(DOCKER_URL);

        if (url.startsWith("unix://")) {
            client = new DefaultDockerClient(url);
        } else {
            client = DefaultDockerClient.builder().uri(url).build();
        }

    }

    @Override
    public String[] getSizes() {
        return new String[]{ModuleLayout.SIZE_1x1, ModuleLayout.FULL_SCREEN};
    }

    @Override
    public int getBackgroundRefreshRate() {
        return NEVER;
    }

    @Override
    public void doInBackground() {
    }

    @Override
    public WebSocketMessage processCommand(String command, String message, Object extra) {
        WebSocketMessage response = new WebSocketMessage();
        response.setCommand(WebSocketMessage.COMMAND_SUCCESS);

        try {
            switch (command) {
                case ACTION_START:
                    startContainer(message);
                    response.setMessage(String.format(SUCCESS_MESSAGE_CONTAINER, command));
                    response.setExtra(refresh(ModuleLayout.FULL_SCREEN));
                    break;
                case ACTION_RESTART:
                    restartContainer(message);
                    response.setMessage(String.format(SUCCESS_MESSAGE_CONTAINER, command));
                    response.setExtra(refresh(ModuleLayout.FULL_SCREEN));
                    break;
                case ACTION_STOP:
                    stopContainer(message);
                    response.setMessage(String.format(SUCCESS_MESSAGE_CONTAINER, command));
                    response.setExtra(refresh(ModuleLayout.FULL_SCREEN));
                    break;
                case ACTION_KILL:
                    killContainer(message);
                    response.setMessage(String.format(SUCCESS_MESSAGE_CONTAINER, command));
                    response.setExtra(refresh(ModuleLayout.FULL_SCREEN));
                    break;
                case ACTION_REMOVE:
                    removeContainer(message);
                    //shitty trick because remove finishes with a e and will bug with String formatting...
                    command = command.substring(0, command.length() - 1);
                    response.setMessage(String.format(SUCCESS_MESSAGE_CONTAINER, command));
                    response.setExtra(refresh(ModuleLayout.FULL_SCREEN));
                    break;
                case ACTION_CONTAINER_DETAILS:
                    response.setCommand(ACTION_CONTAINER_DETAILS);
                    response.setMessage(getContainerDetails(message));
                    break;

                case ACTION_REMOVE_IMAGE:
                    client.removeImage(message);
                    response.setMessage(String.format(SUCCESS_MESSAGE_IMAGE, ACTION_REMOVE));
                    response.setExtra(refresh(ModuleLayout.FULL_SCREEN));
                    break;
                case ACTION_REMOVE_IMAGE_FORCE:
                    client.removeImage(message, true, true);
                    response.setMessage(String.format(SUCCESS_MESSAGE_IMAGE, ACTION_REMOVE));
                    response.setExtra(refresh(ModuleLayout.FULL_SCREEN));
                    break;
            }
        } catch (Exception e) {
            response.setCommand(WebSocketMessage.COMMAND_ERROR);
            response.setMessage("Docker command failed: " + e.getMessage());
        }

        return response;
    }


    @Override
    public Object refresh(String size) throws Exception {
        if (!size.equalsIgnoreCase(ModuleLayout.FULL_SCREEN)) {
            return client.listContainers().size();
        } else {

            List<DockerInfo> containers = getContainersInfo();
            Map<String, Object> data = new HashMap<>();
            data.put("containers", containers);
            data.put("images", getImagesInfo(containers));
            return data;
        }
    }

    @Override
    public int getRefreshRate(String size) {
        // TODO Auto-generated method stub
        if (size.equalsIgnoreCase(ModuleLayout.FULL_SCREEN)) {
            return ONE_MINUTE;
        } else {
            return ONE_MINUTE * 10;
        }
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        Map<String, String> errors = new HashMap<>();
        String url = settings.get(DOCKER_URL);

        logger().info("Testing docker container for url {}", url);

        try {
            DockerClient testClient;
            if (url.startsWith("unix://")) {
                testClient = new DefaultDockerClient(url);
            } else {
                testClient = DefaultDockerClient.builder().uri(settings.get(DOCKER_URL)).build();
            }
            testClient.ping();
        } catch (DockerException | InterruptedException | NullPointerException e) {
            errors.put("Connection error", "Impossible to connect to " + settings.get(DOCKER_URL));
        }

        return errors;

    }

    @Override
    public ModuleExposedData exposeData() {

        try {
            ModuleExposedData data = new ModuleExposedData();
            data.addText(client.listContainers().size() + " containers running.");
            return data;
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Map<String, String> exposeSettings() {
        return settings;
    }

    @Override
    protected void onFirstClientConnect() {

    }

    @Override
    protected void onLastClientDisconnect() {

    }

    @Override
    protected Map<String, Object> getSettingsModel() {
        String dockerHost = System.getenv(ENV_DOCKER_HOST);
        if (dockerHost != null && dockerHost.length() > 0) {

            Map<String, Object> model = new HashMap<>();
            model.put(ENV_DOCKER_HOST, dockerHost);

            logger().info("Docker environment variable found {} = {}", ENV_DOCKER_HOST, dockerHost);

            return model;
        } else {
            return null;
        }
    }


    /**
     * List all the images available
     *
     * @return
     * @throws DockerException
     * @throws InterruptedException
     */
    private List<DockerImageInfo> getImagesInfo(List<DockerInfo> containers) throws DockerException, InterruptedException {

        return client.listImages()
                .stream()
                .map(i -> {
                    DockerImageInfo info = new DockerImageInfo();
                    info.setId(i.id());
                    info.setTag(Optional.ofNullable(i.repoTags()).map(tags -> tags.stream().collect(Collectors.joining(", "))).orElse(""));
                    info.setSize(ByteUtils.humanReadableByteCount(i.size(), true));
                    ZonedDateTime created = Instant.ofEpochSecond(Long.parseLong(i.created())).atZone(ZoneId.systemDefault());
                    info.setCreated(created.format(DateTimeFormatter.RFC_1123_DATE_TIME));

                    String usedBy = containers
                            .stream()
                            .filter(c -> c.imageId.equalsIgnoreCase(i.id()))
                            .map(c -> c.names.stream().collect(Collectors.joining(", ")))
                            .collect(Collectors.joining(", "));

                    info.setUsedBy(usedBy);

                    return info;
                })
                .collect(Collectors.toList());
    }

    /**
     * Gets the list of all the containers with detailed information about them
     *
     * @return
     * @throws DockerException
     * @throws InterruptedException
     */
    private List<DockerInfo> getContainersInfo() throws DockerException, InterruptedException {
        logger().info("Getting container stats for all containers");
        List<DockerInfo> containers = client.listContainers(DockerClient.ListContainersParam.allContainers())
                .stream()
                .map(DockerInfo::new)
                .collect(Collectors.toList());


        List<Callable<Void>> statsTasks = containers.stream().map(c -> {
            return (Callable<Void>) () -> {
                try {
                    ContainerStats stats = client.stats(c.id);
                    c.setStats(stats);
                } catch (Exception e) {
                    logger().info("Error while setting stats", e);
                }
                return null;
            };
        }).collect(Collectors.toList());

        ExecutorService exec = Executors.newFixedThreadPool(containers.size());
        try {
            logger().info("Getting all stats");
            exec.invokeAll(statsTasks);
            logger().info("All stats finish");
        } finally {
            exec.shutdown();
        }
        return containers;
    }


    private ContainerInfo getContainerDetails(String id) throws DockerException, InterruptedException {
        return client.inspectContainer(id);

    }

    private void removeContainer(String id) throws DockerException, InterruptedException {
        logger().info("Removing container #{}", id);
        client.removeContainer(id);
    }

    private void killContainer(String id) throws DockerException, InterruptedException {
        logger().info("Killing container #{}", id);
        client.killContainer(id);
    }

    private void stopContainer(String id) throws DockerException, InterruptedException {
        logger().info("Stopping container #{}", id);
        client.stopContainer(id, 0);
    }

    private void restartContainer(String id) throws DockerException, InterruptedException {
        logger().info("Restarting container #{}", id);
        client.restartContainer(id);
    }

    private void startContainer(String containerId) throws DockerException, InterruptedException {
        logger().info("Starting container #{}", containerId);
        client.startContainer(containerId);
    }
}
