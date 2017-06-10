package com.ftpix.homedash.plugins.dockercompose;

import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.Plugin;
import com.ftpix.homedash.plugins.dockercompose.exceptions.CommandException;
import com.ftpix.homedash.plugins.dockercompose.models.CommandOutput;
import com.ftpix.homedash.plugins.dockercompose.models.DockerContainer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by gz on 4/22/17.
 */
public class DockerComposePlugin extends Plugin {
    public static final String SETTINGS_PATH = "path", DOCKER_COMPOSE_FILE_NAME = "docker-compose.yml", DOCKER_COMPOSE = "docker-compose", PS = "ps", COMMAND_COMPOSE_FILE = "compose-file", COMMAND_SAVE_COMPOSE = "save-compose", COMMAND_CMD = "cmd";

    private Path dockerComposeFolder, dockerComposeFile;

    @Override
    public String getId() {
        return "dockercompose";
    }

    @Override
    public String getDisplayName() {
        return "Docker Compose";
    }

    @Override
    public String getDescription() {
        return "Help you quickly manage a docker compose set up";
    }

    @Override
    public String getExternalLink() {
        return null;
    }

    @Override
    protected void init() {
        dockerComposeFolder = Paths.get(settings.get(SETTINGS_PATH)).toAbsolutePath();
        dockerComposeFile = dockerComposeFolder.resolve(DOCKER_COMPOSE_FILE_NAME).toAbsolutePath();
        logger.info("Docker compose initiated with compose file :[{}]", dockerComposeFile.toString());
    }

    @Override
    public String[] getSizes() {
        return new String[]{ModuleLayout.SIZE_1x1, ModuleLayout.SIZE_2x1, ModuleLayout.FULL_SCREEN};
    }

    @Override
    public int getBackgroundRefreshRate() {
        return 0;
    }

    @Override
    protected WebSocketMessage processCommand(String command, String message, Object extra) {
        WebSocketMessage response = new WebSocketMessage();
        response.setCommand(command);
        switch (command) {
            case COMMAND_COMPOSE_FILE:
                try {
                    response.setMessage(getComposeFileContent());
                } catch (IOException e) {
                    response.setMessage(e.getMessage());
                    response.setCommand(WebSocketMessage.COMMAND_ERROR);
                }
                break;
            case COMMAND_SAVE_COMPOSE:
                try {
                    saveComposeFile(message);
                    response.setCommand(WebSocketMessage.COMMAND_SUCCESS);
                    response.setMessage("docker-compose file saved successfully");
                } catch (IOException e) {
                    response.setMessage(e.getMessage());
                    response.setCommand(WebSocketMessage.COMMAND_ERROR);
                }
                break;
            case COMMAND_CMD:
                try {
                    response.setMessage(runCommand(message));
                } catch (IOException | InterruptedException e) {
                    response.setMessage(e.getMessage());
                    response.setCommand(WebSocketMessage.COMMAND_ERROR);
                }

                break;
        }

        return response;
    }


    @Override
    public void doInBackground() {

    }

    @Override
    protected Object refresh(String size) throws Exception {
        if (size.equals(ModuleLayout.SIZE_1x1) || size.equals(ModuleLayout.SIZE_2x1)) {
            Map<String, String> result = new HashMap<>();
            result.put("count", Long.toString(countContainers()));
            result.put("folder", dockerComposeFolder.toString());

            return result;
        } else {
            return getContainers();
        }

    }

    @Override
    public int getRefreshRate(String size) {
        if (size.equals(ModuleLayout.SIZE_1x1) || size.equals(ModuleLayout.SIZE_2x1)) {
            return ONE_MINUTE * 5;
        } else {//full screen
            return ONE_SECOND * 10;
        }
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        Map<String, String> errors = new HashMap<>();

        String pathString = settings.get(SETTINGS_PATH);

        if (pathString.endsWith(DOCKER_COMPOSE_FILE_NAME)) {
            pathString = pathString.replace(DOCKER_COMPOSE_FILE_NAME, "");
        }

        logger.info("Checking path [{}] for docker compose file", pathString);

        Path path = Paths.get(pathString).toAbsolutePath().resolve(DOCKER_COMPOSE_FILE_NAME);
        if (!Files.exists(path)) {
            errors.put("Wrong path", "Unable to find file: " + path.toString());
        }

        return errors;
    }

    @Override
    public ModuleExposedData exposeData() {
        return null;
    }

    @Override
    public Map<String, String> exposeSettings() {
        return null;
    }

    @Override
    protected void onFirstClientConnect() {

    }

    @Override
    protected void onLastClientDisconnect() {

    }

    @Override
    protected Map<String, Object> getSettingsModel() {
        return null;
    }


    //Inner methods
    private long countContainers() throws IOException, InterruptedException {
        List<String> output = executeCommand(new ProcessBuilder(DOCKER_COMPOSE, PS)).getOutput();

        //Skipping the first 2 lines are they are just formatting strings
        return output.stream().skip(2).count();
    }

    /**
     * Reads from docker-compose ps to get a list of containers
     *
     * @return the list of containers
     * @throws IOException if the command fails
     */
    private List<DockerContainer> getContainers() throws IOException, CommandException, InterruptedException {
        CommandOutput output = executeCommand(new ProcessBuilder(DOCKER_COMPOSE, PS));
        if (output.getReturnCode() == CommandOutput.SUCCESS) {
            return output.getOutput()
                    .stream()
                    .skip(2)
                    .map(s -> s.split("\\s{2,}"))
                    .map(split -> {
                        DockerContainer container = new DockerContainer();
                        container.setName(split[0]);
                        container.setCommand(split[1]);
                        container.setState(split[2]);

                        //ports might not always be available
                        if (split.length > 3) {
                            container.setPorts(split[3]);
                        }

                        return container;
                    }).collect(Collectors.toList());
        } else {
            throw new CommandException(output.getErrorOutput().stream().collect(Collectors.joining("\n")));
        }

    }


    /**
     * Gets the content of the docker compose file
     *
     * @return
     */
    private String getComposeFileContent() throws IOException {
        return new String(Files.readAllBytes(dockerComposeFile), StandardCharsets.UTF_8);
    }


    /**
     * Files the docker compose file
     *
     * @param message the content to save
     */
    private void saveComposeFile(String message) throws IOException {

        Path backup = dockerComposeFolder.resolve(DOCKER_COMPOSE_FILE_NAME + ".bak");
        //backuping the current file in case anything wrong happens
        Files.move(dockerComposeFile, backup);

        FileUtils.write(dockerComposeFile.toFile(), message);

        Files.delete(backup);
    }


    /**
     * Execute a docker-compose command
     *
     * @param cmd
     */
    private CommandOutput runCommand(String cmd) throws IOException, InterruptedException {
        List<String> commands = new ArrayList<>();
        commands.add(DOCKER_COMPOSE);
        commands.addAll(Arrays.asList(cmd.split("\\s+")));

        CommandOutput output = executeCommand(new ProcessBuilder(commands.toArray(new String[commands.size()])));

        return output;
    }

    /**
     * This plugin will use a lot of process commands so it simplifies out life instead of redoing it all the time.
     *
     * @param pb A process builder with the command to execute
     * @return the string of the command output if everything goes well
     * @throws IOException when the command fails to be executed, if the process returns a failure error then the output will be blank and won't thro exception
     */
    private CommandOutput executeCommand(ProcessBuilder pb) throws IOException, InterruptedException {

        //changing directory to docker compose location,  safety instead of repeating this everywhere and maybe forget.

        pb = pb.directory(new File(dockerComposeFolder.toString()));

        final Process process = pb.start();

        CommandOutput output = new CommandOutput();

        List<String> outputStr = new ArrayList<>();
        List<String> errorOutput = new ArrayList<>();
        logger.info("Executing command: {}", pb.command().stream().collect(Collectors.joining(" ")));


        try (InputStream is = process.getInputStream();
             InputStreamReader isr = new InputStreamReader(is);
             BufferedReader br = new BufferedReader(isr);

             InputStream eis = process.getErrorStream();
             InputStreamReader eisr = new InputStreamReader(eis);
             BufferedReader ebr = new BufferedReader(eisr);
        ) {
            String line;
            while ((line = br.readLine()) != null) {
                outputStr.add(line);
            }

            while ((line = ebr.readLine()) != null) {
                errorOutput.add(line);
            }
        }



        output.setErrorOutput(errorOutput);
        output.setOutput(outputStr);

        process.waitFor(5, TimeUnit.MINUTES);//timeout after 5 minutes

        output.setReturnCode(process.exitValue());

        logger.info("Command finished with code [{}]", output.getReturnCode());
        return output;
    }
}
