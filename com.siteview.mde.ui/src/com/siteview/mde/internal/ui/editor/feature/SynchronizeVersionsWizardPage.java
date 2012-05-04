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
package com.siteview.mde.internal.ui.editor.feature;

import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.MonitorRegistry;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.wizard.WizardPage;
import com.siteview.mde.core.IBaseModel;
import com.siteview.mde.internal.core.feature.WorkspaceFeatureModel;
import com.siteview.mde.internal.core.ibundle.IBundleModel;
import com.siteview.mde.internal.core.ibundle.IBundlePluginModelBase;
import com.siteview.mde.internal.core.ifeature.IFeature;
import com.siteview.mde.internal.core.ifeature.IFeaturePlugin;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.util.ModelModification;
import com.siteview.mde.internal.ui.util.PDEModelUtility;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.osgi.framework.Constants;

public class SynchronizeVersionsWizardPage extends WizardPage {
	public static final int USE_PLUGINS_AT_BUILD = 0;
	public static final int USE_FEATURE = 1;
	public static final int USE_PLUGINS = 2;
	private FeatureEditor fFeatureEditor;
	private Button fUsePluginsAtBuildButton;
	private Button fUseComponentButton;
	private Button fUsePluginsButton;

	private static final String PREFIX = MDEPlugin.getPluginId() + ".synchronizeVersions."; //$NON-NLS-1$
	private static final String PROP_SYNCHRO_MODE = PREFIX + "mode"; //$NON-NLS-1$

	public SynchronizeVersionsWizardPage(FeatureEditor featureEditor) {
		super("featureJar"); //$NON-NLS-1$
		setTitle(MDEUIMessages.VersionSyncWizard_title);
		setDescription(MDEUIMessages.VersionSyncWizard_desc);
		this.fFeatureEditor = featureEditor;
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);

		Group group = new Group(container, SWT.SHADOW_ETCHED_IN);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		layout = new GridLayout();
		group.setLayout(layout);
		group.setLayoutData(gd);
		group.setText(MDEUIMessages.VersionSyncWizard_group);

		fUsePluginsAtBuildButton = new Button(group, SWT.RADIO);
		fUsePluginsAtBuildButton.setText(MDEUIMessages.VersionSyncWizard_usePluginsAtBuild);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fUsePluginsAtBuildButton.setLayoutData(gd);

		fUsePluginsButton = new Button(group, SWT.RADIO);
		fUsePluginsButton.setText(MDEUIMessages.VersionSyncWizard_usePlugins);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fUsePluginsButton.setLayoutData(gd);

		fUseComponentButton = new Button(group, SWT.RADIO);
		fUseComponentButton.setText(MDEUIMessages.VersionSyncWizard_useComponent);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fUseComponentButton.setLayoutData(gd);

		setControl(container);
		Dialog.applyDialogFont(container);
		loadSettings();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.FEATURE_SYNCHRONIZE_VERSIONS);
	}

	private IMonitorModelBase findModel(String id) {
		IMonitorModelBase[] models = MonitorRegistry.getWorkspaceModels();
		for (int i = 0; i < models.length; i++) {
			IMonitorModelBase modelBase = models[i];
			if (modelBase != null && id.equals(modelBase.getMonitorBase().getId()))
				return modelBase;
		}
		return null;
	}

	public boolean finish() {
		final int mode = saveSettings();

		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) {
				try {
					runOperation(mode, monitor);
				} catch (CoreException e) {
					MDEPlugin.logException(e);
				} catch (BadLocationException e) {
					MDEPlugin.logException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			PlatformUI.getWorkbench().getProgressService().runInUI(MDEPlugin.getActiveWorkbenchWindow(), operation, MDEPlugin.getWorkspace().getRoot());
		} catch (InvocationTargetException e) {
			MDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}

	/**
	 * Forces a version into plugin/fragment .xml
	 * 
	 * @param targetVersion
	 * @param modelBase
	 * @throws CoreException
	 */
	private void forceVersion(final String targetVersion, IMonitorModelBase modelBase, IProgressMonitor monitor) {
		IFile file = (IFile) modelBase.getUnderlyingResource();
		if (file == null)
			return;

		PDEModelUtility.modifyModel(new ModelModification(file) {
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (model instanceof IBundlePluginModelBase) {
					modifyVersion(((IBundlePluginModelBase) model).getBundleModel(), targetVersion);
				} else if (model instanceof IMonitorModelBase) {
					modifyVersion((IMonitorModelBase) model, targetVersion);
				}
			}
		}, monitor);
	}

	private void modifyVersion(IBundleModel model, String targetVersion) {
		model.getBundle().setHeader(Constants.BUNDLE_VERSION, targetVersion);
	}

	private void modifyVersion(IMonitorModelBase model, String version) throws CoreException {
		model.getMonitorBase().setVersion(version);
	}

	private void loadSettings() {
		IDialogSettings settings = getDialogSettings();
		if (settings.get(PROP_SYNCHRO_MODE) != null) {
			int mode = settings.getInt(PROP_SYNCHRO_MODE);
			switch (mode) {
				case USE_FEATURE :
					fUseComponentButton.setSelection(true);
					break;
				case USE_PLUGINS :
					fUsePluginsButton.setSelection(true);
					break;
				default : // USE_PLUGINS_AT_BUILD
					fUsePluginsAtBuildButton.setSelection(true);
					break;
			}
		} else
			fUsePluginsAtBuildButton.setSelection(true);
	}

	private void runOperation(int mode, IProgressMonitor monitor) throws CoreException, BadLocationException {
		WorkspaceFeatureModel model = (WorkspaceFeatureModel) fFeatureEditor.getAggregateModel();
		IFeature feature = model.getFeature();
		IFeaturePlugin[] plugins = feature.getPlugins();
		int size = plugins.length;
		monitor.beginTask(MDEUIMessages.VersionSyncWizard_synchronizing, size);
		for (int i = 0; i < plugins.length; i++)
			synchronizeVersion(mode, feature.getVersion(), plugins[i], monitor);
	}

	private int saveSettings() {
		IDialogSettings settings = getDialogSettings();
		int mode = USE_PLUGINS_AT_BUILD;
		if (fUseComponentButton.getSelection())
			mode = USE_FEATURE;
		else if (fUsePluginsButton.getSelection())
			mode = USE_PLUGINS;
		settings.put(PROP_SYNCHRO_MODE, mode);
		return mode;
	}

	private void synchronizeVersion(int mode, String featureVersion, IFeaturePlugin ref, IProgressMonitor monitor) throws CoreException, BadLocationException {
		String id = ref.getId();

		if (mode == USE_PLUGINS_AT_BUILD) {
			if (!"0.0.0".equals(ref.getVersion())) //$NON-NLS-1$
				ref.setVersion("0.0.0"); //$NON-NLS-1$
		} else if (mode == USE_PLUGINS) {
			IMonitorModelBase modelBase = MonitorRegistry.findModel(id);
			if (modelBase == null)
				return;
			String baseVersion = modelBase.getMonitorBase().getVersion();
			if (!ref.getVersion().equals(baseVersion))
				ref.setVersion(baseVersion);
		} else /* mode == USE_FEATURE */{
			IMonitorModelBase modelBase = findModel(id);
			if (modelBase == null)
				return;
			ref.setVersion(featureVersion);
			String baseVersion = modelBase.getMonitorBase().getVersion();
			if (!featureVersion.equals(baseVersion))
				forceVersion(featureVersion, modelBase, monitor);
		}
		monitor.worked(1);
	}
}
