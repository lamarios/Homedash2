package com.ftpix.homedash.notifications;

import java.util.Map;

public interface NotificationProvider {
	public String getName();
	public void sendNotification(String title, String content) throws Exception;
	public boolean setSettings(Map<String, String> settings);

}
