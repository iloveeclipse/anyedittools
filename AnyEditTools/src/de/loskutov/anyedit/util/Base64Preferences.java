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

        /**
         * @param parent
         * @param name
         */
        protected Base64Preferences() {
            super(null, "");
        }

        /**
         * Overriden to have access to (package protected) Base64 class
         *
         * @see java.util.prefs.AbstractPreferences#put(java.lang.String,
         *      java.lang.String)
         */
        public void put(String key, String value1) {
            this.value = value1;
        }

        /**
         * Overriden to have access to (package protected) Base64 class
         *
         * @see java.util.prefs.AbstractPreferences#get(java.lang.String,
         *      java.lang.String)
         */
        public String get(String key, String def) {
            return value;
        }

        protected AbstractPreferences childSpi(String name) {
            return null;
        }

        protected String[] childrenNamesSpi() throws BackingStoreException {
            return null;
        }

        protected void flushSpi() throws BackingStoreException {
            // noop
        }

        protected String getSpi(String key) {
            return null;
        }

        protected String[] keysSpi() throws BackingStoreException {
            return null;
        }

        protected void putSpi(String key, String value1) {
            // noop
        }

        protected void removeNodeSpi() throws BackingStoreException {
            // noop
        }

        protected void removeSpi(String key) {
            // noop
        }

        protected void syncSpi() throws BackingStoreException {
            // noop
        }
    }