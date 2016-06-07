package com.ftpix.homedash.models;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import io.gsonfire.GsonFireBuilder;

import java.lang.reflect.Modifier;

public class WebSocketMessage {
	public final static String COMMAND_START = "start", COMMAND_ERROR = "error", COMMAND_SUCCESS = "success", COMMAND_REFRESH = "refresh", COMMAND_CHANGE_PAGE = "changePage",
			REMOTE_MODULE_NOT_FOUND = "remote404", RELOAD_OTHERS = "reloadOthers", COMMAND_CHANGE_LAYOUT = "changeLayout";

	private String command;
	private Object message, extra;
	@SerializedName("id")
	private int moduleId;

	
	

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public Object getMessage() {
		return message;
	}

	public void setMessage(Object message) {
		this.message = message;
	}



	public int getModuleId() {
		return moduleId;
	}

	public void setModuleId(int moduleId) {
		this.moduleId = moduleId;
	}

	public Object getExtra() {
		return extra;
	}

	public void setExtra(Object extra) {
		this.extra = extra;
	}

	public String toJSon() {
		GsonBuilder builder = new GsonFireBuilder().enableExposeMethodResult().createGsonBuilder();
		builder.excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT, Modifier.VOLATILE);

		return builder.serializeSpecialFloatingPointValues().create().toJson(this);

	}
}
