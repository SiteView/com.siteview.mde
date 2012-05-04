/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.core.monitor;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.VersionRange;
import com.siteview.mde.core.build.IBuildModel;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.core.build.WorkspaceBuildModel;
import com.siteview.mde.internal.core.project.PDEProject;
import com.siteview.mde.internal.core.util.VersionUtil;
import org.osgi.framework.Version;

/**
 * The central access point for models representing plug-ins found in the workspace
 * and in the targret platform.
 * <p>
 * This class provides static methods only; it is not intended to be
 * instantiated or subclassed by clients.
 * </p>
 * 
 * @since 3.3
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class MonitorRegistry {

	/**
	 * Filter used when searching for plug-in models.
	 * <p>
	 * Clients may subclass this class to implement custom filters.
	 * </p>
	 * @see MonitorRegistry#findModel(String, String, int, MonitorFilter)
	 * @see MonitorRegistry#findModel(String, VersionRange, MonitorFilter)
	 * @since 3.6
	 */
	public static class MonitorFilter {

		/**
		 * Returns whether the given model is accepted by this filter.
		 * 
		 * @param model plug-in model
		 * @return whether accepted by this filter
		 */
		public boolean accept(IMonitorModelBase model) {
			return true;
		}

	}

	/**
	 * Returns a model entry containing all workspace and target plug-ins by the given ID
	 * 
	 * @param id the plug-in ID
	 * 
	 * @return a model entry containing all workspace and target plug-ins by the given ID
	 */
	public static ModelEntry findEntry(String id) {
		return MDECore.getDefault().getModelManager().findEntry(id);
	}

	/**
	 * Returns the plug-in model for the best match plug-in with the given ID.
	 * A null value is returned if no such bundle is found in the workspace or target platform.
	 * <p>
	 * A workspace plug-in is always preferably returned over a target plug-in.
	 * A plug-in that is checked/enabled on the Target Platform preference page is always
	 * preferably returned over a target plug-in that is unchecked/disabled.
	 * </p>
	 * <p>
	 * In the case of a tie among workspace plug-ins or among target plug-ins,
	 * the plug-in with the highest version is returned.
	 * </p>
	 * <p>
	 * In the case of a tie among more than one suitable plug-in that have the same version, 
	 * one of those plug-ins is randomly returned.
	 * </p>
	 * 
	 * @param id the plug-in ID
	 * @return the plug-in model for the best match plug-in with the given ID
	 */
	public static IMonitorModelBase findModel(String id) {
		return MDECore.getDefault().getModelManager().findModel(id);
	}

	/**
	 * Returns the plug-in model corresponding to the given project, or <code>null</code>
	 * if the project does not represent a plug-in project or if it contains a manifest file
	 * that is malformed or missing vital information.
	 * 
	 * @param project the project
	 * @return a plug-in model corresponding to the project or <code>null</code> if the project
	 * 			is not a plug-in project
	 */
	public static IMonitorModelBase findModel(IProject project) {
		return MDECore.getDefault().getModelManager().findModel(project);
	}

	/**
	 * Returns a plug-in model associated with the given bundle description
	 * 
	 * @param desc the bundle description
	 * 
	 * @return a plug-in model associated with the given bundle description or <code>null</code>
	 * 			if none exists
	 */
	public static IMonitorModelBase findModel(BundleDescription desc) {
		return MDECore.getDefault().getModelManager().findModel(desc);
	}

	/**
	 * Returns all plug-ins and fragments in the workspace as well as all plug-ins and fragments that are
	 * checked on the Target Platform preference page.
	 * <p>
	 * If a workspace plug-in/fragment has the same ID as a target plug-in/fragment, the target counterpart
	 * is skipped and not included.
	 * </p>
	 * <p>
	 * Equivalent to <code>getActiveModels(true)</code>
	 * </p>
	 * 
	 * @return   all plug-ins and fragments in the workspace as well as all plug-ins and fragments that are
	 * 			checked on the Target Platform preference page.
	 */
	public static IMonitorModelBase[] getActiveModels() {
		return getActiveModels(true);
	}

	/**
	 * Returns all plug-ins and (possibly) fragments in the workspace as well as all plug-ins and (possibly)
	 *  fragments that are checked on the Target Platform preference page.
	 * <p>
	 * If a workspace plug-in/fragment has the same ID as a target plug-in, the target counterpart
	 * is skipped and not included.
	 * </p>
	 * <p>
	 * The returned result includes fragments only if <code>includeFragments</code>
	 * is set to true
	 * </p>
	 * @param includeFragments  a boolean indicating if fragments are desired in the returned
	 *							result
	 * @return all plug-ins and (possibly) fragments in the workspace as well as all plug-ins and 
	 * (possibly) fragments that are checked on the Target Platform preference page.
	 */
	public static IMonitorModelBase[] getActiveModels(boolean includeFragments) {
		return MDECore.getDefault().getModelManager().getActiveModels(includeFragments);
	}

	/**
	 * Returns all plug-ins and fragments in the workspace as well as all target plug-ins and fragments, regardless 
	 * whether or not they are checked or not on the Target Platform preference page.
	 * <p>
	 * If a workspace plug-in/fragment has the same ID as a target plug-in, the target counterpart
	 * is skipped and not included.
	 * </p>
	 * <p>
	 * Equivalent to <code>getAllModels(true)</code>
	 * </p>
	 * 
	 * @return   all plug-ins and fragments in the workspace as well as all target plug-ins and fragments, regardless 
	 * whether or not they are checked on the Target Platform preference page.
	 */
	public static IMonitorModelBase[] getAllModels() {
		return getAllModels(true);
	}

	/**
	 * Returns all plug-ins and (possibly) fragments in the workspace as well as all plug-ins 
	 * and (possibly) fragments, regardless whether or not they are
	 * checked on the Target Platform preference page.
	 * <p>
	 * If a workspace plug-in/fragment has the same ID as a target plug-in/fragment, the target counterpart
	 * is skipped and not included.
	 * </p>
	 * <p>
	 * The returned result includes fragments only if <code>includeFragments</code>
	 * is set to true
	 * </p>
	 * @param includeFragments  a boolean indicating if fragments are desired in the returned
	 *							result
	 * @return ll plug-ins and (possibly) fragments in the workspace as well as all plug-ins 
	 * and (possibly) fragments, regardless whether or not they are
	 * checked on the Target Platform preference page.
	 */
	public static IMonitorModelBase[] getAllModels(boolean includeFragments) {
		return MDECore.getDefault().getModelManager().getAllModels(includeFragments);
	}

	/**
	 * Returns all plug-in models in the workspace
	 * 
	 * @return all plug-in models in the workspace
	 */
	public static IMonitorModelBase[] getWorkspaceModels() {
		return MDECore.getDefault().getModelManager().getWorkspaceModels();
	}

	/**
	 * Return the model manager that keeps track of plug-ins in the target platform
	 * 
	 * @return  the model manager that keeps track of plug-ins in the target platform
	 */
	public static IMonitorModelBase[] getExternalModels() {
		return MDECore.getDefault().getModelManager().getExternalModels();
	}

	/**
	 * Returns whether the given model matches the given id, version, and match rule.
	 * 
	 * @param base match candidate
	 * @param id id to match
	 * @param version version to match or <code>null</code>
	 * @param match version match rule
	 * @return whether the model is a match
	 */
	private static boolean isMatch(IMonitorBase base, String id, String version, int match) {
		// if version is null, then match any version with same ID
		if (base == null) {
			return false; // guard against invalid plug-ins
		}
		if (base.getId() == null) {
			return false; // guard against invalid plug-ins
		}
		if (version == null)
			return base.getId().equals(id);
		return VersionUtil.compare(base.getId(), base.getVersion(), id, version, match);
	}

	/**
	 * Returns a model matching the given id, version, match rule, and optional filter,
	 * or <code>null</code> if none.
	 * p>
	 * A workspace plug-in is always preferably returned over a target plug-in.
	 * A plug-in that is checked/enabled on the Target Platform preference page is always
	 * preferably returned over a target plug-in that is unchecked/disabled.
	 * </p>
	 * <p>
	 * In the case of a tie among workspace plug-ins or among target plug-ins,
	 * the plug-in with the highest version is returned.
	 * </p>
	 * <p>
	 * In the case of a tie among more than one suitable plug-in that have the same version, 
	 * one of those plug-ins is randomly returned.
	 * </p>
	 * 
	 * @param id symbolic name of a plug-in to find
	 * @param version minimum version, or <code>null</code> to only match on symbolic name
	 * @param match one of {@link IMatchRules#COMPATIBLE}, {@link IMatchRules#EQUIVALENT},
	 *  {@link IMatchRules#GREATER_OR_EQUAL}, {@link IMatchRules#PERFECT}, or {@link IMatchRules#NONE}
	 *  when a version is unspecified
	 * @param filter a plug-in filter or <code>null</code> 
	 * 
	 * @return a matching model or <code>null</code>
	 * @since 3.6
	 */
	public static IMonitorModelBase findModel(String id, String version, int match, MonitorFilter filter) {
		return getMax(findModels(id, version, match, filter));
	}

	/**
	 * Returns all models matching the given id, version, match rule, and optional filter.
	 * <p>
	 * Target (external) plug-ins/fragments with the same ID as workspace counterparts are not
	 * considered.
	 * </p>
	 * <p>
	 * Returns plug-ins regardless of whether they are checked/enabled or unchecked/disabled
	 * on the Target Platform preference page.
	 * </p>
	 * @param id symbolic name of a plug-ins to find
	 * @param version minimum version, or <code>null</code> to only match on symbolic name
	 * @param match one of {@link IMatchRules#COMPATIBLE}, {@link IMatchRules#EQUIVALENT},
	 *  {@link IMatchRules#GREATER_OR_EQUAL}, {@link IMatchRules#PERFECT}, or {@link IMatchRules#NONE}
	 *  when a version is unspecified
	 * @param filter a plug-in filter or <code>null</code> 
	 * 
	 * @return a matching models, possibly an empty collection
	 * @since 3.6
	 */
	public static IMonitorModelBase[] findModels(String id, String version, int match, MonitorFilter filter) {
		IMonitorModelBase[] models = MonitorRegistry.getAllModels();
		List results = new ArrayList();
		for (int i = 0; i < models.length; i++) {
			IMonitorModelBase model = models[i];
			if ((filter == null || filter.accept(model)) && isMatch(model.getMonitorBase(), id, version, match))
				results.add(model);
		}
		return (IMonitorModelBase[]) results.toArray(new IMonitorModelBase[results.size()]);
	}

	/**
	 * Returns a model matching the given id, version range, and optional filter,
	 * or <code>null</code> if none.
	 * <p>
	 * A workspace plug-in is always preferably returned over a target plug-in.
	 * A plug-in that is checked/enabled on the Target Platform preference page is always
	 * preferably returned over a target plug-in that is unchecked/disabled.
	 * </p>
	 * <p>
	 * In the case of a tie among workspace plug-ins or among target plug-ins,
	 * the plug-in with the highest version is returned.
	 * </p>
	 * <p>
	 * In the case of a tie among more than one suitable plug-in that have the same version, 
	 * one of those plug-ins is randomly returned.
	 * </p>
	 * @param id symbolic name of plug-in to find
	 * @param range acceptable version range to match, or <code>null</code> for any range
	 * @param filter a plug-in filter or <code>null</code>
	 * 
	 * @return a matching model or <code>null</code>
	 * @since 3.6
	 */
	public static IMonitorModelBase findModel(String id, VersionRange range, MonitorFilter filter) {
		return getMax(findModels(id, range, filter));
	}

	/**
	 * Returns the plug-in with the highest version, or <code>null</code> if empty.
	 * 
	 * @param models models
	 * @return plug-in with the highest version or <code>null</code>
	 */
	private static IMonitorModelBase getMax(IMonitorModelBase[] models) {
		if (models.length == 0) {
			return null;
		}
		if (models.length == 1) {
			return models[0];
		}
		IMonitorModelBase max = null;
		Version maxV = null;
		for (int i = 0; i < models.length; i++) {
			IMonitorModelBase model = models[i];
			String versionStr = model.getMonitorBase().getVersion();
			Version version = VersionUtil.validateVersion(versionStr).isOK() ? new Version(versionStr) : Version.emptyVersion;
			if (max == null) {
				max = model;
				maxV = version;
			} else {
				if (VersionUtil.isGreaterOrEqualTo(version, maxV)) {
					max = model;
					maxV = version;
				}
			}
		}
		return max;
	}

	/**
	 * Returns all models matching the given id, version range, and optional filter.
	 * <p>
	 * Target (external) plug-ins/fragments with the same ID as workspace counterparts are not
	 * considered.
	 * </p>
	 * <p>
	 * Returns plug-ins regardless of whether they are checked/enabled or unchecked/disabled
	 * on the Target Platform preference page.
	 * </p>
	 * @param id symbolic name of plug-ins to find
	 * @param range acceptable version range to match, or <code>null</code> for any range
	 * @param filter a plug-in filter or <code>null</code>
	 * 
	 * @return a matching models, possibly empty
	 * @since 3.6
	 */
	public static IMonitorModelBase[] findModels(String id, VersionRange range, MonitorFilter filter) {
		IMonitorModelBase[] models = MonitorRegistry.getAllModels();
		List results = new ArrayList();
		for (int i = 0; i < models.length; i++) {
			IMonitorModelBase model = models[i];
			if ((filter == null || filter.accept(model)) && id.equals(model.getMonitorBase().getId())) {
				String versionStr = model.getMonitorBase().getVersion();
				Version version = VersionUtil.validateVersion(versionStr).isOK() ? new Version(versionStr) : Version.emptyVersion;
				if (range == null || range.isIncluded(version)) {
					results.add(model);
				}
			}
		}
		return (IMonitorModelBase[]) results.toArray(new IMonitorModelBase[results.size()]);
	}

	/**
	 * Creates and returns a model associated with the <code>build.properties</code> of a bundle
	 * in the workspace or <code>null</code> if none.
	 * 
	 * @param model plug-in model base
	 * @return a build model initialized from the plug-in's <code>build.properties</code> or
	 *  <code>null</code> if none. Returns <code>null</code> for external plug-in models (i.e.
	 *  models that are not based on workspace projects).
	 *  @exception CoreException if unable to create a build model
	 * @since 3.7
	 */
	public static IBuildModel createBuildModel(IMonitorModelBase model) throws CoreException {
		IProject project = model.getUnderlyingResource().getProject();
		if (project != null) {
			IFile buildFile = PDEProject.getBuildProperties(project);
			if (buildFile.exists()) {
				IBuildModel buildModel = new WorkspaceBuildModel(buildFile);
				buildModel.load();
				return buildModel;
			}
		}
		return null;
	}
}
