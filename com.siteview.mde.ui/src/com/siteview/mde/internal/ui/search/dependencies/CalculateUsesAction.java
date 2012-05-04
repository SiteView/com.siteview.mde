/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.search.dependencies;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import com.siteview.mde.internal.core.ibundle.IBundlePluginModelBase;
import com.siteview.mde.internal.ui.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.IProgressConstants;

public class CalculateUsesAction extends Action {

	private IProject fProject;
	private IBundlePluginModelBase fModel;

	public CalculateUsesAction(IProject project, IBundlePluginModelBase model) {
		fProject = project;
		fModel = model;
	}

	public void run() {
		Job job = createJob();
		job.setUser(true);
		job.setProperty(IProgressConstants.ICON_PROPERTY, MDEPluginImages.DESC_PSEARCH_OBJ.createImage());
		job.schedule();
	}

	protected Job createJob() {
		return new WorkspaceJob(MDEUIMessages.CalculateUsesAction_jobName) {

			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				CalculateUsesOperation op = getOperation();
				try {
					op.run(monitor);
				} catch (InvocationTargetException e) {
				} catch (InterruptedException e) {
				} finally {
					monitor.done();
				}
				return new Status(IStatus.OK, MDEPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
			}
		};
	}

	protected CalculateUsesOperation getOperation() {
		return new CalculateUsesOperation(fProject, fModel) {

			protected void handleSetUsesDirectives(final Map pkgsAndUses) {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						if (pkgsAndUses.isEmpty())
							return;
						setUsesDirectives(pkgsAndUses);
					}
				});
			}

		};
	}

}
