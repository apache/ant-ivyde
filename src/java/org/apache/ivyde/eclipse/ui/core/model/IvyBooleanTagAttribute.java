/*
 * This file is given under the licence found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package org.apache.ivyde.eclipse.ui.core.model;

public class IvyBooleanTagAttribute extends IvyTagAttribute {

    protected static final String[] BOOLEAN_VALUES = new String[] {"true", "false"};

    private static final IValueProvider VALUE_PROVIDER = new IValueProvider() {
        public String[] getValuesfor(IvyTagAttribute att, IvyFile ivyFile) {
            return BOOLEAN_VALUES;
        }

    };

    public IvyBooleanTagAttribute(String name, String doc, boolean mandatory) {
        super(name, doc, mandatory);
        setValueProvider(VALUE_PROVIDER);
    }

    public IvyBooleanTagAttribute(String name, String doc) {
        super(name, doc);
        setValueProvider(VALUE_PROVIDER);
    }

    public IvyBooleanTagAttribute(String name) {
        super(name);
        setValueProvider(VALUE_PROVIDER);
    }

}
