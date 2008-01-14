/*
 * This file is given under the licence found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package org.apache.ivyde.eclipse.ui.core.model;

import org.apache.ivyde.eclipse.IvyPlugin;

final class PreferenceValueProvider implements IValueProvider {
    private String _name;

    public PreferenceValueProvider(String name) {
        _name = name;
    }

    public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
        try {
            return new String[] {IvyPlugin.getDefault().getPreferenceStore().getString(_name)};
        } catch (Exception e) {
            return null;
        }
    }
}
