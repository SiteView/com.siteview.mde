/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 252329
 *     David Green <dgreen99@gmail.com> - bug 275240
 *******************************************************************************/
package com.siteview.mde.internal.ui.nls;

import com.siteview.mde.internal.core.text.monitor.MonitorExtensionNode;
import com.siteview.mde.internal.core.text.monitor.MonitorExtensionPointNode;

import com.siteview.mde.core.monitor.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.internal.core.*;
import com.siteview.mde.internal.core.ibundle.*;
import com.siteview.mde.internal.core.ischema.*;
import com.siteview.mde.internal.core.schema.SchemaRegistry;
import com.siteview.mde.internal.core.text.IDocumentAttributeNode;
import com.siteview.mde.internal.core.text.IDocumentElementNode;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.util.ModelModification;
import com.siteview.mde.internal.ui.util.PDEModelUtility;

public class GetNonExternalizedStringsOperation implements IRunnableWithProgress {

	private ISelection fSelection;
	private ArrayList fSelectedModels;
	private ModelChangeTable fModelChangeTable;
	private boolean fCanceled;

	//Azure: To indicate that only selected plug-ins under <code>fSelection</code> are to be externalized.
	private boolean fExternalizeSelectedPluginsOnly;

	public GetNonExternalizedStringsOperation(ISelection selection, boolean externalizeSelectedPluginsOnly) {
		fSelection = selection;
		fExternalizeSelectedPluginsOnly = externalizeSelectedPluginsOnly;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

		if (fSelection instanceof IStructuredSelection) {
			Object[] elems = ((IStructuredSelection) fSelection).toArray();
			fSelectedModels = new ArrayList(elems.length);
			for (int i = 0; i < elems.length; i++) {
				if (elems[i] instanceof IFile)
					elems[i] = ((IFile) elems[i]).getProject();

				if (elems[i] instanceof IProject && WorkspaceModelManager.isPluginProject((IProject) elems[i]) && !WorkspaceModelManager.isBinaryProject((IProject) elems[i]))
					fSelectedModels.add(elems[i]);
			}

			fModelChangeTable = new ModelChangeTable();

			/*
			 * Azure: This will add only the preselected plug-ins to the ModelChangeTable
			 * instead of adding the list of all plug-ins in the workspace. This is useful
			 * when the Internationalize action is run on a set of non-externalized plug-ins
			 * where there is no need to display all non-externalized plug-ins in the
			 * workspace, but only those selected.
			 */
			if (fExternalizeSelectedPluginsOnly) {
				monitor.beginTask(MDEUIMessages.GetNonExternalizedStringsOperation_taskMessage, fSelectedModels.size());
				Iterator iterator = fSelectedModels.iterator();
				while (iterator.hasNext() && !fCanceled) {
					IProject project = (IProject) iterator.next();
					if (!WorkspaceModelManager.isBinaryProject(project))
						getUnExternalizedStrings(project, new SubProgressMonitor(monitor, 1));
				}
			} else {
				IMonitorModelBase[] pluginModels = MonitorRegistry.getWorkspaceModels();
				monitor.beginTask(MDEUIMessages.GetNonExternalizedStringsOperation_taskMessage, pluginModels.length);
				for (int i = 0; i < pluginModels.length && !fCanceled; i++) {
					IProject project = pluginModels[i].getUnderlyingResource().getProject();
					if (!WorkspaceModelManager.isBinaryProject(project))
						getUnExternalizedStrings(project, new SubProgressMonitor(monitor, 1));
				}
			}
		}
	}

	private void getUnExternalizedStrings(IProject project, IProgressMonitor monitor) {
		PDEModelUtility.modifyModel(new ModelModification(project) {
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (model instanceof IBundlePluginModelBase)
					inspectManifest((IBundlePluginModelBase) model, monitor);

				if (monitor.isCanceled()) {
					fCanceled = true;
					return;
				}

				if (model instanceof IMonitorModelBase)
					inspectXML((IMonitorModelBase) model, monitor);

				if (monitor.isCanceled()) {
					fCanceled = true;
					return;
				}
			}
		}, monitor);
		monitor.done();
	}

	private void inspectManifest(IBundlePluginModelBase model, IProgressMonitor monitor) throws CoreException {
		IFile manifestFile = (IFile) model.getBundleModel().getUnderlyingResource();
		IBundle bundle = model.getBundleModel().getBundle();
		for (int i = 0; i < ICoreConstants.TRANSLATABLE_HEADERS.length; i++) {
			IManifestHeader header = bundle.getManifestHeader(ICoreConstants.TRANSLATABLE_HEADERS[i]);
			if (header != null && isNotTranslated(header.getValue()))
				fModelChangeTable.addToChangeTable(model, manifestFile, header, selected(manifestFile));
		}
	}

	private void inspectXML(IMonitorModelBase model, IProgressMonitor monitor) throws CoreException {
		IFile file;
		if (model instanceof IBundlePluginModelBase) {
			ISharedExtensionsModel extModel = ((IBundlePluginModelBase) model).getExtensionsModel();
			if (extModel == null)
				return;
			file = (IFile) extModel.getUnderlyingResource();
		} else
			file = (IFile) model.getUnderlyingResource();

		IMonitorBase base = model.getMonitorBase();
		if (base instanceof IDocumentElementNode) {
			// old style xml plugin
			// check xml name declaration
			IDocumentAttributeNode attr = ((IDocumentElementNode) base).getDocumentAttribute(IMonitorObject.P_NAME);
			if (attr != null && isNotTranslated(attr.getAttributeValue()))
				fModelChangeTable.addToChangeTable(model, file, attr, selected(file));

			// check xml provider declaration
			attr = ((IDocumentElementNode) base).getDocumentAttribute(IMonitorBase.P_PROVIDER);
			if (attr != null && isNotTranslated(attr.getAttributeValue()))
				fModelChangeTable.addToChangeTable(model, file, attr, selected(file));
		}

		SchemaRegistry registry = MDECore.getDefault().getSchemaRegistry();
		IMonitorExtension[] extensions = model.getMonitorBase().getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			ISchema schema = registry.getSchema(extensions[i].getPoint());
			if (schema != null)
				inspectExtension(schema, extensions[i], model, file);
		}

		IPluginExtensionPoint[] extensionPoints = model.getMonitorBase().getExtensionPoints();
		for (int i = 0; i < extensionPoints.length; i++) {
			inspectExtensionPoint(extensionPoints[i], model, file);
		}
	}

	private void inspectExtension(ISchema schema, IMonitorParent parent, IMonitorModelBase memModel, IFile file) {
		if (parent instanceof MonitorExtensionNode) {
			MonitorExtensionNode parentNode = (MonitorExtensionNode) parent;
			IDocumentAttributeNode[] attributes = parentNode.getNodeAttributes();
			ISchemaElement schemaElement = schema.findElement(parentNode.getXMLTagName());
			if (schemaElement != null) {
				for (int j = 0; j < attributes.length; j++) {
					IMonitorAttribute attr = (IMonitorAttribute) attributes[j];
					ISchemaAttribute attInfo = schemaElement.getAttribute(attr.getName());
					if (attInfo != null && attInfo.isTranslatable())
						if (isNotTranslated(attr.getValue()))
							fModelChangeTable.addToChangeTable(memModel, file, attr, selected(file));
				}
			}
		}

		IMonitorObject[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			IMonitorElement child = (IMonitorElement) children[i];
			ISchemaElement schemaElement = schema.findElement(child.getName());
			if (schemaElement != null) {
				if (schemaElement.hasTranslatableContent())
					if (isNotTranslated(child.getText()))
						fModelChangeTable.addToChangeTable(memModel, file, child, selected(file));

				IMonitorAttribute[] attributes = child.getAttributes();
				for (int j = 0; j < attributes.length; j++) {
					IMonitorAttribute attr = attributes[j];
					ISchemaAttribute attInfo = schemaElement.getAttribute(attr.getName());
					if (attInfo != null && attInfo.isTranslatable())
						if (isNotTranslated(attr.getValue()))
							fModelChangeTable.addToChangeTable(memModel, file, attr, selected(file));
				}
			}
			inspectExtension(schema, child, memModel, file);
		}
	}

	private void inspectExtensionPoint(IPluginExtensionPoint extensionPoint, IMonitorModelBase memModel, IFile file) {
		if (extensionPoint instanceof MonitorExtensionPointNode)
			if (isNotTranslated(extensionPoint.getName()))
				fModelChangeTable.addToChangeTable(memModel, file, ((MonitorExtensionPointNode) extensionPoint).getNodeAttributesMap().get(IMonitorObject.P_NAME), selected(file));
	}

	private boolean isNotTranslated(String value) {
		if (value == null)
			return false;
		if (value.length() > 0 && value.charAt(0) == '%')
			return false;
		return true;
	}

	protected ModelChangeTable getChangeTable() {
		return fModelChangeTable;
	}

	public boolean wasCanceled() {
		return fCanceled;
	}

	private boolean selected(IFile file) {
		return fSelectedModels.contains(file.getProject());
	}
}
