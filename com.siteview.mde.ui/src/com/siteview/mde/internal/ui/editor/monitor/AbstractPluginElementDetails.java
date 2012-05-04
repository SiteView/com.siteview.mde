/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.siteview.mde.internal.ui.editor.monitor;

import com.siteview.mde.internal.ui.editor.MDEDetails;
import com.siteview.mde.internal.ui.editor.MDESection;

public abstract class AbstractPluginElementDetails extends MDEDetails {

	private MDESection fMasterSection;

	public AbstractPluginElementDetails(MDESection masterSection) {
		fMasterSection = masterSection;
	}

	public MDESection getMasterSection() {
		return fMasterSection;
	}

}
