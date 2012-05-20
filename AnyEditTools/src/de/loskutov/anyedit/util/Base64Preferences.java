/*******************************************************************************
 * Copyright (c) 2009 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.util;

import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

/**
 * This is a dummy class which only purpose is to re-use (package protected)
 * java.util.Base64 class. Unfortunately sun.misc.BASE64Encoder might be not available
 * on non-Sun JDK's too.
 *
 * @author Andrei
 */
class Base64Preferences extends AbstractPreferences {

    private String value;

    protected Base64Preferences() {
        super(null, "");
    }

    /**
     * Overriden to have access to (package protected) Base64 class
     *
     * @see java.util.prefs.AbstractPreferences#put(java.lang.String,
     *      java.lang.String)
     */
    @Override
    public void put(String key, String value1) {
        this.value = value1;
    }

    /**
     * Overriden to have access to (package protected) Base64 class
     *
     * @see java.util.prefs.AbstractPreferences#get(java.lang.String,
     *      java.lang.String)
     */
    @Override
    public String get(String key, String def) {
        return value;
    }

    @Override
    protected AbstractPreferences childSpi(String name) {
        return null;
    }

    @Override
    protected String[] childrenNamesSpi() throws BackingStoreException {
        return new String[0];
    }

    @Override
    protected void flushSpi() throws BackingStoreException {
        // noop
    }

    @Override
    protected String getSpi(String key) {
        return null;
    }

    @Override
    protected String[] keysSpi() throws BackingStoreException {
        return new String[0];
    }

    @Override
    protected void putSpi(String key, String value1) {
        // noop
    }

    @Override
    protected void removeNodeSpi() throws BackingStoreException {
        // noop
    }

    @Override
    protected void removeSpi(String key) {
        // noop
    }

    @Override
    protected void syncSpi() throws BackingStoreException {
        // noop
    }
}