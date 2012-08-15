/**
 * BigBlueButton open source conferencing system - http://www.bigbluebutton.org/
 *
 * Copyright (c) 2012 BigBlueButton Inc. and by respective authors (see below).
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
 * Author: Felipe Cecagno <felipe@mconf.org>
 */
package org.bigbluebutton.conference.service.callback;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.bigbluebutton.conference.service.messaging.MessagingConstants;
import org.bigbluebutton.conference.service.messaging.RedisMessagingService;
import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class CallbackEventsRetriever extends RedisMessagingService {
	private static Logger log = Red5LoggerFactory.getLogger(CallbackEventsRetriever.class, "bigbluebutton");
	private HashMap<String, String[]> events = new HashMap<String, String[]>();
	private String callbackUrl = "";
	
	public CallbackEventsRetriever() {
		super();
		
		events.put("MeetingStartedEvent", new String[] { "meetingId" });
		events.put("MeetingEndedEvent", new String[] { "meetingId" });
		events.put("UserJoinedEvent", new String[] { "meetingId", "internalUserId", "fullname", "role" });
		events.put("UserLeftEvent", new String[] { "meetingId", "internalUserId" });
		events.put("UserStatusChangeEvent", new String[] { "meetingId", "internalUserId", "status", "value" });
	}
	
	@Override
	public void start() {
		log.debug("Starting redis pubsub...");		

		if (callbackUrl.isEmpty()) {
			log.debug("The callback.url is empty, so it won't subscribe to redis pubsub");
			return;
		}
		
		final Jedis jedis = redisPool.getResource();
		try {
			pubsubListener = new Runnable() {
			    public void run() {
			    	jedis.psubscribe(new ParticipantEventsListener(), MessagingConstants.BIGBLUEBUTTON_PATTERN);
			    }
			};
			exec.execute(pubsubListener);
		} catch (Exception e) {
			log.error("Error subscribing to channels: " + e.getMessage());
		}
	}
	
	private class ParticipantEventsListener extends JedisPubSub {
		public ParticipantEventsListener() {
			super();			
		}

		@Override
		public void onMessage(String channel, String message) {
			// Not used.
		}

		@Override
		public void onPMessage(String pattern, String channel, String message) {
			log.debug("Message received in channel: " + channel);
			Gson gson = new Gson();
			HashMap<String,String> map = gson.fromJson(message, new TypeToken<Map<String, String>>() {}.getType());
			log.debug("Message: " + map.toString());
			
			if (!map.containsKey("messageId"))
				return;
			
			try {
				String messageId = map.get("messageId");
				if (events.containsKey(messageId)) {
					String url = callbackUrl + "?event=" + messageId;
					String[] fields = events.get(messageId);
					for (String f : fields) {
						if (map.containsKey(f))
							url += "&" + f + "=" + URLEncoder.encode(map.get(f), "UTF-8");
						else
							log.error("There's no field named " + f);
					}
					log.debug("Generated URL: " + url);
					URL urlConn = new URL(url);
				    URLConnection conn = urlConn.openConnection();
				    conn.setConnectTimeout(2000);
				    conn.getContent();
				}
			} catch (Exception e) {
				log.error("Couldn't get the URL (" + e.toString() + ")");
			}
		}

		@Override
		public void onPSubscribe(String pattern, int subscribedChannels) {
			log.debug("Subscribed to the pattern: " + pattern);
		}

		@Override
		public void onPUnsubscribe(String pattern, int subscribedChannels) {
			// Not used.
		}

		@Override
		public void onSubscribe(String channel, int subscribedChannels) {
			// Not used.
		}

		@Override
		public void onUnsubscribe(String channel, int subscribedChannels) {
			// Not used.
		}		
	}
	
	public void setCallbackUrl(String url) {
		this.callbackUrl  = url;
	}
}
