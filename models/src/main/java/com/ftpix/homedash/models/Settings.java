package com.ftpix.homedash.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "settings")
public class Settings {
	
	public static final String USE_AUTH = "use_auth", USERNAME="username", PASSWORD="password", PUSHBULLET="use_pushbullet", PUSHBULLET_API_KEY = "pushbullet_api_key",
			PUSHALOT="use_pushalot", PUSHALOT_API_KEY = "pushalot_api_key", PUSHOVER = "use_pushover", PUSHOVER_API_KEY = "pushover_api_key", PUSHOVER_APP_TOKEN = "pushover_app_token";
	
	
	@DatabaseField(id = true)
	private String name;
	
	@DatabaseField
	private String value;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}	
}
