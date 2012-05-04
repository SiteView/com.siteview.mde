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
package com.siteview.mde.internal.ui.wizards.feature;

import org.eclipse.jface.operation.IRunnableWithProgress;
import com.siteview.mde.internal.ui.MDEPluginImages;
import com.siteview.mde.internal.ui.MDEUIMessages;

public class NewFeaturePatchWizard extends AbstractNewFeatureWizard {

	public NewFeaturePatchWizard() {
		super();
		setDefaultPageImageDescriptor(MDEPluginImages.DESC_NEWFTRPTCH_WIZ);
		setWindowTitle(MDEUIMessages.FeaturePatch_wtitle);
	}

	protected AbstractFeatureSpecPage createFirstPage() {
		return new PatchSpecPage();
	}

	protected IRunnableWithProgress getOperation() {
		return new CreateFeaturePatchOperation(fProvider.getProject(), fProvider.getLocationPath(), fProvider.getFeatureData(), fProvider.getFeatureToPatch(), getShell());
	}

}