/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.lgtv.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.openhab.binding.lgtv.lginteraction.LgTvMessageReader;
/**
 * Extension of the default OSGi bundle activator
 * 
 * @author Martin Fluch
 * @since 1.6.0
 */
public final class LgtvActivator implements BundleActivator {

	private static Logger logger = LoggerFactory.getLogger(LgtvActivator.class);

	private static BundleContext context;

	/**
	 * Called whenever the OSGi framework starts our bundle
	 */
	public void start(BundleContext bc) throws Exception {
		context = bc;

		//LgTvMessageReader m=new LgTvMessageReader();
		//org.osgi.service.cm.ManagedService service = new LgTvMessageReader();
    		//context.registerService(LgTvMessageReader.class.getName(), m,null);




		logger.debug("Lgtv binding has been started.");

	}

	/**
	 * Called whenever the OSGi framework stops our bundle
	 */
	public void stop(BundleContext bc) throws Exception {
		context = null;
		logger.debug("Lgtv binding has been stopped.");
	}

	/**
	 * Returns the bundle context of this bundle
	 * 
	 * @return the bundle context
	 */
	public static BundleContext getContext() {
		return context;
	}

}
