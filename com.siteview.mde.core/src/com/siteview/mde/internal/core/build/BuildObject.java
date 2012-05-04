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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import com.siteview.mde.core.build.IBuildModel;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.core.MDECoreMessages;

public class BuildObject implements IBuildObject {
	private IBuildModel fModel;

	private boolean fInTheModel;

	public boolean isInTheModel() {
		return fInTheModel;
	}

	public void setInTheModel(boolean inTheModel) {
		fInTheModel = inTheModel;
	}

	protected void ensureModelEditable() throws CoreException {
		if (!fModel.isEditable()) {
			throwCoreException(MDECoreMessages.BuildObject_readOnlyException);
		}
	}

	public IBuildModel getModel() {
		return fModel;
	}

	void setModel(IBuildModel model) {
		fModel = model;
	}

	protected void throwCoreException(String message) throws CoreException {
		Status status = new Status(IStatus.ERROR, MDECore.PLUGIN_ID, IStatus.OK, message, null);
		throw new CoreException(status);
	}

	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
	}
}
