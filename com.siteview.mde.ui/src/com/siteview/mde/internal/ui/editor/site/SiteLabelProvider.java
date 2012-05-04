/*******************************************************************************
 *  Copyright (c) 2003, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.site;

import org.eclipse.jface.viewers.LabelProvider;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.core.isite.ISiteCategoryDefinition;
import com.siteview.mde.internal.core.isite.ISiteFeature;
import com.siteview.mde.internal.ui.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.editor.IFormPage;

class SiteLabelProvider extends LabelProvider {

	private MDELabelProvider fSharedProvider;

	/**
	 * Comment for <code>fLabelProvider</code>
	 */
	private Image fSiteFeatureImage;

	private Image fMissingSiteFeatureImage;

	private Image fPageImage;

	private Image fCatDefImage;

	public SiteLabelProvider() {
		fSiteFeatureImage = MDEPluginImages.DESC_FEATURE_OBJ.createImage();
		fMissingSiteFeatureImage = MDEPluginImages.DESC_NOREF_FEATURE_OBJ.createImage();
		fCatDefImage = MDEPluginImages.DESC_CATEGORY_OBJ.createImage();
		fPageImage = MDEPluginImages.DESC_PAGE_OBJ.createImage();
		fSharedProvider = MDEPlugin.getDefault().getLabelProvider();
		fSharedProvider.connect(this);
	}

	public Image getImage(Object element) {
		if (element instanceof ISiteCategoryDefinition)
			return fCatDefImage;
		if (element instanceof SiteFeatureAdapter) {
			if (MDECore.getDefault().getFeatureModelManager().findFeatureModelRelaxed(((SiteFeatureAdapter) element).feature.getId(), ((SiteFeatureAdapter) element).feature.getVersion()) == null)
				return fMissingSiteFeatureImage;
			return fSiteFeatureImage;
		}
		if (element instanceof IFormPage)
			return fPageImage;
		return fSharedProvider.getImage(element);
	}

	public String getText(Object element) {
		if (element instanceof ISiteCategoryDefinition)
			return ((ISiteCategoryDefinition) element).getName();
		if (element instanceof SiteFeatureAdapter) {
			ISiteFeature feature = ((SiteFeatureAdapter) element).feature;
			return fSharedProvider.getObjectText(feature);
		}
		if (element instanceof IFormPage)
			return ((IFormPage) element).getTitle();
		return fSharedProvider.getText(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
	 */
	public void dispose() {
		fSharedProvider.disconnect(this);
		// Dispose of images
		if ((fCatDefImage != null) && (fCatDefImage.isDisposed() == false)) {
			fCatDefImage.dispose();
			fCatDefImage = null;
		}
		if ((fSiteFeatureImage != null) && (fSiteFeatureImage.isDisposed() == false)) {
			fSiteFeatureImage.dispose();
			fSiteFeatureImage = null;
		}
		if ((fMissingSiteFeatureImage != null) && (fMissingSiteFeatureImage.isDisposed() == false)) {
			fMissingSiteFeatureImage.dispose();
			fMissingSiteFeatureImage = null;
		}
		if ((fPageImage != null) && (fPageImage.isDisposed() == false)) {
			fPageImage.dispose();
			fPageImage = null;
		}
		super.dispose();
	}
}