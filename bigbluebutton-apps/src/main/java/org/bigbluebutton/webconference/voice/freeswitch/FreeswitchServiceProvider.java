/** 
* ===License Header===
*
* BigBlueButton open source conferencing system - http://www.bigbluebutton.org/
*
* Copyright (c) 2010 BigBlueButton Inc. and by respective authors (see below).
*
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU Lesser General Public License as published by the Free Software
* Foundation; either version 2.1 of the License, or (at your option) any later
* version.
*
* BigBlueButton is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
* PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License along
* with BigBlueButton; if not, see <http://www.gnu.org/licenses/>.
* 
* ===License Header===
*/
package org.bigbluebutton.webconference.voice.freeswitch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bigbluebutton.conference.service.messaging.MessageListener;
import org.bigbluebutton.conference.service.messaging.MessagingService;
import org.bigbluebutton.webconference.voice.ConferenceServiceProvider;
import org.bigbluebutton.webconference.voice.events.ConferenceEventListener;
import org.freeswitch.esl.client.manager.ManagerConnection;
import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

public class FreeswitchServiceProvider implements ConferenceServiceProvider {
	private static Logger log = Red5LoggerFactory.getLogger(FreeswitchServiceProvider.class, "bigbluebutton");
	
	private Map<String, String> hostnameByClientId = new HashMap<String, String>();
	private Map<String, Integer> appDelegateByHostname = new HashMap<String, Integer>();
	private Map<String, Integer> appDelegateByRoom = new HashMap<String, Integer>();
	private List<ConferenceServiceProvider> appDelegate = new ArrayList<ConferenceServiceProvider>();

	private ManagerConnection connection;
	private ConferenceEventListener conferenceEventListener;
	private MessagingService messagingService;
	
	@Override
    public void record(String room, String meetingid){
		Integer id = appDelegateByRoom.get(room);
		if (id != null) appDelegate.get(id).record(room,meetingid);
		else log.error("Cannot find the connection to handle the call RECORD");
    }
	
	@Override
	public void eject(String room, Integer participant) {
		Integer id = appDelegateByRoom.get(room);
		if (id != null) appDelegate.get(id).eject(room, participant);
		else log.error("Cannot find the connection to handle the call EJECT");
	}

	@Override
	public void mute(String room, Integer participant, Boolean mute) {
		Integer id = appDelegateByRoom.get(room);
		if (id != null) appDelegate.get(id).mute(room, participant, mute);
		else log.error("Cannot find the connection to handle the call MUTE");
	}

	@Override
	public void populateRoom(String room) {
		// this is called when the ESL channel is established 
	}

	@Override
	public void shutdown() {
		for (ConferenceServiceProvider p : appDelegate)
			p.shutdown();
	}

	@Override
	public boolean startup() {
		if (connection == null) {
			log.error("Cannot start application as ESL Client has not been set.");
			return false;
		}
		return true;
	}
	
	public void setManagerConnection(ManagerConnection c) {
		connection = c;
	}

	@Override
	public void setConferenceEventListener(ConferenceEventListener l) {
		conferenceEventListener = l;		
		// this call have been moved to handleESLConnection
		// appDelegate.setConferenceEventListener(conferenceEventListener);
	}
	
	public void setMessagingService(MessagingService messagingService) {
		this.messagingService = messagingService;
		this.messagingService.addListener(new MessageListener() {
			
			@Override
			public void presentationUpdates(HashMap<String, String> map) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void endMeetingRequest(String meetingId) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void handleESLConnection(HashMap<String, String> map) {
				log.debug("handleESLConnection parameters: {}", map.toString());
				String method = map.get("method");

				if (method.equals("onCallAccepted")) {
					// example: <sip:72702@143.54.12.94:5060;transport=udp>
					String contact = map.get("contact");
					String clientId = map.get("clientId");
					
					if (hostnameByClientId.containsKey(clientId)) {
						log.debug("There's already a server handling the voice conference of the client {}", clientId);
						return;
					}
					
					int firstColon = contact.indexOf(':'),
							at = contact.indexOf('@'),
							secondColon = contact.indexOf(':', at);
					
					String room = contact.substring(firstColon + 1, at);
					// if the configured hostname is localhost, it means that the FS
					// is running in the same server, and it's not listening to the
					// external IP, so I must change the hostname to localhost
					String hostname = connection.getHostname().equals("127.0.0.1")? "127.0.0.1": contact.substring(at + 1, secondColon);
					
					log.debug("INVITE reply contact: {}", contact);
					log.debug("FreeSWITCH hostname: {}", hostname);
					log.debug("FreeSWITCH number: {}", room);

					Integer appIndex = appDelegateByHostname.get(hostname);
					ConferenceServiceProvider p = null;
					if (appIndex == null) {
						log.debug("There's no opened ESL connection with {}, opening...", hostname);
						p = new FreeswitchApplication();
						p.setConferenceEventListener(conferenceEventListener);
						((FreeswitchApplication) p).setDebugNullConferenceAction(true);
						((FreeswitchApplication) p).connect(hostname, connection.getPort(), connection.getPassword());
						
						appDelegate.add(p);
						appIndex = appDelegate.size() - 1;
						appDelegateByHostname.put(hostname, appIndex);
					} else {
						log.debug("There's already an opened ESL connection with {}", hostname);
						p = appDelegate.get(appIndex);
					}
					appDelegateByRoom.put(room, appIndex);
					hostnameByClientId.put(clientId, hostname);
					p.populateRoom(room);
				} else if (method.equals("onCallClosed")) {
					String clientId = map.get("clientId");
					
					if (!hostnameByClientId.containsKey(clientId)) {
						log.debug("Can't find the clientId {}, it's most probably a duplicate call", clientId);
						return;
					}
					
					String hostname = hostnameByClientId.get(clientId);
					hostnameByClientId.remove(clientId);
					
					// Can't close the ESL connection because when the last user
					// leaves the voice conference, the event of his leaving never 
					// comes. Instead, the ESL connection should be closed when the room
					// is destroyed.
//					if (hostnameByClientId.containsValue(hostname)) {
//						log.debug("The voice server is serving another clients, don't close the ESL connection");
//					} else {
//						log.debug("Closing the ESL connection with {}", hostname);
//						int appIndex = appDelegateByHostname.remove(hostname);
//						ConferenceServiceProvider p = appDelegate.get(appIndex);
//						p.shutdown();
//						appDelegate.remove(appIndex);
//						
//						List<String> toRemove = new ArrayList<String>();
//						for (Entry<String, Integer> entry : appDelegateByRoom.entrySet()) {
//							if (entry.getValue().equals(appIndex))
//								toRemove.add(entry.getKey());
//						}
//						for (String key : toRemove) {
//							appDelegateByRoom.remove(key);
//						}
//					}
				}
			}
		});
		this.messagingService.start();
	}
}
