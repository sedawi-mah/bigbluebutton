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

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

public class CallbackApplication {
	private static Logger log = Red5LoggerFactory.getLogger(CallbackApplication.class, "bigbluebutton");
	private CallbackEventsRetriever retriever;

	public CallbackApplication() {
		log.debug("Instantiated CallbackApplication");
	}
	
	public void setEventsRetriever(CallbackEventsRetriever retriever) {
		this.retriever = retriever;
		this.retriever.start();
		log.debug("setting retriever");
	}
}
