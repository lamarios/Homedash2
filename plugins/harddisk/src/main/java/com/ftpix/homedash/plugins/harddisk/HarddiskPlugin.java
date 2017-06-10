package com.ftpix.homedash.plugins.harddisk;


import com.ftpix.homedash.Utils.ByteUtils;
import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.Plugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.AttributeView;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.Strings;
import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.software.os.OSFileStore;

import javax.xml.bind.DatatypeConverter;

/**
 * Created by gz on 06-Jun-16.
 */
public class HarddiskPlugin extends Plugin {
    private final static String MOUNT = "mount", COMMAND_BROWSE = "browse", COMMAND_DELETE = "delete", COMMAND_RENAME = "rename", COMMAND_MOVE = "move", COMMAND_COPY = "copy", COMMAND_NEW_FOLDER = "newFolder", COMMAND_UPLOAD_FILE = "uploadFile";
    private final static int MAX_DATA = 100;
    private SystemInfo systemInfo = new SystemInfo();
    private Path mountPoint;


    @Override
    public String getId() {
        return "harddisk";
    }

    @Override
    public String getDisplayName() {
        return "Hard Disk";
    }

    @Override
    public String getDescription() {
        return "Help you monitor the space on a mount point";
    }

    @Override
    public String getExternalLink() {
        return null;
    }

    @Override
    protected void init() {
        mountPoint = Paths.get(settings.get(MOUNT)).toAbsolutePath();
    }

    @Override
    public String[] getSizes() {
        return new String[]{"1x1", "2x1", ModuleLayout.FULL_SCREEN};
    }

    @Override
    public int getBackgroundRefreshRate() {
        return 0;
    }

    @Override
    public WebSocketMessage processCommand(String command, String message, Object extra) {
        WebSocketMessage webSocketMessage = new WebSocketMessage();
        webSocketMessage.setCommand(command);
        logger.info("Message: {}", message);
        try {
            switch (command) {
                case COMMAND_BROWSE:
                    webSocketMessage.setMessage(browse(message));
                    break;
                case COMMAND_COPY:
                    FileOperation operation = gson.fromJson(message, FileOperation.class);
                    copy(operation);
                    webSocketMessage.setCommand(WebSocketMessage.COMMAND_SUCCESS);
                    webSocketMessage.setMessage("File copied successfully");
                    webSocketMessage.setExtra(operation);
                    break;
                case COMMAND_DELETE:
                    delete(message);
                    webSocketMessage.setCommand(WebSocketMessage.COMMAND_SUCCESS);
                    webSocketMessage.setMessage("File deleted successfully");
                    break;
                case COMMAND_MOVE:
                    FileOperation fileOperation = gson.fromJson(message, FileOperation.class);
                    move(fileOperation);
                    webSocketMessage.setCommand(WebSocketMessage.COMMAND_SUCCESS);
                    webSocketMessage.setMessage("File moved successfully");
                    webSocketMessage.setExtra(fileOperation);
                    break;
                case COMMAND_RENAME:
                    rename(gson.fromJson(message, FileOperation.class));
                    webSocketMessage.setCommand(WebSocketMessage.COMMAND_SUCCESS);
                    webSocketMessage.setMessage("File renamed successfully");
                    break;
                case COMMAND_NEW_FOLDER:
                    createFolder(gson.fromJson(message, FileOperation.class));
                    webSocketMessage.setCommand(WebSocketMessage.COMMAND_SUCCESS);
                    webSocketMessage.setMessage("Folder created successfully");
                    break;
                case COMMAND_UPLOAD_FILE:
                    uploadFile(gson.fromJson(message, FileOperation.class));
                    webSocketMessage.setCommand(WebSocketMessage.COMMAND_SUCCESS);
                    webSocketMessage.setMessage("File uploaded successfully");
            }
        } catch (Exception e) {
            logger.error("Error while processing message", e);
            webSocketMessage.setCommand(WebSocketMessage.COMMAND_ERROR);
            webSocketMessage.setMessage(e.getMessage());
        }

        return webSocketMessage;
    }


    @Override
    public void doInBackground() {
    }

    @Override
    protected Object refresh(String size) throws Exception {
        File root = new File(mountPoint.toAbsolutePath().toString());

        long usedSpace = root.getTotalSpace() - root.getFreeSpace();


        Map<String, Object> spaces = new Hashtable<>();

        spaces.put("path", root.getAbsolutePath());
        spaces.put("total", Long.toString(root.getTotalSpace()));
        spaces.put("free", Long.toString(root.getFreeSpace()));
        spaces.put("used", Long.toString(usedSpace));
        spaces.put("pretty", ByteUtils.humanReadableByteCount(usedSpace, root.getTotalSpace(), true));

        return spaces;
    }

    @Override
    public int getRefreshRate(String size) {
        return ONE_MINUTE;
    }

    @Override
    public Map<String, String> validateSettings(Map<String, String> settings) {
        Map<String, String> errors = new Hashtable<>();

        if (!Files.exists(Paths.get(settings.get(MOUNT)))) {
            errors.put("Path not found", "The specified path  doesn't exist");
        } else if (!Files.isReadable(Paths.get(settings.get(MOUNT)))) {
            errors.put("Path not readable", "The specified path is not readable");
        }
        return errors;
    }

    @Override
    public ModuleExposedData exposeData() {
        ModuleExposedData data = new ModuleExposedData();

        File root = mountPoint.toFile();
        long usedSpace = root.getTotalSpace() - root.getFreeSpace();

        data.addText(root.getAbsolutePath());
        data.addText(ByteUtils.humanReadableByteCount(usedSpace, root.getTotalSpace(), true));
        return data;
    }

    @Override
    public Map<String, String> exposeSettings() {
        Map<String, String> result = new Hashtable<>();
        result.put("Path", mountPoint.toString());
        return result;
    }

    @Override
    protected void onFirstClientConnect() {

    }

    @Override
    protected void onLastClientDisconnect() {

    }

    @Override
    protected Map<String, Object> getSettingsModel() {

        return Stream.of(systemInfo.getOperatingSystem().getFileSystem().getFileStores()).filter(fs -> {
            File f = new File(fs.getMount());
            return f.exists() && f.canRead();
        }).collect(Collectors.toMap(OSFileStore::getMount, Function.identity(), (o, o2) -> o));
    }


    /**
     * Browse a specified path
     *
     * @param message the desired path to browse
     * @return the list of file we want to see
     * @throws IOException If there is any issue trying to get the list of files.
     */
    private List<DiskFile> browse(String message) throws IOException, OperationException {
        return browse(message, (diskFile, diskFile2) -> {
            if (diskFile.folder == diskFile2.folder) {
                return diskFile.name.compareTo(diskFile2.name);
            } else {
                return Boolean.compare(diskFile2.folder, diskFile.folder);
            }
        });
    }

    /**
     * Browse a specified path
     *
     * @param message the desired path to browse
     * @param order   in which order the files should be displayed
     * @return the list of file we want to see
     * @throws IOException If there is any issue trying to get the list of files.
     */
    private List<DiskFile> browse(String message, Comparator<DiskFile> order) throws IOException, OperationException {
        Path p = mountPoint.resolve(message);

        if (message.contains("..")) {
            throw new OperationException("\"..\" not authorized in file path");
        }
        return Files.list(p).map(path -> {
            DiskFile file = new DiskFile();
            file.name = path.getFileName().toString();
            file.folder = Files.isDirectory(path);

            return file;
        }).sorted(order).collect(Collectors.toList());

    }

    /**
     * Move a file.
     *
     * @param fileOperation the file operation containing source and destination
     */
    private void rename(FileOperation fileOperation) throws IOException, OperationException {
        Path source = mountPoint.resolve(fileOperation.getSource());
        Path destination = mountPoint.resolve(fileOperation.getDestination());

        if (source.toString().contains("..") || destination.toString().contains("..")) {
            throw new OperationException("\"..\" not authorized in file path");
        }

        if (Files.exists(destination)) {
            throw new OperationException(destination.toString() + " already exists");
        }

        logger.info("Attempting to rename {} to {}", source.toString(), destination.toString());

        //if it is a folder, Files.copy will only create an empty folder so we need to copy the files inside as well.
        if (Files.isDirectory(source)) {
            moveFolder(source, destination);
        }
        Files.move(source, destination);
    }

    /**
     * Move a file.
     *
     * @param fileOperation the file operation containing source and destination
     */
    private void move(FileOperation fileOperation) throws IOException, OperationException {
        Path source = mountPoint.resolve(fileOperation.getSource());
        Path destination = mountPoint.resolve(fileOperation.getDestination());

        if (source.toString().contains("..") || destination.toString().contains("..")) {
            throw new OperationException("\"..\" not authorized in file path");
        }
        //checking if we can do the operation
        if (Files.isSameFile(source, destination)) {
            throw new OperationException("Source and destination are the same");
        }

        if (!Files.isDirectory(destination)) {
            throw new OperationException(destination.toString() + " is not a folder");
        }

        if (!Files.isWritable(destination)) {
            throw new OperationException(destination.toString() + " is not writable");
        }


        destination = destination.resolve(source.getFileName());

        if (Files.exists(destination)) {
            throw new OperationException(destination.toString() + " already exists");
        }

        logger.info("Attempting to move {} to {}", source.toString(), destination.toString());

        //if it is a folder, Files.copy will only create an empty folder so we need to copy the files inside as well.
        if (Files.isDirectory(source)) {
            moveFolder(source, destination);
        }
        Files.move(source, destination);
    }

    /**
     * Delete a file.
     *
     * @param fileName name of the file to delete
     */
    private void delete(String fileName) throws OperationException, IOException {

        Path source = mountPoint.resolve(fileName);


        if (source.toString().contains("..")) {
            throw new OperationException("\"..\" not authorized in file path");
        }

        if (Files.isDirectory(source)) {
            clearFolder(source);
        } else {
            Files.delete(source);
        }

    }


    /**
     * Copy a file.
     *
     * @param fileOperation the file operation containing source and destination
     */
    private void copy(FileOperation fileOperation) throws OperationException, IOException {

        Path source = mountPoint.resolve(fileOperation.getSource());
        Path destination = mountPoint.resolve(fileOperation.getDestination());

        if (source.toString().contains("..") || destination.toString().contains("..")) {
            throw new OperationException("\"..\" not authorized in file path");
        }

        //checking if we can do the operation
        if (Files.isSameFile(source, destination)) {
            throw new OperationException("Source and destination are the same");
        }

        if (!Files.isDirectory(destination)) {
            throw new OperationException(destination.toString() + " is not a folder");
        }

        if (!Files.isWritable(destination)) {
            throw new OperationException(destination.toString() + " is not writable");
        }


        destination = destination.resolve(source.getFileName());

        if (Files.exists(destination)) {
            throw new OperationException(destination.toString() + " already exists");
        }

        logger.info("Attempting to copy {} to {}", source.toString(), destination.toString());
        Files.copy(source, destination);

        //if it is a folder, Files.copy will only create an empty folder so we need to copy the files inside as well.
        if (Files.isDirectory(source)) {
            copyFolder(source, destination);
        }
    }

    /**
     * Copies a folder to a specified path
     *
     * @param source      the folder to copy
     * @param destination where to copy it
     * @throws IOException
     */
    private void copyFolder(Path source, Path destination) throws IOException {
        Files.list(source).forEach(path -> {
            try {
                logger.info("Copying [" + path.toString() + "] to [" + destination.resolve(path.getFileName()).toString() + "]");
                Files.copy(path, destination.resolve(path.getFileName()));

                if (Files.isDirectory(path)) {
                    copyFolder(path, destination.resolve(path.getFileName()));
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        });
    }

    /**
     * Moves a folder to a specified path
     *
     * @param source      the folder to move
     * @param destination where to copy it
     * @throws IOException
     */
    private void moveFolder(Path source, Path destination) throws IOException {
        Files.list(source).forEach(path -> {
            try {
                logger.info("Copying [" + path.toString() + "] to [" + destination.resolve(path.getFileName()).toString() + "]");

                if (Files.isDirectory(path)) {
                    moveFolder(path, destination.resolve(path.getFileName()));
                }
                Files.copy(path, destination.resolve(path.getFileName()));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        });
    }

    /**
     * Delete a folder and its content
     *
     * @param folder which folder
     * @throws IOException what happens if a file doesn't exist or can't be deleted
     */
    private void clearFolder(Path folder) throws IOException {
        Files.list(folder).forEach(path -> {
            try {
                logger.info("Deleting:" + path.toString());
                if (Files.isDirectory(path)) {
                    clearFolder(path);
                } else {
                    Files.deleteIfExists(path);
                }


            } catch (IOException e) {
                logger.info("Error while deleteing" + path.toString());
                e.printStackTrace();
                return;
            }

        });

        Files.deleteIfExists(folder);
    }

    /**
     * Creates a new folder
     *
     * @param fileOperation the source if the parent folder of the new folder, destination is the name of the folder
     */
    private void createFolder(FileOperation fileOperation) throws IOException {

        Path p = mountPoint.resolve(fileOperation.getSource());

        if (!fileOperation.getDestination().contains("..") && !fileOperation.getDestination().contains("/") && !fileOperation.getDestination().contains("\\")) {
            Path newFolder = p.resolve(fileOperation.getDestination());
            Files.createDirectory(newFolder);
        } else {
            new OperationException("The folder name contains invalid characters");
        }
    }

    /**
     * Uploads a file
     *
     * @param fileOperation source will be the complete path of the file, destination will be the base64 encoding of the file.
     */
    private void uploadFile(FileOperation fileOperation) throws IOException {
        String partSeparator = ",";
        if(fileOperation.getDestination().contains(partSeparator)) {
            Path fullPath = mountPoint.resolve(fileOperation.getSource());
            byte[] base64 = java.util.Base64.getDecoder().decode(fileOperation.getDestination().split(partSeparator)[1].getBytes(StandardCharsets.UTF_8));
            logger.info("Creating file: [{}]", fullPath);
            try (BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(fullPath.toFile()))) {
                stream.write(base64);
            }

            logger.info("File written");
        }
    }
}
