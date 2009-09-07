/*******************************************************************************
 * Copyright (c) 2004 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.anyedit;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import de.loskutov.anyedit.actions.internal.StartupHelper;


/**
 * The main plugin class to be used in the desktop.
 */
public class AnyEditToolsPlugin extends AbstractUIPlugin implements IStartup {

    // The shared instance.
    private static AnyEditToolsPlugin plugin;

    private static boolean isSaveHookInitialized;

    /**
     * The constructor.
     */
    public AnyEditToolsPlugin() {
        super();
        if(plugin != null) {
            throw new IllegalStateException("AnyEditToolsPlugin is a singleton!");
        }
        plugin = this;
    }

    public static String getId(){
        return getDefault().getBundle().getSymbolicName();
    }

    /**
     * Returns the shared instance.
     */
    public static AnyEditToolsPlugin getDefault() {
        return plugin;
    }

    /**
     * Returns the workspace instance.
     */
    public static Shell getShell() {
        return getDefault().getWorkbench().getActiveWorkbenchWindow().getShell();
    }

    public static void errorDialog(String message, Throwable error) {
        Shell shell = getShell();
        if (message == null) {
            message = Messages.error;
        }
        message = message + " " + error.getMessage();

        getDefault().getLog().log(
                new Status(IStatus.ERROR, getId(), IStatus.OK, message, error));

        MessageDialog.openError(shell, Messages.title, message);
    }

    /**
     * @param error
     */
    public static void logError(String message, Throwable error) {
        if (message == null) {
            message = error.getMessage();
            if (message == null) {
                message = error.toString();
            }
        }
        getDefault().getLog().log(
                new Status(IStatus.ERROR, getId(), IStatus.OK, message, error));
    }

    public static void logInfo(String message) {
        getDefault().getLog().log(
                new Status(IStatus.INFO, getId(), IStatus.OK, message, null));
    }

    public static void errorDialog(String message) {
        Shell shell = getShell();
        MessageDialog.openError(shell, Messages.title, message);
    }

    /**
     * @param isSaveHookInitialized
     *            The isSaveHookInitialized to set.
     */
    public static void setSaveHookInitialized(boolean isSaveHookInitialized) {
        AnyEditToolsPlugin.isSaveHookInitialized = isSaveHookInitialized;
    }

    /**
     * @return Returns the isSaveHookInitialized.
     */
    public static boolean isSaveHookInitialized() {
        return isSaveHookInitialized;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IStartup#earlyStartup()
     */
    public void earlyStartup() {
        // hooks into the global toolbar/menu
        new StartupHelper().init();
        setSaveHookInitialized(true);
    }
}
