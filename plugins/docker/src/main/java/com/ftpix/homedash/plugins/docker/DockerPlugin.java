package com.ftpix.homedash.plugins.docker;

import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.Plugin;
import com.ftpix.homedash.plugins.docker.models.DockerInfo;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.ContainerStats;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class DockerPlugin extends Plugin {

    private static final String ENV_DOCKER_HOST = "DOCKER_HOST", DOCKER_URL = "url", ACTION_CONTAINER_DETAILS = "details", ACTION_RESTART = "restart", ACTION_START = "start", ACTION_STOP = "stop", ACTION_REMOVE = "remove", ACTION_KILL = "kill", SUCCESS_MESSAGE = "Container %sed successfully";
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

        System.out.println(url);
        if (url.startsWith("unix://")) {
            client = new DefaultDockerClient(url);
        } else {
            client = DefaultDockerClient.builder().uri(url).build();
        }

        try {
            System.out.println(client.listImages().size());
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
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
                    response.setMessage(String.format(SUCCESS_MESSAGE, command));
                    response.setExtra(getContainersInfo());
                    break;
                case ACTION_RESTART:
                    restartContainer(message);
                    response.setMessage(String.format(SUCCESS_MESSAGE, command));
                    response.setExtra(getContainersInfo());
                    break;
                case ACTION_STOP:
                    stopContainer(message);
                    response.setMessage(String.format(SUCCESS_MESSAGE, command));
                    response.setExtra(getContainersInfo());
                    break;
                case ACTION_KILL:
                    killContainer(message);
                    response.setMessage(String.format(SUCCESS_MESSAGE, command));
                    response.setExtra(getContainersInfo());
                    break;
                case ACTION_REMOVE:
                    removeContainer(message);
                    //shitty trick because remove finishes with a e and will bug with String formatting...
                    command = command.substring(0, command.length() - 1);
                    response.setMessage(String.format(SUCCESS_MESSAGE, command));
                    response.setExtra(getContainersInfo());
                    break;
                case ACTION_CONTAINER_DETAILS:
                    response.setCommand(ACTION_CONTAINER_DETAILS);
                    response.setMessage(getContainerDetails(message));
                    break;
            }
        } catch (DockerException | InterruptedException e) {
            response.setCommand(WebSocketMessage.COMMAND_ERROR);
            response.setMessage("Docker command failed: " + e.getMessage());
        }


        return response;
    }

    public static void main(String[] args) {
        DockerClient d = DefaultDockerClient.builder().uri("http://localhost:4243").build();
        try {
            System.out.println(d.listImages().size());
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    @Override
    public Object refresh(String size) throws Exception {
        System.out.print(client);
        if (!size.equalsIgnoreCase(ModuleLayout.FULL_SCREEN)) {
            return client.listContainers().size();
        } else {
            return getContainersInfo();
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

        logger.info("Testing docker container !");

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

            logger.info("Docker environment variable found {} = {}", ENV_DOCKER_HOST, dockerHost);

            return model;
        } else {
            return null;
        }
    }


    //Class methods
    private List<DockerInfo> getContainersInfo() throws DockerException, InterruptedException {
        logger.info("Getting container stats for all containers");
        List<DockerInfo> containers = client.listContainers(DockerClient.ListContainersParam.allContainers())
                .stream()
                .map(DockerInfo::new)
                .collect(Collectors.toList());


        List<Callable<Void>> statsTasks = containers.stream().map(c -> {
            return (Callable<Void>) () -> {
                ContainerStats stats = client.stats(c.id);
                c.setStats(stats);

                return null;
            };
        }).collect(Collectors.toList());

        ExecutorService exec = Executors.newFixedThreadPool(containers.size());
        try {
            logger.info("Getting all stats");
            exec.invokeAll(statsTasks);
            logger.info("All stats finish");
        }finally{
            exec.shutdown();
        }
        return containers;
    }


    private ContainerInfo getContainerDetails(String id) throws DockerException, InterruptedException {
        return client.inspectContainer(id);
    }

    private void removeContainer(String id) throws DockerException, InterruptedException {
        logger.info("Removing container #{}", id);
        client.removeContainer(id);
    }

    private void killContainer(String id) throws DockerException, InterruptedException {
        logger.info("Killing container #{}", id);
        client.killContainer(id);
    }

    private void stopContainer(String id) throws DockerException, InterruptedException {
        logger.info("Stopping container #{}", id);
        client.stopContainer(id, 0);
    }

    private void restartContainer(String id) throws DockerException, InterruptedException {
        logger.info("Restarting container #{}", id);
        client.restartContainer(id);
    }

    private void startContainer(String containerId) throws DockerException, InterruptedException {
        logger.info("Starting container #{}", containerId);
        client.startContainer(containerId);
    }
}
