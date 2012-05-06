/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.core.search;

import java.util.ArrayList;
import java.util.regex.Pattern;
import org.eclipse.core.runtime.IProgressMonitor;

import com.siteview.mde.core.monitor.*;
import com.siteview.mde.internal.core.util.PatternConstructor;

public class PluginSearchOperation {
	protected PluginSearchInput fInput;
	private ISearchResultCollector fCollector;
	private Pattern fPattern;

	public PluginSearchOperation(PluginSearchInput input, ISearchResultCollector collector) {
		this.fInput = input;
		this.fCollector = collector;
		this.fPattern = PatternConstructor.createPattern(input.getSearchString(), input.isCaseSensitive());
	}

	public void execute(IProgressMonitor monitor) {
		IMonitorModelBase[] entries = fInput.getSearchScope().getMatchingModels();
		monitor.beginTask("", entries.length); //$NON-NLS-1$

		try {
			for (int i = 0; i < entries.length; i++) {
				IMonitorModelBase candidate = entries[i];
				visit(candidate);
				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}
	}

	private void visit(IMonitorModelBase model) {
		ArrayList matches = findMatch(model);
		for (int i = 0; i < matches.size(); i++) {
			fCollector.accept(matches.get(i));
		}
	}

	private ArrayList findMatch(IMonitorModelBase model) {
		ArrayList result = new ArrayList();
		int searchLimit = fInput.getSearchLimit();
		switch (fInput.getSearchElement()) {
			case PluginSearchInput.ELEMENT_PLUGIN :
				if (searchLimit != PluginSearchInput.LIMIT_REFERENCES)
					findPluginDeclaration(model, result);
				if (searchLimit != PluginSearchInput.LIMIT_DECLARATIONS)
					findPluginReferences(model, result);
				break;
			case PluginSearchInput.ELEMENT_FRAGMENT :
				findFragmentDeclaration(model, result);
				break;
			case PluginSearchInput.ELEMENT_EXTENSION_POINT :
				if (searchLimit != PluginSearchInput.LIMIT_REFERENCES)
					findExtensionPointDeclarations(model, result);
				if (searchLimit != PluginSearchInput.LIMIT_DECLARATIONS)
					findExtensionPointReferences(model, result);
				break;
		}
		return result;
	}

	private void findFragmentDeclaration(IMonitorModelBase model, ArrayList result) {
		IMonitorBase pluginBase = model.getMonitorBase();
		if (pluginBase instanceof IFragment && fPattern.matcher(pluginBase.getId()).matches()) {
			result.add(pluginBase);
		}
	}

	private void findPluginDeclaration(IMonitorModelBase model, ArrayList result) {
		IMonitorBase pluginBase = model.getMonitorBase();
		if (pluginBase instanceof IMonitor && fPattern.matcher(pluginBase.getId()).matches())
			result.add(pluginBase);
	}

	private void findPluginReferences(IMonitorModelBase model, ArrayList result) {
		IMonitorBase pluginBase = model.getMonitorBase();
		if (pluginBase instanceof IFragment) {
			if (fPattern.matcher(((IFragment) pluginBase).getPluginId()).matches())
				result.add(pluginBase);
		}
		IMonitorImport[] imports = pluginBase.getImports();
		for (int i = 0; i < imports.length; i++) {
			if (fPattern.matcher(imports[i].getId()).matches())
				result.add(imports[i]);
		}
	}

	private void findExtensionPointDeclarations(IMonitorModelBase model, ArrayList result) {
		IMonitorExtensionPoint[] extensionPoints = model.getMonitorBase().getExtensionPoints();
		for (int i = 0; i < extensionPoints.length; i++) {
			if (fPattern.matcher(extensionPoints[i].getFullId()).matches())
				result.add(extensionPoints[i]);
		}
	}

	private void findExtensionPointReferences(IMonitorModelBase model, ArrayList result) {
		IMonitorExtension[] extensions = model.getMonitorBase().getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			if (fPattern.matcher(extensions[i].getPoint()).matches())
				result.add(extensions[i]);
		}
	}

}
