package com.ftpix.homedash.updater;

import com.ftpix.homedash.updater.exceptions.WrongInstallPathStructure;
import com.ftpix.homedash.updater.exceptions.WrongVersionPatternException;
import edu.emory.mathcs.backport.java.util.Collections;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by gz on 3/26/17.
 */
public class Updater {
  private final static Logger logger = LogManager.getLogger();
  private final static int INDEX_DOWNLOAD_PATH = 0, INDEX_INSTALL_PATH = 1, ERROR_ARGUMENTS = 100, ERROR_WRONG_PATHS_GIVEN = 101, ERROR_INSTALL_PATH_WRONG_STRUCTURE = 102;
  private final static String PATH_STARTUP_SH = "homedash.sh", PATH_STARTUP_BAT = "homedash.bat", PATH_CPAPPEND = "cpappend.bat", PATH_BIN = "bin", PATH_CFG = "cfg", PATH_PLUGINS = "plugins", PATH_LIB = "lib", PATH_BACKUP_EXTENSION = ".bak";
  private final static String VERSION_PATTERN = "(\\d+)\\.(\\d+)\\.(\\d+)";
  private final String REPO_NAME = "lamarios/Homedash2";
  private final String currentVersion;
  private GHRepository repository;

  public Updater() {
    ResourceBundle rs = ResourceBundle.getBundle("version");

    currentVersion = rs.getString("version");

    try {
      repository = GitHub.connectAnonymously().getRepository(REPO_NAME);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * auto updating process
   */
  public static void main(String[] args) throws IOException, ZipException {
    if (args.length == 2) {
      Updater updater = new Updater();

      GHRelease latestVersion = updater.getLatestVersion();
      logger.info("Updating homedash to version:" + latestVersion.getName());

      String downloadPath = args[INDEX_DOWNLOAD_PATH];
      String installPath = args[INDEX_INSTALL_PATH];

      try {
        updater.doUpdate(downloadPath, installPath);
        System.exit(0);
      } catch (FileNotFoundException e) {
        logger.info("One or both the path given don't exist");
        logger.info("Download Path:" + downloadPath);
        logger.info("Install Path:" + installPath);
        System.exit(ERROR_WRONG_PATHS_GIVEN);
      } catch (WrongInstallPathStructure wrongInstallPathStructure) {
        logger.info("The install path structure doesn't match what is expected");
        System.exit(ERROR_INSTALL_PATH_WRONG_STRUCTURE);
      }

    } else {
      logger.info("Missing arguments, arguments given:" + Stream.of(args).collect(Collectors.joining("],[", "[", "]")));
      logger.info("Expected: [downloadPath], [installPath]");
      System.exit(ERROR_ARGUMENTS);
    }

  }

  public String getCurrentVersion() {
    return currentVersion;
  }

  /**
   * This method will stop homedash and trigger the update
   *
   * @param downloadPath
   * @throws IOException
   */
  public void stopHomedashAndTriggerUpdate(Path downloadPath) throws IOException {
    if (isFolderStructureOkForAutoUpdate()) {
      Path pidFile = Paths.get("homedash.pid").toAbsolutePath();
      Path binFolder = downloadPath.resolve("bin").toAbsolutePath();
      //Path autoUpdaterJar = Paths.get("/tmp/homedash-update5156811059531871535/web-1.0.3/bin/updater-" + getLatestVersion().getName() + "-jar-with-dependencies.jar");
      Files.list(binFolder).filter(p -> p.getFileName().toString().startsWith("updater-")).findFirst().map(Path::toAbsolutePath).ifPresent(autoUpdaterJar -> {

        try {
          logger.info("PID file :[{}]", pidFile.toString());
          logger.info("Update jar [{}]", autoUpdaterJar.toString());

          //we need to delete the pid file, start the auto updater and exit
          if (Files.notExists(pidFile))
            throw new FileNotFoundException("File " + pidFile.toAbsolutePath().toString() + " not found");

          if (Files.notExists(autoUpdaterJar))
            throw new FileNotFoundException("File " + autoUpdaterJar.toAbsolutePath().toString() + " not found");


          Files.deleteIfExists(pidFile);


          //Building the jar command with all the necessary parameters
          ProcessBuilder pb = new ProcessBuilder("java", "-jar", autoUpdaterJar.toString(), downloadPath.toString(), Paths.get(".").toAbsolutePath().toString());
          pb.directory(downloadPath.toAbsolutePath().toFile());
          pb.start();

          //when the process is starting, we need to exist as the updater is going to start homedash again
          System.exit(0);
        } catch (Exception e) {
          logger.error("Error while updating ", e);
        }
      });
    }
  }

  /**
   * Starts homedash on a linux system.
   *
   * @param installFolder where did the installation happned
   */
  private void startHomedashLinux(Path installFolder) throws IOException {
    logger.info("Starting homedash...");
    if (Files.exists(installFolder.resolve(PATH_STARTUP_SH))) {
      try {
        Files.setPosixFilePermissions(installFolder.resolve(PATH_STARTUP_SH), PosixFilePermissions.fromString("rwxr--r--"));
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", installFolder.resolve(PATH_STARTUP_SH) + " start");
        pb.directory(installFolder.toAbsolutePath().toFile());

        pb.start();

      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      logger.info("Can't find startup script");
    }
  }


  /**
   * Does the actual update
   *
   * @param downloadPath where the new release has been downloaded
   * @param installPath  where to install the new release
   */
  private void doUpdate(String downloadPath, String installPath) throws IOException, WrongInstallPathStructure {
    Path downloadFolder = Paths.get(downloadPath).toAbsolutePath();
    Path installFolder = Paths.get(installPath).toAbsolutePath();

    if (downloadFolder.toFile().exists() && installFolder.toFile().exists()) {
      if (isFolderStructureOkForAutoUpdate(installFolder.toFile().getAbsolutePath())) {
        backupInstall(installFolder);
        installNewVersion(downloadFolder, installFolder);
        cleanBackup(installFolder);
        startHomedashLinux(installFolder);
      } else {
        throw new WrongInstallPathStructure();
      }
    } else {
      throw new FileNotFoundException();
    }

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
   * Install the ne wversion of homedash to the specified folder
   *
   * @param downloadFolder Where to get the upate from
   * @param installFolder  where to install the update to
   */
  private void installNewVersion(Path downloadFolder, Path installFolder) throws IOException {
    logger.info("Copying [" + downloadFolder.resolve(PATH_BIN).toString() + "] to [" + installFolder.resolve(PATH_BIN).toString() + "]");
    Files.copy(downloadFolder.resolve(PATH_BIN), installFolder.resolve(PATH_BIN));
    copyFolder(downloadFolder.resolve(PATH_BIN), installFolder.resolve(PATH_BIN));

    logger.info("Copying [" + downloadFolder.resolve(PATH_PLUGINS).toString() + "] to [" + installFolder.resolve(PATH_PLUGINS).toString() + "]");
    Files.copy(downloadFolder.resolve(PATH_PLUGINS), installFolder.resolve(PATH_PLUGINS));
    copyFolder(downloadFolder.resolve(PATH_PLUGINS), installFolder.resolve(PATH_PLUGINS));


    logger.info("Copying [" + downloadFolder.resolve(PATH_LIB).toString() + "] to [" + installFolder.resolve(PATH_LIB).toString() + "]");
    Files.copy(downloadFolder.resolve(PATH_LIB), installFolder.resolve(PATH_LIB));
    copyFolder(downloadFolder.resolve(PATH_LIB), installFolder.resolve(PATH_LIB));


    logger.info("Copying [" + downloadFolder.resolve(PATH_STARTUP_BAT).toString() + "] to [" + installFolder.resolve(PATH_STARTUP_BAT).toString() + "]");
    Files.copy(downloadFolder.resolve(PATH_STARTUP_BAT), installFolder.resolve(PATH_STARTUP_BAT));


    logger.info("Copying [" + downloadFolder.resolve(PATH_STARTUP_SH).toString() + "] to [" + installFolder.resolve(PATH_STARTUP_SH).toString() + "]");
    Files.copy(downloadFolder.resolve(PATH_STARTUP_SH), installFolder.resolve(PATH_STARTUP_SH));


    logger.info("Copying [" + downloadFolder.resolve(PATH_CPAPPEND).toString() + "] to [" + installFolder.resolve(PATH_CPAPPEND).toString() + "]");
    Files.copy(downloadFolder.resolve(PATH_CPAPPEND), installFolder.resolve(PATH_CPAPPEND));

  }

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
   * Cleans the backed up files and folder
   */
  private void cleanBackup(Path installFolder) throws IOException {
    clearFolder(installFolder.resolve(PATH_BIN + PATH_BACKUP_EXTENSION));
    clearFolder(installFolder.resolve(PATH_LIB + PATH_BACKUP_EXTENSION));
    clearFolder(installFolder.resolve(PATH_PLUGINS + PATH_BACKUP_EXTENSION));
    Files.deleteIfExists(installFolder.resolve(PATH_STARTUP_BAT + PATH_BACKUP_EXTENSION));
    Files.deleteIfExists(installFolder.resolve(PATH_STARTUP_SH + PATH_BACKUP_EXTENSION));
    Files.deleteIfExists(installFolder.resolve(PATH_CPAPPEND + PATH_BACKUP_EXTENSION));
  }

  /**
   * This will backup the current folder, in case anything wrong happends then we will restore it
   *
   * @param installFolder
   */
  private void backupInstall(Path installFolder) throws IOException {
    logger.info("From: " + installFolder.toString());
    logger.info("Backuping [" + PATH_BIN + "] to: [" + PATH_BIN + PATH_BACKUP_EXTENSION + "]");
    Files.move(installFolder.resolve(PATH_BIN), installFolder.resolve(PATH_BIN + PATH_BACKUP_EXTENSION));

    logger.info("Backuping [" + PATH_PLUGINS + "] to: [" + PATH_PLUGINS + PATH_BACKUP_EXTENSION + "]");
    Files.move(installFolder.resolve(PATH_PLUGINS), installFolder.resolve(PATH_PLUGINS + PATH_BACKUP_EXTENSION));

    logger.info("Backuping [" + PATH_LIB + "] to: [" + PATH_LIB + PATH_BACKUP_EXTENSION + "]");
    Files.move(installFolder.resolve(PATH_LIB), installFolder.resolve(PATH_LIB + PATH_BACKUP_EXTENSION));

    logger.info("Backuping [" + PATH_STARTUP_BAT + "] to: [" + PATH_STARTUP_BAT + PATH_BACKUP_EXTENSION + "]");
    Files.move(installFolder.resolve(PATH_STARTUP_BAT), installFolder.resolve(PATH_STARTUP_BAT + PATH_BACKUP_EXTENSION));

    logger.info("Backuping [" + PATH_STARTUP_SH + "] to: [" + PATH_STARTUP_SH + PATH_BACKUP_EXTENSION + "]");
    Files.move(installFolder.resolve(PATH_STARTUP_SH), installFolder.resolve(PATH_STARTUP_SH + PATH_BACKUP_EXTENSION));

    logger.info("Backuping [" + PATH_CPAPPEND + "] to: [" + PATH_CPAPPEND + PATH_BACKUP_EXTENSION + "]");
    Files.move(installFolder.resolve(PATH_CPAPPEND), installFolder.resolve(PATH_CPAPPEND + PATH_BACKUP_EXTENSION));
  }


  private String getAssetToDownload(GHRelease release) throws IOException {
    return release.getAssets().stream().filter(ghAsset -> ghAsset.getName().endsWith("assembly.zip")).map(GHAsset::getBrowserDownloadUrl).findFirst().orElse(null);
  }

  public GHRelease getLatestVersion() throws IOException {
    return (GHRelease) Optional.ofNullable(repository)
        .map(ghRepository -> {
          try {
            return ghRepository.listReleases();
          } catch (IOException e) {
            return Collections.emptyList();
          }
        }).map(t -> t.iterator())
        .map(Iterator::next)
        .orElse(null);
  }


  public File downloadLatestVersion() throws IOException, ZipException {
    return downloadRelease(getAssetToDownload(getLatestVersion()));
  }

  /**
   * Downloads the release and unzip it.
   *
   * @param url
   * @return
   * @throws IOException
   */
  private File downloadRelease(String url) throws IOException, ZipException {
    File tmp = File.createTempFile("assembly", ".zip");

    logger.info("Downloading from [{}]", url);
    URL website = new URL(url);
    ReadableByteChannel rbc = Channels.newChannel(website.openStream());
    FileOutputStream fos = new FileOutputStream(tmp.getAbsolutePath());
    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

    File tmpDir = Files.createTempDirectory("homedash-update").toFile();

    unZipIt(tmp.getAbsolutePath(), tmpDir.getAbsolutePath());

    return Stream.of(tmpDir.listFiles()).findFirst().orElse(null);
  }

  /**
   * Unzip it
   *
   * @param zipFile      input zip file
   * @param outputFolder zip file output folder
   */
  private void unZipIt(String zipFile, String outputFolder) throws ZipException {
    ZipFile zip = new ZipFile(zipFile);
    zip.extractAll(outputFolder);
  }


  public boolean isFolderStructureOkForAutoUpdate() {
    return isFolderStructureOkForAutoUpdate(".");
  }

  private boolean isFolderStructureOkForAutoUpdate(String path) {
    List<String> expectedFiles = Arrays.asList(PATH_BIN, PATH_CFG, PATH_LIB, PATH_PLUGINS);

    File f = new File(path);

    return Stream.of(f.listFiles())
        .map(File::getName)
        .filter(file -> expectedFiles.contains(file))
        .count() == expectedFiles.size();

  }

  public int compareVersion(String v1String, String v2String) throws WrongVersionPatternException {
    if (v1String.matches(VERSION_PATTERN) && v2String.matches(VERSION_PATTERN)) {
      int[] v1 = Stream.of(v1String.split("\\.")).mapToInt(Integer::valueOf).toArray();
      int[] v2 = Stream.of(v2String.split("\\.")).mapToInt(Integer::valueOf).toArray();

      int majorCompare = Integer.compare(v1[0], v2[0]);
      int minorCompare = Integer.compare(v1[1], v2[1]);
      int patchCompare = Integer.compare(v1[2], v2[2]);
      if (majorCompare == 0) {
        if (minorCompare == 0) {
          return patchCompare;
        } else {
          return minorCompare;
        }
      } else {
        return majorCompare;
      }


    } else {
      throw new WrongVersionPatternException();
    }
  }
}
