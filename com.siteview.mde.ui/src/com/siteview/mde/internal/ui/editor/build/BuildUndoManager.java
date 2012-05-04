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
package com.siteview.mde.internal.ui.editor.build;

import org.eclipse.core.runtime.CoreException;
import com.siteview.mde.core.IModelChangedEvent;
import com.siteview.mde.core.build.*;
import com.siteview.mde.internal.core.build.BuildObject;
import com.siteview.mde.internal.core.build.IBuildObject;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.editor.ModelUndoManager;
import com.siteview.mde.internal.ui.editor.MDEFormEditor;

public class BuildUndoManager extends ModelUndoManager {

	public BuildUndoManager(MDEFormEditor editor) {
		super(editor);
		setUndoLevelLimit(30);
	}

	protected String getPageId(Object obj) {
		if (obj instanceof IBuildEntry)
			return BuildPage.PAGE_ID;
		return null;
	}

	protected void execute(IModelChangedEvent event, boolean undo) {
		Object[] elements = event.getChangedObjects();
		int type = event.getChangeType();
		String propertyName = event.getChangedProperty();
		IBuildModel model = (IBuildModel) event.getChangeProvider();

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

	private void executeAdd(IBuildModel model, Object[] elements) {
		IBuild build = model.getBuild();

		try {
			for (int i = 0; i < elements.length; i++) {
				Object element = elements[i];

				if (element instanceof IBuildEntry) {
					build.add((IBuildEntry) element);
				}
			}
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
	}

	private void executeRemove(IBuildModel model, Object[] elements) {
		IBuild build = model.getBuild();

		try {
			for (int i = 0; i < elements.length; i++) {
				Object element = elements[i];

				if (element instanceof IBuildEntry) {
					build.remove((IBuildEntry) element);
				}
			}
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
	}

	private void executeChange(Object element, String propertyName, Object oldValue, Object newValue) {
		if (element instanceof BuildObject) {
			BuildObject bobj = (BuildObject) element;
			try {
				bobj.restoreProperty(propertyName, oldValue, newValue);
			} catch (CoreException e) {
				MDEPlugin.logException(e);
			}
		}
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			Object obj = event.getChangedObjects()[0];
			if (obj instanceof IBuildObject) {
				IBuildObject bobj = (IBuildObject) event.getChangedObjects()[0];
				//Ignore events from objects that are not yet in the model.
				if (!(bobj instanceof IBuild) && bobj.isInTheModel() == false)
					return;
			}
		}
		super.modelChanged(event);
	}
}
