/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * LGInteraction / Implementation of LG API
 * Author Martin Fluch martinfluch@gmx.net 
 */


package org.openhab.binding.lgtv.lginteraction;


//todo
/*
 * 01:05:27.611 DEBUG o.o.b.l.i.LgtvConnection[:148]- eventresult=<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
 <envelope>
 <api>
 <name>byebye</name>
 <major>0</major>
 <minor>0</minor>
 <sourceIndex>0</sourceIndex>
 <physicalNum>0</physicalNum>
 </api>
 </envelope>


 01:00:37.299 DEBUG o.o.b.l.i.LgtvConnection[:137]- httphandler called from remoteaddr=192.168.77.15 result=<?xml version="1.0" encoding="utf-8"?><envelope><api type="event"><name>Mobilehome_App_Errstate</name><action>Execute</action><detail>OK</detail></api></envelope>

 01:00:37.316 DEBUG o.o.b.l.i.LgtvConnection[:148]- eventresult=<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
 <envelope>
 <api>
 <name>Mobilehome_App_Errstate</name>
 <major>0</major>
 <minor>0</minor>
 <sourceIndex>0</sourceIndex>
 <physicalNum>0</physicalNum>
 </api>
 </envelope>


 * 
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;

import javax.xml.bind.JAXBException;

import org.openhab.binding.lgtv.internal.LgtvConnection;
import org.openhab.binding.lgtv.internal.LgtvEventListener;
import org.openhab.binding.lgtv.internal.LgtvStatusUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;


/**
 * This class forks the reader task to receive tv's messages 
 * 
 * @author Martin Fluch
 * @since 1.5.0
 */
public class LgTvMessageReader {

	private static Logger logger = LoggerFactory
			.getLogger(LgtvConnection.class);
	private static List<LgtvEventListener> _listeners = new ArrayList<LgtvEventListener>();

	private HttpServer server;
	private int serverport = 0;
	private static int status = 0;

	public synchronized void addEventListener(LgtvEventListener listener) {
		_listeners.add(listener);
	}

	/**
	 * Remove event listener.
	 **/
	public synchronized void removeEventListener(LgtvEventListener listener) {
		_listeners.remove(listener);
	}

	public LgTvMessageReader(int portno) {
		serverport = portno;
		logger.debug("LgTvMessageReader initialized");
	}

	public void startserver() throws IOException {

		if (status == 0) {
			InetSocketAddress addr = new InetSocketAddress(serverport);
			server = HttpServer.create(addr, 0);

			server.createContext("/", new MyHandler());
			server.setExecutor(Executors.newCachedThreadPool());
			server.start();
			logger.debug("LgTvMessageReader Server is listening on port "
					+ serverport);

			status = 1;
		} else {
			logger.debug("LgTvMessageReader server already started");
		}

	}

	public void stopserver() throws IOException {

		server.stop(0);
		logger.debug("LgTvMessageReader Server stopped");
		status = 0;
	}


	public void sendtohandlers(LgtvStatusUpdateEvent event, String remoteaddr,
			String message) {
		// send message to event listeners
		logger.debug("sendtohandlers remoteaddr=" + remoteaddr + " message="
				+ message);
		try {
			Iterator<LgtvEventListener> iterator = _listeners.iterator();

			while (iterator.hasNext()) {
				((LgtvEventListener) iterator.next()).statusUpdateReceived(
						event, remoteaddr, message);
			}

		} catch (Exception e) {
			logger.error(
					"Cannot send to EventListeners / maybe not initialized yet",
					e);
		}

	}

	class MyHandler implements HttpHandler {

		public void handle(HttpExchange exchange) throws IOException {
			String requestMethod = exchange.getRequestMethod();
			BufferedReader rd = null;
			StringBuilder sb = null;

			logger.debug("myhandler called");
			if (requestMethod.equalsIgnoreCase("POST")) {
				Headers responseHeaders = exchange.getResponseHeaders();
				responseHeaders.set("Content-Type", "text/plain");
				exchange.sendResponseHeaders(200, 0);

				OutputStream responseBody = exchange.getResponseBody();
				Headers requestHeaders = exchange.getRequestHeaders();

				LgtvStatusUpdateEvent event = new LgtvStatusUpdateEvent(this);

				rd = new BufferedReader(new InputStreamReader(
						exchange.getRequestBody()));
				sb = new StringBuilder();
				String line;
				while ((line = rd.readLine()) != null) {
					sb.append(line + '\n');
				}

				String remoteaddr = exchange.getRemoteAddress().toString();

				int start = remoteaddr.indexOf(":");
				String t;
				if (start > -1)
					t = remoteaddr.substring(0, start);
				else
					t = remoteaddr;
				remoteaddr = t;

				start = remoteaddr.indexOf("/");
				if (start > -1)
					t = remoteaddr.substring(start + 1, remoteaddr.length());
				else
					t = remoteaddr;
				remoteaddr = t;

				logger.debug("httphandler called from remoteaddr=" + remoteaddr
						+ " result=" + sb.toString());

				LgTvEventChannelChanged myevent = new LgTvEventChannelChanged();

				String result = "";
				try {
					result = myevent.readevent(sb.toString());
				} catch (JAXBException e) {
					logger.error("error in httphandler",e);
				}
				logger.debug("eventresult=" + result);

				LgTvEventChannelChanged.envelope envel = myevent.getenvel();

				String eventname = envel.getchannel().geteventname();

				if (eventname.equals("ChannelChanged")) {

					String name = "CHANNEL_CURRENTNAME="
							+ envel.getchannel().getchname();
					String number = "CHANNEL_CURRENTNUMBER="
							+ envel.getchannel().getmajor();
					String set = "CHANNEL_SET=" + envel.getchannel().getmajor();

					sendtohandlers(event, remoteaddr, name);
					sendtohandlers(event, remoteaddr, number);
					sendtohandlers(event, remoteaddr, set);

				} else if (eventname.equals("byebye")) {

					sendtohandlers(event, remoteaddr, "BYEBYE_SEEN=1");

				} else
					logger.debug("warning - unhandled event");

				responseBody.close();
			}
		}
	}

}
