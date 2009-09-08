/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.compare;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IEditableContentExtension;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.ui.editor.AbstractEditor;
import de.loskutov.anyedit.util.TextUtil;

public class TextStreamContent implements StreamContent, IStreamContentAccessor, IEditableContent,
 IEditableContentExtension {

    private static final String ANY_EDIT_COMPARE = "AnyEditTools.compare";
    private final String selectedText;
    private final Position position;
    private final ContentWrapper content;
    private final AbstractEditor editor;
    private byte[] bytes;
    private boolean dirty;
    private final IPartListener2 partListener;
    private DefaultPositionUpdater positionUpdater;
    private IDocumentListener docListener;
    private Annotation lineAnnotation;
    private AnyeditCompareInput compareInput;
    private boolean disposed;

    /**
     * @param content NOT null
     * @param editor might be null
     */
    public TextStreamContent(ContentWrapper content, AbstractEditor editor) {
        this(content, editor, editor != null ? editor.getSelectedText() : null,
                createPosition(content.getSelection()));
    }

    private TextStreamContent(ContentWrapper content, AbstractEditor editor, String selectedText,
            Position position) {
        this.content = content;
        this.editor = editor == null? new AbstractEditor(null) : editor;
        this.selectedText = selectedText;
        this.position = position;
        this.partListener = new PartListener2Impl();
        if(this.editor.getEditorPart() != null) {
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(
                    partListener);
            IDocument document = this.editor.getDocument();
            // trigger recompare
            if(document != null) {
                docListener = new IDocumentListener(){
                    public void documentAboutToBeChanged(DocumentEvent event) {
                        // noop
                    }
                    public void documentChanged(DocumentEvent event) {
                        updateCompareEditor(event);
                    }
                };
                document.addDocumentListener(docListener);
            }
        }
        if (selectedText != null && isEditable()) {
            hookOnSelection();
        }
    }

    void updateCompareEditor(DocumentEvent event){
        if (event.getText() != null) {
            if ((event.fOffset >= position.offset && event.fOffset < position.offset
                    + position.length)
                    || (event.fOffset <= position.offset && event.fOffset + event.fLength > position.offset)) {
                compareInput.reuseEditor();
            }
        }
    }

    private static Position createPosition(ITextSelection selection) {
        if (selection != null) {
            return new Position(selection.getOffset(), selection.getLength());
        }
        Position pos = new Position(0, 0);
        pos.isDeleted = true;
        return pos;
    }

    private void hookOnSelection() {
        try {
            IDocument document = editor.getDocument();
            positionUpdater = new DefaultPositionUpdater(ANY_EDIT_COMPARE) {
                public void update(DocumentEvent event) {
                    super.update(event);
                }
            };
            document.addPositionCategory(ANY_EDIT_COMPARE);
            document.addPositionUpdater(positionUpdater);
            document.addPosition(ANY_EDIT_COMPARE, position);
            addSelectionAnnotation();
        } catch (BadLocationException e) {
            AnyEditToolsPlugin.logError("Can't create position in document", e);
        } catch (BadPositionCategoryException e) {
            AnyEditToolsPlugin.logError("Can't create position in document", e);
        }
    }

    protected boolean hasDifferecesToEditor() {
        String myText = getChangedCompareText();
        if(myText == null){
            boolean different = selectedText != null && !selectedText.equals(getText(position));
            return different;
        }
        return myText.equals(getText(position));
    }

    private String getChangedCompareText() {
        if(bytes == null){
            return null;
        }
        // use charset from editor
        String charset = editor.computeEncoding();
        if(charset == null){
            charset = TextUtil.SYSTEM_CHARSET;
        }
        try {
            return new String(bytes, charset);
        } catch (UnsupportedEncodingException e) {
            return new String(bytes);
        }
    }

    public Image getImage() {
        return CompareUI.getImage(getType());
    }

    public String getName() {
        return selectedText == null ? content.getName() : "Selection in " + content.getName();
    }

    public String getFullName() {
        return selectedText == null ? content.getFullName() : "Selection in " + content.getFullName();
    }

    public String getType() {
        return content.getFileExtension();
    }

    public Object[] getChildren() {
        return new StreamContent[0];
    }

    public boolean commitChanges(IProgressMonitor pm) throws CoreException {
        if (!dirty || !isEditable()) {
            return true;
        }
        IDocument document = editor.getDocument();
        ITextSelection selection = content.getSelection();
        String text = getChangedCompareText();
        if(text == null){
            dirty = false;
            return true;
        }
        dirty = false;
        if (selection == null) {
            document.set(text);
        } else {
            try {
                document.replace(position.getOffset(), position.getLength(), text);
            } catch (BadLocationException e) {
                AnyEditToolsPlugin.logError("Can't update text in editor", e);
                position.isDeleted = true;
                document.removePositionUpdater(positionUpdater);
                removeSelectionAnnotation();
                return false;
            }
        }
        return true;
    }



    public boolean isDirty() {
        return dirty;
    }

    public InputStream getContents() throws CoreException {
        String charset = editor.computeEncoding();
        if(charset == null){
            charset = TextUtil.SYSTEM_CHARSET;
        }
        if (selectedText != null) {
            byte[] bytes2;
            try {
                bytes2 = selectedText.getBytes(charset);
            } catch (UnsupportedEncodingException e) {
                bytes2 = selectedText.getBytes();
            }
            return new ByteArrayInputStream(bytes2);
        }
        String documentContent = editor.getText();
        if (documentContent != null) {
            byte[] bytes2;
            try {
                bytes2 = documentContent.getBytes(charset);
            } catch (UnsupportedEncodingException e) {
                bytes2 = documentContent.getBytes();
            }
            return new ByteArrayInputStream(bytes2);
        }
        return null;
    }

    public boolean isEditable() {
        if (selectedText != null) {
            return !position.isDeleted && content.isModifiable() && !editor.isDisposed();
        }
        return content.isModifiable() && editor.getDocument() != null;
    }

    public ITypedElement replace(ITypedElement dest, ITypedElement src) {
        return null;
    }

    public void setContent(byte[] newContent) {
        bytes = newContent;
        if(isEditable()) {
            dirty = true;
        }
    }

    public void dispose() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        if(workbench.isClosing()){
            return;
        }
        IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();
        page.removePartListener(partListener);
        if (selectedText != null) {
            removeSelectionAnnotation();
        }
        IDocument document = editor.getDocument();
        if(document != null) {
            document.removeDocumentListener(docListener);
            document.removePosition(position);
            document.removePositionUpdater(positionUpdater);
        }
        position.isDeleted = true;
        lineAnnotation = null;
        editor.dispose();
        dirty = false;
        disposed = true;
    }

    public boolean isDisposed() {
        return disposed;
    }

    private synchronized void addSelectionAnnotation() {
        if(editor.isDisposed()){
            return;
        }
        IEditorInput input = editor.getInput();
        IDocumentProvider documentProvider = editor.getDocumentProvider();
        IAnnotationModel extension = documentProvider.getAnnotationModel(input);
        if (!(extension instanceof IAnnotationModelExtension)) {
            return;
        }
        lineAnnotation = new Annotation(ANY_EDIT_COMPARE, false,
                "This text is being compared with anoter one");
        IAnnotationModelExtension modelExtension = (IAnnotationModelExtension) extension;
        IAnnotationModel model = modelExtension.getAnnotationModel(TextStreamContent.class);
        if (model == null) {
            model = new AnnotationModel();
        }
        model.addAnnotation(lineAnnotation, new Position(position.offset, position.length));
        modelExtension.addAnnotationModel(TextStreamContent.class, model);
    }

    private synchronized void removeSelectionAnnotation() {
        if(editor.isDisposed()){
            return;
        }
        IEditorInput input = editor.getInput();
        IDocumentProvider documentProvider = editor.getDocumentProvider();
        IAnnotationModel extension = documentProvider.getAnnotationModel(input);
        if (!(extension instanceof IAnnotationModelExtension)) {
            return;
        }
        IAnnotationModelExtension modelExtension = (IAnnotationModelExtension) extension;
        IAnnotationModel model = modelExtension.getAnnotationModel(TextStreamContent.class);
        if(model == null){
            return;
        }
        for (Iterator iterator = model.getAnnotationIterator(); iterator.hasNext();) {
            Annotation ann = (Annotation) iterator.next();
            if (lineAnnotation == ann) {
                model.removeAnnotation(ann);
            }
        }
        if (!model.getAnnotationIterator().hasNext()) {
            modelExtension.removeAnnotationModel(TextStreamContent.class);
        }
    }

    private final class PartListener2Impl implements IPartListener2 {

        public void partClosed(IWorkbenchPartReference partRef) {
            if (editor.getEditorPart() == partRef.getPart(false)) {

                positionUpdater = null;
                dispose();
            }
        }

        public void partInputChanged(IWorkbenchPartReference partRef) {
            partClosed(partRef);
        }

        public void partActivated(IWorkbenchPartReference partRef) {
            // noop
        }

        public void partBroughtToTop(IWorkbenchPartReference partRef) {
            // noop
        }

        public void partDeactivated(IWorkbenchPartReference partRef) {
            // noop
        }

        public void partHidden(IWorkbenchPartReference partRef) {
            // noop
        }

        public void partOpened(IWorkbenchPartReference partRef) {
            // noop
        }

        public void partVisible(IWorkbenchPartReference partRef) {
            // noop
        }
    }

    public void init(AnyeditCompareInput input) {
        this.compareInput = input;
    }

    /** create new one with the selection re-created from annotation, if any */
    public StreamContent recreate() {
        TextStreamContent tc;

        // we should not dispose editor OR should re-create editor here
        if (selectedText == null || position.isDeleted) {
            tc = new TextStreamContent(content, editor.recreate(), null, createPosition(null));
        } else {
            Position pos = new Position(position.getOffset(), position.getLength());
            TextSelection sel = new TextSelection(editor.getDocument(), pos.getOffset(), pos.getLength());
            content.setSelection(sel);
            String text = getText(pos);
            tc = new TextStreamContent(content, editor.recreate(), text, pos);
        }
        return tc;
    }

    private String getText(Position pos) {
        String text = null;
        try {
            text = editor.getDocument().get(pos.getOffset(), pos.getLength());
        } catch (BadLocationException e) {
            // ignore, shit happens
        }
        return text;
    }

    public boolean isReadOnly() {
        return editor.isEditorInputModifiable();
    }

    public IStatus validateEdit(Shell shell) {
        boolean state = editor.validateEditorInputState();
        if(state){
            return Status.OK_STATUS;
        }
        return Status.CANCEL_STATUS;
    }

}
