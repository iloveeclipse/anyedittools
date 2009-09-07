/*******************************************************************************
 * Copyright (c) 2004 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
/* This class is started as extension of Rahul Kuchal's whitespace plugin.
 * Rahul Kuchal - http://www.kuchhal.com/ */

package de.loskutov.anyedit.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;
import de.loskutov.anyedit.Messages;
import de.loskutov.anyedit.ui.editor.AbstractEditor;
import de.loskutov.anyedit.util.LineReplaceResult;
import de.loskutov.anyedit.util.TextReplaceResultSet;
import de.loskutov.anyedit.util.TextUtil;

public abstract class AbstractTextAction extends AbstractAction
    implements IEditorActionDelegate {

    public static final String ACTION_ID_CONVERT_TABS = IAnyEditConstants.ACTION_ID_CONVERT_TABS;
    public static final String ACTION_ID_CONVERT_SPACES = IAnyEditConstants.ACTION_ID_CONVERT_SPACES;
    public static final String ACTION_ID_UNESCAPE = "AnyEdit.unescape";
    public static final String ACTION_ID_ENCODE = "AnyEdit.base64encode";
    public static final String ACTION_ID_UNICODIFY = "AnyEdit.unicodify";
    public static final String ACTION_ID_TO_UPPER = "AnyEdit.toUpperCase";
    public static final String ACTION_ID_TO_LOWER = "AnyEdit.toLowerCase";
    public static final String ACTION_ID_CAPITALIZE = "AnyEdit.capitalize";
    public static final String ACTION_ID_CAMEL = "AnyEdit.camel";

    protected TextUtil textUtil;
    private boolean isUsedOnSave;

    public AbstractTextAction() {
        super();
        init();
    }

    protected final void init() {
         textUtil = TextUtil.getDefaultTextUtilities();
    }

    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
        if(targetEditor == null){
            return;
        }
        setEditor(new AbstractEditor(targetEditor));
    }

    protected abstract TextReplaceResultSet estimateActionRange(IDocument doc);

    /**
     * @see IActionDelegate#run(IAction)
     */
    public final void run(IAction action) {
        super.run(action);
        if(getEditor() == null){
            return;
        }
        AbstractEditor currEditor = getEditor();
        IDocument doc = currEditor.getDocument();
        if (doc == null) {
            return;
        }

        TextReplaceResultSet result = estimateActionRange(doc);

        // no lines affected - return immediately
        if(result.getNumberOfLines() == 0){
            return;
        }

        // save dirty buffer, if enabled and if this action is not chained with
        // "save" operation
        if(!isUsedOnSave() && currEditor.isDirty() && isSaveDirtyBufferEnabled()){
            IProgressMonitor monitor = new NullProgressMonitor();
            currEditor.doSave(monitor);
            if(monitor.isCanceled()){
                boolean ok = MessageDialog.openConfirm(
                    AnyEditToolsPlugin.getShell(),
                    Messages.title,
                    Messages.continueOperationMessage);
                if(!ok){
                    return;
                }
            }
        }

        try {
            doTextOperation(doc, action.getId(), result);
        } catch (Exception ex) {
            AnyEditToolsPlugin.errorDialog(null, ex);
            return;
        }

        if (!result.areResultsChanged()) {
            return;
        }

        // prepare to save: make sure, that file is not readonly
        currEditor.validateEditorInputState();
        if (!currEditor.isEditorInputModifiable()) {
            if(!isUsedOnSave()) {
                Shell shell = AnyEditToolsPlugin.getShell();
                MessageDialog.openInformation(
                    shell,
                    Messages.title,
                    Messages.fileIsReadOnly);
            }
            return;
        }

        int docLinesNbr = doc.getNumberOfLines();
        int changedLinesNbr = result.getNumberOfLines();
        boolean rewriteWholeDoc = changedLinesNbr >= docLinesNbr;
        DocumentRewriteSession rewriteSession =
            currEditor.startSequentialRewriteMode(rewriteWholeDoc);

        // some oddities with document??? prevent overflow in changedLinesNbr
        if(rewriteWholeDoc){
            changedLinesNbr = docLinesNbr;
        }

//        Map partitioners = null;
//        if(EclipseUtils.is31Compatible()){
//            // seems to have problems with StructuredTextReconciler in 3.0
//            partitioners = TextUtilities.removeDocumentPartitioners(doc);
//        }

        /*
         * TODO think on long running operations
         */
//        if (changedLinesNbr > 150) {
//            Display display= getTextEditor().getEditorSite().getWorkbenchWindow().getShell().getDisplay();
//            BusyIndicator.showWhile(display, runnable);
//        } else
//            runnable.run();

        try {
            for (int i = 0; i < changedLinesNbr; i++) {
                LineReplaceResult trr = result.get(i);
                if(trr != null){
                    IRegion lineInfo = doc.getLineInformation(i + result.getStartLine());
                    doc.replace(lineInfo.getOffset() + trr.startReplaceIndex,
                            trr.rangeToReplace, trr.textToReplace);
                }
            }
        } catch (Exception ex) {
            AnyEditToolsPlugin.errorDialog(null, ex);
        } finally {
            currEditor.stopSequentialRewriteMode(rewriteSession);
            // seems to have problems with StructuredTextReconciler
//            if(partitioners != null){
//                TextUtilities.addDocumentPartitioners(doc, partitioners);
//            }
            result.clear();
        }
    }

    /**
     * Should be invoked always after estimateActionRange() to ensure that
     * operaton is possible
     * @param doc cannot be null
     * @param actionID desired text action id
     * @param resultSet cannot be null
     */
    protected abstract void doTextOperation(IDocument doc, String actionID, TextReplaceResultSet resultSet) throws BadLocationException;

    protected static boolean isSaveDirtyBufferEnabled() {
        return AnyEditToolsPlugin.getDefault().getPreferenceStore().getBoolean(
            IAnyEditConstants.SAVE_DIRTY_BUFFER);
    }

    /**
     * @return true, only if this action is intended to be run (chained) always
     * just before "save" operation.
     */
    public boolean isUsedOnSave() {
        return isUsedOnSave;
    }

    /**
     * @param isUsedOnSave true, only if this action is intended to be run
     * (chained) always just before "save" operation.
     */
    public void setUsedOnSave(boolean isUsedOnSave) {
        this.isUsedOnSave = isUsedOnSave;
    }

    public static boolean isSaveAndConvertEnabled() {
        return AnyEditToolsPlugin.getDefault().getPreferenceStore().getBoolean(
            IAnyEditConstants.SAVE_AND_CONVERT_ENABLED);
    }

    public static boolean isSaveAndTrimEnabled() {
        return AnyEditToolsPlugin.getDefault().getPreferenceStore().getBoolean(
            IAnyEditConstants.SAVE_AND_TRIM_ENABLED);
    }
}
