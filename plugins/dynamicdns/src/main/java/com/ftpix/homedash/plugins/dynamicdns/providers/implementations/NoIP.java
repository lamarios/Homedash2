package com.ftpix.homedash.plugins.dynamicdns.providers.implementations;

public class NoIP extends StandardProvider{

	@Override
	protected String getUrl() {
		return "https://dynupdate.no-ip.com/nic/update";
	}

	@Override
	public String getName() {
		return "No-IP.org";
	}

}
