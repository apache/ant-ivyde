package org.apache.ivyde.eclipse.ui.core.model;

public interface IValueProvider {
    String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile);
}
