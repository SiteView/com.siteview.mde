/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.core.ibundle;

import com.siteview.mde.core.IModelChangedListener;
import com.siteview.mde.core.monitor.IMonitorBase;

/**
 * This interface represents an adapter that merges the notion of
 * a plug-in base (either plug-in or fragment) and OSGi bundle.
 * The goal is to maintain the expected plug-in interface
 * to the rest of the framework for both classic plug-ins
 * (with plugin.xml/fragment.xml) and new style (OSGi)
 * plug-ins with META-INF/MANIFEST.MF file.
 */
public interface IBundlePluginBase extends IMonitorBase, IModelChangedListener {

	String getTargetVersion();

	void setTargetVersion(String target);

}
