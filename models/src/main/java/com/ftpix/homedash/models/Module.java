package com.ftpix.homedash.models;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "modules")
public class Module {
	
	public static final int REMOTE = 1, LOCAL = 0;
	
	@DatabaseField(generatedId = true, allowGeneratedIdInsert = true)
	private int id;


	@DatabaseField
	private String pluginClass;
	
	@DatabaseField
	private String data;

	@DatabaseField
	private int remote = LOCAL;

	@DatabaseField(foreign = true, foreignAutoRefresh = true, maxForeignAutoRefreshLevel = 1)
	private Page page;

	@ForeignCollectionField(eager = false, maxEagerLevel = 0)
	public ForeignCollection<ModuleSettings> settings;
	
	@ForeignCollectionField(eager = false, maxEagerLevel = 0)
	public ForeignCollection<ModuleLayout> layouts;
	


	public int getId() {
		return id;
	}


	public String getPluginClass() {
		return pluginClass;
	}

	public void setPluginClass(String pluginClass) {
		this.pluginClass = pluginClass;
	}

	public int getRemote() {
		return remote;
	}

	public void setRemote(int remote) {
		this.remote = remote;
	}

	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}


	public ForeignCollection<ModuleSettings> getSettings() {
		return settings;
	}


	public void setSettings(ForeignCollection<ModuleSettings> settings) {
		this.settings = settings;
	}


	public ForeignCollection<ModuleLayout> getLayouts() {
		return layouts;
	}


	public void setLayouts(ForeignCollection<ModuleLayout> layouts) {
		this.layouts = layouts;
	}


	public String getData() {
		return data;
	}


	public void setData(String data) {
		this.data = data;
	}


	public void setId(int id) {
		this.id = id;
	}


	
}
