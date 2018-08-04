package com.ftpix.homedash.app.controllers;

import com.ftpix.homedash.plugin.plex.PlexPlugin;
import com.ftpix.homedash.plugins.Plugin;
import com.ftpix.homedash.plugins.SonarrTvPlugin;
import com.ftpix.homedash.plugins.SystemInfoPlugin;
import com.ftpix.homedash.plugins.TransmissionPlugin;
import com.ftpix.homedash.plugins.couchpotato.CouchPotatoPlugin;
import com.ftpix.homedash.plugins.docker.DockerPlugin;
import com.ftpix.homedash.plugins.dockercompose.DockerComposePlugin;
import com.ftpix.homedash.plugins.dynamicdns.DynamicDnsPlugin;
import com.ftpix.homedash.plugins.googlepubliccalendar.GooglePublicCalendarPlugin;
import com.ftpix.homedash.plugins.harddisk.HarddiskPlugin;
import com.ftpix.homedash.plugins.kvm.KvmPlugin;
import com.ftpix.homedash.plugins.mma.MmaPlugin;
import com.ftpix.homedash.plugins.networkmonitor.NetworkMonitorPlugin;
import com.ftpix.homedash.plugins.pihole.PiHolePlugin;
import com.ftpix.homedash.plugins.portmapper.PortMapperPlugin;
import com.ftpix.homedash.plugins.spotify.SpotifyPlugin;
import com.ftpix.homedash.plugins.unifi.UnifiPlugin;
import com.ftpix.logreader.LogReaderPlugin;
import de.neuland.jade4j.exceptions.JadeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

public enum PluginController {
    INSTANCE;
    private Logger logger = LogManager.getLogger();


    private final static Set<Class> subTypes = Set.of(
            CouchPotatoPlugin.class,
            DockerPlugin.class,
            DockerComposePlugin.class,
            DynamicDnsPlugin.class,
            GooglePublicCalendarPlugin.class,
            HarddiskPlugin.class,
            KvmPlugin.class,
            LogReaderPlugin.class,
            MmaPlugin.class,
            NetworkMonitorPlugin.class,
            PiHolePlugin.class,
            PlexPlugin.class,
            PortMapperPlugin.class,
            SonarrTvPlugin.class,
            SpotifyPlugin.class,
            SystemInfoPlugin.class,
            TransmissionPlugin.class,
            UnifiPlugin.class
            );


    public void defineEndpoints() {

    }

    /**
     * Lists all the available controller, looking for class implementing the Plugin class
     *
     * @return
     */
    public List<Plugin> listAvailablePlugins() {
        logger.info("listAvailablePlugins()");
        List<Plugin> availablePlugins = new ArrayList<>();


        logger.info("Found {} plugins", subTypes.size());

        subTypes.forEach(plugin -> {
            try {
                availablePlugins.add((Plugin) plugin.newInstance());
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Error while creating new instance of " + plugin.getCanonicalName(), e);
            }
        });


        Collections.sort(availablePlugins, Comparator.comparing(Plugin::getDisplayName));
        return availablePlugins;
    }


    /**
     * @return the html of the plugin
     * @throws IOException
     * @throws JadeException
     */
    public String getPluginSettingsHtml(Plugin plugin, Map<String, String> settings) throws Exception {
        return plugin.getSettingsHtml(settings);
    }

    public String getPluginSettingsHtml(Plugin plugin) throws Exception {
        return plugin.getSettingsHtml();
    }

    /**
     * Get the smallest available size for a plugin
     *
     * @param plugin
     */
    public String getSmallestAvailableSize(Plugin plugin) {
        logger.info("getSmallestAvailableSize [{}]", plugin.getId());
        String[] sizes = plugin.getSizes();
        Arrays.sort(sizes);
        logger.info("Smallest size: {}", sizes[0]);
        return sizes[0];
    }


    public String[] getPluginSizes(Plugin modulePlugin) {
        String[] sizes = modulePlugin.getSizes();
        Arrays.sort(sizes);
        return sizes;
    }

    public Plugin createPluginFromClass(String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return (Plugin) Class.forName(className).newInstance();
    }
}
