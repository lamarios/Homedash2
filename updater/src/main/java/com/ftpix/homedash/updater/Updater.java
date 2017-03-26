package com.ftpix.homedash.updater;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by gz on 3/26/17.
 */
public class Updater {
    private GHRepository repository;
    private final String REPO_NAME = "lamarios/Homedash2";
    private final String currentVersion;

    public Updater() {
        ResourceBundle rs = ResourceBundle.getBundle("version");

        currentVersion = rs.getString("version");

        try {
            repository = GitHub.connectAnonymously().getRepository(REPO_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws IOException, ZipException {
        Updater updater = new Updater();

        GHRelease latestVersion = updater.getLatestVersion();
        System.out.println(latestVersion.getName());

        File releaseFolder = updater.downloadRelease(updater.getAssetToDownload(latestVersion));

        Queue<File> queue = new LinkedList<>();

        queue.add(releaseFolder);
        while(!queue.isEmpty()){
            File current = queue.remove();
            System.out.println(current.getAbsolutePath());

            if(current.isDirectory()){
                queue.addAll(Arrays.asList(current.listFiles()));
            }
        }

    }


    private String getAssetToDownload(GHRelease release) throws IOException {
        return release.getAssets().stream().filter(ghAsset -> ghAsset.getName().endsWith("assembly.zip")).map(GHAsset::getBrowserDownloadUrl).findFirst().orElse(null);
    }

    private GHRelease getLatestVersion() throws IOException {
        return Optional.ofNullable(repository.listReleases().iterator().next()).orElse(null);
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
}
