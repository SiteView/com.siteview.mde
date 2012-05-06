/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.wizards.extension;

import com.siteview.mde.internal.core.text.monitor.MonitorExtensionNode;
import com.siteview.mde.internal.core.text.monitor.MonitorExtensionPointNode;

import com.siteview.mde.core.monitor.*;

import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.internal.text.html.HTMLPrinter;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.osgi.util.NLS;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.core.ischema.ISchema;
import com.siteview.mde.internal.core.ischema.ISchemaElement;
import com.siteview.mde.internal.core.schema.*;
import com.siteview.mde.internal.core.util.*;
import com.siteview.mde.internal.ui.*;
import com.siteview.mde.internal.ui.dialogs.PluginSelectionDialog;
import com.siteview.mde.internal.ui.editor.contentassist.XMLInsertionComputer;
import com.siteview.mde.internal.ui.elements.DefaultContentProvider;
import com.siteview.mde.internal.ui.elements.ElementLabelProvider;
import com.siteview.mde.internal.ui.search.ShowDescriptionAction;
import com.siteview.mde.internal.ui.util.SharedLabelProvider;
import com.siteview.mde.internal.ui.util.TextUtil;
import com.siteview.mde.internal.ui.wizards.*;
import com.siteview.mde.internal.ui.wizards.templates.NewExtensionTemplateWizard;
import com.siteview.mde.ui.IBasePluginWizard;
import com.siteview.mde.ui.IExtensionWizard;
import com.siteview.mde.ui.templates.ITemplateSection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class PointSelectionPage extends BaseWizardSelectionPage {
	private TableViewer fPointListViewer;
	private TableViewer fTemplateViewer;

	private IMonitorModelBase fModel;
	private Button fFilterCheck;
	private IMonitorExtensionPoint fCurrentPoint;
	private HashSet fAvailableImports;
	private IProject fProject;
	private Label fTemplateLabel;
	private ExtensionTreeSelectionPage fWizardsPage;

	private IMonitorExtension fNewExtension;
	private WizardCollectionElement fTemplateCollection;
	private WizardCollectionElement fWizardCollection;
	private NewExtensionWizard fWizard;
	private Text fFilterText;
	private WildcardFilter fWildCardFilter;
	private Text fPointDescription;
	private Link fDescLink;
	private Browser fPointDescBrowser;

	class PointFilter extends ViewerFilter {
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (!fFilterCheck.getSelection())
				return true;

			IMonitorExtensionPoint point = (IMonitorExtensionPoint) element;
			if (point instanceof MonitorExtensionPointNode)
				return true;

			return fAvailableImports.contains(point.getMonitorBase().getId());
		}
	}

	class WildcardFilter extends ViewerFilter {
		private String wMatch = "*"; //$NON-NLS-1$

		protected void setMatchText(String match) {
			if (match.indexOf("*") != 0 & match.indexOf("?") != 0 & match.indexOf(".") != 0) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				match = "*" + match; //$NON-NLS-1$
			}
			wMatch = match + "*"; //$NON-NLS-1$
		}

		public boolean select(Viewer viewer, Object parentElement, Object element) {
			String text = ((PointLabelProvider) fPointListViewer.getLabelProvider()).getColumnText(element, 0);
			Pattern pattern = null;
			try {
				pattern = PatternConstructor.createPattern(wMatch, false);
			} catch (PatternSyntaxException e) {
				return false;
			}
			return pattern != null && pattern.matcher(text.subSequence(0, text.length())).matches();
		}
	}

	class TemplateContentProvider extends DefaultContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof IMonitorExtensionPoint) {
				IMonitorExtensionPoint point = (IMonitorExtensionPoint) inputElement;
				String pointID = IdUtil.getFullId(point, fModel);
				ArrayList result = new ArrayList();
				if (fTemplateCollection.getWizards() != null) {
					Object[] wizards = fTemplateCollection.getWizards().getChildren();
					for (int i = 0; i < wizards.length; i++) {
						String wizardContributorId = ((WizardElement) wizards[i]).getContributingId();
						if (wizardContributorId == null || pointID == null)
							continue;
						if (wizards[i] instanceof WizardElement && wizardContributorId.equals(pointID))
							result.add(wizards[i]);
					}
					return result.toArray();
				}
			}
			return new Object[0];
		}
	}

	class PointContentProvider extends DefaultContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			ArrayList extPoints = new ArrayList();
			IMonitorModelBase[] plugins = MonitorRegistry.getActiveModels();
			for (int i = 0; i < plugins.length; i++) {
				IMonitorExtensionPoint[] points = plugins[i].getMonitorBase().getExtensionPoints();
				String id = plugins[i].getMonitorBase().getId();
				if (id.equals(fModel.getMonitorBase().getId()))
					continue;
				for (int j = 0; j < points.length; j++)
					extPoints.add(points[j]);
			}

			IMonitorExtensionPoint[] points = fModel.getMonitorBase().getExtensionPoints();
			for (int i = 0; i < points.length; i++)
				extPoints.add(points[i]);

			return extPoints.toArray();
		}
	}

	class PointLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getText(Object obj) {
			return getColumnText(obj, 0);
		}

		public String getColumnText(Object obj, int index) {
			IMonitorExtensionPoint extPoint = (IMonitorExtensionPoint) obj;
			MDELabelProvider provider = MDEPlugin.getDefault().getLabelProvider();
			if (provider.isFullNameModeEnabled())
				return provider.getText(extPoint);

			return IdUtil.getFullId(extPoint, fModel);
		}

		public Image getImage(Object obj) {
			return getColumnImage(obj, 0);
		}

		public Image getColumnImage(Object obj, int index) {
			IMonitorExtensionPoint exp = (IMonitorExtensionPoint) obj;

			if (((TemplateContentProvider) fTemplateViewer.getContentProvider()).getElements(exp).length > 0) {
				return MDEPlugin.getDefault().getLabelProvider().get(MDEPluginImages.DESC_NEWEXP_WIZ_TOOL, 0);
			}

			// If the schema is deprecated add a warning flag
			int flags = 0;
			SchemaRegistry reg = MDECore.getDefault().getSchemaRegistry();
			ISchema schema = reg.getSchema(exp.getFullId());
			if (schema != null && schema.isDeperecated()) {
				MDEPlugin.getDefault().getLabelProvider();
				flags = SharedLabelProvider.F_WARNING;
			}

			return MDEPlugin.getDefault().getLabelProvider().get(MDEPluginImages.DESC_EXT_POINT_OBJ, flags);
		}
	}

	public PointSelectionPage(IProject project, IMonitorModelBase model, WizardCollectionElement element, WizardCollectionElement templates, NewExtensionWizard wizard) {
		super("pointSelectionPage", MDEUIMessages.NewExtensionWizard_PointSelectionPage_title); //$NON-NLS-1$ 
		this.fModel = model;
		this.fWizardCollection = element;
		this.fTemplateCollection = templates;
		this.fWizard = wizard;
		this.fProject = project;
		fWildCardFilter = new WildcardFilter();
		fAvailableImports = PluginSelectionDialog.getExistingImports(model, true);
		setTitle(MDEUIMessages.NewExtensionWizard_PointSelectionPage_title);
		setDescription(MDEUIMessages.NewExtensionWizard_PointSelectionPage_desc);
	}

	public void createControl(Composite parent) {
		// tab folder
		final TabFolder tabFolder = new TabFolder(parent, SWT.FLAT);
		TabItem firstTab = new TabItem(tabFolder, SWT.NULL);
		firstTab.setText(MDEUIMessages.PointSelectionPage_tab1);
		TabItem secondTab = new TabItem(tabFolder, SWT.NULL);
		secondTab.setText(MDEUIMessages.PointSelectionPage_tab2);
		secondTab.setControl(createWizardsPage(tabFolder));
		tabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateTabSelection(tabFolder.getSelectionIndex());
			}
		});
		// top level group
		Composite outerContainer = new Composite(tabFolder, SWT.NONE);
		firstTab.setControl(outerContainer);
		GridLayout layout = new GridLayout();
		outerContainer.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		outerContainer.setLayoutData(gd);

		Composite pointContainer = new Composite(outerContainer, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		pointContainer.setLayout(layout);
		gd = new GridData(GridData.FILL_BOTH);
		pointContainer.setLayoutData(gd);

		Composite labelContainer = new Composite(pointContainer, SWT.NONE);
		layout = new GridLayout(2, false);
		layout.marginHeight = layout.marginWidth = 0;
		labelContainer.setLayout(layout);
		labelContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label filterLabel = new Label(labelContainer, SWT.NONE);
		filterLabel.setText(MDEUIMessages.NewExtensionWizard_PointSelectionPage_availExtPoints_label);
		gd = new GridData();
		gd.verticalAlignment = GridData.CENTER;
		filterLabel.setLayoutData(gd);
		fFilterText = new Text(labelContainer, SWT.BORDER);
		fFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fFilterText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fWildCardFilter.setMatchText(fFilterText.getText());
				fPointListViewer.refresh();
			}
		});
		fFilterText.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN)
					fPointListViewer.getControl().setFocus();
			}

			public void keyReleased(KeyEvent e) {
			}
		});

		fFilterCheck = new Button(outerContainer, SWT.CHECK);
		fFilterCheck.setText(MDEUIMessages.NewExtensionWizard_PointSelectionPage_filterCheck);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fFilterCheck.setLayoutData(gd);
		fFilterCheck.setSelection(true);
		fFilterCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fPointListViewer.refresh();
			}
		});

		fPointListViewer = new TableViewer(pointContainer, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		fPointListViewer.setContentProvider(new PointContentProvider());
		fPointListViewer.setLabelProvider(new PointLabelProvider());
		fPointListViewer.addSelectionChangedListener(this);
		fPointListViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				if (canFinish()) {
					fWizard.performFinish();
					fWizard.getShell().close();
					fWizard.dispose();
					fWizard.setContainer(null);
				}
			}
		});
		fPointListViewer.addFilter(fWildCardFilter);
		fPointListViewer.setComparator(ListUtil.NAME_COMPARATOR);
		gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 150;
		fPointListViewer.getTable().setLayoutData(gd);

		Composite templateComposite = new Composite(outerContainer, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 4;
		layout.marginWidth = 0;
		templateComposite.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		templateComposite.setLayoutData(gd);

		fDescLink = new Link(templateComposite, SWT.NONE);
		fDescLink.setText(NLS.bind(MDEUIMessages.PointSelectionPage_extPointDesc, "")); //$NON-NLS-1$
		fDescLink.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fCurrentPoint != null)
					new ShowDescriptionAction(fCurrentPoint, true).run();
			}
		});
		fDescLink.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control c = null;
		Composite comp = new Composite(templateComposite, SWT.BORDER);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		try {
			c = fPointDescBrowser = new Browser(comp, SWT.NONE);
		} catch (SWTError e) {
		}
		if (c == null)
			c = fPointDescription = new Text(comp, SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.READ_ONLY);

		setPointDescriptionText(MDEUIMessages.NewExtensionWizard_PointSelectionPage_extPointDescription);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 80;
		c.setLayoutData(gd);

		fTemplateLabel = new Label(templateComposite, SWT.NONE | SWT.WRAP);
		fTemplateLabel.setText(MDEUIMessages.NewExtensionWizard_PointSelectionPage_contributedTemplates_title);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fTemplateLabel.setLayoutData(gd);

		SashForm templateSashForm = new SashForm(templateComposite, SWT.HORIZONTAL);
		templateSashForm.setLayout(new GridLayout());
		gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 80;
		templateSashForm.setLayoutData(gd);

		Composite wizardComposite = new Composite(templateSashForm, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		wizardComposite.setLayout(layout);
		gd = new GridData(GridData.FILL_BOTH | GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		wizardComposite.setLayoutData(gd);
		fTemplateViewer = new TableViewer(wizardComposite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		fTemplateViewer.setContentProvider(new TemplateContentProvider());
		fTemplateViewer.setLabelProvider(ElementLabelProvider.INSTANCE);
		fTemplateViewer.setComparator(ListUtil.NAME_COMPARATOR);
		fTemplateViewer.addSelectionChangedListener(this);
		gd = new GridData(GridData.FILL_BOTH);

		fTemplateViewer.getTable().setLayoutData(gd);
		TableItem[] selection = fPointListViewer.getTable().getSelection();
		if (selection != null && selection.length > 0)
			fTemplateViewer.setInput(selection[0]);
		fTemplateViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				if (canFlipToNextPage()) {
					advanceToNextPage();
				}
			}
		});

		Composite descriptionComposite = new Composite(templateSashForm, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		descriptionComposite.setLayout(layout);
		gd = new GridData(GridData.FILL_BOTH | GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		descriptionComposite.setLayoutData(gd);
		createDescriptionIn(descriptionComposite);

		initialize();
		setControl(tabFolder);
		Dialog.applyDialogFont(outerContainer);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(outerContainer.getParent(), IHelpContextIds.ADD_EXTENSIONS_SCHEMA_BASED);
	}

	private Control createWizardsPage(Composite parent) {
		fWizardsPage = new ExtensionTreeSelectionPage(fWizardCollection, null, MDEUIMessages.PointSelectionPage_categories);
		fWizardsPage.createControl(parent);
		fWizardsPage.setWizard(fWizard);
		fWizardsPage.getSelectionProvider().addSelectionChangedListener(this);
		fWizardsPage.init(fProject, fModel.getMonitorBase());
		return fWizardsPage.getControl();
	}

	public void advanceToNextPage() {
		getContainer().showPage(getNextPage());
	}

	public boolean canFlipToNextPage() {
		return getNextPage() != null;
	}

	public boolean canFinish() {
		if (fTemplateViewer != null) {
			ISelection selection = fTemplateViewer.getSelection();
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ssel = (IStructuredSelection) selection;
				if (!ssel.isEmpty())
					return false;
			}
		}
		if (fPointListViewer != null) {
			ISelection selection = fPointListViewer.getSelection();
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ssel = (IStructuredSelection) selection;
				if (ssel.isEmpty() == false)
					return true;
			}
		}
		return false;
	}

	public void dispose() {
		fWizardsPage.dispose();
		super.dispose();
	}

	public boolean finish() {
		String point = IdUtil.getFullId(fCurrentPoint, fModel);

		try {
			IMonitorExtension extension = fModel.getFactory().createExtension();
			// Set the point attribute
			// The point value overrides an auto-generated value
			extension.setPoint(point);
			fModel.getMonitorBase().add(extension);

			// Recursively auto-insert required child elements and attributes 
			// respecting multiplicity
			ISchemaElement schemaElement = null;
			// Get the extension's schema
			Object object = extension.getSchema();
			if ((object != null) && (object instanceof Schema)) {
				Schema schema = (Schema) object;
				if (extension instanceof MonitorExtensionNode) {
					// Get the extension's XML element name
					String elementName = ((MonitorExtensionNode) extension).getXMLTagName();
					// Find the extension's corresponding schema element
					schemaElement = schema.findElement(elementName);
				}
				// If there is an associated schema, do the auto-insert
				if (schemaElement != null) {
					XMLInsertionComputer.computeInsertion(schemaElement, extension);
				}
			}

			String pluginID = fCurrentPoint.getMonitorBase().getId();
			if (!(fCurrentPoint instanceof MonitorExtensionPointNode) && !fAvailableImports.contains(pluginID) && !(fCurrentPoint.getMonitorBase() instanceof IFragment)) {
				if (MessageDialog.openQuestion(getShell(), MDEUIMessages.NewExtensionWizard_PointSelectionPage_dependencyTitle, NLS.bind(MDEUIMessages.NewExtensionWizard_PointSelectionPage_dependencyMessage, new String[] {pluginID, fCurrentPoint.getId()}))) {
					IMonitorImport importNode = fModel.getMonitorFactory().createImport();
					importNode.setId(pluginID);
					fModel.getMonitorBase().add(importNode);
				}
			}
		} catch (CoreException e) {
			MDEPlugin.logException(e);
		}
		return true;
	}

	public IMonitorExtension getNewExtension() {
		return fNewExtension;
	}

	protected void initialize() {
		fPointListViewer.addFilter(new PointFilter());
		fPointListViewer.setInput(MDECore.getDefault().getModelManager());
		fPointListViewer.getTable().setFocus();
	}

	public void selectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel != null && !ssel.isEmpty()) {
				Object element = ssel.getFirstElement();
				if (element instanceof WizardElement)
					handleTemplateSelection((WizardElement) element);
				else if (element instanceof IMonitorExtensionPoint)
					handlePointSelection((IMonitorExtensionPoint) element);
			} else {
				setDescription(""); //$NON-NLS-1$
				setDescriptionText(""); //$NON-NLS-1$
				fTemplateLabel.setText(MDEUIMessages.NewExtensionWizard_PointSelectionPage_contributedTemplates_title);
				setPointDescriptionText(MDEUIMessages.PointSelectionPage_noDescAvailable);
				fDescLink.setText(NLS.bind(MDEUIMessages.PointSelectionPage_extPointDesc, "")); //$NON-NLS-1$
				setSelectedNode(null);
				setPageComplete(false);
			}
			getContainer().updateButtons();
		}
	}

	private void handleTemplateSelection(WizardElement element) {
		setSelectedNode(createWizardNode(element));
		setDescriptionText(element.getDescription());
		setDescription(NLS.bind(MDEUIMessages.NewExtensionWizard_PointSelectionPage_templateDescription, element.getLabel()));
		setPageComplete(false);
	}

	private void handlePointSelection(IMonitorExtensionPoint element) {
		fCurrentPoint = element;
		fTemplateViewer.setInput(fCurrentPoint);
		fTemplateViewer.setSelection(StructuredSelection.EMPTY);
		String fullPointID = IdUtil.getFullId(fCurrentPoint, fModel);

		String description = XMLComponentRegistry.Instance().getDescription(fullPointID, XMLComponentRegistry.F_SCHEMA_COMPONENT);
		String name = XMLComponentRegistry.Instance().getName(fullPointID, XMLComponentRegistry.F_SCHEMA_COMPONENT);
		URL url = null;
		if ((description == null) || (name == null)) {
			url = SchemaRegistry.getSchemaURL(fCurrentPoint, fModel);
		}
		if (url != null) {
			SchemaAnnotationHandler handler = new SchemaAnnotationHandler();
			SchemaUtil.parseURL(url, handler);
			description = handler.getDescription();
			name = handler.getName();
		}
		if (description == null) {
			setPointDescriptionText(MDEUIMessages.PointSelectionPage_noDescAvailable);
		} else {
			setPointDescriptionText(description);
		}
		if (name == null) {
			name = fullPointID;
		}
		// Check if the extension point is deprecated and display a warning
		SchemaRegistry reg = MDECore.getDefault().getSchemaRegistry();
		ISchema schema = reg.getSchema(fCurrentPoint.getFullId());
		if (schema != null && schema.isDeperecated()) {
			setMessage(NLS.bind(MDEUIMessages.NewExtensionWizard_PointSelectionPage_pluginDescription_deprecated, name), IMessageProvider.WARNING);
		} else {
			setMessage(null);
			setDescription(NLS.bind(MDEUIMessages.NewExtensionWizard_PointSelectionPage_pluginDescription, name));
		}
		setDescriptionText(""); //$NON-NLS-1$
		fTemplateLabel.setText(NLS.bind(MDEUIMessages.NewExtensionWizard_PointSelectionPage_contributedTemplates_label, name.toLowerCase(Locale.ENGLISH)));
		fDescLink.setText(NLS.bind(MDEUIMessages.PointSelectionPage_extPointDesc, name));
		setSelectedNode(null);
		setPageComplete(true);

		XMLComponentRegistry.Instance().putDescription(fullPointID, description, XMLComponentRegistry.F_SCHEMA_COMPONENT);
		XMLComponentRegistry.Instance().putName(fullPointID, name, XMLComponentRegistry.F_SCHEMA_COMPONENT);
	}

	private void updateTabSelection(int index) {
		if (index == 0) {
			// extension point page
			ISelection selection = fTemplateViewer.getSelection();
			if (selection.isEmpty() == false)
				selectionChanged(new SelectionChangedEvent(fTemplateViewer, selection));
			else
				selectionChanged(new SelectionChangedEvent(fPointListViewer, fPointListViewer.getSelection()));
			fFilterText.setFocus();
		} else {
			// wizard page
			ISelectionProvider provider = fWizardsPage.getSelectionProvider();
			selectionChanged(new SelectionChangedEvent(provider, provider.getSelection()));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.BaseWizardSelectionPage#createWizardNode(org.eclipse.pde.internal.ui.wizards.WizardElement)
	 */
	protected IWizardNode createWizardNode(WizardElement element) {
		return new WizardNode(this, element) {
			public IBasePluginWizard createWizard() throws CoreException {
				IExtensionWizard wizard = createWizard(wizardElement);
				if (wizard == null)
					throw new CoreException(new Status(IStatus.ERROR, wizardElement.getConfigurationElement().getNamespaceIdentifier(), MDEUIMessages.PointSelectionPage_cannotFindTemplate));
				wizard.init(fProject, fModel);
				return wizard;
			}

			protected IExtensionWizard createWizard(WizardElement element) throws CoreException {
				if (element.isTemplate()) {
					IConfigurationElement template = element.getTemplateElement();
					if (template == null)
						return null;
					ITemplateSection section = (ITemplateSection) template.createExecutableExtension("class"); //$NON-NLS-1$
					return new NewExtensionTemplateWizard(section);
				}
				return (IExtensionWizard) element.createExecutableExtension();
			}
		};
	}

	public void checkModel() {
		IWizardNode node = getSelectedNode();
		if (node == null)
			return;
		IWizard wizard = node.getWizard();
		if (wizard instanceof NewExtensionTemplateWizard) {
			if (((NewExtensionTemplateWizard) wizard).updatedDependencies()) {
				if (MessageDialog.openQuestion(getShell(), MDEUIMessages.PointSelectionPage_newDepFound, MDEUIMessages.PointSelectionPage_newDepMessage)) {
					fWizard.getEditor().doSave(new NullProgressMonitor());
				}
			}
		}
	}

	public void setVisible(boolean visible) {
		if (visible)
			fFilterText.setFocus();
		super.setVisible(visible);
	}

	private void setPointDescriptionText(String text) {
		if (fPointDescBrowser != null) {
			StringBuffer desc = new StringBuffer();
			HTMLPrinter.insertPageProlog(desc, 0, TextUtil.getJavaDocStyleSheerURL());
			desc.append(text);
			HTMLPrinter.addPageEpilog(desc);
			fPointDescBrowser.setText(desc.toString());
		} else
			fPointDescription.setText(PDEHTMLHelper.stripTags(text));
	}
}
