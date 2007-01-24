/*
 * This file is given under the licence found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package org.jayasoft.ivyde.eclipse.ui.core.model;

public class ListValueProvider implements IValueProvider {

    private String[] _values;

    public ListValueProvider(String commaSeparatedValuesList) {
        if (commaSeparatedValuesList != null) {
            init(commaSeparatedValuesList.split(","), true);
        }
    }

    public ListValueProvider(String[] values) {
        if (values != null) {
            init(values, false);
        }
    }

    private void init(String[] values, boolean trim) {
        _values = new String[values.length];
        if (trim) {
            for (int i = 0; i < values.length; i++) {
                _values[i] = values[i].trim();
            }
        } else {
            System.arraycopy(values, 0, _values, 0, values.length);
        }
    }

    public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
        return _values;
    }

}
