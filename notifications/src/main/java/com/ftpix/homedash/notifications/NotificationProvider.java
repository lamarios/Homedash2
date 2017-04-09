package com.ftpix.homedash.notifications;

import java.util.Map;

public interface NotificationProvider {
	String getName();
	void sendNotification(String title, String content) throws Exception;
	boolean setSettings(Map<String, String> settings);

}
