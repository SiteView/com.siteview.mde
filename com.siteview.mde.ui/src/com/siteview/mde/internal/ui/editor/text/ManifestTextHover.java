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
package com.siteview.mde.internal.ui.editor.text;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.core.IModel;
import com.siteview.mde.internal.core.ICoreConstants;
import com.siteview.mde.internal.core.ibundle.IManifestHeader;
import com.siteview.mde.internal.core.text.IDocumentRange;
import com.siteview.mde.internal.ui.editor.MDEFormEditor;
import com.siteview.mde.internal.ui.editor.MDESourcePage;
import com.siteview.mde.internal.ui.util.PDEJavaHelperUI;

public class ManifestTextHover extends PDETextHover {

	private MDESourcePage fSourcePage;
	private IJavaProject fJP;

	public ManifestTextHover(MDESourcePage sourcePage) {
		fSourcePage = sourcePage;
		IProject project = ((MDEFormEditor) fSourcePage.getEditor()).getCommonProject();
		fJP = JavaCore.create(project);
	}

	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		int offset = hoverRegion.getOffset();
		IDocumentRange range = fSourcePage.getRangeElement(offset, false);
		if (range instanceof IManifestHeader) {
			IManifestHeader header = (IManifestHeader) range;
			String headerName = header.getName();
			if (offset >= header.getOffset() + headerName.length())
				return checkForTranslatable(header);
			if (fJP != null)
				return PDEJavaHelperUI.getOSGIConstantJavaDoc(headerName, fJP);
		}
		return null;
	}

	private String checkForTranslatable(IManifestHeader header) {
		String name = header.getName();
		String value = header.getValue();
		for (int i = 0; i < ICoreConstants.TRANSLATABLE_HEADERS.length; i++) {
			if (name.equals(ICoreConstants.TRANSLATABLE_HEADERS[i]) && value.startsWith("%")) { //$NON-NLS-1$
				IBaseModel model = ((MDEFormEditor) fSourcePage.getEditor()).getAggregateModel();
				if (model instanceof IModel)
					return ((IModel) model).getResourceString(value);
			}
		}
		return null;
	}
}
