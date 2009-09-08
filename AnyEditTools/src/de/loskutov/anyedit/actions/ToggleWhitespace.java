/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.ui.editor.AbstractEditor;

public class ToggleWhitespace extends AbstractAction implements IPartListener,
ISelectionChangedListener, IPageChangedListener, IPropertyListener {

    private static final String SPACES = "AnyEditTools.spaces";

    private static final String TABS = "AnyEditTools.tabs";

    private Map/*<Integer,Boolean>*/partToggleState;

    private IAction proxyAction;


    public ToggleWhitespace() {
        super();
        partToggleState = new HashMap();
    }

    public void run(IAction action) {
        super.run(action);
        this.proxyAction = action;
        toggleEditorAnnotations(action.isChecked());
    }

    public void init(IWorkbenchWindow window1) {
        super.init(window1);
        window1.getActivePage().addPartListener(this);
        IWorkbenchPart activePart = window1.getActivePage().getActivePart();
        if (activePart != null) {
            partActivated(activePart);
        }
        // TODO if we are activated on startup, should get the action from toolbar and set the state
    }

    public void dispose() {
        // causes NPE if no active page is there but dispose is called
        // window.getActivePage().removePartListener(this);
        if(getWindow() == null){
            // strange init issue, just return
            return;
        }
        IWorkbenchPage[] pages = getWindow().getPages();
        for (int i = 0; i < pages.length; i++) {
            pages[i].removePartListener(this);
        }
        proxyAction = null;
        partToggleState = null;
        super.dispose();
    }

    /**
     *
     * @param activeEditor
     * @param rememberEditor true to remember editor, false to discard old one
     * @return may be null
     */
    private Integer getEditorCookie(IEditorPart activeEditor, boolean rememberEditor) {
        if (activeEditor == null) {
            return null;
        }
        AbstractEditor aEditor = new AbstractEditor(activeEditor);
        if (rememberEditor) {
            setEditor(aEditor);
        }
        return new Integer(aEditor.hashCode());
    }

    private void toggleEditorAnnotations(boolean on) {
        AbstractEditor aEditor = getEditor();
        if (aEditor == null) {
            disableButton();
            return;
        }
        IDocumentProvider documentProvider = aEditor.getDocumentProvider();
        if (documentProvider == null) {
            disableButton();
            return;
        }
        Integer editorCookie = new Integer(aEditor.hashCode());
        partToggleState.put(editorCookie, Boolean.valueOf(on));
        IEditorInput input = aEditor.getInput();
        IAnnotationModel annotationModel = documentProvider.getAnnotationModel(input);
        if (!(annotationModel instanceof IAnnotationModelExtension)) {
            return;
        }

        final IAnnotationModelExtension extension = (IAnnotationModelExtension) annotationModel;
        if (!on) {
            removeAnnotations(extension);
            return;
        }

        addAnnotations(aEditor, extension);
    }

    private void addAnnotations(final AbstractEditor aEditor,
            final IAnnotationModelExtension extension) {
        final AnnotationModel annotationModel = new AnnotationModel();
        final IDocument doc = aEditor.getDocument();
        final int lines = doc.getNumberOfLines();
        final Job job = new Job("Toggle whitespace") {

            public IStatus run(IProgressMonitor monitor) {
                //                long start = System.currentTimeMillis();
                monitor.beginTask("Whitespace annotation ...", lines);
                extension.removeAnnotationModel(ToggleWhitespace.class);

                final Annotation sAnnotation = new Annotation(SPACES, false, "Spaces");
                final Annotation tAnnotation = new Annotation(TABS, false, "Tabs");

                for (int i = 0; i < lines && !monitor.isCanceled(); i++) {
                    monitor.internalWorked(1);
                    try {
                        IRegion region = doc.getLineInformation(i);
                        String line = doc.get(region.getOffset(), region.getLength());
                        addAnnotations(annotationModel, line, region, ' ', sAnnotation,
                                monitor);
                        addAnnotations(annotationModel, line, region, '\t', tAnnotation,
                                monitor);
                    } catch (BadLocationException e) {
                        AnyEditToolsPlugin.logError(
                                "Problem during annotation of whitespace", e);
                    }
                }

                if (!monitor.isCanceled()) {
                    extension.addAnnotationModel(ToggleWhitespace.class, annotationModel);
                }
                monitor.done();
                //                long stop = System.currentTimeMillis();
                // XXY
                //                System.out.println("time for " + lines + " lines and "
                //                        + annotationModel.count + " annotations: " + (stop - start));
                return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
            }
        };
        job.setUser(true);
        job.setPriority(Job.INTERACTIVE);
        job.schedule();
    }

    private void addAnnotations(IAnnotationModel annotationModel, String line,
            IRegion region, char c, final Annotation annotation, IProgressMonitor monitor) {
        int startIdx = line.indexOf(c, 0);
        int stopIdx = startIdx;
        Annotation lineAnnotation = null;
        while (stopIdx >= 0 && !monitor.isCanceled()) {
            int oldStopIdx = stopIdx;
            stopIdx = line.indexOf(c, stopIdx + 1);
            if (stopIdx == oldStopIdx + 1) {
                continue;
            }
            if ((stopIdx > oldStopIdx + 1 || stopIdx < 0) && oldStopIdx - startIdx >= 0) {
                int offset = region.getOffset() + startIdx;

                int length = oldStopIdx - startIdx + 1;
                if (true /*length > 1 || c == '\t'*/) {
                    Position position = new Position(offset, length);
                    // on the same line editor must have different annotation object
                    lineAnnotation = new Annotation(annotation.getType(), false,
                            annotation.getText());
                    annotationModel.addAnnotation(lineAnnotation, position);
                }
                startIdx = stopIdx;
            }
        }
    }

    private void removeAnnotations(final IAnnotationModelExtension extension) {
        final Job job = new Job("Toggle whitespace") {
            public IStatus run(IProgressMonitor monitor) {
                monitor.beginTask("Removing whitespace annotations",
                        IProgressMonitor.UNKNOWN);
                extension.removeAnnotationModel(ToggleWhitespace.class);
                extension.addAnnotationModel(ToggleWhitespace.class,
                        new AnnotationModel());
                monitor.done();
                return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
            }
        };
        job.setUser(true);
        job.setPriority(Job.INTERACTIVE);
        job.schedule();
    }

    public void partActivated(IWorkbenchPart part) {
        if (!(part instanceof IEditorPart)) {
            disableButton();
            return;
        }
        Integer cookie = getEditorCookie((IEditorPart) part, true);
        AbstractEditor abstractEditor = getEditor();
        if (abstractEditor.isMultiPage()) {
            addPageListener(part);
        }
        IDocumentProvider documentProvider = abstractEditor.getDocumentProvider();
        if (documentProvider == null) {
            disableButton();
            return;
        }
        enableButton();
        part.addPropertyListener(this);
        if (partToggleState.get(cookie) != null) {
            boolean checked = ((Boolean) partToggleState.get(cookie)).booleanValue();
            setChecked(checked);
        } else {
            partToggleState.put(cookie, Boolean.FALSE);
            setChecked(false);
        }
    }

    private void setChecked(boolean enabled) {
        if (proxyAction != null) {
            proxyAction.setChecked(enabled);
        }
    }

    private void enableButton() {
        if (proxyAction != null) {
            proxyAction.setEnabled(true);
        }
    }

    private void disableButton() {
        if (proxyAction != null) {
            proxyAction.setEnabled(false);
            proxyAction.setChecked(false);
        }
        setEditor(null);
    }

    public void partDeactivated(IWorkbenchPart part) {
        if (!(part instanceof IEditorPart)) {
            return;
        }
        removePageListener(part);
        part.removePropertyListener(this);
    }

    public void partBroughtToTop(IWorkbenchPart part) {
        // ignore
    }

    public void partOpened(IWorkbenchPart part) {
        // not used
    }

    public void partClosed(IWorkbenchPart part) {
        if (!(part instanceof IEditorPart)) {
            return;
        }
        Integer cookie = getEditorCookie((IEditorPart) part, false);
        partToggleState.remove(cookie);
        if (proxyAction != null && proxyAction.isEnabled()) {
            boolean hasEditors = part.getSite().getPage().getEditorReferences().length > 0;
            if (!hasEditors) {
                disableButton();
            }
        }
    }

    /**
     * @param part expected to be a multi page part, never null
     */
    private void addPageListener(IWorkbenchPart part) {
        if (part instanceof FormEditor) {
            FormEditor formEditor = (FormEditor) part;
            formEditor.addPageChangedListener(this);
        } else {
            ISelectionProvider selectionProvider = part.getSite().getSelectionProvider();
            if (selectionProvider instanceof IPostSelectionProvider) {
                ((IPostSelectionProvider) selectionProvider)
                .addSelectionChangedListener(this);
            }
        }
    }

    /**
     * @param part must be non null
     */
    private void removePageListener(IWorkbenchPart part) {
        if (part instanceof FormEditor) {
            FormEditor formEditor = (FormEditor) part;
            formEditor.removePageChangedListener(this);
        } else {
            ISelectionProvider selectionProvider = part.getSite().getSelectionProvider();
            if (selectionProvider instanceof IPostSelectionProvider) {
                ((IPostSelectionProvider) selectionProvider)
                .removeSelectionChangedListener(this);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
        this.proxyAction = action;
    }

    /**
     * to catch the page selection in multi page editors which do not extend FormEditor
     */
    public void selectionChanged(SelectionChangedEvent event) {
        ISelection selection = event.getSelection();
        if (!(selection instanceof ITextSelection)) {
            // TODO this would prevent disabling of the button on web tools xml editor
            // but it does not have sense at all because they do not support highlighting,
            // only underligning
            // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=156086
            if (!(selection instanceof ITreeSelection)) {
                disableButton();
            }
            return;
        }
        if(getWindow() == null){
            // strange init issue, just return
            return;
        }
        AbstractEditor abstractEditor = getEditor();
        IEditorPart editorPart = getWindow().getActivePage().getActiveEditor();
        if (!new AbstractEditor(editorPart).equals(abstractEditor)) {
            partActivated(editorPart);
        }
    }

    /**
     * to catch the page selection in multi page editors
     */
    public void pageChanged(PageChangedEvent event) {
        if(getWindow() == null){
            // strange init issue, just return
            return;
        }
        partActivated(getWindow().getActivePage().getActiveEditor());
    }

    /**
     * to catch the dirty state
     */
    public void propertyChanged(Object source, int propId) {
        if (propId != ISaveablePart.PROP_DIRTY) {
            return;
        }
        if (proxyAction != null && proxyAction.isChecked()) {
            AbstractEditor abstractEditor = getEditor();
            if (abstractEditor != null && !abstractEditor.isDirty()) {
                toggleEditorAnnotations(true);
            }
        }
    }

}
