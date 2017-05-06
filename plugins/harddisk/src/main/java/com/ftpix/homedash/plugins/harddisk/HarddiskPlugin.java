package com.ftpix.homedash.plugins.harddisk;


import com.ftpix.homedash.Utils.ByteUtils;
import com.ftpix.homedash.models.ModuleExposedData;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.AttributeView;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.Strings;
import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.software.os.OSFileStore;

/**
 * Created by gz on 06-Jun-16.
 */
public class HarddiskPlugin extends Plugin {
    private final static String MOUNT = "mount", COMMAND_BROWSE = "browse", COMMAND_DELETE = "delete", COMMAND_RENAME = "rename", COMMAND_MOVE = "move", COMMAND_COPY = "copy";
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
        try {
            switch (command) {
                case COMMAND_BROWSE:
                    webSocketMessage.setMessage(browse(message));
                    break;
                case COMMAND_COPY:
                    copy(gson.fromJson(message, FileOperation.class));
                    webSocketMessage.setCommand(WebSocketMessage.COMMAND_SUCCESS);
                    webSocketMessage.setMessage("File copied successfully");
                    break;
                case COMMAND_DELETE:
                    delete(message);
                    webSocketMessage.setCommand(WebSocketMessage.COMMAND_SUCCESS);
                    webSocketMessage.setMessage("File deleted successfully");
                    break;
                case COMMAND_MOVE:
                    move(gson.fromJson(message, FileOperation.class));
                    webSocketMessage.setCommand(WebSocketMessage.COMMAND_SUCCESS);
                    webSocketMessage.setMessage("File moved successfully");
                    break;
                case COMMAND_RENAME:
                    move(gson.fromJson(message, FileOperation.class));
                    webSocketMessage.setCommand(WebSocketMessage.COMMAND_SUCCESS);
                    webSocketMessage.setMessage("File renamed successfully");
                    break;
            }
        } catch (Exception e) {
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
    protected Map<String, Object> getSettingsModel() {

        return Stream.of(systemInfo.getOperatingSystem().getFileSystem().getFileStores()).collect(Collectors.toMap(OSFileStore::getMount, Function.identity(), (o, o2) -> o));
    }


    /**
     * Browse a specified path
     *
     * @param message the desired path to browse
     * @return the list of file we want to see
     * @throws IOException If there is any issue trying to get the list of files.
     */
    private List<DiskFile> browse(String message) throws IOException {
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
    private List<DiskFile> browse(String message, Comparator<DiskFile> order) throws IOException {
        Path p = mountPoint.resolve(message);

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
    private void move(FileOperation fileOperation) {
    }

    /**
     * Delete a file.
     *
     * @param fileName name of the file to delete
     */
    private void delete(String fileName) throws OperationException, IOException {

        Path source = mountPoint.resolve(fileName);



        if (!Files.isWritable(source)) {
            throw new OperationException(source.toString() + " is not writable");
        }

        if(Files.isDirectory(source)){
            clearFolder(source);
        }

        Files.delete(source);
    }


    /**
     * Copy a file.
     *
     * @param fileOperation the file operation containing source and destination
     */
    private void copy(FileOperation fileOperation) throws OperationException, IOException {
        Path source = mountPoint.resolve(fileOperation.source);
        Path destination = mountPoint.resolve(fileOperation.destination);

        //checking if we can do the operation
        if (Files.isSameFile(source, destination)) {
            throw new OperationException("Source and destination are the same");
        }

        if (!Files.isDirectory(source)) {
            throw new OperationException(destination.toString() + " is not a folder");
        }

        if (!Files.isWritable(destination)) {
            throw new OperationException(destination.toString() + " is not writable");
        }


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
}
