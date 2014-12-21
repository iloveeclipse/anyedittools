/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions.internal;

import org.eclipse.ui.IStartup;
import org.osgi.framework.Version;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.util.EclipseUtils;


/**
 * Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=455165
 * Eclipse 4.5 seem to changed IStartup contributions processing
 * @author andrei
 */
public class AnyeditStartup implements IStartup {

    @Override
    public void earlyStartup() {
        // hooks into the global toolbar/menu
        try {
            if(EclipseUtils.getWorkbenchVersion().compareTo(new Version(3,7,0)) >= 0) {
                new StartupHelper2().init();
            } else {
                new StartupHelper().init();
            }
        } catch (NoSuchMethodError e){
            // it's old Eclipse...
            new StartupHelper().init();
        }
        AnyEditToolsPlugin.setSaveHookInitialized(true);
    }

}
