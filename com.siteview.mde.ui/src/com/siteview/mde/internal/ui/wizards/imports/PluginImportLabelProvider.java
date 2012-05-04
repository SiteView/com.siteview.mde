/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.wizards.imports;

import com.siteview.mde.core.monitor.IMonitorModelBase;

import org.eclipse.jface.viewers.*;
import com.siteview.mde.internal.ui.MDEPlugin;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Version;

/**
 * Label provider for lists of plug-ins in the plug-in import wizard.  Uses images
 * from the PDELabelProvider, but uses a styled string for the text to colour the 
 * versions in a different colour.
 * 
 * @since 3.6
 */
public class PluginImportLabelProvider extends StyledCellLabelProvider implements ILabelProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		return MDEPlugin.getDefault().getLabelProvider().getImage(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		return getStyledText(element).getString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StyledCellLabelProvider#update(org.eclipse.jface.viewers.ViewerCell)
	 */
	public void update(ViewerCell cell) {
		StyledString string = getStyledText(cell.getElement());
		cell.setText(string.getString());
		cell.setStyleRanges(string.getStyleRanges());
		cell.setImage(getImage(cell.getElement()));
		super.update(cell);
	}

	private StyledString getStyledText(Object element) {
		StyledString styledString = new StyledString();
		if (element instanceof IMonitorModelBase) {
			IMonitorModelBase plugin = (IMonitorModelBase) element;
			String symbolicName = plugin.getBundleDescription().getSymbolicName();
			Version version = plugin.getBundleDescription().getVersion();
			styledString.append(symbolicName);
			styledString.append(' ');
			styledString.append('(', StyledString.QUALIFIER_STYLER);
			styledString.append(version.toString(), StyledString.QUALIFIER_STYLER);
			styledString.append(')', StyledString.QUALIFIER_STYLER);
			return styledString;
		}
		styledString.append(element.toString());
		return styledString;
	}
}