/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Gary Duprex <Gary.Duprex@aspectstools.com> - bug 150225
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 214156
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 219513
 *******************************************************************************/
package com.siteview.mde.internal.ui.correction;

import java.util.ArrayList;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import com.siteview.mde.internal.core.ICoreConstants;
import com.siteview.mde.internal.core.TargetPlatformHelper;
import com.siteview.mde.internal.core.builders.MDEMarkerFactory;
import com.siteview.mde.internal.ui.MDEUIMessages;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.osgi.framework.Constants;

public class ResolutionGenerator implements IMarkerResolutionGenerator2 {

	private static IMarkerResolution[] NO_RESOLUTIONS = new IMarkerResolution[0];

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolutionGenerator#getResolutions(org.eclipse.core.resources.IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker) {
		int problemID = marker.getAttribute("id", MDEMarkerFactory.NO_RESOLUTION); //$NON-NLS-1$
		switch (problemID) {
			case MDEMarkerFactory.M_DEPRECATED_AUTOSTART :
				// if targetVersion <= 3.3, we can add Eclipse-LazyStart header even if previous header's value was false.  We can't do this for 3.4+ since Bundle-AcativationPolicy does not have a "false" value.
				if (marker.getAttribute(MDEMarkerFactory.ATTR_CAN_ADD, true) || TargetPlatformHelper.getTargetVersion() <= 3.3)
					return new IMarkerResolution[] {new UpdateActivationResolution(AbstractPDEMarkerResolution.RENAME_TYPE, marker.getAttribute(MDEMarkerFactory.ATTR_HEADER, ICoreConstants.ECLIPSE_AUTOSTART)), new AddActivationHeaderResolution(AbstractPDEMarkerResolution.CREATE_TYPE, marker.getAttribute("header", ICoreConstants.ECLIPSE_AUTOSTART))}; //$NON-NLS-1$
				return new IMarkerResolution[] {new UpdateActivationResolution(AbstractPDEMarkerResolution.RENAME_TYPE, marker.getAttribute(MDEMarkerFactory.ATTR_HEADER, ICoreConstants.ECLIPSE_AUTOSTART))};
			case MDEMarkerFactory.M_JAVA_PACKAGE__PORTED :
				return new IMarkerResolution[] {new CreateJREBundleHeaderResolution(AbstractPDEMarkerResolution.CREATE_TYPE)};
			case MDEMarkerFactory.M_SINGLETON_DIR_NOT_SET :
				return new IMarkerResolution[] {new AddSingletonToSymbolicName(AbstractPDEMarkerResolution.RENAME_TYPE, true)};
			case MDEMarkerFactory.M_SINGLETON_ATT_NOT_SET :
				return new IMarkerResolution[] {new AddSingletonToSymbolicName(AbstractPDEMarkerResolution.RENAME_TYPE, false)};
			case MDEMarkerFactory.M_SINGLETON_DIR_NOT_SUPPORTED :
				return new IMarkerResolution[] {new UnsupportedSingletonDirectiveResolution(AbstractPDEMarkerResolution.RENAME_TYPE)};
			case MDEMarkerFactory.M_PROJECT_BUILD_ORDER_ENTRIES :
				return new IMarkerResolution[] {new RemoveStaticProjectReferences(AbstractPDEMarkerResolution.REMOVE_TYPE)};
			case MDEMarkerFactory.M_EXPORT_PKG_NOT_EXIST :
				return getUnresolvedExportProposals(marker);
			case MDEMarkerFactory.M_IMPORT_PKG_NOT_AVAILABLE :
				return getUnresolvedImportPackageProposals(marker);
			case MDEMarkerFactory.M_REQ_BUNDLE_NOT_AVAILABLE :
				return getUnresolvedBundle(marker);
			case MDEMarkerFactory.M_UNKNOWN_ACTIVATOR :
				return new IMarkerResolution[] {new CreateManifestClassResolution(AbstractPDEMarkerResolution.CREATE_TYPE, Constants.BUNDLE_ACTIVATOR), new ChooseManifestClassResolution(AbstractPDEMarkerResolution.RENAME_TYPE, Constants.BUNDLE_ACTIVATOR)};
			case MDEMarkerFactory.M_UNKNOWN_CLASS :
				return new IMarkerResolution[] {new CreateManifestClassResolution(AbstractPDEMarkerResolution.CREATE_TYPE, ICoreConstants.PLUGIN_CLASS), new ChooseManifestClassResolution(AbstractPDEMarkerResolution.RENAME_TYPE, ICoreConstants.PLUGIN_CLASS)};
			case MDEMarkerFactory.M_DIRECTIVE_HAS_NO_EFFECT :
				return getRemoveInternalDirectiveResolution(marker);
			case MDEMarkerFactory.M_MISMATCHED_EXEC_ENV :
				return new IMarkerResolution[] {new UpdateClasspathResolution(AbstractPDEMarkerResolution.RENAME_TYPE)};
			case MDEMarkerFactory.M_UNKNOW_EXEC_ENV :
				return new IMarkerResolution[] {new RemoveUnknownExecEnvironments(AbstractPDEMarkerResolution.REMOVE_TYPE)};
			case MDEMarkerFactory.M_DEPRECATED_IMPORT_SERVICE :
				return new IMarkerResolution[] {new RemoveImportExportServicesResolution(AbstractPDEMarkerResolution.REMOVE_TYPE, ICoreConstants.IMPORT_SERVICE)};
			case MDEMarkerFactory.M_DEPRECATED_EXPORT_SERVICE :
				return new IMarkerResolution[] {new RemoveImportExportServicesResolution(AbstractPDEMarkerResolution.REMOVE_TYPE, ICoreConstants.EXPORT_SERVICE)};
			case MDEMarkerFactory.M_UNECESSARY_DEP :
				return new IMarkerResolution[] {new RemoveRequireBundleResolution(AbstractPDEMarkerResolution.REMOVE_TYPE, marker.getAttribute("bundleId", null))}; //$NON-NLS-1$
			case MDEMarkerFactory.M_MISSING_EXPORT_PKGS :
				return new IMarkerResolution[] {new AddExportPackageMarkerResolution(AbstractPDEMarkerResolution.CREATE_TYPE, marker.getAttribute("packages", null))}; //$NON-NLS-1$
			case MDEMarkerFactory.B_REMOVE_SLASH_FILE_ENTRY :
				return new IMarkerResolution[] {new RemoveSeperatorBuildEntryResolution(AbstractPDEMarkerResolution.RENAME_TYPE, marker)};
			case MDEMarkerFactory.B_APPEND_SLASH_FOLDER_ENTRY :
				return new IMarkerResolution[] {new AppendSeperatorBuildEntryResolution(AbstractPDEMarkerResolution.RENAME_TYPE, marker)};
			case MDEMarkerFactory.B_ADDITION :
				return getBuildEntryAdditionResolutions(marker, null);
			case MDEMarkerFactory.B_JAVA_ADDDITION :
				return getBuildEntryAdditionResolutions(marker, MDEUIMessages.MultiFixResolution_JavaFixAll);
			case MDEMarkerFactory.B_SOURCE_ADDITION :
				return new IMarkerResolution[] {new AddSourceBuildEntryResolution(AbstractPDEMarkerResolution.CREATE_TYPE, marker)};
			case MDEMarkerFactory.B_REMOVAL :
				return new IMarkerResolution[] {new RemoveBuildEntryResolution(AbstractPDEMarkerResolution.REMOVE_TYPE, marker)};
			case MDEMarkerFactory.B_REPLACE :
				return new IMarkerResolution[] {new ReplaceBuildEntryResolution(AbstractPDEMarkerResolution.RENAME_TYPE, marker)};
			case MDEMarkerFactory.P_ILLEGAL_XML_NODE :
				return new IMarkerResolution[] {new RemoveNodeXMLResolution(AbstractPDEMarkerResolution.REMOVE_TYPE, marker)};
			case MDEMarkerFactory.P_UNTRANSLATED_NODE :
				return new IMarkerResolution[] {new ExternalizeResolution(AbstractPDEMarkerResolution.RENAME_TYPE, marker), new ExternalizeStringsResolution(AbstractPDEMarkerResolution.RENAME_TYPE)};
			case MDEMarkerFactory.P_UNKNOWN_CLASS :
				return new IMarkerResolution[] {new CreateClassXMLResolution(AbstractPDEMarkerResolution.CREATE_TYPE, marker), new ChooseClassXMLResolution(AbstractPDEMarkerResolution.RENAME_TYPE, marker)};
			case MDEMarkerFactory.P_USELESS_FILE :
				return new IMarkerResolution[] {new DeletePluginBaseResolution(AbstractPDEMarkerResolution.REMOVE_TYPE), new AddNewExtensionResolution(AbstractPDEMarkerResolution.CREATE_TYPE), new AddNewExtensionPointResolution(AbstractPDEMarkerResolution.CREATE_TYPE)};
			case MDEMarkerFactory.M_DEPRECATED_PROVIDE_PACKAGE :
				return new IMarkerResolution[] {new RenameProvidePackageResolution(AbstractPDEMarkerResolution.RENAME_TYPE)};
			case MDEMarkerFactory.M_EXECUTION_ENVIRONMENT_NOT_SET :
				return new IMarkerResolution[] {new AddDefaultExecutionEnvironmentResolution(AbstractPDEMarkerResolution.CREATE_TYPE, marker.getAttribute("ee_id", null))}; //$NON-NLS-1$
			case MDEMarkerFactory.M_MISSING_BUNDLE_CLASSPATH_ENTRY :
				return new IMarkerResolution[] {new AddBundleClassPathMarkerResolution(AbstractPDEMarkerResolution.CREATE_TYPE, marker.getAttribute("entry", null))}; //$NON-NLS-1$
			case MDEMarkerFactory.M_LAZYLOADING_HAS_NO_EFFECT :
				return new IMarkerResolution[] {new RemoveLazyLoadingDirectiveResolution(AbstractPDEMarkerResolution.REMOVE_TYPE, marker.getAttribute("header", ICoreConstants.ECLIPSE_LAZYSTART))}; //$NON-NLS-1$
			case MDEMarkerFactory.M_NO_LINE_TERMINATION :
				if (marker.getAttribute(MDEMarkerFactory.ATTR_HAS_CONTENT, true)) {
					return new IMarkerResolution[] {new NoLineTerminationResolution(AbstractPDEMarkerResolution.CREATE_TYPE)};
				}
				return new IMarkerResolution[] {new NoLineTerminationResolution(AbstractPDEMarkerResolution.REMOVE_TYPE)};
			case MDEMarkerFactory.M_R4_SYNTAX_IN_R3_BUNDLE :
				return new IMarkerResolution[] {new AddBundleManifestVersionResolution()};
		}
		return NO_RESOLUTIONS;
	}

	private IMarkerResolution[] getBuildEntryAdditionResolutions(IMarker marker, String multiFixDescription) {
		ArrayList resolutions = new ArrayList(2);
		resolutions.add(new AddBuildEntryResolution(AbstractPDEMarkerResolution.CREATE_TYPE, marker));
		for (int i = 0;; i++) {
			try {
				String entry = (String) marker.getAttribute(MDEMarkerFactory.BK_BUILD_ENTRY + '.' + i);
				if (entry == null)
					break;
				String value = (String) marker.getAttribute(MDEMarkerFactory.BK_BUILD_TOKEN + '.' + i);
				resolutions.add(new AddBuildEntryResolution(AbstractPDEMarkerResolution.CREATE_TYPE, marker, entry, value));

			} catch (CoreException e) {
				break;
			}
		}
		try {
			String markerCategory = (String) marker.getAttribute(MDEMarkerFactory.CAT_ID);
			int problemID = marker.getAttribute("id", MDEMarkerFactory.NO_RESOLUTION); //$NON-NLS-1$
			IMarker[] relatedMarkers = marker.getResource().findMarkers(marker.getType(), true, IResource.DEPTH_INFINITE);
			for (int i = 0; i < relatedMarkers.length; i++) {
				if (markerCategory.equals(relatedMarkers[i].getAttribute(MDEMarkerFactory.CAT_ID)) && relatedMarkers[i].getAttribute("id", MDEMarkerFactory.NO_RESOLUTION) == problemID && !marker.equals(relatedMarkers[i])) { //$NON-NLS-1$
					resolutions.add(new MultiFixResolution(marker, multiFixDescription));
					break;
				}
			}
		} catch (CoreException e) {
		}

		return (IMarkerResolution[]) resolutions.toArray(new IMarkerResolution[resolutions.size()]);
	}

	private IMarkerResolution[] getRemoveInternalDirectiveResolution(IMarker marker) {
		String packageName = marker.getAttribute("packageName", (String) null); //$NON-NLS-1$
		if (packageName != null) {
			IResource res = marker.getResource();
			if (res != null)
				return new IMarkerResolution[] {new RemoveInternalDirectiveEntryResolution(AbstractPDEMarkerResolution.REMOVE_TYPE, packageName)};
		}
		return NO_RESOLUTIONS;
	}

	private IMarkerResolution[] getUnresolvedExportProposals(IMarker marker) {
		String packageName = marker.getAttribute("packageName", (String) null); //$NON-NLS-1$
		if (packageName != null) {
			IResource res = marker.getResource();
			if (res != null)
				return new IMarkerResolution[] {new RemoveExportPackageResolution(AbstractPDEMarkerResolution.REMOVE_TYPE, packageName)};
		}
		return NO_RESOLUTIONS;
	}

	private IMarkerResolution[] getUnresolvedImportPackageProposals(IMarker marker) {
		String packageName = marker.getAttribute("packageName", (String) null); //$NON-NLS-1$
		if (packageName == null)
			return NO_RESOLUTIONS;

		boolean optionalPkg = marker.getAttribute("optional", false); //$NON-NLS-1$
		if (optionalPkg)
			return new IMarkerResolution[] {new RemoveImportPackageResolution(AbstractPDEMarkerResolution.REMOVE_TYPE, packageName)};

		return new IMarkerResolution[] {new RemoveImportPackageResolution(AbstractPDEMarkerResolution.REMOVE_TYPE, packageName), new OptionalImportPackageResolution(AbstractPDEMarkerResolution.RENAME_TYPE, packageName)};
	}

	private IMarkerResolution[] getUnresolvedBundle(IMarker marker) {
		String bundleId = marker.getAttribute("bundleId", (String) null); //$NON-NLS-1$
		if (bundleId == null)
			return NO_RESOLUTIONS;

		boolean optionalBundle = marker.getAttribute("optional", false); //$NON-NLS-1$
		if (optionalBundle)
			return new IMarkerResolution[] {new RemoveRequireBundleResolution(AbstractPDEMarkerResolution.REMOVE_TYPE, bundleId)};

		//		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		//		boolean removeImports = store.getString(IPreferenceConstants.PROP_RESOLVE_IMPORTS).equals(IPreferenceConstants.VALUE_REMOVE_IMPORT);
		return new IMarkerResolution[] {new RemoveRequireBundleResolution(AbstractPDEMarkerResolution.REMOVE_TYPE, bundleId), new OptionalRequireBundleResolution(AbstractPDEMarkerResolution.RENAME_TYPE, bundleId)
		//				new OrganizeRequireBundleResolution(AbstractPDEMarkerResolution.RENAME_TYPE, removeImports)
		};
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolutionGenerator2#hasResolutions(org.eclipse.core.resources.IMarker)
	 */
	public boolean hasResolutions(IMarker marker) {
		return marker.getAttribute("id", MDEMarkerFactory.NO_RESOLUTION) > 0; //$NON-NLS-1$
	}

}
