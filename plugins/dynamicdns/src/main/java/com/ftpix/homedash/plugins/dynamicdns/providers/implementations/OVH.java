package com.ftpix.homedash.plugins.dynamicdns.providers.implementations;


public class OVH extends StandardProvider{


	@Override
	protected String getUrl() {
		// TODO Auto-generated method stub
		return "https://www.ovh.com/nic/update?system=dyndns";
	}

	@Override
	public String getName() {
		return "OVH";
	}

}
