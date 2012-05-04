/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 218618
 *******************************************************************************/
package com.siteview.mde.internal.ui;

import com.siteview.mde.internal.core.monitor.ImportObject;

import com.siteview.mde.core.monitor.*;

import java.util.Locale;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.util.NLS;
import com.siteview.mde.core.build.IBuildEntry;
import com.siteview.mde.internal.core.*;
import com.siteview.mde.internal.core.builders.CompilerFlags;
import com.siteview.mde.internal.core.feature.*;
import com.siteview.mde.internal.core.ifeature.*;
import com.siteview.mde.internal.core.iproduct.IProductFeature;
import com.siteview.mde.internal.core.iproduct.IProductPlugin;
import com.siteview.mde.internal.core.ischema.*;
import com.siteview.mde.internal.core.isite.*;
import com.siteview.mde.internal.core.text.bundle.*;
import com.siteview.mde.internal.core.util.VersionUtil;
import com.siteview.mde.internal.ui.elements.NamedElement;
import com.siteview.mde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.internal.BidiUtil;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Version;

public class MDELabelProvider extends SharedLabelProvider {
	private static final String SYSTEM_BUNDLE = "system.bundle"; //$NON-NLS-1$

	public MDELabelProvider() {
	}

	public String getText(Object obj) {
		if (obj instanceof IMonitorModelBase) {
			return getObjectText(((IMonitorModelBase) obj).getMonitorBase());
		}
		if (obj instanceof IMonitorBase) {
			return getObjectText((IMonitorBase) obj);
		}
		if (obj instanceof ImportObject) {
			return getObjectText((ImportObject) obj);
		}
		if (obj instanceof IProductPlugin) {
			return getObjectText((IProductPlugin) obj);
		}
		if (obj instanceof BundleDescription) {
			return getObjectText((BundleDescription) obj);
		}
		if (obj instanceof IMonitorImport) {
			return getObjectText((IMonitorImport) obj);
		}
		if (obj instanceof IMonitorLibrary) {
			return getObjectText((IMonitorLibrary) obj);
		}
		if (obj instanceof IMonitorExtension) {
			return getObjectText((IMonitorExtension) obj);
		}
		if (obj instanceof IPluginExtensionPoint) {
			return getObjectText((IPluginExtensionPoint) obj);
		}
		if (obj instanceof NamedElement) {
			return ((NamedElement) obj).getLabel();
		}
		if (obj instanceof ISchemaObject) {
			return getObjectText((ISchemaObject) obj);
		}
		if (obj instanceof FeaturePlugin) {
			return getObjectText((FeaturePlugin) obj);
		}
		if (obj instanceof FeatureImport) {
			return getObjectText((FeatureImport) obj);
		}
		if (obj instanceof IFeatureModel) {
			return getObjectText((IFeatureModel) obj);
		}
		if (obj instanceof FeatureChild) {
			return getObjectText((FeatureChild) obj);
		}
		if (obj instanceof IProductFeature) {
			return getObjectText((IProductFeature) obj);
		}
		if (obj instanceof ISiteFeature) {
			return getObjectText((ISiteFeature) obj);
		}
		if (obj instanceof ISiteArchive) {
			return getObjectText((ISiteArchive) obj);
		}
		if (obj instanceof ISiteCategoryDefinition) {
			return getObjectText((ISiteCategoryDefinition) obj);
		}
		if (obj instanceof ISiteCategory) {
			return getObjectText((ISiteCategory) obj);
		}
		if (obj instanceof IBuildEntry) {
			return getObjectText((IBuildEntry) obj);
		}
		if (obj instanceof PackageObject) {
			return getObjectText((PackageObject) obj);
		}
		if (obj instanceof ExecutionEnvironment) {
			return getObjectText((ExecutionEnvironment) obj);
		}
		if (obj instanceof Locale) {
			return getObjectText((Locale) obj);
		}
		if (obj instanceof IStatus) {
			return getObjectText((IStatus) obj);
		}
		return super.getText(obj);
	}

	private String getObjectText(ExecutionEnvironment environment) {
		return preventNull(environment.getName());
	}

	public String getObjectText(IMonitorBase pluginBase) {
		String name = isFullNameModeEnabled() ? pluginBase.getTranslatedName() : pluginBase.getId();
		name = preventNull(name);
		String version = pluginBase.getVersion();

		String text;

		if (version != null && version.length() > 0)
			text = name + ' ' + formatVersion(pluginBase.getVersion());
		else
			text = name;
		if (SYSTEM_BUNDLE.equals(pluginBase.getId())) {
			text += getSystemBundleInfo();
		}
		if (pluginBase.getModel() != null && !pluginBase.getModel().isInSync())
			text += " " + MDEUIMessages.PluginModelManager_outOfSync; //$NON-NLS-1$
		return text;
	}

	private String getSystemBundleInfo() {
		IMonitorBase systemBundle = MonitorRegistry.findModel(SYSTEM_BUNDLE).getMonitorBase();
		return NLS.bind(" [{0}]", systemBundle.getId()); //$NON-NLS-1$
	}

	private String preventNull(String text) {
		return text != null ? text : ""; //$NON-NLS-1$
	}

	public String getObjectText(IMonitorExtension extension) {
		return preventNull(isFullNameModeEnabled() ? extension.getTranslatedName() : extension.getPoint());
	}

	public String getObjectText(IPluginExtensionPoint point) {
		return preventNull(isFullNameModeEnabled() ? point.getTranslatedName() : point.getId());
	}

	public String getObjectText(ImportObject obj) {
		String version = obj.getImport().getVersion();
		if (version != null && version.length() > 0)
			version = formatVersion(version);

		String text = isFullNameModeEnabled() ? obj.toString() : preventNull(obj.getId());
		if (SYSTEM_BUNDLE.equals(obj.getId()))
			return text + getSystemBundleInfo();
		return version == null || version.length() == 0 ? text : text + " " + version; //$NON-NLS-1$
	}

	public String getObjectText(IProductPlugin obj) {
		// TODO, should we just get the model and call the proper method?
		String name = obj.getId();
		String version = obj.getVersion();
		String text;
		if (version != null && version.length() > 0)
			text = name + ' ' + formatVersion(obj.getVersion());
		else
			text = name;
		return preventNull(text);
	}

	public String getObjectText(BundleDescription bundle) {
		String id = bundle.getSymbolicName();
		if (isFullNameModeEnabled()) {
			IMonitorModelBase model = MonitorRegistry.findModel(bundle);
			if (model != null) {
				return model.getMonitorBase().getTranslatedName();
			}
			return id != null ? id : "?"; //$NON-NLS-1$
		}
		return preventNull(id);
	}

	public String getObjectText(IMonitorImport obj) {
		if (isFullNameModeEnabled()) {
			String id = obj.getId();
			IMonitorModelBase model = MonitorRegistry.findModel(obj.getId());
			if (model != null) {
				return model.getMonitorBase().getTranslatedName();
			}
			return id != null ? id : "?"; //$NON-NLS-1$
		}
		return preventNull(obj.getId());
	}

	public String getObjectText(IBuildEntry obj) {
		return obj.getName();
	}

	public String getObjectText(IMonitorLibrary obj) {
		return preventNull(obj.getName());
	}

	public String getObjectText(ISchemaObject obj) {
		StringBuffer text = new StringBuffer(obj.getName());
		if (obj instanceof ISchemaRepeatable) {
			ISchemaRepeatable rso = (ISchemaRepeatable) obj;
			boolean unbounded = rso.getMaxOccurs() == Integer.MAX_VALUE;
			int maxOccurs = rso.getMaxOccurs();
			int minOccurs = rso.getMinOccurs();
			if (maxOccurs != 1 || minOccurs != 1) {
				if (isRTL() && BidiUtil.isBidiPlatform())
					text.append('\u200f');
				text.append(" ("); //$NON-NLS-1$
				text.append(minOccurs);
				text.append(" - "); //$NON-NLS-1$
				if (unbounded)
					text.append('*');
				else
					text.append(maxOccurs);
				text.append(')');
			}
		}

		return text.toString();
	}

	private String getObjectText(Locale obj) {
		String country = " (" + obj.getDisplayCountry() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		return obj.getDisplayLanguage() + (" ()".equals(country) ? "" : country); //$NON-NLS-1$//$NON-NLS-2$
	}

	public String getObjectText(FeaturePlugin obj) {
		String name = isFullNameModeEnabled() ? obj.getLabel() : obj.getId();
		String version = obj.getVersion();

		String text;

		if (version != null && version.length() > 0)
			text = name + ' ' + formatVersion(version);
		else
			text = name;
		return preventNull(text);
	}

	public String getObjectText(FeatureImport obj) {
		int type = obj.getType();
		if (type == IFeatureImport.PLUGIN) {
			IMonitor plugin = obj.getPlugin();
			if (plugin != null && isFullNameModeEnabled()) {
				return preventNull(plugin.getTranslatedName());
			}
		} else if (type == IFeatureImport.FEATURE) {
			IFeature feature = obj.getFeature();
			if (feature != null && isFullNameModeEnabled()) {
				return preventNull(feature.getTranslatableLabel());
			}
		}
		return preventNull(obj.getId());
	}

	public String getObjectText(IFeatureModel obj, boolean showVersion) {
		IFeature feature = obj.getFeature();
		String name = (isFullNameModeEnabled()) ? feature.getTranslatableLabel() : feature.getId();
		if (!showVersion)
			return preventNull(name);
		return preventNull(name) + ' ' + formatVersion(feature.getVersion());

	}

	public String getObjectText(IFeatureModel obj) {
		return getObjectText(obj, true);
	}

	public String getObjectText(FeatureChild obj) {
		return preventNull(obj.getId()) + ' ' + formatVersion(obj.getVersion());
	}

	public String getObjectText(IProductFeature obj) {
		String name = preventNull(obj.getId());
		if (VersionUtil.isEmptyVersion(obj.getVersion()))
			return name;
		return name + ' ' + formatVersion(obj.getVersion());
	}

	public String getObjectText(ISiteFeature obj) {
		IFeatureModel model = MDECore.getDefault().getFeatureModelManager().findFeatureModel(obj.getId(), obj.getVersion() != null ? obj.getVersion() : "0.0.0"); //$NON-NLS-1$
		if (model != null)
			return getObjectText(model);
		return preventNull(obj.getURL());
	}

	public String getObjectText(ISiteArchive obj) {
		return preventNull(obj.getPath());
	}

	public String getObjectText(ISiteCategoryDefinition obj) {
		return preventNull(obj.getLabel());
	}

	public String getObjectText(PackageObject obj) {
		StringBuffer buffer = new StringBuffer(obj.getName());
		String version = obj.getVersion();
		if (version != null && !version.equals(Version.emptyVersion.toString())) {
			// Format version range for ImportPackageObject.  ExportPackageObject is handled correctly in this function
			version = formatVersion(version);
			buffer.append(' ').append(version);
		}
		return buffer.toString();
	}

	public String getObjectText(ISiteCategory obj) {
		ISiteCategoryDefinition def = obj.getDefinition();
		if (def != null)
			return preventNull(def.getLabel());
		return preventNull(obj.getName());
	}

	private String getObjectText(IStatus status) {
		return status.getMessage();
	}

	public Image getImage(Object obj) {
		if (obj instanceof IMonitor) {
			return getObjectImage((IMonitor) obj);
		}
		if (obj instanceof IFragment) {
			return getObjectImage((IFragment) obj);
		}
		if (obj instanceof IMonitorModel) {
			return getObjectImage(((IMonitorModel) obj).getMonitor());
		}
		if (obj instanceof IFragmentModel) {
			return getObjectImage(((IFragmentModel) obj).getFragment());
		}
		if (obj instanceof ImportObject) {
			return getObjectImage((ImportObject) obj);
		}
		if (obj instanceof IMonitorImport) {
			return getObjectImage((IMonitorImport) obj);
		}
		if (obj instanceof IProductPlugin) {
			return getObjectImage((IProductPlugin) obj);
		}
		if (obj instanceof BundleDescription) {
			return getObjectImage((BundleDescription) obj);
		}
		if (obj instanceof IMonitorLibrary) {
			return getObjectImage((IMonitorLibrary) obj);
		}
		if (obj instanceof IMonitorExtension) {
			return getObjectImage((IMonitorExtension) obj);
		}
		if (obj instanceof IPluginExtensionPoint) {
			return getObjectImage((IPluginExtensionPoint) obj);
		}
		if (obj instanceof NamedElement) {
			return ((NamedElement) obj).getImage();
		}
		if (obj instanceof ISchemaElement) {
			return getObjectImage((ISchemaElement) obj);
		}
		if (obj instanceof ISchemaAttribute) {
			return getObjectImage((ISchemaAttribute) obj);
		}
		if (obj instanceof ISchemaInclude) {
			ISchema schema = ((ISchemaInclude) obj).getIncludedSchema();
			return get(MDEPluginImages.DESC_PAGE_OBJ, schema == null || !schema.isValid() ? F_ERROR : 0);
		}
		if (obj instanceof IDocumentSection || obj instanceof ISchema) {
			return get(MDEPluginImages.DESC_DOC_SECTION_OBJ);
		}
		if (obj instanceof ISchemaCompositor) {
			return getObjectImage((ISchemaCompositor) obj);
		}
		if (obj instanceof IFeatureURLElement) {
			return getObjectImage((IFeatureURLElement) obj);
		}
		if (obj instanceof IFeatureModel) {
			int flags = 0;
			if (((IFeatureModel) obj).getUnderlyingResource() == null)
				flags |= F_EXTERNAL;
			return get(MDEPluginImages.DESC_FEATURE_OBJ, flags);
		}
		if (obj instanceof IFeatureChild) {
			return getObjectImage((IFeatureChild) obj);
		}
		if (obj instanceof IProductFeature) {
			return getObjectImage((IProductFeature) obj);
		}
		if (obj instanceof IFeaturePlugin) {
			return getObjectImage((IFeaturePlugin) obj);
		}
		if (obj instanceof IFeatureData) {
			return getObjectImage((IFeatureData) obj);
		}
		if (obj instanceof IFeatureImport) {
			return getObjectImage((IFeatureImport) obj);
		}
		if (obj instanceof IFeatureInfo) {
			return getObjectImage((IFeatureInfo) obj);
		}
		if (obj instanceof IBuildEntry) {
			return get(MDEPluginImages.DESC_BUILD_VAR_OBJ);
		}
		if (obj instanceof ISiteFeature) {
			return getObjectImage((ISiteFeature) obj);
		}
		if (obj instanceof ISiteArchive) {
			return getObjectImage((ISiteArchive) obj);
		}
		if (obj instanceof ISiteCategoryDefinition) {
			return getObjectImage((ISiteCategoryDefinition) obj);
		}
		if (obj instanceof ISiteCategory) {
			return getObjectImage((ISiteCategory) obj);
		}
		if (obj instanceof ExportPackageObject) {
			return getObjectImage((ExportPackageObject) obj);
		}
		if (obj instanceof PackageObject) {
			return getObjectImage((PackageObject) obj);
		}
		if (obj instanceof ExecutionEnvironment) {
			return getObjectImage((ExecutionEnvironment) obj);
		}
		if (obj instanceof ResolverError) {
			return getObjectImage((ResolverError) obj);
		}
		if (obj instanceof Locale) {
			return get(MDEPluginImages.DESC_DISCOVERY);
		}
		if (obj instanceof IStatus) {
			return getObjectImage((IStatus) obj);
		}
		return super.getImage(obj);
	}

	private Image getObjectImage(ResolverError obj) {
		int type = obj.getType();
		switch (type) {
			case ResolverError.MISSING_IMPORT_PACKAGE :
			case ResolverError.EXPORT_PACKAGE_PERMISSION :
			case ResolverError.IMPORT_PACKAGE_PERMISSION :
			case ResolverError.IMPORT_PACKAGE_USES_CONFLICT :
				return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKAGE);
			case ResolverError.MISSING_EXECUTION_ENVIRONMENT :
				return get(MDEPluginImages.DESC_JAVA_LIB_OBJ);
			case ResolverError.MISSING_FRAGMENT_HOST :
			case ResolverError.MISSING_REQUIRE_BUNDLE :
			case ResolverError.PROVIDE_BUNDLE_PERMISSION :
			case ResolverError.REQUIRE_BUNDLE_PERMISSION :
			case ResolverError.REQUIRE_BUNDLE_USES_CONFLICT :
			case ResolverError.HOST_BUNDLE_PERMISSION :
				return get(MDEPluginImages.DESC_PLUGIN_OBJ);
		}
		return null;
	}

	private Image getObjectImage(ExecutionEnvironment environment) {
		return get(MDEPluginImages.DESC_JAVA_LIB_OBJ);
	}

	private Image getObjectImage(IMonitor plugin) {
		return getObjectImage(plugin, false, false);
	}

	private Image getObjectImage(BundleDescription bundle) {
		return bundle.getHost() == null ? get(MDEPluginImages.DESC_PLUGIN_OBJ) : get(MDEPluginImages.DESC_FRAGMENT_OBJ);
	}

	public Image getObjectImage(IMonitor plugin, boolean checkEnabled, boolean javaSearch) {
		IMonitorModelBase model = plugin.getMonitorModel();
		int flags = getModelFlags(model);

		if (javaSearch)
			flags |= F_JAVA;
		ImageDescriptor desc = MDEPluginImages.DESC_PLUGIN_OBJ;
		if (checkEnabled && model.isEnabled() == false)
			desc = MDEPluginImages.DESC_EXT_PLUGIN_OBJ;
		return get(desc, flags);
	}

	private int getModelFlags(IMonitorModelBase model) {
		int flags = 0;
		if (!(model.isLoaded() && model.isInSync()))
			flags = F_ERROR;
		IResource resource = model.getUnderlyingResource();
		if (resource == null) {
			flags |= F_EXTERNAL;
		} else {
			IProject project = resource.getProject();
			try {
				if (WorkspaceModelManager.isBinaryProject(project)) {
					String property = project.getPersistentProperty(MDECore.EXTERNAL_PROJECT_PROPERTY);
					if (property != null) {
						/*
						if (property.equals(PDECore.EXTERNAL_PROJECT_VALUE))
							flags |= F_EXTERNAL;
						else if (property.equals(PDECore.BINARY_PROJECT_VALUE))
						*/
						flags |= F_BINARY;
					}
				}
			} catch (CoreException e) {
			}
		}
		return flags;
	}

	private Image getObjectImage(IFragment fragment) {
		return getObjectImage(fragment, false, false);
	}

	public Image getObjectImage(IFragment fragment, boolean checkEnabled, boolean javaSearch) {
		IMonitorModelBase model = fragment.getMonitorModel();
		int flags = getModelFlags(model);
		if (javaSearch)
			flags |= F_JAVA;
		ImageDescriptor desc = MDEPluginImages.DESC_FRAGMENT_OBJ;
		if (checkEnabled && !model.isEnabled())
			desc = MDEPluginImages.DESC_EXT_FRAGMENT_OBJ;
		return get(desc, flags);
	}

	private Image getObjectImage(ImportObject iobj) {
		int flags = 0;
		IMonitorImport iimport = iobj.getImport();
		if (!iobj.isResolved())
			flags = iimport.isOptional() ? F_WARNING : F_ERROR;
		else if (iimport.isReexported())
			flags = F_EXPORT;
		if (iimport.isOptional())
			flags |= F_OPTIONAL;
		IMonitor plugin = iobj.getPlugin();
		if (plugin != null) {
			IMonitorModelBase model = plugin.getMonitorModel();
			flags |= getModelFlags(model);
		}
		return get(getRequiredPluginImageDescriptor(iimport), flags);
	}

	protected ImageDescriptor getRequiredPluginImageDescriptor(IMonitorImport iobj) {
		return MDEPluginImages.DESC_REQ_PLUGIN_OBJ;
	}

	private Image getObjectImage(IMonitorImport obj) {
		int flags = 0;
		if (obj.isReexported())
			flags |= F_EXPORT;
		return get(getRequiredPluginImageDescriptor(obj), flags);
	}

	private Image getObjectImage(IProductPlugin obj) {
		Version version = (obj.getVersion() != null && obj.getVersion().length() > 0 && !obj.getVersion().equals("0.0.0")) ? Version.parseVersion(obj.getVersion()) : null; //$NON-NLS-1$
		BundleDescription desc = TargetPlatformHelper.getState().getBundle(obj.getId(), version);
		if (desc != null) {
			return desc.getHost() == null ? get(MDEPluginImages.DESC_PLUGIN_OBJ) : get(MDEPluginImages.DESC_FRAGMENT_OBJ);
		}
		return get(MDEPluginImages.DESC_PLUGIN_OBJ, F_ERROR);
	}

	private Image getObjectImage(IMonitorLibrary library) {
		return get(MDEPluginImages.DESC_JAVA_LIB_OBJ);
	}

	private Image getObjectImage(IMonitorExtension point) {
		return get(MDEPluginImages.DESC_EXTENSION_OBJ);
	}

	private Image getObjectImage(IPluginExtensionPoint point) {
		return get(MDEPluginImages.DESC_EXT_POINT_OBJ);
	}

	private Image getObjectImage(ISchemaElement element) {
		int flags = 0;
		if (element instanceof ISchemaObjectReference && ((ISchemaObjectReference) element).getReferencedObject() == null)
			flags |= F_ERROR;
		ImageDescriptor desc = element instanceof ISchemaObjectReference ? MDEPluginImages.DESC_XML_ELEMENT_REF_OBJ : MDEPluginImages.DESC_GEL_SC_OBJ;
		return get(desc, flags);
	}

	private Image getObjectImage(ISchemaAttribute att) {
		int kind = att.getKind();
		String type = att.getType().getName();
		int use = att.getUse();
		int flags = 0;
		if (use == ISchemaAttribute.OPTIONAL)
			flags = 0; //|= F_OPTIONAL;
		if (kind == IMetaAttribute.JAVA)
			return get(MDEPluginImages.DESC_ATT_CLASS_OBJ, flags);
		if (kind == IMetaAttribute.RESOURCE)
			return get(MDEPluginImages.DESC_ATT_FILE_OBJ, flags);
		if (kind == IMetaAttribute.IDENTIFIER)
			return get(MDEPluginImages.DESC_ATT_ID_OBJ, flags);
		if (type.equals(ISchemaAttribute.TYPES[ISchemaAttribute.BOOL_IND]))
			return get(MDEPluginImages.DESC_ATT_BOOLEAN_OBJ, flags);

		return get(MDEPluginImages.DESC_ATT_STRING_OBJ);
	}

	private Image getObjectImage(ISchemaCompositor compositor) {
		switch (compositor.getKind()) {
			case ISchemaCompositor.ALL :
				return get(MDEPluginImages.DESC_ALL_SC_OBJ);
			case ISchemaCompositor.CHOICE :
				return get(MDEPluginImages.DESC_CHOICE_SC_OBJ);
			case ISchemaCompositor.SEQUENCE :
				return get(MDEPluginImages.DESC_SEQ_SC_OBJ);
			case ISchemaCompositor.GROUP :
				return get(MDEPluginImages.DESC_GROUP_SC_OBJ);
		}
		return null;
	}

	private Image getObjectImage(IFeatureURLElement url) {
		return get(MDEPluginImages.DESC_LINK_OBJ);
	}

	private Image getObjectImage(IFeaturePlugin plugin) {
		int flags = 0;
		if (((FeaturePlugin) plugin).getPluginBase() == null) {
			int cflag = CompilerFlags.getFlag(null, CompilerFlags.F_UNRESOLVED_PLUGINS);
			if (cflag == CompilerFlags.ERROR)
				flags = F_ERROR;
			else if (cflag == CompilerFlags.WARNING)
				flags = F_WARNING;
		}
		if (plugin.isFragment())
			return get(MDEPluginImages.DESC_FRAGMENT_OBJ, flags);
		return get(MDEPluginImages.DESC_PLUGIN_OBJ, flags);
	}

	private Image getObjectImage(IFeatureChild feature) {
		int flags = 0;
		if (((FeatureChild) feature).getReferencedFeature() == null) {
			int cflag = CompilerFlags.getFlag(null, CompilerFlags.F_UNRESOLVED_FEATURES);
			if (cflag == CompilerFlags.ERROR)
				flags = F_ERROR;
			else if (cflag == CompilerFlags.WARNING)
				flags = F_WARNING;
		}
		return get(MDEPluginImages.DESC_FEATURE_OBJ, flags);
	}

	private Image getObjectImage(IProductFeature feature) {
		int flags = 0;
		String version = feature.getVersion().length() > 0 ? feature.getVersion() : "0.0.0"; //$NON-NLS-1$
		IFeatureModel model = MDECore.getDefault().getFeatureModelManager().findFeatureModel(feature.getId(), version);
		if (model == null)
			flags = F_ERROR;
		return get(MDEPluginImages.DESC_FEATURE_OBJ, flags);
	}

	private Image getObjectImage(IFeatureData data) {
		int flags = 0;
		if (!data.exists())
			flags = F_ERROR;
		ImageDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(data.getId());
		return get(desc, flags);
	}

	private Image getObjectImage(IFeatureImport obj) {
		FeatureImport iimport = (FeatureImport) obj;
		int type = iimport.getType();
		ImageDescriptor base;
		int flags = 0;

		if (type == IFeatureImport.FEATURE) {
			base = MDEPluginImages.DESC_FEATURE_OBJ;
			IFeature feature = iimport.getFeature();
			if (feature == null)
				flags = F_ERROR;
		} else {
			base = MDEPluginImages.DESC_REQ_PLUGIN_OBJ;
			IMonitor plugin = iimport.getPlugin();
			if (plugin == null)
				flags = F_ERROR;
		}

		return get(base, flags);
	}

	private Image getObjectImage(IFeatureInfo info) {
		int flags = 0;
		String text = info.getDescription();
		if (text != null)
			text = text.trim();
		if (text != null && text.length() > 0) {
			// complete
			flags = F_EDIT;
		}
		return get(MDEPluginImages.DESC_DOC_SECTION_OBJ, flags);
	}

	public Image getObjectImage(ISiteFeature obj) {
		int flags = 0;
		if (obj.getArchiveFile() != null) {
			flags = F_BINARY;
		}
		return get(MDEPluginImages.DESC_JAVA_LIB_OBJ, flags);
	}

	public Image getObjectImage(ISiteArchive obj) {
		return get(MDEPluginImages.DESC_JAVA_LIB_OBJ, 0);
	}

	public Image getObjectImage(ISiteCategoryDefinition obj) {
		return get(MDEPluginImages.DESC_CATEGORY_OBJ);
	}

	public Image getObjectImage(ISiteCategory obj) {
		int flags = obj.getDefinition() == null ? F_ERROR : 0;
		return get(MDEPluginImages.DESC_CATEGORY_OBJ, flags);
	}

	public Image getObjectImage(ExportPackageObject obj) {
		int flags = 0;
		if (obj.isInternal()) {
			flags = F_INTERNAL;
			// if internal with at least one friend
			if (obj.getFriends().length > 0)
				flags = F_FRIEND;
		}
		ImageDescriptor desc = JavaUI.getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_PACKAGE);
		return get(desc, flags);
	}

	public Image getObjectImage(PackageObject obj) {
		return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKAGE);
	}

	private Image getObjectImage(IStatus status) {
		int sev = status.getSeverity();
		switch (sev) {
			case IStatus.ERROR :
				return PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_OBJS_ERROR_TSK);
			case IStatus.WARNING :
				return PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_OBJS_WARN_TSK);
			default :
				return PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_OBJS_INFO_TSK);
		}
	}

	public boolean isFullNameModeEnabled() {
		return MDEPlugin.isFullNameModeEnabled();
	}

	/*
	 * BIDI support (bug 183417)
	 * Any time we display a bracketed version, we should preface it with /u200f (zero width arabic character).
	 * Then inside the bracket, we should include /u200e (zero width latin character).  Since the leading separator
	 * will be resolved based on its surrounding text, when it is surrounded by a arabic character and a latin character
	 * the bracket will take the proper shape based on the underlying embedded direction.  The latin character must
	 * come after the bracket since versions are represented with latin numbers.  This ensure proper number format.
	 */

	/*
	 * returns true if instance has either arabic of hebrew locale (text displayed RTL)
	 */
	private static boolean isRTL() {
		Locale locale = Locale.getDefault();
		String localeString = locale.toString();
		return (localeString.startsWith("ar") || //$NON-NLS-1$
		localeString.startsWith("he")); //$NON-NLS-1$
	}

	/*
	 * Returns a String containing the unicode to properly display the version ranging when running bidi. 
	 */
	public static String formatVersion(String versionRange) {
		boolean isBasicVersion = versionRange == null || versionRange.length() == 0 || Character.isDigit(versionRange.charAt(0));
		if (isBasicVersion) {
			if (BidiUtil.isBidiPlatform())
				// The versionRange is a single version.  Since parenthesis is neutral, it direction is determined by leading and following character.
				// Since leading character is arabic and following character is latin, the parenthesis will take default (proper) direction.  
				// Must have the following character be the latin character to ensure version is formatted as latin (LTR)
				return "\u200f(\u200e" + versionRange + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			return "(" + versionRange + ')'; //$NON-NLS-1$
		} else if (isRTL() && BidiUtil.isBidiPlatform()) {
			// when running RTL and formatting a versionRange, we need to break up the String to make sure it is properly formatted.
			// A version should always be formatted LTR (start with \u202d, ends with \u202c) since it is composed of latin characaters.  
			// With specifying this format, if the qualifier has a latin character, it will not be formatted correctly.
			int index = versionRange.indexOf(',');
			if (index > 0) {
				// begin with zero length arabic character so version appears on left (correct) side of id.  
				// Then add RTL strong encoding so parentheses and comma have RTL formatting. 
				StringBuffer buffer = new StringBuffer("\u200f\u202e"); //$NON-NLS-1$
				// begin with leading separator (either parenthesis or bracket)
				buffer.append(versionRange.charAt(0));
				// start LTR encoding for min version
				buffer.append('\u202d');
				// min version
				buffer.append(versionRange.substring(1, index));
				// end LTR encoding, add ',' (which will be RTL due to first RTL strong encoding), and start LTR encoding for max version
				// We require a space between the two numbers otherwise it is considered 1 number in arabic (comma is digit grouping system).
				buffer.append("\u202c, \u202d"); //$NON-NLS-1$
				// max version
				buffer.append(versionRange.substring(index + 1, versionRange.length() - 1));
				// end LTR encoding
				buffer.append('\u202c');
				// add trailing separator
				buffer.append(versionRange.charAt(versionRange.length() - 1));
				return buffer.toString();
			}
			//			} else {
			//			    since the version is LTR and we are running LTR, do nothing.  This is the case for version ranges when run in LTR
		}
		return versionRange;
	}
}
