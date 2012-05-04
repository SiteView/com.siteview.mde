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
package com.siteview.mde.internal.core.feature;

import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;

import com.siteview.mde.core.monitor.IFragment;
import com.siteview.mde.core.monitor.IFragmentModel;
import com.siteview.mde.core.monitor.IMonitorBase;
import com.siteview.mde.core.monitor.IMonitorModel;
import com.siteview.mde.core.monitor.IMonitorModelBase;
import com.siteview.mde.core.monitor.ModelEntry;
import com.siteview.mde.core.monitor.MonitorRegistry;
import com.siteview.mde.internal.core.ifeature.IFeaturePlugin;
import org.w3c.dom.Node;

public class FeaturePlugin extends FeatureData implements IFeaturePlugin {
	private static final long serialVersionUID = 1L;
	private boolean fFragment;
	private String fVersion;
	private boolean fUnpack = true;

	public FeaturePlugin() {
	}

	protected void reset() {
		super.reset();
		fVersion = null;
		fFragment = false;
	}

	public boolean isFragment() {
		return fFragment;
	}

	public IMonitorBase getPluginBase() {
		if (id == null) {
			return null;
		}
		String version = getVersion();
		IMonitorModelBase model = null;
		if (version == null || version.equals("0.0.0")) //$NON-NLS-1$
			model = MonitorRegistry.findModel(id);
		else {
			ModelEntry entry = MonitorRegistry.findEntry(id);
			// if no plug-ins match the id, entry == null
			if (entry != null) {
				IMonitorModelBase bases[] = entry.getActiveModels();
				for (int i = 0; i < bases.length; i++) {
					if (bases[i].getMonitorBase().getVersion().equals(version)) {
						model = bases[i];
						break;
					}
				}
			}
		}
		if (fFragment && model instanceof IFragmentModel)
			return model.getMonitorBase();
		if (!fFragment && model instanceof IMonitorModel)
			return model.getMonitorBase();
		return null;
	}

	public String getVersion() {
		return fVersion;
	}

	public boolean isUnpack() {
		return fUnpack;
	}

	public void setVersion(String version) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fVersion;
		this.fVersion = version;
		firePropertyChanged(this, P_VERSION, oldValue, version);
	}

	public void setUnpack(boolean unpack) throws CoreException {
		ensureModelEditable();
		boolean oldValue = fUnpack;
		this.fUnpack = unpack;
		firePropertyChanged(this, P_UNPACK, new Boolean(oldValue), new Boolean(unpack));
	}

	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_VERSION)) {
			setVersion(newValue != null ? newValue.toString() : null);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}

	public void setFragment(boolean fragment) throws CoreException {
		ensureModelEditable();
		this.fFragment = fragment;
	}

	protected void parse(Node node) {
		super.parse(node);
		fVersion = getNodeAttribute(node, "version"); //$NON-NLS-1$
		String f = getNodeAttribute(node, "fragment"); //$NON-NLS-1$
		if (f != null && f.equalsIgnoreCase("true")) //$NON-NLS-1$
			fFragment = true;
		String unpack = getNodeAttribute(node, "unpack"); //$NON-NLS-1$
		if (unpack != null && unpack.equalsIgnoreCase("false")) //$NON-NLS-1$
			fUnpack = false;
	}

	public void loadFrom(IMonitorBase plugin) {
		id = plugin.getId();
		label = plugin.getTranslatedName();
		fVersion = plugin.getVersion();
		fFragment = plugin instanceof IFragment;
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<plugin"); //$NON-NLS-1$
		String indent2 = indent + Feature.INDENT + Feature.INDENT;
		writeAttributes(indent2, writer);
		if (getVersion() != null) {
			writer.println();
			writer.print(indent2 + "version=\"" + getVersion() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (isFragment()) {
			writer.println();
			writer.print(indent2 + "fragment=\"true\""); //$NON-NLS-1$
		}
		if (!isUnpack()) {
			writer.println();
			writer.print(indent2 + "unpack=\"false\""); //$NON-NLS-1$
		}
		writer.println("/>"); //$NON-NLS-1$
		//writer.println(indent + "</plugin>");
	}

	public String getLabel() {
		IMonitorBase pluginBase = getPluginBase();
		if (pluginBase != null) {
			return pluginBase.getTranslatedName();
		}
		String name = super.getLabel();
		if (name == null)
			name = getId();
		return name;
	}

	public String toString() {
		return getLabel();
	}

}
