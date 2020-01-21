package com.deepoove.swagger.diff.model;

import io.swagger.models.properties.Property;

/**
 * property with expression Language grammar
 * @author Sayi
 * @version 
 */
public class ElProperty extends ChangedExtensionGroup {

	private String el;

	private Property property;

	private Property newProperty;

	public Property getNewProperty() {
		return newProperty;
	}

	public void setNewProperty(Property newProperty) {
		this.newProperty = newProperty;
	}

	public Property getProperty() {
		return property;
	}

	public void setProperty(Property property) {
		this.property = property;
	}

	public String getEl() {
		return el;
	}

	public void setEl(String el) {
		this.el = el;
	}

}
