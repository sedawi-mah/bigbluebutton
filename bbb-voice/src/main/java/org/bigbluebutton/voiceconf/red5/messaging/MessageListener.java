package org.bigbluebutton.voiceconf.red5.messaging;

import java.util.HashMap;

public interface MessageListener {
	void endMeetingRequest(String meetingId);
	void presentationUpdates(HashMap<String,String> map);
	void handleESLConnection(HashMap<String, String> map);
}
