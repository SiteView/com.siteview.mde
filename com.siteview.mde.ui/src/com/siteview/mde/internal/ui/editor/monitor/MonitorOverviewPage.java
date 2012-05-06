/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.editor.monitor;

import com.siteview.mde.core.monitor.IMonitorModelBase;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import com.siteview.mde.core.build.*;
import com.siteview.mde.internal.core.build.WorkspaceBuildModel;
import com.siteview.mde.internal.core.project.PDEProject;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.editor.*;
import com.siteview.mde.internal.ui.editor.build.BuildInputContext;
import com.siteview.mde.internal.ui.editor.build.BuildPage;
import com.siteview.mde.internal.ui.editor.context.InputContext;
import com.siteview.mde.internal.ui.nls.GetNonExternalizedStringsAction;
import com.siteview.mde.internal.ui.util.SharedLabelProvider;
import com.siteview.mde.internal.ui.wizards.tools.OrganizeManifestsAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.IProgressService;
import org.osgi.service.prefs.BackingStoreException;

public class MonitorOverviewPage extends LaunchShortcutOverviewPage {
	public static final String PAGE_ID = "overview"; //$NON-NLS-1$
	private PluginExportAction fExportAction;
	private MonitorInfoSection fInfoSection;
	private boolean fDisposed = false;
	private ILauncherFormPageHelper fLauncherHelper;

	public MonitorOverviewPage(MDELauncherFormEditor editor) {
		super(editor, PAGE_ID, MDEUIMessages.OverviewPage_tabName);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#getHelpResource()
	 */
	protected String getHelpResource() {
		return IHelpContextIds.MONITOR_PLUGIN_OVERVIEW;
	}

	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		if (isFragment()) {
			form.setImage(MDEPlugin.getDefault().getLabelProvider().get(MDEPluginImages.DESC_FRAGMENT_MF_OBJ));
		} else {
			form.setImage(MDEPlugin.getDefault().getLabelProvider().get(MDEPluginImages.DESC_PLUGIN_MF_OBJ));
		}
		form.setText(MDEUIMessages.MonitorEditor_OverviewPage_title);
		fillBody(managedForm, toolkit);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MONITOR_PLUGIN_OVERVIEW);
	}

	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		body.setLayout(FormLayoutFactory.createFormTableWrapLayout(true, 2));

		Composite left = toolkit.createComposite(body);
		left.setLayout(FormLayoutFactory.createFormPaneTableWrapLayout(false, 1));
		left.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
//		if (isFragment())
//		fInfoSection = new FragmentMonitorInfoSection(this, left);
//		else
//		fInfoSection = new PluginMonitorInfoSection(this, left);

		fInfoSection = new SimpleMonitorInfoSection(this, left);
//		fInfoSection = new PluginMonitorInfoSection(this, left);
		managedForm.addPart(fInfoSection);
		if (isBundle())
			managedForm.addPart(new MonitorExecutionEnvironmentSection(this, left));

		Composite right = toolkit.createComposite(body);
		right.setLayout(FormLayoutFactory.createFormPaneTableWrapLayout(false, 1));
		right.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		createContentSection(managedForm, right, toolkit);
		if (isEditable() || getMDEEditor().hasInputContext(MonitorInputContext.CONTEXT_ID))
			createExtensionSection(managedForm, right, toolkit);
		if (isEditable()) {
			createTestingSection(managedForm, isBundle() ? right : left, toolkit);
		}
		if (isEditable())
			createExportingSection(managedForm, right, toolkit);
	}

	private void createContentSection(IManagedForm managedForm, Composite parent, FormToolkit toolkit) {
		String sectionTitle;
//		if (isFragment()) {
//			sectionTitle = PDEUIMessages.ManifestEditor_ContentSection_ftitle;
//		} else {
//			sectionTitle = PDEUIMessages.ManifestEditor_ContentSection_title;
//		}
		sectionTitle = MDEUIMessages.MonitorEditor_ContentSection_title;

		Section section = createStaticSection(toolkit, parent, sectionTitle);

		Composite container = createStaticSectionClient(toolkit, section);

		FormText text = createClient(container, isFragment() ? MDEUIMessages.OverviewPage_fContent : MDEUIMessages.OverviewPage_content, toolkit);
		MDELabelProvider lp = MDEPlugin.getDefault().getLabelProvider();
		text.setImage("page", lp.get(MDEPluginImages.DESC_PAGE_OBJ, SharedLabelProvider.F_EDIT)); //$NON-NLS-1$

		if (!isBundle() && isEditable()) {
			String content;
			if (isFragment()) {
				content = MDEUIMessages.OverviewPage_fOsgi;
			} else {
				content = MDEUIMessages.OverviewPage_osgi;
			}
			text = createClient(container, content, toolkit);
		}
		section.setClient(container);
	}

	private void createExtensionSection(IManagedForm managedForm, Composite parent, FormToolkit toolkit) {
		String sectionTitle = MDEUIMessages.ManifestEditor_ExtensionSection_title;
		Section section = createStaticSection(toolkit, parent, sectionTitle);

		Composite container = createStaticSectionClient(toolkit, section);

		FormText text = createClient(container, isFragment() ? MDEUIMessages.OverviewPage_fExtensionContent : MDEUIMessages.OverviewPage_extensionContent, toolkit);
		MDELabelProvider lp = MDEPlugin.getDefault().getLabelProvider();
		text.setImage("page", lp.get(MDEPluginImages.DESC_PAGE_OBJ, SharedLabelProvider.F_EDIT)); //$NON-NLS-1$

		section.setClient(container);
	}

	private void createTestingSection(IManagedForm managedForm, Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(toolkit, parent, MDEUIMessages.ManifestEditor_TestingSection_title);
		MDELabelProvider lp = MDEPlugin.getDefault().getLabelProvider();

		Composite container = createStaticSectionClient(toolkit, section);

		String prefixText = (!((MonitorEditor) getEditor()).showExtensionTabs()) ? MDEUIMessages.OverviewPage_OSGiTesting : isFragment() ? MDEUIMessages.OverviewPage_fTesting : MDEUIMessages.OverviewPage_testing;
		FormText text = createClient(container, getLauncherText(getLauncherHelper().isOSGi(), prefixText), toolkit);
		text.setImage("run", lp.get(MDEPluginImages.DESC_RUN_EXC)); //$NON-NLS-1$
		text.setImage("debug", lp.get(MDEPluginImages.DESC_DEBUG_EXC)); //$NON-NLS-1$
		text.setImage("profile", lp.get(MDEPluginImages.DESC_PROFILE_EXC)); //$NON-NLS-1$
		section.setClient(container);
	}

	private void createExportingSection(IManagedForm managedForm, Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(toolkit, parent, MDEUIMessages.ManifestEditor_DeployingSection_title);
		Composite container = createStaticSectionClient(toolkit, section);
		createClient(container, isFragment() ? MDEUIMessages.OverviewPage_fDeploying : MDEUIMessages.OverviewPage_deploying, toolkit);
		section.setClient(container);
	}

	protected Composite createStaticSectionClient(FormToolkit toolkit, Composite parent) {
		Composite container = toolkit.createComposite(parent, SWT.NONE);
		container.setLayout(FormLayoutFactory.createSectionClientTableWrapLayout(false, 1));
		TableWrapData data = new TableWrapData(TableWrapData.FILL_GRAB);
		container.setLayoutData(data);
		return container;
	}

	private boolean isFragment() {
		IMonitorModelBase model = (IMonitorModelBase) getMDEEditor().getContextManager().getAggregateModel();
		return model.isFragmentModel();
	}

	private boolean isBundle() {
		return getMDEEditor().getContextManager().findContext(BundleInputContext.CONTEXT_ID) != null;
	}

	private boolean isEditable() {
		IMonitorModelBase model = (IMonitorModelBase) getMDEEditor().getContextManager().getAggregateModel();
		return model.isEditable();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.events.HyperlinkListener#linkActivated(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkActivated(HyperlinkEvent e) {
		String href = (String) e.getHref();
		// try page references
		if (href.equals("dependencies")) //$NON-NLS-1$
			getEditor().setActivePage(DependenciesPage.PAGE_ID);
		else if (href.equals("runtime")) //$NON-NLS-1$
			getEditor().setActivePage(RuntimePage.PAGE_ID);
		else if (href.equals("extensions")) { //$NON-NLS-1$
			if (getEditor().setActivePage(ExtensionsPage.PAGE_ID) == null)
				activateExtensionPages(ExtensionsPage.PAGE_ID);
		} else if (href.equals("ex-points")) { //$NON-NLS-1$
			if (getEditor().setActivePage(ExtensionPointsPage.PAGE_ID) == null)
				activateExtensionPages(ExtensionPointsPage.PAGE_ID);
		} else if (href.equals("build")) { //$NON-NLS-1$
			if (!getMDEEditor().hasInputContext(BuildInputContext.CONTEXT_ID)) {
				if (!MessageDialog.openQuestion(MDEPlugin.getActiveWorkbenchShell(), MDEUIMessages.OverviewPage_buildTitle, MDEUIMessages.OverviewPage_buildQuestion))
					return;
				IFile file = PDEProject.getBuildProperties(getMDEEditor().getCommonProject());
				WorkspaceBuildModel model = new WorkspaceBuildModel(file);
				model.save();
				IEditorInput in = new FileEditorInput(file);
				getMDEEditor().getContextManager().putContext(in, new BuildInputContext(getMDEEditor(), in, false));
			}
			getEditor().setActivePage(BuildPage.PAGE_ID);
		} else if (href.equals("export")) { //$NON-NLS-1$
			getExportAction().run();
		} else if (href.equals("action.convert")) { //$NON-NLS-1$
			handleConvert();
		} else if (href.equals("organize")) { //$NON-NLS-1$
			getEditor().doSave(null);
			OrganizeManifestsAction organizeAction = new OrganizeManifestsAction();
			organizeAction.selectionChanged(null, new StructuredSelection(getMDEEditor().getCommonProject()));
			organizeAction.run(null);
		} else if (href.equals("externalize")) { //$NON-NLS-1$
			getEditor().doSave(null);
			GetNonExternalizedStringsAction externalizeAction = new GetNonExternalizedStringsAction();
			externalizeAction.selectionChanged(null, new StructuredSelection(getMDEEditor().getCommonProject()));
			externalizeAction.run(null);
		} else
			super.linkActivated(e);
	}

	private PluginExportAction getExportAction() {
		if (fExportAction == null)
			fExportAction = new PluginExportAction((MDEFormEditor) getEditor());
		return fExportAction;
	}

	private void handleConvert() {
		try {
			// remove listeners of Info section before we convert.  If we don't 
			// we may get a model changed event while disposing the page.  Bug 156414
			fInfoSection.removeListeners();
			MDEFormEditor editor = getMDEEditor();
			IMonitorModelBase model = (IMonitorModelBase) editor.getAggregateModel();
			IRunnableWithProgress op = new CreateManifestOperation(model);
			IProgressService service = PlatformUI.getWorkbench().getProgressService();
			editor.doSave(null);
			service.runInUI(service, op, MDEPlugin.getWorkspace().getRoot());
			updateBuildProperties();
			editor.doSave(null);
		} catch (InvocationTargetException e) {
			MessageDialog.openError(MDEPlugin.getActiveWorkbenchShell(), MDEUIMessages.OverviewPage_error, e.getCause().getMessage());
			// if convert failed and this OverviewPage hasn't been removed from the editor, reattach listeners
			if (!fDisposed)
				fInfoSection.addListeners();
		} catch (InterruptedException e) {
			// if convert failed and this OverviewPage hasn't been removed from the editor, reattach listeners
			if (!fDisposed)
				fInfoSection.addListeners();
		}
	}

	private void updateBuildProperties() throws InvocationTargetException {
		try {
			InputContext context = getMDEEditor().getContextManager().findContext(BuildInputContext.CONTEXT_ID);
			if (context != null) {
				IBuildModel buildModel = (IBuildModel) context.getModel();
				IBuild build = buildModel.getBuild();
				IBuildEntry entry = build.getEntry("bin.includes"); //$NON-NLS-1$
				if (entry == null) {
					entry = buildModel.getFactory().createEntry("bin.includes"); //$NON-NLS-1$
					build.add(entry);
				}
				if (!entry.contains("META-INF")) //$NON-NLS-1$
					entry.addToken("META-INF/"); //$NON-NLS-1$
			}
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
	}

	private void activateExtensionPages(String activePageId) {
		MessageDialog mdiag = new MessageDialog(MDEPlugin.getActiveWorkbenchShell(), MDEUIMessages.OverviewPage_extensionPageMessageTitle, null, MDEUIMessages.OverviewPage_extensionPageMessageBody, MessageDialog.QUESTION, new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0);
		if (mdiag.open() != Window.OK)
			return;
		try {
			MonitorEditor manifestEditor = (MonitorEditor) getEditor();
			manifestEditor.addExtensionTabs();
			manifestEditor.setShowExtensions(true);
			manifestEditor.setActivePage(activePageId);
		} catch (PartInitException e) {
		} catch (BackingStoreException e) {
		}
	}

	public void dispose() {
		fDisposed = true;
		super.dispose();
	}

	protected short getIndent() {
		return 5;
	}

	protected ILauncherFormPageHelper getLauncherHelper() {
		if (fLauncherHelper == null)
			fLauncherHelper = new MonitorLauncherFormPageHelper(getPDELauncherEditor());
		return fLauncherHelper;
	}
}
