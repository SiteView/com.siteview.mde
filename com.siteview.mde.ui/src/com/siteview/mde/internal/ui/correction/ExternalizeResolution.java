/*******************************************************************************
 *  Copyright (c) 2005, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.correction;

import com.siteview.mde.core.monitor.IMonitorModelBase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.eclipse.core.filebuffers.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;
import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.internal.core.ICoreConstants;
import com.siteview.mde.internal.core.MDEManager;
import com.siteview.mde.internal.core.ibundle.IBundle;
import com.siteview.mde.internal.core.ibundle.IBundlePluginModelBase;
import com.siteview.mde.internal.core.project.PDEProject;
import com.siteview.mde.internal.core.util.CoreUtility;
import com.siteview.mde.internal.ui.MDEPlugin;
import com.siteview.mde.internal.ui.MDEUIMessages;
import com.siteview.mde.internal.ui.nls.*;
import com.siteview.mde.internal.ui.util.ModelModification;
import com.siteview.mde.internal.ui.util.PDEModelUtility;
import org.eclipse.text.edits.MalformedTreeException;
import org.osgi.framework.Constants;

public class ExternalizeResolution extends AbstractXMLMarkerResolution {

	public ExternalizeResolution(int resolutionType, IMarker marker) {
		super(resolutionType, marker);
	}

	protected void createChange(IMonitorModelBase model) {
		Object node = findNode(model);
		ModelChange change = new ModelChange(model, true);
		ModelChangeElement element = new ModelChangeElement(change, node);
		if (element.updateValue()) {
			String localization = MDEManager.getBundleLocalization(model);
			if (localization == null)
				addLocalization(model, localization = "plugin"); //$NON-NLS-1$
			IProject project = model.getUnderlyingResource().getProject();
			IFile file = PDEProject.getLocalizationFile(project);
			checkPropertiesFile(file);
			try {
				ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
				manager.connect(file.getFullPath(), LocationKind.IFILE, null);
				ITextFileBuffer buffer = manager.getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
				if (buffer.isDirty())
					buffer.commit(null, true);

				IDocument document = buffer.getDocument();
				ExternalizeStringsOperation.getPropertiesInsertEdit(document, element).apply(document);
				buffer.commit(null, true);
			} catch (CoreException e) {
				MDEPlugin.log(e);
			} catch (MalformedTreeException e) {
				MDEPlugin.log(e);
			} catch (BadLocationException e) {
				MDEPlugin.log(e);
			} finally {
				try {
					FileBuffers.getTextFileBufferManager().disconnect(file.getFullPath(), LocationKind.IFILE, null);
				} catch (CoreException e) {
					MDEPlugin.log(e);
				}
			}
		}
	}

	public String getLabel() {
		if (isAttrNode())
			return NLS.bind(MDEUIMessages.ExternalizeResolution_attrib, getNameOfNode());
		if (fLocationPath.charAt(0) == '(')
			return NLS.bind(MDEUIMessages.ExternalizeResolution_text, getNameOfNode());
		return NLS.bind(MDEUIMessages.ExternalizeResolution_header, fLocationPath);
	}

	private void addLocalization(IMonitorModelBase model, String localizationValue) {
		// should always be IBundlePluginModelBase.  Only time wasn't was when we only passed in plugin.xml to ModelModification contructor.
		// Now that we pass in both the Manifest and plugin.xml if we are externalizing the a plugin.xml string (see run(IMarker)), 
		// model should always be IBundlePluginModelBase
		if (model instanceof IBundlePluginModelBase) {
			IBundle bundle = ((IBundlePluginModelBase) model).getBundleModel().getBundle();
			bundle.setHeader(Constants.BUNDLE_LOCALIZATION, localizationValue);
		}
	}

	public void run(IMarker marker) {
		fResource = marker.getResource();
		IFile file = ((IFile) marker.getResource());
		ModelModification modification = null;
		// if file we are externalizing is not manifest, try to pass manifest in if it exists
		if (!file.getName().equals(ICoreConstants.MANIFEST_FILENAME)) {
			IFile manifest = PDEProject.getManifest(file.getProject());
			if (manifest.exists()) {
				modification = new ModelModification(manifest, file) {
					protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
						createChange(model);
					}
				};
			}
		}
		if (modification == null) {
			modification = new ModelModification(file) {
				protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
					createChange(model);
				}
			};
		}
		PDEModelUtility.modifyModel(modification, null);
	}

	private void checkPropertiesFile(IFile file) {
		if (!file.exists()) {
			String propertiesFileComment = ExternalizeStringsOperation.getPropertiesFileComment(file);
			ByteArrayInputStream pStream = new ByteArrayInputStream(propertiesFileComment.getBytes());
			try {
				IContainer container = file.getParent();
				if (!container.exists())
					// project will exists, therefore we can assume if !IContainer.exist(), the object is an IFolder
					CoreUtility.createFolder((IFolder) container);
				file.create(pStream, true, new NullProgressMonitor());
				pStream.close();
			} catch (CoreException e1) {
			} catch (IOException e) {
			}
		}
	}
}
