/*******************************************************************************
 * Copyright (c) 2009 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.anyedit.ui.editor;

import static de.loskutov.anyedit.util.EclipseUtils.getAdapter;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.editors.text.IStorageDocumentProvider;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.PageBookView;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension;
import org.eclipse.ui.texteditor.ITextEditorExtension2;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.util.EclipseUtils;
import de.loskutov.anyedit.util.TextUtil;

/**
 * @author Andrey
 */
public class AbstractEditor implements ITextEditorExtension2 {
    private IWorkbenchPart wPart;
    private boolean multipage;

    /**
     * Proxy for different editor types
     */
    public AbstractEditor(final @Nullable IWorkbenchPart editorPart) {
        this();
        wPart = editorPart;
        if(editorPart instanceof FormEditor){
            FormEditor fe = (FormEditor)editorPart;
            wPart = fe.getActiveEditor();
            multipage = true;
        } else if(editorPart instanceof MultiPageEditorPart){
            MultiPageEditorPart me = (MultiPageEditorPart)editorPart;
            // followed is because "getActiveEditor" method is protected in
            // MultiPageEditorPart class.
            try {
                Method method = MultiPageEditorPart.class.getDeclaredMethod(
                        "getActiveEditor", null);
                method.setAccessible(true);
                wPart = (IEditorPart) method.invoke(me, null);
                multipage = true;
            } catch (Exception e) {
                AnyEditToolsPlugin.logError("Can't get current page", e);
            }
        } else if(editorPart!= null
                && !(editorPart instanceof ITextEditor)
                && !(editorPart instanceof IViewPart)) {
            /*
             * added to support different multipage editors which are not extending
             * MultiPageEditorPart, like adobe Flex family editors
             */
            try {
                Method[] declaredMethods = editorPart.getClass().getDeclaredMethods();
                for (int i = 0; i < declaredMethods.length; i++) {
                    Method method = declaredMethods[i];
                    String methodName = method.getName();
                    if(("getActiveEditor".equals(methodName)
                            // lines below are for Flex 3, above for Flex 2
                            ||  "getCodeEditor".equals(methodName)
                            ||  "getTextEditor".equals(methodName))
                            && method.getParameterTypes().length == 0){
                        method.setAccessible(true);
                        IEditorPart ePart = (IEditorPart) method.invoke(editorPart, null);
                        if(ePart == null) {
                            continue;
                        }
                        wPart = ePart;
                        multipage = true;
                        break;
                    }
                }
            } catch (Exception e) {
                AnyEditToolsPlugin.logError("Can't get current page", e);
            }
        }
    }

    private AbstractEditor() {
        super();
    }

    public AbstractEditor recreate(){
        AbstractEditor ae = new AbstractEditor();
        ae.wPart = wPart;
        ae.multipage = multipage;
        return ae;
    }

    public boolean isMultiPage(){
        return multipage;
    }

    /**
     * @return may return null
     */
    @Nullable
    public IDocumentProvider getDocumentProvider() {
        if (wPart == null) {
            return null;
        }
        if (wPart instanceof ITextEditor) {
            return ((ITextEditor) wPart).getDocumentProvider();
        }

        IDocumentProvider docProvider = getAdapter(wPart, IDocumentProvider.class);
        return docProvider;
    }

    /**
     * @return may return null
     */
    @Nullable
    public IEditorInput getInput() {
        if (!(wPart instanceof IEditorPart)) {
            return null;
        }
        return ((IEditorPart) wPart).getEditorInput();
    }

    /**
     * @return may return null
     */
    @Nullable
    public IFile getIFile(){
        if(wPart == null){
            return null;
        }
        IEditorInput input = getInput();
        if(input != null){
            IFile adapter = EclipseUtils.getIFile(input, true);
            if(adapter != null){
                return adapter;
            }
        }
        IFile adapter = EclipseUtils.getIFile(wPart, true);
        return adapter;
    }

    @Nullable
    public File getFile(){
        if(wPart == null){
            return null;
        }
        IEditorInput input = getInput();
        if(input != null){
            File adapter = EclipseUtils.getFile(input, true);
            if(adapter != null){
                return adapter;
            }
        }
        File adapter = EclipseUtils.getFile(wPart, true);
        if(adapter != null){
            return adapter;
        }
        ISelectionProvider sp = getSelectionProvider();
        if(sp != null){
            ISelection selection = sp.getSelection();
            if(selection != null){
                adapter = EclipseUtils.getFile(selection, true);
            }
        }
        return adapter;
    }

    /**
     * @see ITypedElement#getType()
     */
    @Nullable
    public String getContentType(){
        URI uri = getURI();
        String path;
        if(uri != null) {
            path = uri.toString();
        } else {
            File file = getFile();
            if(file == null) {
                return null;
            }
            path = file.getAbsolutePath();
        }
        int dot = path.lastIndexOf('.') + 1;
        if(dot >= 0){
            return path.substring(dot);
        }
        return path;
    }

    @NonNull
    public String getTitle(){
        if(wPart == null){
            return "";
        }
        String title = wPart.getTitle();
        return title != null? title : "";
    }

    /**
     * @return may return null
     */
    @Nullable
    private URI getURI(){
        return EclipseUtils.getURI(getInput());
    }

    @NonNull
    public String computeEncoding() {
        IFile file = getIFile();
        if(file != null) {
            try {
                String charset = file.getCharset();
                if(charset != null) {
                    return charset;
                }
            } catch (CoreException e) {
                // ignore;
            }
        }
        IDocumentProvider provider = getDocumentProvider();
        if(provider instanceof IStorageDocumentProvider) {
            IStorageDocumentProvider docProvider = (IStorageDocumentProvider) provider;
            String encoding = docProvider.getEncoding(getInput());
            if(encoding != null) {
                return encoding;
            }
        }
        return TextUtil.SYSTEM_CHARSET;
    }

    @Nullable
    public ISelectionProvider getSelectionProvider() {
        if (wPart == null) {
            return null;
        }
        if (wPart instanceof ITextEditor) {
            return ((ITextEditor) wPart).getSelectionProvider();
        }
        // PDEMultiPageEditor doesn't implement ITextEditor interface
        ISelectionProvider adapter = getAdapter(wPart, ISelectionProvider.class);
        if(adapter != null){
            return adapter;
        }
        ISelectionProvider sp = wPart.getSite().getSelectionProvider();
        if(sp != null){
            return sp;
        }
        TextViewer viewer = getAdapter(wPart, TextViewer.class);
        if(viewer != null){
            return viewer.getSelectionProvider();
        }
        return null;
    }

    @Nullable
    public IDocument getDocument() {
        IEditorInput input = getInput();
        if(input != null) {
            IDocumentProvider provider = getDocumentProvider();
            if (provider != null) {
                return provider.getDocument(input);
            }
        }
        if (wPart instanceof PageBookView) {
            IPage page = ((PageBookView) wPart).getCurrentPage();
            ITextViewer viewer = EditorPropertyTester.getViewer(page);
            if( viewer != null ) {
                return  viewer.getDocument();
            }
        }
        if (wPart instanceof IViewPart) {
            ISelectionProvider sp = ((IViewPart) wPart).getViewSite().getSelectionProvider();
            if(sp instanceof ITextViewer){
                return ((ITextViewer) sp).getDocument();
            }
        }
        if(wPart != null){
            TextViewer viewer = getAdapter(wPart, TextViewer.class);
            if(viewer != null){
                return viewer.getDocument();
            }
        }
        return null;
    }

    @Nullable
    public ITextSelection getSelection(){
        ISelectionProvider selectionProvider = getSelectionProvider();
        if (selectionProvider == null) {
            return null;
        }
        ISelection selection = selectionProvider.getSelection();
        if (selection instanceof ITextSelection) {
            return (ITextSelection) selection;
        }
        return null;
    }

    @Nullable
    public String getSelectedText(){
        ITextSelection selection = getSelection();
        if(selection == null){
            return null;
        }
        String selectedText = selection.getText();
        if(selectedText != null && selectedText.length() > 0) {
            return selectedText;
        }
        return null;
    }

    public void selectAndReveal(int lineNumber){
        if (!(wPart instanceof ITextEditor)) {
            return;
        }
        IDocument document = getDocument();
        if(document != null){
            try {
                // line count internally starts with 0, and not with 1 like in GUI
                IRegion lineInfo = document.getLineInformation(lineNumber - 1);
                if(lineInfo != null){
                    ((ITextEditor)wPart).selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
                }
            } catch (BadLocationException e) {
                //ignored because line number may not really exist in document, we guess this...
            }
        }
    }

    public boolean isDirty(){
        if (!(wPart instanceof ISaveablePart)) {
            return false;
        }
        return ((ISaveablePart) wPart).isDirty();
    }

    @Nullable
    private <T> T getAdapterFromPart(Class<T> clazz){
        if (wPart == null) {
            return null;
        }
        return getAdapter(wPart, clazz);
    }

    public void doSave(IProgressMonitor moni){
        if (!(wPart instanceof ISaveablePart)) {
            return;
        }
        ((ISaveablePart) wPart).doSave(moni);
    }

    @Override
    public boolean isEditorInputModifiable() {
        if (wPart == null) {
            return false;
        }
        if (wPart instanceof ITextEditorExtension2) {
            return ((ITextEditorExtension2)wPart).isEditorInputModifiable();
        }
        if (wPart instanceof ITextEditorExtension) {
            return !((ITextEditorExtension) wPart).isEditorInputReadOnly();
        }
        if (wPart instanceof ITextEditor) {
            return ((ITextEditor)wPart).isEditable();
        }
        return true;
    }

    @Override
    public boolean validateEditorInputState() {
        if (wPart == null) {
            return false;
        }
        if (wPart instanceof ITextEditorExtension2) {
            return ((ITextEditorExtension2)wPart).validateEditorInputState();
        }
        return true;
    }

    /**
     * Sets the sequential rewrite mode of the viewer's document.
     */
    public void stopSequentialRewriteMode(DocumentRewriteSession session) {
        IDocument document = getDocument();
        if (document instanceof IDocumentExtension4) {
            IDocumentExtension4 extension= (IDocumentExtension4) document;
            extension.stopRewriteSession(session);
        } else if (document instanceof IDocumentExtension) {
            IDocumentExtension extension= (IDocumentExtension)document;
            extension.stopSequentialRewrite();
        }

        IRewriteTarget target = getAdapterFromPart(IRewriteTarget.class);
        if (target != null) {
            target.endCompoundChange();
            target.setRedraw(true);
        }
    }

    /**
     * Starts  the sequential rewrite mode of the viewer's document.
     * @param normalized <code>true</code> if the rewrite is performed
     * from the start to the end of the document
     */
    @Nullable
    public DocumentRewriteSession startSequentialRewriteMode(boolean normalized) {
        // de/activate listeners etc, prepare multiple replace
        IRewriteTarget target = getAdapterFromPart(IRewriteTarget.class);
        if (target != null) {
            target.setRedraw(false);
            target.beginCompoundChange();
        }

        DocumentRewriteSession rewriteSession = null;
        IDocument document = getDocument();
        if (document instanceof IDocumentExtension4) {
            IDocumentExtension4 extension= (IDocumentExtension4) document;
            if (normalized) {
                rewriteSession = extension
                        .startRewriteSession(DocumentRewriteSessionType.STRICTLY_SEQUENTIAL);
            } else {
                rewriteSession = extension
                        .startRewriteSession(DocumentRewriteSessionType.SEQUENTIAL);
            }
        } else if (document instanceof IDocumentExtension) {
            IDocumentExtension extension = (IDocumentExtension) document;
            extension.startSequentialRewrite(normalized);
        }
        return rewriteSession;
    }

    /**
     * clean reference to wrapped "real" editor object
     */
    public void dispose(){
        wPart = null;
    }

    public boolean isDisposed(){
        return wPart == null;
    }

    @Override
    public int hashCode() {
        IDocumentProvider provider = getDocumentProvider();
        IEditorInput input = getInput();
        int code = 0;
        if(provider != null){
            code += provider.hashCode();
        }
        if(input != null){
            code += input.hashCode();
        }
        if(wPart != null){
            code += wPart.hashCode();
        }
        return code;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){
            return true;
        }
        if(!(obj instanceof AbstractEditor)){
            return false;
        }
        AbstractEditor other = (AbstractEditor) obj;
        if(this.wPart != other.wPart){
            return false;
        }
        // now check for multi page stuff
        if(!isMultiPage()){
            return true;
        }
        return this.hashCode() == other.hashCode();
    }

    @Nullable
    public IWorkbenchPart getPart() {
        return wPart;
    }

    @Nullable
    public String getText() {
        IDocument doc = getDocument();
        return doc != null? doc.get() : null;
    }

}
