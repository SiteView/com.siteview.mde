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
package com.siteview.mde.internal.ui.wizards.tools;

import com.siteview.mde.core.monitor.IMonitorModelBase;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaCore;
import com.siteview.mde.internal.core.ClasspathComputer;
import com.siteview.mde.internal.core.builders.MDEMarkerFactory;
import com.siteview.mde.internal.ui.*;

public class UpdateClasspathJob extends Job {
	IMonitorModelBase[] fModels;

	/**
	 * @param name
	 */
	public UpdateClasspathJob(IMonitorModelBase[] models) {
		super(MDEUIMessages.UpdateClasspathJob_title);
		setPriority(Job.LONG);
		fModels = models;
	}

	/*
	 * return canceled
	 */
	public boolean doUpdateClasspath(IProgressMonitor monitor, IMonitorModelBase[] models) throws CoreException {
		monitor.beginTask(MDEUIMessages.UpdateClasspathJob_task, models.length);
		try {
			for (int i = 0; i < models.length; i++) {
				IMonitorModelBase model = models[i];
				monitor.subTask(models[i].getMonitorBase().getId());
				// no reason to compile classpath for a non-Java model
				IProject project = model.getUnderlyingResource().getProject();
				if (!project.hasNature(JavaCore.NATURE_ID)) {
					monitor.worked(1);
					continue;
				}
				IProjectDescription projDesc = project.getDescription();
				if (projDesc == null)
					continue;
				projDesc.setReferencedProjects(new IProject[0]);
				project.setDescription(projDesc, null);
				IFile file = project.getFile(".project"); //$NON-NLS-1$
				if (file.exists())
					file.deleteMarkers(MDEMarkerFactory.MARKER_ID, true, IResource.DEPTH_ZERO);
				ClasspathComputer.setClasspath(project, model);
				monitor.worked(1);
				if (monitor.isCanceled())
					return false;
			}
		} finally {
			monitor.done();
		}
		return true;
	}

	class UpdateClasspathWorkspaceRunnable implements IWorkspaceRunnable {
		boolean fCanceled = false;

		public void run(IProgressMonitor monitor) throws CoreException {
			fCanceled = doUpdateClasspath(monitor, fModels);
		}

		public boolean isCanceled() {
			return fCanceled;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.internal.jobs.InternalJob#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus run(IProgressMonitor monitor) {
		try {
			UpdateClasspathWorkspaceRunnable runnable = new UpdateClasspathWorkspaceRunnable();
			MDEPlugin.getWorkspace().run(runnable, monitor);
			if (runnable.isCanceled()) {
				return new Status(IStatus.CANCEL, IMDEUIConstants.PLUGIN_ID, IStatus.CANCEL, "", null); //$NON-NLS-1$
			}

		} catch (CoreException e) {
			String title = MDEUIMessages.UpdateClasspathJob_error_title;
			String message = MDEUIMessages.UpdateClasspathJob_error_message;
			MDEPlugin.logException(e, title, message);
			return new Status(IStatus.ERROR, IMDEUIConstants.PLUGIN_ID, IStatus.OK, message, e);
		}
		return new Status(IStatus.OK, IMDEUIConstants.PLUGIN_ID, IStatus.OK, "", null); //$NON-NLS-1$
	}

}
