/*******************************************************************************
 * Copyright (c) 2009 eXXcellent solutions gmbh and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Achim Demelt, eXXcellent solutions gmbh - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.IStatusHandler;
import com.siteview.mde.internal.ui.MDEPlugin;

public class GenericExceptionStatusHandler implements IStatusHandler {

	public Object handleStatus(IStatus status, Object source) throws CoreException {
		MDEPlugin.logException(status.getException());
		return null;
	}

}
