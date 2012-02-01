/** 
*
* BigBlueButton open source conferencing system - http://www.bigbluebutton.org/
*
* Copyright (c) 2010 BigBlueButton Inc. and by respective authors (see below).
*
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2.1 of the License, or (at your option) any later
* version.
*
* BigBlueButton is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
* PARTICULAR PURPOSE. See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License along
* with BigBlueButton; if not, see <http://www.gnu.org/licenses/>.
* 
**/
package org.bigbluebutton.voiceconf.red5;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bigbluebutton.voiceconf.red5.messaging.MessagingConstants;
import org.bigbluebutton.voiceconf.red5.messaging.MessagingService;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.service.IServiceCapableConnection;
import org.slf4j.Logger;

import com.google.gson.Gson;

public class ClientConnectionManager {
	private static Logger log = Red5LoggerFactory.getLogger(ClientConnectionManager.class, "sip");
	
	private Map<String, ClientConnection> clients = new ConcurrentHashMap<String, ClientConnection>();
	private MessagingService messagingService;
	
	public void createClient(String id, String userid, String username, IServiceCapableConnection connection) {
		ClientConnection cc = new ClientConnection(id, userid, username, connection);
		clients.put(id, cc);
	}
	
	public void removeClient(String id) {
		ClientConnection cc = clients.remove(id);
		if (cc == null) {
			log.warn("Failed to remove client {}.", id);
		} else {
			log.debug("Removed client {} from ConnectionManager.", id);
		}
	}
	
	public void joinConferenceSuccess(String clientId, String usertalkStream, String userListenStream, String codec) {
		ClientConnection cc = clients.get(clientId);
		if (cc != null) {
			cc.onJoinConferenceSuccess(usertalkStream, userListenStream, codec);
		} else {
			log.warn("Can't find client {} to inform user that she has joined the conference.", clientId);
		}
	}
	
	public void joinConferenceFailed(String clientId) {
		ClientConnection cc = clients.get(clientId);
		if (cc != null) {
			cc.onJoinConferenceFail();
		} else {
			log.warn("Can't find client {} to inform user that she failed to join conference.", clientId);
		}
	}
	
	public void leaveConference(String clientId) {
		ClientConnection cc = clients.get(clientId);
		if (cc != null) {
			cc.onLeaveConference();
		} else {
			log.warn("Can't find client {} to inform user that she has left the conference.", clientId);
		}
	}
	
	public void setMessagingService(MessagingService messagingService) {
		this.messagingService = messagingService;
		this.messagingService.start();
	}
	
	public void addContact(String contact) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("add", contact);
		Gson gson = new Gson();
		messagingService.send(MessagingConstants.VOICE_CHANNEL, gson.toJson(map));
	}
}
