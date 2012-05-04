/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.siteview.mde.internal.ui.wizards.product;

import com.siteview.mde.core.monitor.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.osgi.util.NLS;
import com.siteview.mde.internal.core.util.PDETextHelper;
import com.siteview.mde.internal.ui.MDEUIMessages;

public class UpdateSplashHandlerAction extends Action implements ISplashHandlerConstants {

	private IMonitorModelBase fModel;

	private IProgressMonitor fMonitor;

	private CoreException fException;

	private String fFieldID;

	private String fFieldSplashID;

	private String fFieldProductID;

	private String fFieldClass;

	private String fFieldTemplate;

	private String fFieldPluginID;

	public UpdateSplashHandlerAction() {
		reset();
	}

	/**
	 * @param fieldID the fFieldID to set
	 */
	public void setFieldID(String fieldID) {
		fFieldID = fieldID;
	}

	/**
	 * @param fieldSplashID the fFieldSplashID to set
	 */
	public void setFieldSplashID(String fieldSplashID) {
		fFieldSplashID = fieldSplashID;
	}

	/**
	 * @param fieldProductID the fFieldProductID to set
	 */
	public void setFieldProductID(String fieldProductID) {
		fFieldProductID = fieldProductID;
	}

	/**
	 * @param fieldClass the fFieldClass to set
	 */
	public void setFieldClass(String fieldClass) {
		fFieldClass = fieldClass;
	}

	/**
	 * @param fieldTemplate the fFieldTemplate to set
	 */
	public void setFieldTemplate(String fieldTemplate) {
		fFieldTemplate = fieldTemplate;
	}

	/**
	 * @param fieldPluginID
	 */
	public void setFieldPluginID(String fieldPluginID) {
		fFieldPluginID = fieldPluginID;
	}

	/**
	 * 
	 */
	public void reset() {
		fModel = null;
		fMonitor = null;
		fException = null;

		fFieldID = null;
		fFieldClass = null;
		fFieldSplashID = null;
		fFieldProductID = null;
		fFieldTemplate = null;
		fFieldPluginID = null;
	}

	public void setModel(IMonitorModelBase model) {
		fModel = model;
	}

	public void setMonitor(IProgressMonitor monitor) {
		fMonitor = monitor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		try {
			updateModel();
		} catch (CoreException e) {
			fException = e;
		}
	}

	public void hasException() throws CoreException {
		// Release any caught exceptions
		if (fException != null) {
			throw fException;
		}
	}

	private void updateModel() throws CoreException {
		// Find the first splash handler extension
		IMonitorExtension extension = findFirstExtension(F_SPLASH_HANDLERS_EXTENSION);
		// Check to see if one was found
		if (extension == null) {
			// None found, add a new splash handler extension
			addExtensionSplashHandlers();
		} else {
			// Found one, modify the existing splash handler extension
			modifyExtensionSplashHandlers(extension);
		}
		// Determine whether the extensible template was selected
		if (isExtensibleTemplateSelected(fFieldTemplate)) {
			// Extensible template was selected
			// Extra model modifications required for this template
			// Find the first spash extension point declaration (should only
			// ever be one)
			IPluginExtensionPoint extensionPoint = findFirstExtensionPoint(F_SPLASH_EXTENSION_POINT);
			// Check to see if one was found
			// If one is found, just assume all its values are correct (no sync)
			if (extensionPoint == null) {
				// No splash extension point definition found, add one
				addExtensionPointSplashExtension();
			}
			// Find the first splash extension contribution
			String fullExtensionPointID = fFieldPluginID + '.' + F_SPLASH_EXTENSION_POINT;
			IMonitorExtension extensionSplash = findFirstExtension(fullExtensionPointID);
			// Check to see if one was found
			// If one is found, just assume all its values are correct (no sync)
			if (extensionSplash == null) {
				// No splash extension contribution found, add one
				addExtensionSplash();
			}
		}
	}

	private void addExtensionSplash() throws CoreException {
		// Update progress work units
		String fullExtensionPointID = fFieldPluginID + '.' + F_SPLASH_EXTENSION_POINT;
		fMonitor.beginTask(NLS.bind(MDEUIMessages.UpdateSplashHandlerInModelAction_msgAddingExtension, fullExtensionPointID), 1);
		// Create the new extension
		IMonitorExtension extension = createExtensionSplash();
		// Add extension to the model
		fModel.getMonitorBase().add(extension);
		// Update progress work units
		fMonitor.done();
	}

	private void addExtensionPointSplashExtension() throws CoreException {
		// Update progress work units
		fMonitor.beginTask(NLS.bind(MDEUIMessages.UpdateSplashHandlerInModelAction_msgAddingExtensionPoint, F_SPLASH_EXTENSION_POINT), 1);
		// Create the new extension point
		IPluginExtensionPoint extensionPoint = createExtensionPointSplash();
		// Add extension point to the model
		fModel.getMonitorBase().add(extensionPoint);
		// Update progress work units
		fMonitor.done();
	}

	private IPluginExtensionPoint createExtensionPointSplash() throws CoreException {
		// Create the extension point
		IPluginExtensionPoint extensionPoint = fModel.getFactory().createExtensionPoint();
		// ID
		extensionPoint.setId(F_SPLASH_EXTENSION_POINT);
		// Name
		extensionPoint.setName(MDEUIMessages.UpdateSplashHandlerInModelAction_splashExtensionPointName);
		// Schema
		extensionPoint.setSchema("schema/splashExtension.exsd"); //$NON-NLS-1$

		return extensionPoint;
	}

	private IMonitorExtension findFirstExtension(String extensionPointID) {
		// Get all the extensions
		IMonitorExtension[] extensions = fModel.getMonitorBase().getExtensions();
		// Get the first extension matching the specified extension point ID
		for (int i = 0; i < extensions.length; i++) {
			String point = extensions[i].getPoint();
			if (extensionPointID.equals(point)) {
				return extensions[i];
			}
		}
		return null;
	}

	private IPluginExtensionPoint findFirstExtensionPoint(String extensionPointID) {
		// Get all the extension points
		IPluginExtensionPoint[] extensionPoints = fModel.getMonitorBase().getExtensionPoints();
		// Get the first extension point (should only be one) matching the
		// specified extension point ID
		for (int i = 0; i < extensionPoints.length; i++) {
			// Not full ID
			String point = extensionPoints[i].getId();
			if (extensionPointID.equals(point)) {
				return extensionPoints[i];
			}
		}
		return null;
	}

	private void addExtensionSplashHandlers() throws CoreException {
		// Update progress work units
		fMonitor.beginTask(NLS.bind(MDEUIMessages.UpdateSplashHandlerInModelAction_msgAddingExtension, F_SPLASH_HANDLERS_EXTENSION), 1);
		// Create the new extension
		IMonitorExtension extension = createExtensionSplashHandlers();
		fModel.getMonitorBase().add(extension);
		// Update progress work units
		fMonitor.done();
	}

	private IMonitorExtension createExtensionSplashHandlers() throws CoreException {
		// Create the extension
		IMonitorExtension extension = fModel.getFactory().createExtension();
		// Point
		extension.setPoint(F_SPLASH_HANDLERS_EXTENSION);
		// NO id
		// NO name 
		// Create the extension's children
		createExtensionChildrenSplashHandlers(extension);

		return extension;
	}

	private void createExtensionChildrenSplashHandlers(IMonitorExtension extension) throws CoreException {
		// Add a splash handler element
		addElementSplashHandler(extension);
		// Add a product handler element
		addElementProductBinding(extension);
	}

	private void addElementSplashHandler(IMonitorExtension extension) throws CoreException {
		// Create the element
		IMonitorElement splashHandlerElement = createElementSplashHandler(extension);
		// Ensure element was defined and add it to the extension
		if (splashHandlerElement != null) {
			// Extension uses the first element only when choosing a splash
			// handler. Always set as the first extension element to 
			// override any previous elements
			extension.add(0, splashHandlerElement);
		}
	}

	private void addElementProductBinding(IMonitorExtension extension) throws CoreException {
		// Create the element
		IMonitorElement productBindingElement = createElementProductBinding(extension);
		// Ensure element was defined and add it to the extension
		if (productBindingElement != null) {
			// Extension uses the first element only when choosing a splash
			// handler. Always set as the first extension element to 
			// override any previous elements
			extension.add(1, productBindingElement);
		}
	}

	private IMonitorElement createElementSplashHandler(IMonitorExtension extension) throws CoreException {
		// Create the element
		IMonitorElement element = extension.getModel().getFactory().createElement(extension);
		// Element: Splash handler
		element.setName(F_ELEMENT_SPLASH_HANDLER);
		// Attribute: ID
		element.setAttribute(F_ATTRIBUTE_ID, fFieldID);
		// Attribute: Class
		element.setAttribute(F_ATTRIBUTE_CLASS, fFieldClass);

		return element;
	}

	private IMonitorElement createElementProductBinding(IMonitorExtension extension) throws CoreException {
		// Create the element
		IMonitorElement element = extension.getModel().getFactory().createElement(extension);
		// Element: Product binding
		element.setName(F_ELEMENT_PRODUCT_BINDING);
		// Attribute: Product ID
		element.setAttribute(F_ATTRIBUTE_PRODUCT_ID, fFieldProductID);
		// Attribute: Splash ID
		element.setAttribute(F_ATTRIBUTE_SPLASH_ID, fFieldSplashID);

		return element;
	}

	private void modifyExtensionSplashHandlers(IMonitorExtension extension) throws CoreException {
		// Update progress work units
		fMonitor.beginTask(NLS.bind(MDEUIMessages.UpdateSplashHandlerInModelAction_msgModifyingExtension, F_SPLASH_HANDLERS_EXTENSION), 1);
		// modify the existing extension children
		modifyExtensionChildrenSplashHandlers(extension);
		// Update progress work units
		fMonitor.done();
	}

	private void modifyExtensionChildrenSplashHandlers(IMonitorExtension extension) throws CoreException {
		// Find a matching pre-generated splash handler element
		IMonitorElement splashHandlerElement = findSplashHandlerElement(extension);
		// Check to see if one was found
		if (splashHandlerElement == null) {
			// No element found, add one
			addElementSplashHandler(extension);
		} else {
			// One element found, synchronize it
			syncSplashHandlerElement(splashHandlerElement);
		}
		// Find a matching pre-generated product binding element
		IMonitorElement productBindingElement = findProductBindingElement(extension);
		// Remove any product binding elements bound to the target product but 
		// NOT bound to the target splash ID (if any elements are found)
		// The splash handler extension provider uses the first product
		// binding it finds in the extension.
		// By removing all product bindings bound to a single product, we can 
		// override an existing splash handler with another existing 
		// splash handler.
		removeMatchingProductBindingElements(extension);
		// Check to see if one was found
		if (productBindingElement == null) {
			// No element found, add one
			addElementProductBinding(extension);
		} else {
			// One element found, synchronize it
			syncProductBindingElement(productBindingElement);
		}
	}

	private void removeMatchingProductBindingElements(IMonitorExtension extension) throws CoreException {
		// Check to see if the extension has any children
		if (extension.getChildCount() == 0) {
			// Extension has no children
			return;
		}
		IMonitorObject[] pluginObjects = extension.getChildren();
		// Process all children
		for (int j = 0; j < pluginObjects.length; j++) {
			if (pluginObjects[j] instanceof IMonitorElement) {
				IMonitorElement element = (IMonitorElement) pluginObjects[j];
				// Find splash handler elements
				if (element.getName().equals(F_ELEMENT_PRODUCT_BINDING)) {
					// Get the splash ID attribute
					IMonitorAttribute splashIDAttribute = element.getAttribute(F_ATTRIBUTE_SPLASH_ID);
					// Get the product ID attribute
					IMonitorAttribute productIDAttribute = element.getAttribute(F_ATTRIBUTE_PRODUCT_ID);
					// (1) Remove any product binding that has an undefined
					// product ID or splash ID
					// (2) Remove any product binding bound to the target 
					// product ID but NOT bound to the target splash ID
					if ((productIDAttribute == null) || (PDETextHelper.isDefined(productIDAttribute.getValue()) == false) || (splashIDAttribute == null) || (PDETextHelper.isDefined(splashIDAttribute.getValue()) == false)) {
						// Remove product binding element 
						extension.remove(element);
					} else if (productIDAttribute.getValue().equals(fFieldProductID) && (splashIDAttribute.getValue().equals(fFieldSplashID) == false)) {
						// Remove product binding element 
						extension.remove(element);
					}
				}
			}
		}
	}

	private IMonitorElement findSplashHandlerElement(IMonitorExtension extension) {
		// Check to see if the extension has any children
		if (extension.getChildCount() == 0) {
			// Extension has no children
			return null;
		}
		IMonitorObject[] pluginObjects = extension.getChildren();
		// Process all children
		for (int j = 0; j < pluginObjects.length; j++) {
			if (pluginObjects[j] instanceof IMonitorElement) {
				IMonitorElement element = (IMonitorElement) pluginObjects[j];
				// Find splash handler elements
				if (element.getName().equals(F_ELEMENT_SPLASH_HANDLER)) {
					// Get the id attribute
					IMonitorAttribute idAttribute = element.getAttribute(F_ATTRIBUTE_ID);
					// Check for the generated ID 
					if ((idAttribute != null) && PDETextHelper.isDefined(idAttribute.getValue()) && idAttribute.getValue().equals(fFieldID)) {
						// Matching element found
						return element;
					}
				}
			}
		}
		return null;
	}

	private void syncSplashHandlerElement(IMonitorElement element) throws CoreException {
		// Get the class attribute
		IMonitorAttribute classAttribute = element.getAttribute(F_ATTRIBUTE_CLASS);
		// Check to see if an update is necessary
		if ((classAttribute != null) && PDETextHelper.isDefined(classAttribute.getValue()) && classAttribute.getValue().equals(fFieldClass)) {
			// Exact match, no update necessary
			return;
		}
		// No match, override
		element.setAttribute(F_ATTRIBUTE_CLASS, fFieldClass);
	}

	private void syncProductBindingElement(IMonitorElement element) throws CoreException {
		// Get the product ID attribute
		IMonitorAttribute productIDAttribute = element.getAttribute(F_ATTRIBUTE_PRODUCT_ID);
		// Check to see if an update is necessary
		if ((productIDAttribute != null) && PDETextHelper.isDefined(productIDAttribute.getValue()) && productIDAttribute.getValue().equals(fFieldProductID)) {
			// Exact match, no update necessary
			return;
		}
		// No match, override
		element.setAttribute(F_ATTRIBUTE_PRODUCT_ID, fFieldProductID);
	}

	private IMonitorElement findProductBindingElement(IMonitorExtension extension) {
		// Check to see if the extension has any children
		if (extension.getChildCount() == 0) {
			// Extension has no children
			return null;
		}
		IMonitorObject[] pluginObjects = extension.getChildren();
		// Process all children
		for (int j = 0; j < pluginObjects.length; j++) {
			if (pluginObjects[j] instanceof IMonitorElement) {
				IMonitorElement element = (IMonitorElement) pluginObjects[j];
				// Find product binding elements
				if (element.getName().equals(F_ELEMENT_PRODUCT_BINDING)) {
					// Get the id attribute
					IMonitorAttribute splashIDAttribute = element.getAttribute(F_ATTRIBUTE_SPLASH_ID);
					// Check for the generated ID 
					if ((splashIDAttribute != null) && PDETextHelper.isDefined(splashIDAttribute.getValue()) && splashIDAttribute.getValue().equals(fFieldSplashID)) {
						// Matching element found
						return element;
					}
				}
			}
		}
		return null;
	}

	private IMonitorExtension createExtensionSplash() throws CoreException {

		String fullExtensionPointID = fFieldPluginID + '.' + F_SPLASH_EXTENSION_POINT;
		// Create the extension
		IMonitorExtension extension = fModel.getFactory().createExtension();
		// Point
		extension.setPoint(fullExtensionPointID);
		// NO id
		// NO name 
		// Create the extension's children
		createExtensionChildrenSplash(extension);

		return extension;
	}

	private void createExtensionChildrenSplash(IMonitorExtension extension) throws CoreException {

		String iconsDir = "icons" + '/'; //$NON-NLS-1$

		// Splash element: Application Framework
		IMonitorElement splashElementAf = createElementSplash(extension, "af", iconsDir + "af.png", MDEUIMessages.UpdateSplashHandlerInModelAction_nameApplicationFramework); //$NON-NLS-1$ //$NON-NLS-2$
		if (splashElementAf != null) {
			extension.add(splashElementAf);
		}
		// Splash element: Embedded
		IMonitorElement splashElementEmbedded = createElementSplash(extension, "embedded", iconsDir + "embedded.png", MDEUIMessages.UpdateSplashHandlerInModelAction_nameEmbedded); //$NON-NLS-1$ //$NON-NLS-2$
		if (splashElementEmbedded != null) {
			extension.add(splashElementEmbedded);
		}
		// Splash element: Enterprise
		IMonitorElement splashElementEnterprise = createElementSplash(extension, "enterprise", iconsDir + "enterprise.png", MDEUIMessages.UpdateSplashHandlerInModelAction_nameEnterprise); //$NON-NLS-1$ //$NON-NLS-2$
		if (splashElementEnterprise != null) {
			extension.add(splashElementEnterprise);
		}
		// Splash element: Languages
		IMonitorElement splashElementLanguages = createElementSplash(extension, "languages", iconsDir + "languages.png", MDEUIMessages.UpdateSplashHandlerInModelAction_nameLanguages); //$NON-NLS-1$ //$NON-NLS-2$
		if (splashElementLanguages != null) {
			extension.add(splashElementLanguages);
		}
		// Splash element: RCP
		IMonitorElement splashElementRCP = createElementSplash(extension, "rcp", iconsDir + "rcp.png", MDEUIMessages.UpdateSplashHandlerInModelAction_nameRCP); //$NON-NLS-1$ //$NON-NLS-2$
		if (splashElementRCP != null) {
			extension.add(splashElementRCP);
		}
	}

	private IMonitorElement createElementSplash(IMonitorExtension extension, String id, String icon, String tooltip) throws CoreException {
		// Create the element
		IMonitorElement element = extension.getModel().getFactory().createElement(extension);
		// Element: Splash handler
		element.setName(F_ELEMENT_SPLASH);
		// Attribute: ID
		element.setAttribute(F_ATTRIBUTE_ID, id);
		// Attribute: Icon
		element.setAttribute(F_ATTRIBUTE_ICON, icon);
		// Attribute: Tooltip
		element.setAttribute(F_ATTRIBUTE_TOOLTIP, tooltip);

		return element;
	}

	public static boolean isExtensibleTemplateSelected(String template) {
		if (template.equals(F_SPLASH_SCREEN_TYPE_CHOICES[2][0])) {
			return true;
		}
		return false;
	}

}
