package com.ftpix.homedash.plugins;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.channels.NotYetBoundException;
import java.util.HashMap;
import java.util.Map;

import com.ftpix.homedash.models.ModuleExposedData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ftpix.homedash.models.Module;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.exceptions.JadeException;
import de.neuland.jade4j.template.ClasspathTemplateLoader;
import de.neuland.jade4j.template.JadeTemplate;
import de.neuland.jade4j.template.TemplateLoader;

public abstract class Plugin {
	protected Logger logger = LogManager.getLogger();

	public static int NEVER = 0, ONE_SECOND = 1, ONE_MINUTE = 60, ONE_HOUR = 60*ONE_MINUTE;

	private String cacheBase;

	private Module module;

	protected Map<String, String> settings;
	protected Map<String, Object> data;

	protected Gson gson = new GsonBuilder().create();

	public Plugin() {
	}

	public Plugin(Module module) {
		this.module = module;

	}

	/**
	 * Unique name for the plugin
	 * 
	 * @return
	 */
	public abstract String getId();

	/**
	 * Nice to read name of your plugin
	 * 
	 * @return
	 */
	public abstract String getDisplayName();

	/**
	 * Description of what it's doing
	 * 
	 * @return
	 */
	public abstract String getDescription();


	/**
	 * Provide an external link if available
	 * @return
     */
	public abstract String getExternalLink();

	/**
	 * Give chance to a plugin to run some stuff when creating it
	 * Settings can be accessed via settings object
	 */
	protected abstract void init();

	/**
	 * Get the szes available for this module
	 * 
	 * @return
	 */
	public abstract String[] getSizes();

	/**
	 * How often (in second) this module should be refreshed in the background ,
	 * 0 = never
	 * 
	 * @return
	 */
	public abstract int getBackgroundRefreshRate();

	/**
	 * Process a command sent by a client
	 * 
	 * @param command
	 * @param message
	 * @param extra
	 * @return
	 */
	public abstract Object processCommand(String command, String message, Object extra);

	/**
	 * Do background task if any
	 */
	public abstract void doInBackground();

	/**
	 * Get data to send to clients via web socket
	 * 
	 * @param size
	 *            of the module
	 * @return
	 * @throws Exception
	 */
	protected abstract Object refresh(String size) throws Exception;

	/**
	 * Get refresh rate for front page display
	 * 
	 * @return
	 */
	public abstract int getRefreshRate();


	/**
	 * Validates a given set of settings
	 * @param settings
	 * @return
     */
	public abstract Map<String, String> validateSettings(Map<String, String> settings);


	/**
	 * Expose a chunk of selected data on request
	 * @return
     */
	public abstract ModuleExposedData exposeData();


	/**
	 * Expose a chunk of selected settings on request
	 * @return
	 */
	public abstract Map<String, String> exposeSettings();


	/**
	 * Gets the html for a specific size
	 * 
	 * @param size
	 * @return
	 * @throws JadeException
	 * @throws IOException
	 */
	public String getView(String size) throws JadeException, IOException {

		JadeConfiguration config = new JadeConfiguration();

		TemplateLoader loader = new ClasspathTemplateLoader();

		config.setTemplateLoader(loader);

		Map<String, Object> model = new HashMap<String, Object>();
		model.put("module", module);

		logger.info("id:[{}]", getId());
		logger.info("size:[{}]", size);
		JadeTemplate template = config.getTemplate("templates/" + getId() + "-" + size + ".jade");

		return config.renderTemplate(template, model);
	}

	/**
	 * Refresh called from the websocket
	 * 
	 * @param size
	 * @return
	 * @throws Exception
	 */
	public Object refreshPlugin(String size) throws Exception {
		if (module.getRemote() == module.LOCAL) {
			Object obj = refresh(size);
			return obj;
		} else {
			return null;
		}
	}

	/**
	 * Gets the settings view if there's any
	 * 
	 * @return
	 * @throws JadeException
	 * @throws IOException
	 */
	public String getSettingsHtml() throws Exception {
		try {
			logger.info("Getting settings for [{}]", this.getId());
			JadeConfiguration config = new JadeConfiguration();

			TemplateLoader loader = new ClasspathTemplateLoader();

			config.setTemplateLoader(loader);

			Map<String, Object> model = new HashMap<String, Object>();
			if (module != null) {
				model.put("settings", getSettingsAsMap());
			}

			System.out.println(getId());
			String templateFile = "templates/" + getId() + "-settings.jade";
			logger.info("Looking for template: [{}]", templateFile);
			JadeTemplate template = config.getTemplate(templateFile);

			logger.info("Found setting template, returning it");
			return config.renderTemplate(template, model);
		}catch(Exception e){
			logger.error("Error while getting settings template", e);
			throw e;
		}
	}

	/**
	 * Get the module settings as a map
	 * 
	 * @return
	 */
	private Map<String, String> getSettingsAsMap() {
		Map<String, String> settings = new HashMap<>();

		module.getSettings().forEach(ms -> {
			settings.put(ms.getName(), ms.getValue());
		});

		return settings;
	}

	/**
	 * Get the module
	 * 
	 * @return
	 */
	public Module getModule() {
		return module;
	}

	/**
	 * Set the module, it will load the settings and the data from the module as
	 * well
	 * 
	 * @param module
	 */
	public void setModule(Module module) {
		Map<String, String> oldSettings = null;
		if(this.module != null){

			oldSettings = getSettingsAsMap();
		}
		this.module = module;

		this.settings = getSettingsAsMap();

		if(oldSettings == null ||!getSettingsAsMap().equals(oldSettings)){
			init();
		}

		// Converts the data
		if (module.getData() != null) {
			try {
				Type type = new TypeToken<Map<String, String>>() {
				}.getType();
				this.data = gson.fromJson(module.getData(), type);
			} catch (Exception e) {
				logger.error("Fail to create data map from :[" + module.getData() + "]", e);
			}
		}
	}

	/**
	 * COnverts the data in a json string for the module
	 */
	public void saveData() {
		module.setData(gson.toJson(data));
	}


	public void setCacheBase(String cacheBase){
		this.cacheBase = cacheBase;
		if(!cacheBase.endsWith("/")){
			this.cacheBase+="/";
		}
	}

	protected String getCacheFolder() throws NotYetBoundException{
		if(module != null) {
			return cacheBase + module.getId()+"/";
		}else {
			throw new NotYetBoundException();
		}
	}

}
