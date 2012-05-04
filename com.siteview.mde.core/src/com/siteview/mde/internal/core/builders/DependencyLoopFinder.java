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
package com.siteview.mde.internal.core.builders;

import java.util.Vector;

import org.eclipse.osgi.util.NLS;

import com.siteview.mde.core.monitor.IMonitor;
import com.siteview.mde.core.monitor.IMonitorImport;
import com.siteview.mde.core.monitor.IMonitorModel;
import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.MonitorRegistry;
import com.siteview.mde.internal.core.MDECoreMessages;

public class DependencyLoopFinder {

	public static DependencyLoop[] findLoops(IMonitor root) {
		return findLoops(root, null);
	}

	public static DependencyLoop[] findLoops(IMonitor root, IMonitor[] candidates) {
		return findLoops(root, candidates, false);
	}

	public static DependencyLoop[] findLoops(IMonitor root, IMonitor[] candidates, boolean onlyCandidates) {
		Vector loops = new Vector();

		Vector path = new Vector();
		findLoops(loops, path, root, candidates, onlyCandidates, new Vector());
		return (DependencyLoop[]) loops.toArray(new DependencyLoop[loops.size()]);
	}

	private static void findLoops(Vector loops, Vector path, IMonitor subroot, IMonitor[] candidates, boolean onlyCandidates, Vector exploredPlugins) {
		if (path.size() > 0) {
			// test the path so far
			// is the subroot the same as root - if yes, that's it

			IMonitor root = (IMonitor) path.elementAt(0);
			if (isEquivalent(root, subroot)) {
				// our loop!!
				DependencyLoop loop = new DependencyLoop();
				loop.setMembers((IMonitor[]) path.toArray(new IMonitor[path.size()]));
				int no = loops.size() + 1;
				loop.setName(NLS.bind(MDECoreMessages.Builders_DependencyLoopFinder_loopName, ("" + no))); //$NON-NLS-1$
				loops.add(loop);
				return;
			}
			// is the subroot the same as any other node?
			// if yes, abort - local loop that is not ours
			for (int i = 1; i < path.size(); i++) {
				IMonitor node = (IMonitor) path.elementAt(i);
				if (isEquivalent(subroot, node)) {
					// local loop
					return;
				}
			}
		}
		Vector newPath = path.size() > 0 ? ((Vector) path.clone()) : path;
		newPath.add(subroot);

		if (!onlyCandidates) {
			IMonitorImport[] iimports = subroot.getImports();
			for (int i = 0; i < iimports.length; i++) {
				IMonitorImport iimport = iimports[i];
				String id = iimport.getId();
				//Be paranoid
				if (id == null)
					continue;
				if (!exploredPlugins.contains(id)) {
					// is plugin in list of non loop yielding plugins
					//Commenting linear lookup - was very slow 
					//when called from here. We will use
					//model manager instead because it
					//has a hash table lookup that is much faster.
					//IPlugin child = PDECore.getDefault().findPlugin(id);
					IMonitor child = findPlugin(id);
					if (child != null) {
						// number of loops before traversing plugin
						int oldLoopSize = loops.size();

						findLoops(loops, newPath, child, null, false, exploredPlugins);

						// number of loops after traversing plugin
						int newLoopsSize = loops.size();

						if (oldLoopSize == newLoopsSize) {// no change in number of loops
							// no loops from going to this node, skip next time
							exploredPlugins.add(id);
						}
					}
				}

			}

		}
		if (candidates != null) {
			for (int i = 0; i < candidates.length; i++) {
				IMonitor candidate = candidates[i];

				// number of loops before traversing plugin
				int oldLoopSize = loops.size();

				findLoops(loops, newPath, candidate, null, false, exploredPlugins);

				// number of loops after traversing plugin
				int newLoopsSize = loops.size();

				if (oldLoopSize == newLoopsSize) { // no change in number of loops
					// no loops from going to this node, skip next time
					exploredPlugins.add(candidate.getId());
				}
			}
		}

	}

	private static IMonitor findPlugin(String id) {
		IMonitorModelBase childModel = MonitorRegistry.findModel(id);
		if (childModel == null || !(childModel instanceof IMonitorModel))
			return null;
		return (IMonitor) childModel.getMonitorBase();
	}

	private static boolean isEquivalent(IMonitor left, IMonitor right) {
		return left.getId().equals(right.getId());
	}
}
