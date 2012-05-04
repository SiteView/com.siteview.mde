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
package com.siteview.mde.internal.core.build;

import com.siteview.mde.core.build.IBuildEntry;
import com.siteview.mde.core.build.IBuildModel;
import com.siteview.mde.core.build.IBuildModelFactory;

public class BuildModelFactory implements IBuildModelFactory {
	private IBuildModel model;

	public BuildModelFactory(IBuildModel model) {
		this.model = model;
	}

	public IBuildEntry createEntry(String name) {
		BuildEntry entry = new BuildEntry(name);
		entry.setModel(model);
		return entry;
	}
}
