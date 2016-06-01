package com.ftpix.homedash.plugins.docker;

import java.util.Map;

import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.plugins.Plugin;

public class DockerPlugin extends Plugin {
	public DockerPlugin() {
	}

	public DockerPlugin(Module module) {
		super(module);
	}

	@Override
	public String getId() {
		return "docker";
	}

	@Override
	public String getDisplayName() {
		return "Docker";
	}

	@Override
	public String getDescription() {
		return "Control your docker containers with this module";
	}

	@Override
	public String[] getSizes() {
		return new String[]{ModuleLayout.SIZE_1x1};
	}

	@Override
	public int getBackgroundRefreshRate() {
		return NEVER;
	}

	@Override
	public void doInBackground() {}

	@Override
	public Object processCommand(String command, String message, Object extra) {
		return null;
	}

	@Override
	public Object refresh(String size) throws Exception {
		return NEVER;
	}

	@Override
	public int getRefreshRate() {
		// TODO Auto-generated method stub
		return 0;
	}

}
