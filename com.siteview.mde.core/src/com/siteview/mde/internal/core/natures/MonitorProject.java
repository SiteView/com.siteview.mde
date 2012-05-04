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
package com.siteview.mde.internal.core.natures;

import org.eclipse.core.runtime.CoreException;

/**
 */
public class MonitorProject extends BaseProject {
	/**
	 * PluginProject constructor comment.
	 */
	public MonitorProject() {
		super();
	}

	public void configure() throws CoreException {
		addToBuildSpec(MDE.MANIFEST_BUILDER_ID);
		addToBuildSpec(MDE.SCHEMA_BUILDER_ID);
	}

	public void deconfigure() throws CoreException {
		removeFromBuildSpec(MDE.MANIFEST_BUILDER_ID);
		removeFromBuildSpec(MDE.SCHEMA_BUILDER_ID);
	}
}
