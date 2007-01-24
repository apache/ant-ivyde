/*
 * This file is given under the licence found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package org.apache.ivyde.eclipse.ui.core.model;

public class SingleValueProvider extends ListValueProvider {
    public SingleValueProvider(String value) {
        super(new String[] {value});
    }
}
