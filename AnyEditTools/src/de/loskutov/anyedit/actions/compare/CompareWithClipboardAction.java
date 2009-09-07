/*******************************************************************************
 * Copyright (c) 2008 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions.compare;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;

import de.loskutov.anyedit.compare.ClipboardStreamContent;
import de.loskutov.anyedit.compare.StreamContent;
import de.loskutov.anyedit.util.EclipseUtils;
import de.loskutov.anyedit.util.TextUtil;

/**
 * @author Andrei
 */
public class CompareWithClipboardAction extends CompareWithAction {

    public CompareWithClipboardAction() {
        super();
    }

    public void selectionChanged(IAction action, ISelection selection) {
        super.selectionChanged(action, selection);
        // XXX action.isEnabled() check causes StackOverflow on Eclipse 3.3
//        if (action.isEnabled()) {
            /* XXX bug in SWT: if running eclipse in debugger, debugger stops to respond on Windows
             * as soon as the first eclipse tries to access the clipboard
            at org.eclipse.swt.internal.ole.win32.COM.VtblCall(Native Method)
            at org.eclipse.swt.internal.ole.win32.IDataObject.GetData(IDataObject.java:25)
            at org.eclipse.swt.dnd.TextTransfer.nativeToJava(TextTransfer.java:121)
            at org.eclipse.swt.dnd.Clipboard.getContents(Clipboard.java:332)
            at org.eclipse.swt.dnd.Clipboard.getContents(Clipboard.java:253)
            at de.loskutov.anyedit.util.EclipseUtils.getClipboardContent(EclipseUtils.java:517)
            at de.loskutov.anyedit.actions.compare.CompareWithClipboardAction.selectionChanged(CompareWithClipboardAction.java:35)
            at org.eclipse.ui.internal.PluginAction.refreshEnablement(PluginAction.java:206)
            at org.eclipse.ui.internal.PluginAction.selectionChanged(PluginAction.java:277)
                */
//            String clipboardContent = EclipseUtils.getClipboardContent();
//            if (clipboardContent == null || clipboardContent.length() == 0) {
//                action.setEnabled(false);
//            }
//        }
    }

    protected StreamContent createRightContent(StreamContent left) throws CoreException {
        String type = left.getType();
        String content = EclipseUtils.getClipboardContent();
        String newLine = null;

        IFile file = selectedContent.getIFile();
        IDocument document = editor.getDocument();
        if (document != null){
            newLine = EclipseUtils.getNewLineFromDocument(document);
        } else if (file != null) {
            newLine = EclipseUtils.getNewLineFromFile(file);
        }
        if (content == null || content.length() == 0) {
            return null;
        }
        return new ClipboardStreamContent(type, newLine, TextUtil.SYSTEM_CHARSET);
    }

}
