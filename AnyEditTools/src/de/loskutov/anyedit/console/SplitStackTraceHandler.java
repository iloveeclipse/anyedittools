/*******************************************************************************
 * Copyright (c) 2014 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Holger Meyer - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.console;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.handlers.HandlerUtil;

public class SplitStackTraceHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        if(!isEnabled()){
            return null;
        }
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
        if (window != null) {
            IWorkbenchPart part = HandlerUtil.getActivePart(event);
            if (part instanceof IConsoleView) {
                IConsoleView view = (IConsoleView) part;
                IConsole console = view.getConsole();
                if (console instanceof TextConsole) {
                    IDocument document = ((TextConsole) console).getDocument();
                    String splittedText = splitUp(document.get());
                    if (splittedText != null) {
                        document.set(splittedText);
                    }
                }
            }
        }
        return null;
    }

    private String splitUp(String string) {
        if (string != null) {
            String temp = string;
            temp = temp.replaceAll( "\\. Messages:",    ".\nMessages:"    );
            temp = temp.replaceAll( "\\. First error:", ".\nFirst error:" );
            temp = temp.replaceAll( "\\. Stack:",       ".\nStack:"       );
            temp = temp.replaceAll( "\\]. at",          "].\nat"          );
            temp = temp.replaceAll( "\\]\\) . ",        "]) .\n"          );
            temp = temp.replaceAll( "] at",             "]\nat"           );
            return temp.replaceAll( "\\) at",           ")\nat"           );
        }
        return null;
    }
}
