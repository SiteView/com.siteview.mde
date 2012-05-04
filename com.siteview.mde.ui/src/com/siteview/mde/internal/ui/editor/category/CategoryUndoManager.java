/******************************************************************************* 
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package com.siteview.mde.internal.ui.editor.category;

import org.eclipse.core.runtime.CoreException;
import com.siteview.mde.core.IModelChangeProvider;
import com.siteview.mde.core.IModelChangedEvent;
import com.siteview.mde.internal.core.isite.*;
import com.siteview.mde.internal.core.site.SiteObject;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.editor.ModelUndoManager;

public class CategoryUndoManager extends ModelUndoManager {
	public CategoryUndoManager(CategoryEditor editor) {
		super(editor);
		setUndoLevelLimit(30);
	}

	protected String getPageId(Object obj) {
		if (obj instanceof ISiteFeature || obj instanceof ISiteCategory || obj instanceof ISiteCategoryDefinition) {
			return FeaturesPage.PAGE_ID;
		}
		// site elements and attributes are on different pages, stay on the
		// current page
		return null;
	}

	/*
	 * @see IModelUndoManager#execute(ModelUndoOperation)
	 */
	protected void execute(IModelChangedEvent event, boolean undo) {
		IModelChangeProvider model = event.getChangeProvider();
		Object[] elements = event.getChangedObjects();
		int type = event.getChangeType();
		String propertyName = event.getChangedProperty();

		switch (type) {
			case IModelChangedEvent.INSERT :
				if (undo)
					executeRemove(model, elements);
				else
					executeAdd(model, elements);
				break;
			case IModelChangedEvent.REMOVE :
				if (undo)
					executeAdd(model, elements);
				else
					executeRemove(model, elements);
				break;
			case IModelChangedEvent.CHANGE :
				if (undo)
					executeChange(elements[0], propertyName, event.getNewValue(), event.getOldValue());
				else
					executeChange(elements[0], propertyName, event.getOldValue(), event.getNewValue());
		}
	}

	private void executeAdd(IModelChangeProvider model, Object[] elements) {
		ISiteModel siteModel = (model instanceof ISiteModel) ? (ISiteModel) model : null;
		ISite site = siteModel != null ? siteModel.getSite() : null;

		try {
			for (int i = 0; i < elements.length; i++) {
				Object element = elements[i];

				if (element instanceof ISiteFeature) {
					site.addFeatures(new ISiteFeature[] {(ISiteFeature) element});
				} else if (element instanceof ISiteArchive) {
					site.addArchives(new ISiteArchive[] {(ISiteArchive) element});
				} else if (element instanceof ISiteCategoryDefinition) {
					site.addCategoryDefinitions(new ISiteCategoryDefinition[] {(ISiteCategoryDefinition) element});
				} else if (element instanceof ISiteCategory) {
					ISiteCategory category = (ISiteCategory) element;
					ISiteFeature feature = (ISiteFeature) category.getParent();
					feature.addCategories(new ISiteCategory[] {category});
				}
			}
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
	}

	private void executeRemove(IModelChangeProvider model, Object[] elements) {
		ISiteModel siteModel = (model instanceof ISiteModel) ? (ISiteModel) model : null;
		ISite site = siteModel != null ? siteModel.getSite() : null;

		try {
			for (int i = 0; i < elements.length; i++) {
				Object element = elements[i];

				if (element instanceof ISiteFeature) {
					site.removeFeatures(new ISiteFeature[] {(ISiteFeature) element});
				} else if (element instanceof ISiteArchive) {
					site.removeArchives(new ISiteArchive[] {(ISiteArchive) element});
				} else if (element instanceof ISiteCategoryDefinition) {
					site.removeCategoryDefinitions(new ISiteCategoryDefinition[] {(ISiteCategoryDefinition) element});
				} else if (element instanceof ISiteCategory) {
					ISiteCategory category = (ISiteCategory) element;
					ISiteFeature feature = (ISiteFeature) category.getParent();
					feature.removeCategories(new ISiteCategory[] {category});
				}
			}
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
	}

	private void executeChange(Object element, String propertyName, Object oldValue, Object newValue) {

		if (element instanceof SiteObject) {
			SiteObject sobj = (SiteObject) element;
			try {
				sobj.restoreProperty(propertyName, oldValue, newValue);
			} catch (CoreException e) {
				MDEPlugin.logException(e);
			}
		}
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			Object object = event.getChangedObjects()[0];
			if (object instanceof ISiteObject) {
				ISiteObject obj = (ISiteObject) object;
				//Ignore events from objects that are not yet in the model.
				if (!(obj instanceof ISite) && !obj.isInTheModel())
					return;
			}
		}
		super.modelChanged(event);
	}
}
