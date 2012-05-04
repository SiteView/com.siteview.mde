/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.siteview.mde.internal.ui.nls;

import com.siteview.mde.internal.core.text.monitor.*;

import com.siteview.mde.core.monitor.IFragmentModel;

import java.util.Properties;
import org.eclipse.core.runtime.CoreException;
import com.siteview.mde.internal.core.text.IDocumentAttributeNode;
import com.siteview.mde.internal.core.text.IDocumentTextNode;
import com.siteview.mde.internal.core.text.bundle.ManifestHeader;

public class ModelChangeElement {

	private static final String DELIM = "."; //$NON-NLS-1$
	private static final String KEY_PREFIX = "%"; //$NON-NLS-1$
	private static final String FRAGMENT_PREFIX = "f"; //$NON-NLS-1$

	private String fValue = ""; //$NON-NLS-1$
	private String fKey = ""; //$NON-NLS-1$
	private int fOffset = 0;
	private int fLength = 0;
	private boolean fExternalized = true;
	private ModelChange fParent;
	private Object fUnderlying;

	public ModelChangeElement(ModelChange parent, Object incoming) {
		fParent = parent;
		fUnderlying = incoming;
		if (incoming instanceof MonitorElementNode) {
			MonitorElementNode elem = (MonitorElementNode) incoming;
			IDocumentTextNode text = elem.getTextNode();
			fValue = elem.getText();
			generateValidKey(elem.getParent().getName(), elem.getName());
			fOffset = text.getOffset();
			fLength = text.getLength();
		} else if (incoming instanceof MonitorAttribute) {
			MonitorAttribute attr = (MonitorAttribute) incoming;
			fValue = attr.getValue();
			generateValidKey(attr.getEnclosingElement().getXMLTagName(), attr.getName());
			fOffset = attr.getValueOffset();
			fLength = attr.getValueLength();
		} else if (incoming instanceof MonitorExtensionPointNode) {
			MonitorExtensionPointNode extP = (MonitorExtensionPointNode) incoming;
			fValue = extP.getName();
			generateValidKey("extension-point", "name"); //$NON-NLS-1$ //$NON-NLS-2$
			IDocumentAttributeNode attr = extP.getDocumentAttribute("name"); //$NON-NLS-1$
			fOffset = attr.getValueOffset();
			fLength = attr.getValueLength();
		} else if (incoming instanceof ManifestHeader) {
			ManifestHeader header = (ManifestHeader) incoming;
			fValue = header.getValue();
			generateValidKey(header.getName());
			fLength = fValue.length();
			fOffset = header.getOffset() + header.getLength() - header.getLineLimiter().length() - fLength;
		}
	}

	public String getKey() {
		return fKey;
	}

	public void setKey(String key) {
		fKey = key;
	}

	public String getValue() {
		return fValue;
	}

	public void setValue(String value) {
		fValue = value;
	}

	public boolean isExternalized() {
		return fExternalized;
	}

	public void setExternalized(boolean externalzied) {
		fExternalized = externalzied;
	}

	public int getOffset() {
		return fOffset;
	}

	public int getLength() {
		return fLength;
	}

	private void generateValidKey(String pre, String mid) {
		generateValidKey(pre + DELIM + mid);
	}

	private void generateValidKey(String key) {
		Properties properties = fParent.getProperties();
		fKey = key;
		// Only generate counter with key, if key already exists in properties
		if (properties.containsKey(fKey)) {
			String delimiter = fParent.getParentModel() instanceof IFragmentModel ? DELIM + FRAGMENT_PREFIX : DELIM;
			int suffix = 0;
			while (properties.containsKey(fKey + delimiter + suffix))
				suffix += 1;
			fKey += delimiter + suffix;
		}
		properties.setProperty(fKey, fValue);
	}

	public String getExternKey() {
		return KEY_PREFIX + fKey;
	}

	public boolean updateValue() {
		try {
			String key = getExternKey();
			if (fUnderlying instanceof MonitorElementNode) {
				MonitorElementNode elem = (MonitorElementNode) fUnderlying;
				elem.setText(key);
			} else if (fUnderlying instanceof MonitorAttribute) {
				MonitorAttribute attr = (MonitorAttribute) fUnderlying;
				String attrName = attr.getName();
				attr.getEnclosingElement().setXMLAttribute(attrName, key);
			} else if (fUnderlying instanceof MonitorExtensionPointNode) {
				MonitorExtensionPointNode extP = (MonitorExtensionPointNode) fUnderlying;
				extP.setName(key);
			} else if (fUnderlying instanceof ManifestHeader) {
				ManifestHeader header = (ManifestHeader) fUnderlying;
				header.setValue(key);
			} else
				return false;
		} catch (CoreException e) {
			return false;
		}
		return true;
	}
}
