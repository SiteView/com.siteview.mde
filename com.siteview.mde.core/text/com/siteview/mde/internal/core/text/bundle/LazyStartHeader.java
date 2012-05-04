/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.core.text.bundle;

import com.siteview.mde.internal.core.ibundle.IBundle;

public class LazyStartHeader extends SingleManifestHeader {

	private static final long serialVersionUID = 1L;

	public LazyStartHeader(String name, String value, IBundle bundle, String lineDelimiter) {
		super(name, value, bundle, lineDelimiter);
	}

	public boolean isLazyStart() {
		return "true".equals(getMainComponent()); //$NON-NLS-1$
	}

	public void setLazyStart(boolean lazy) {
		setMainComponent(Boolean.toString(lazy));
	}

}
