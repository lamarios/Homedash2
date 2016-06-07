package com.ftpix.homedash.models;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;

public class WebSocketSession {
	private Session session;
	private Page page;
	private Layout layout;
	private  Logger logger = LogManager.getLogger();

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}
	
	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}

	

	public Layout getLayout() {
		return layout;
	}

	public void setLayout(Layout layout) {
		this.layout = layout;
	}

	@Override
	public boolean equals(Object obj) {
		//logger.info("equals !!!");

		try{
			WebSocketSession wsSession = (WebSocketSession) obj;
			return wsSession.getSession().equals(session);
		}catch(Exception e){
			//logger.info("Can't convert");
		}
		
		//trying if it is just a session
		try{
			Session otherSession = (Session) obj;
			//logger.info("{} == {}", otherSession.hashCode(), session.hashCode());
			return otherSession.hashCode() == session.hashCode();
		}catch(Exception e){
			//logger.info("Can't convert to session either ?");

		}
		
		return false;
	}
}
