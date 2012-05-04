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
package com.siteview.mde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.nls.GetNonExternalizedStringsAction;
import com.siteview.mde.internal.ui.util.SWTUtil;
import org.eclipse.swt.custom.BusyIndicator;

public class ExternalizeStringsResolution extends AbstractPDEMarkerResolution {

	public ExternalizeStringsResolution(int type) {
		super(type);
	}

	public void run(final IMarker marker) {
		BusyIndicator.showWhile(SWTUtil.getStandardDisplay(), new Runnable() {
			public void run() {
				GetNonExternalizedStringsAction fGetExternAction = new GetNonExternalizedStringsAction();
				IStructuredSelection selection = new StructuredSelection(marker.getResource().getProject());
				fGetExternAction.selectionChanged(null, selection);
				fGetExternAction.run(null);
			}
		});
	}

	protected void createChange(IBaseModel model) {
		// nothin to do - all handled by run
	}

	public String getDescription() {
		return MDEUIMessages.ExternalizeStringsResolution_desc;
	}

	public String getLabel() {
		return MDEUIMessages.ExternalizeStringsResolution_label;
	}

}
