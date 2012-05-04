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
package com.siteview.mde.internal.ui.search;

import com.siteview.mde.core.monitor.*;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.*;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.views.plugins.ImportActionGroup;
import com.siteview.mde.internal.ui.views.plugins.JavaSearchActionGroup;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionContext;

public class PluginSearchResultPage extends AbstractSearchResultPage {

	class SearchLabelProvider extends LabelProvider {
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			return MDEPlugin.getDefault().getLabelProvider().getImage(element);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object object) {
			if (object instanceof IMonitorBase)
				return ((IMonitorBase) object).getId();

			if (object instanceof IMonitorImport) {
				IMonitorImport dep = (IMonitorImport) object;
				return dep.getId() + " - " //$NON-NLS-1$
						+ dep.getMonitorBase().getId();
			}

			if (object instanceof IMonitorExtension) {
				IMonitorExtension extension = (IMonitorExtension) object;
				return extension.getPoint() + " - " + extension.getMonitorBase().getId(); //$NON-NLS-1$
			}

			if (object instanceof IPluginExtensionPoint)
				return ((IPluginExtensionPoint) object).getFullId();

			return MDEPlugin.getDefault().getLabelProvider().getText(object);
		}
	}

	public PluginSearchResultPage() {
		super();
		MDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	protected void fillContextMenu(IMenuManager mgr) {
		super.fillContextMenu(mgr);
		mgr.add(new Separator());
		IStructuredSelection selection = (IStructuredSelection) getViewer().getSelection();
		ActionContext context = new ActionContext(selection);
		PluginSearchActionGroup actionGroup = new PluginSearchActionGroup();
		actionGroup.setContext(context);
		actionGroup.fillContextMenu(mgr);
		if (ImportActionGroup.canImport(selection)) {
			mgr.add(new Separator());
			ImportActionGroup importActionGroup = new ImportActionGroup();
			importActionGroup.setContext(context);
			importActionGroup.fillContextMenu(mgr);
		}
		mgr.add(new Separator());

		JavaSearchActionGroup jsActionGroup = new JavaSearchActionGroup();
		jsActionGroup.setContext(new ActionContext(selection));
		jsActionGroup.fillContextMenu(mgr);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.search.AbstractSearchResultPage#createLabelProvider()
	 */
	protected ILabelProvider createLabelProvider() {
		return new SearchLabelProvider();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.search.AbstractSearchResultPage#createViewerSorter()
	 */
	protected ViewerComparator createViewerComparator() {
		return new ViewerComparator();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#showMatch(org.eclipse.search.ui.text.Match, int, int, boolean)
	 */
	protected void showMatch(Match match, int currentOffset, int currentLength, boolean activate) throws PartInitException {
		ManifestEditorOpener.open(match, activate);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#dispose()
	 */
	public void dispose() {
		MDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

}
