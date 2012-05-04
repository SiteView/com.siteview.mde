package com.siteview.mde.internal.runtime.spy;

import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.MonitorRegistry;
import com.siteview.mde.internal.core.MDECore;
import com.siteview.mde.internal.core.SearchablePluginsManager;
import com.siteview.mde.internal.runtime.MDERuntimeMessages;
import com.siteview.mde.internal.runtime.PDERuntimePlugin;
import com.siteview.mde.internal.ui.editor.monitor.ManifestEditor;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;

/**
 * @since 3.4
 */
public class SpyIDEUtil {
	public static void openClass(String pluginId, String clazz) {
		IMonitorModelBase model = MonitorRegistry.findModel(pluginId);
		IResource resource = model != null ? model.getUnderlyingResource() : null;
		IJavaProject project = null;

		// if we don't find a model
		if (model == null) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(), MDERuntimeMessages.SpyIDEUtil_noSourceFound_title, NLS.bind(MDERuntimeMessages.SpyIDEUtil_noSourceFound_message, new Object[] {clazz}));
			return;
		}

		if (resource != null) { // project is open in workspace
			project = JavaCore.create(resource.getProject());
		} else {
			SearchablePluginsManager manager = MDECore.getDefault().getSearchablePluginsManager();
			try {
				manager.createProxyProject(new NullProgressMonitor());
				manager.addToJavaSearch(new IMonitorModelBase[] {model});
				project = manager.getProxyProject();
			} catch (CoreException e) {
			}
		}
		if (project != null)
			openInEditor(project, clazz);
	}

	public static void openInEditor(IJavaProject project, String clazz) {
		try {
			IType type = project.findType(clazz);
			JavaUI.openInEditor(type, false, true);
		} catch (JavaModelException e) {
			PDERuntimePlugin.log(e);
		} catch (PartInitException e) {
			PDERuntimePlugin.log(e);
		}
	}

	public static void openBundleManifest(String bundleID) {
		ManifestEditor.openPluginEditor(bundleID);
	}

}
