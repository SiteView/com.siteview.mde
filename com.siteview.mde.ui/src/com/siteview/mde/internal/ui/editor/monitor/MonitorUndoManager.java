/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.monitor;

import com.siteview.mde.core.IModelChangeProvider;
import com.siteview.mde.core.IModelChangedEvent;
import com.siteview.mde.core.build.*;
import com.siteview.mde.core.monitor.*;
import com.siteview.mde.internal.core.build.BuildObject;
import com.siteview.mde.internal.core.build.IBuildObject;
import com.siteview.mde.internal.core.bundle.BundleObject;
import com.siteview.mde.internal.core.ibundle.IBundleModel;
import com.siteview.mde.internal.core.ibundle.IManifestHeader;
import com.siteview.mde.internal.core.monitor.*;
import com.siteview.mde.internal.core.monitor.MonitorAttribute;
import com.siteview.mde.internal.core.text.bundle.*;
import com.siteview.mde.internal.core.text.monitor.*;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.editor.MDEFormEditor;
import com.siteview.mde.internal.ui.editor.ModelUndoManager;
import org.eclipse.core.runtime.CoreException;
import org.osgi.framework.Constants;

public class MonitorUndoManager extends ModelUndoManager {

	public MonitorUndoManager(MDEFormEditor editor) {
		super(editor);
		setUndoLevelLimit(30);
	}

	protected String getPageId(Object obj) {
		if (obj instanceof IMonitorBase)
			return OverviewPage.PAGE_ID;
		if (obj instanceof IMonitorImport)
			return DependenciesPage.PAGE_ID;
		if (obj instanceof IMonitorLibrary || (obj instanceof IMonitorElement && ((IMonitorElement) obj).getParent() instanceof IMonitorLibrary))
			return RuntimePage.PAGE_ID;
		if (obj instanceof IMonitorExtension || (obj instanceof IMonitorElement && ((IMonitorElement) obj).getParent() instanceof IMonitorParent) || obj instanceof IMonitorAttribute)
			return ExtensionsPage.PAGE_ID;
		if (obj instanceof IPluginExtensionPoint)
			return ExtensionPointsPage.PAGE_ID;
		return null;
	}

	protected void execute(IModelChangedEvent event, boolean undo) {
		Object[] elements = event.getChangedObjects();
		int type = event.getChangeType();
		String propertyName = event.getChangedProperty();
		IModelChangeProvider model = event.getChangeProvider();

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
				if (event instanceof AttributeChangedEvent) {
					executeAttributeChange((AttributeChangedEvent) event, undo);
				} else {
					if (undo)
						executeChange(elements[0], propertyName, event.getNewValue(), event.getOldValue());
					else
						executeChange(elements[0], propertyName, event.getOldValue(), event.getNewValue());
				}
		}
	}

	private void executeAdd(IModelChangeProvider model, Object[] elements) {
		IMonitorBase pluginBase = null;
		IBuild build = null;
		IBundleModel bundleModel = null;
		if (model instanceof IMonitorModelBase) {
			pluginBase = ((IMonitorModelBase) model).getMonitorBase();
		} else if (model instanceof IBuildModel) {
			build = ((IBuildModel) model).getBuild();
		} else if (model instanceof IBundleModel) {
			bundleModel = (IBundleModel) model;
		}

		try {
			for (int i = 0; i < elements.length; i++) {
				Object element = elements[i];

				if (element instanceof IMonitorImport) {
					pluginBase.add((IMonitorImport) element);
				} else if (element instanceof IMonitorLibrary) {
					pluginBase.add((IMonitorLibrary) element);
				} else if (element instanceof IPluginExtensionPoint) {
					pluginBase.add((IPluginExtensionPoint) element);
				} else if (element instanceof IMonitorExtension) {
					pluginBase.add((IMonitorExtension) element);
				} else if (element instanceof IMonitorElement) {
					IMonitorElement e = (IMonitorElement) element;
					Object parent = e.getParent();
					if (parent instanceof MonitorLibraryNode && e instanceof MonitorElementNode) {
						((MonitorLibraryNode) parent).addContentFilter((MonitorElementNode) e);
					} else if (parent instanceof IMonitorParent) {
						((IMonitorParent) parent).add(e);
					}
				} else if (element instanceof IBuildEntry) {
					IBuildEntry e = (IBuildEntry) element;
					build.add(e);
				} else if (element instanceof BundleObject) {
					if (element instanceof ImportPackageObject) {
						IManifestHeader header = bundleModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
						if (header != null && header instanceof ImportPackageHeader) {
							((ImportPackageHeader) header).addPackage((PackageObject) element);
						}
					}
				}
			}
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
	}

	private void executeRemove(IModelChangeProvider model, Object[] elements) {
		IMonitorBase pluginBase = null;
		IBuild build = null;
		IBundleModel bundleModel = null;
		if (model instanceof IMonitorModelBase) {
			pluginBase = ((IMonitorModelBase) model).getMonitorBase();
		} else if (model instanceof IBuildModel) {
			build = ((IBuildModel) model).getBuild();
		} else if (model instanceof IBundleModel) {
			bundleModel = (IBundleModel) model;
		}

		try {
			for (int i = 0; i < elements.length; i++) {
				Object element = elements[i];

				if (element instanceof IMonitorImport) {
					pluginBase.remove((IMonitorImport) element);
				} else if (element instanceof IMonitorLibrary) {
					pluginBase.remove((IMonitorLibrary) element);
				} else if (element instanceof IPluginExtensionPoint) {
					pluginBase.remove((IPluginExtensionPoint) element);
				} else if (element instanceof IMonitorExtension) {
					pluginBase.remove((IMonitorExtension) element);
				} else if (element instanceof IMonitorElement) {
					IMonitorElement e = (IMonitorElement) element;
					Object parent = e.getParent();
					if (parent instanceof MonitorLibraryNode && e instanceof MonitorElementNode) {
						((MonitorLibraryNode) parent).removeContentFilter((MonitorElementNode) e);
					} else if (parent instanceof IMonitorParent) {
						((IMonitorParent) parent).remove(e);
					}
				} else if (element instanceof IBuildEntry) {
					IBuildEntry e = (IBuildEntry) element;
					build.remove(e);
				} else if (element instanceof BundleObject) {
					if (element instanceof ImportPackageObject) {
						IManifestHeader header = bundleModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
						if (header != null && header instanceof ImportPackageHeader) {
							((ImportPackageHeader) header).removePackage((PackageObject) element);
						}
					}
				}
			}
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
	}

	private void executeAttributeChange(AttributeChangedEvent e, boolean undo) {
		MonitorElement element = (MonitorElement) e.getChangedObjects()[0];
		MonitorAttribute att = (MonitorAttribute) e.getChangedAttribute();
		Object oldValue = e.getOldValue();
		Object newValue = e.getNewValue();
		try {
			if (undo)
				element.setAttribute(att.getName(), oldValue.toString());
			else
				element.setAttribute(att.getName(), newValue.toString());
		} catch (CoreException ex) {
			MDEPlugin.logException(ex);
		}
	}

	private void executeChange(Object element, String propertyName, Object oldValue, Object newValue) {
		if (element instanceof MonitorObject) {
			MonitorObject pobj = (MonitorObject) element;
			try {
				pobj.restoreProperty(propertyName, oldValue, newValue);
			} catch (CoreException e) {
				MDEPlugin.logException(e);
			}
		} else if (element instanceof BuildObject) {
			BuildObject bobj = (BuildObject) element;
			try {
				bobj.restoreProperty(propertyName, oldValue, newValue);
			} catch (CoreException e) {
				MDEPlugin.logException(e);
			}
		} else if (element instanceof MonitorObjectNode) {
			MonitorObjectNode node = (MonitorObjectNode) element;
			String newString = newValue != null ? newValue.toString() : null;
			node.setXMLAttribute(propertyName, newString);
		} else if (element instanceof BundleObject) {
			if (element instanceof ImportPackageObject) {
				ImportPackageObject ipObj = (ImportPackageObject) element;
				ipObj.restoreProperty(propertyName, oldValue, newValue);
			}
		}
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			Object changedObject = event.getChangedObjects()[0];
			if (changedObject instanceof IMonitorObject) {
				IMonitorObject obj = (IMonitorObject) event.getChangedObjects()[0];
				//Ignore events from objects that are not yet in the model.
				if (!(obj instanceof IMonitorBase) && obj.isInTheModel() == false)
					return;
			}
			if (changedObject instanceof IBuildObject) {
				IBuildObject obj = (IBuildObject) event.getChangedObjects()[0];
				//Ignore events from objects that are not yet in the model.
				if (obj.isInTheModel() == false)
					return;
			}
		}
		super.modelChanged(event);
	}
}
