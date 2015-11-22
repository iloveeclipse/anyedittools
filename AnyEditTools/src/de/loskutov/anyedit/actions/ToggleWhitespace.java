/*******************************************************************************
 * Copyright (c) 2009 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.texteditor.IDocumentProvider;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;
import de.loskutov.anyedit.ui.editor.AbstractEditor;
import de.loskutov.anyedit.util.EclipseUtils;

public class ToggleWhitespace extends AbstractAction {

    private static final String SHOW_WS_COMMAND = "AnyEdit.ShowWhiteSpace.command";

    private static final String SPACES = "AnyEditTools.spaces";

    private static final String TABS = "AnyEditTools.tabs";

    private static final String TRAILING = "AnyEditTools.trailingws";

    private IAction proxyAction;

    private SuperListener mainListener;

    public ToggleWhitespace() {
        super();
    }

    @Override
    public void init(IWorkbenchWindow window1) {
        super.init(window1);
        mainListener = installGlobalListener(window1);
        // TODO if we are activated on startup, should get the action from toolbar and set the state
    }

    @Override
    public void run(IAction action) {
        super.run(action);
        this.proxyAction = action;
        boolean newValue = !isChecked();
        setChecked(newValue);
        applyEditorAnnotations(newValue);
        // XXX update the "toggled" state based on the current editor
        mainListener.ws.updateCheckState();
    }

    SuperListener installGlobalListener(IWorkbenchWindow window1){
        IHandlerService hs = (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
        Object variable = hs.getCurrentState().getRoot().getVariable(SHOW_WS_COMMAND);
        if(variable instanceof SuperListener) {
            return (SuperListener) variable;
        }
        SuperListener superListener = new SuperListener(this);
        hs.getCurrentState().getRoot().addVariable(SHOW_WS_COMMAND, superListener);
        IWorkbenchPage activePage = window1.getActivePage();
        activePage.addPartListener(superListener);
        IWorkbenchPart activePart = activePage.getActivePart();
        if (activePart != null) {
            superListener.partActivated(activePart);
        }
        AnyEditToolsPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(superListener);
        return superListener;
    }

    void uninstallGlobalListener(){
        AnyEditToolsPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(mainListener);
        IHandlerService hs = (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
        Object variable = hs.getCurrentState().getRoot().getVariable(SHOW_WS_COMMAND);
        if(variable == null || variable.equals(Boolean.FALSE)) {
            return;
        }
        hs.getCurrentState().getRoot().addVariable(SHOW_WS_COMMAND, Boolean.FALSE);
        IWorkbenchPage[] pages = getWindow().getPages();
        for (int i = 0; i < pages.length; i++) {
            pages[i].removePartListener(mainListener);
        }
    }

    @Override
    public void dispose() {
        // causes NPE if no active page is there but dispose is called
        // window.getActivePage().removePartListener(this);
        if(getWindow() == null){
            // strange init issue, just return
            return;
        }

        proxyAction = null;
        uninstallGlobalListener();
        super.dispose();
    }

    private void applyEditorAnnotations(boolean on) {
        AbstractEditor aEditor = getEditor();
        if (aEditor == null) {
            // XXX ?
            //            disableButton();
            return;
        }
        IDocumentProvider documentProvider = aEditor.getDocumentProvider();
        if (documentProvider == null) {
            // XXX ?
            //            disableButton();
            return;
        }

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

    private static void addAnnotations(final AbstractEditor aEditor,
            final IAnnotationModelExtension extension) {
        final AnnotationModel annotationModel = new AnnotationModel();
        final IDocument doc = aEditor.getDocument();
        final int lines = doc.getNumberOfLines();
        final Job job = new Job("Toggle whitespace") {

            @Override
            public IStatus run(IProgressMonitor monitor) {
                if(aEditor.isDisposed() || aEditor.getPart() == null){
                    return Status.CANCEL_STATUS;
                }

                monitor.beginTask("Whitespace annotation ...", lines);
                extension.removeAnnotationModel(ToggleWhitespace.class);

                final Annotation sAnnotation = new Annotation(SPACES, false, "Spaces");
                final Annotation tAnnotation = new Annotation(TABS, false, "Tabs");
                boolean showTrailingDifferently = isTrailingShownDifferently();
                for (int i = 0; i < lines && !monitor.isCanceled(); i++) {
                    monitor.internalWorked(1);
                    try {
                        IRegion region = doc.getLineInformation(i);
                        String line = doc.get(region.getOffset(), region.getLength());
                        if(showTrailingDifferently && line.length() > 0 && line.trim().length() == 0){
                            addTrailingAnnotation(annotationModel, line, region);
                        } else {
                            addAnnotations(annotationModel, line, region, ' ', sAnnotation, monitor);
                            addAnnotations(annotationModel, line, region, '\t', tAnnotation, monitor);
                        }
                    } catch (BadLocationException e) {
                        AnyEditToolsPlugin.logError(
                                "Problem during annotation of whitespace", e);
                    }
                }

                if (!monitor.isCanceled()) {
                    extension.addAnnotationModel(ToggleWhitespace.class, annotationModel);
                }
                monitor.done();
                return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
            }
        };
        job.setUser(true);
        job.setPriority(Job.INTERACTIVE);
        job.schedule();
    }

    private static void addAnnotations(IAnnotationModel annotationModel, String line,
            IRegion region, char c, final Annotation annotation, IProgressMonitor monitor) {
        int startIdx = line.indexOf(c, 0);
        int stopIdx = startIdx;
        boolean showTrailingDifferently = isTrailingShownDifferently();
        boolean showTrailingOnly = isTrailingOnly();
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
                    Annotation lineAnnotation = null;
                    Position position = new Position(offset, length);

                    if(oldStopIdx == line.length() - 1) {
                        if (showTrailingDifferently) {
                            // on the same line editor must have different annotation object
                            lineAnnotation = new Annotation(TRAILING, false, "Trailing whitespace");
                        } else {
                            // on the same line editor must have different annotation object
                            lineAnnotation = new Annotation(annotation.getType(), false,
                                    annotation.getText());
                        }
                    } else if(!showTrailingOnly){
                        // on the same line editor must have different annotation object
                        lineAnnotation = new Annotation(annotation.getType(), false,
                                annotation.getText());
                    }
                    if(lineAnnotation != null) {
                        annotationModel.addAnnotation(lineAnnotation, position);
                    }
                }
                startIdx = stopIdx;
            }
        }
    }

    private static void addTrailingAnnotation(IAnnotationModel annotationModel, String line,
            IRegion region) {
        int offset = region.getOffset();
        int length = line.length();
        Position position = new Position(offset, length);
        Annotation lineAnnotation = new Annotation(TRAILING, false, "Trailing whitespace");
        annotationModel.addAnnotation(lineAnnotation, position);
    }

    private static void removeAnnotations(final IAnnotationModelExtension extension) {
        final Job job = new Job("Toggle whitespace") {
            @Override
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

    static class SuperListener implements IPartListener, ISelectionChangedListener,
    IPageChangedListener, IPropertyListener, IPropertyChangeListener {

        private final ToggleWhitespace ws;

        public SuperListener(ToggleWhitespace ws) {
            this.ws = ws;
        }

        @Override
        public void partActivated(IWorkbenchPart part) {
            if (!(part instanceof IEditorPart)) {
                ws.disableButton();
                return;
            }
            IEditorPart activeEditor = EclipseUtils.getActiveEditor();
            if(activeEditor != part){
                ws.disableButton();
                return;
            }
            AbstractEditor abstractEditor = ws.createActiveEditorDelegate();
            ws.setEditor(abstractEditor);

            if (abstractEditor.isMultiPage()) {
                addPageListener(part);
            }
            IDocumentProvider documentProvider = abstractEditor.getDocumentProvider();
            if (documentProvider == null) {
                ws.disableButton();
                return;
            }
            ws.enableButton();
            part.addPropertyListener(this);
            ws.applyEditorAnnotations(ToggleWhitespace.isChecked());
        }

        @Override
        public void partDeactivated(IWorkbenchPart part) {
            if (!(part instanceof IEditorPart)) {
                return;
            }
            removePageListener(part);
            part.removePropertyListener(this);
        }

        @Override
        public void partBroughtToTop(IWorkbenchPart part) {
            // ignore
        }

        @Override
        public void partOpened(IWorkbenchPart part) {
            // not used
        }

        @Override
        public void partClosed(IWorkbenchPart part) {
            if (!(part instanceof IEditorPart)) {
                return;
            }
            if (ws.proxyAction != null && ws.proxyAction.isEnabled()) {
                boolean hasEditors = part.getSite().getPage().getEditorReferences().length > 0;
                if (!hasEditors) {
                    ws.disableButton();
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


        /**
         * to catch the page selection in multi page editors which do not extend FormEditor
         */
        @Override
        public void selectionChanged(SelectionChangedEvent event) {
            ISelection selection = event.getSelection();
            if (!(selection instanceof ITextSelection)) {
                // TODO this would prevent disabling of the button on web tools xml editor
                // but it does not have sense at all because they do not support highlighting,
                // only underligning
                // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=156086
                if (!(selection instanceof ITreeSelection)) {
                    ws.disableButton();
                }
                return;
            }
            if(ws.getWindow() == null){
                // strange init issue, just return
                return;
            }
            AbstractEditor abstractEditor = ws.getEditor();
            IEditorPart editorPart = ws.getWindow().getActivePage().getActiveEditor();
            if (!new AbstractEditor(editorPart).equals(abstractEditor)) {
                partActivated(editorPart);
            }
        }

        /**
         * to catch the page selection in multi page editors
         */
        @Override
        public void pageChanged(PageChangedEvent event) {
            if(ws.getWindow() == null){
                // strange init issue, just return
                return;
            }
            partActivated(ws.getWindow().getActivePage().getActiveEditor());
        }

        /**
         * to catch the dirty state
         */
        @Override
        public void propertyChanged(Object source, int propId) {
            if (propId != ISaveablePart.PROP_DIRTY) {
                return;
            }
            if (ws.proxyAction != null && ws.proxyAction.isChecked()) {
                AbstractEditor abstractEditor = ws.getEditor();
                if (abstractEditor != null && !abstractEditor.isDirty()) {
                    ws.applyEditorAnnotations(ToggleWhitespace.isChecked());
                }
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            String key = event.getProperty();
            if (!IAnyEditConstants.SHOW_TRAILING_ONLY.equals(key)
                    && !IAnyEditConstants.SHOW_TRAILING_DIFFERENTLY.equals(key)) {
                return;
            }
            if(ToggleWhitespace.isChecked()){
                ws.applyEditorAnnotations(true);
            }

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
        }
        setEditor(null);
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        this.proxyAction = action;
        updateCheckState();
    }

    private void updateCheckState() {
        if(proxyAction == null) {
            return;
        }
        boolean checked = isChecked();
        if(checked != proxyAction.isChecked()) {
            // XXX does not work with commands
            proxyAction.setChecked(checked);
        }
        IEditorPart activeEditor = EclipseUtils.getActiveEditor();
        if(activeEditor == null){
            disableButton();
            return;
        }
    }


    protected static boolean isChecked() {
        return getPrefs().getBoolean(IAnyEditConstants.SHOW_WHITESPACE);
    }

    private static IPreferenceStore getPrefs() {
        return AnyEditToolsPlugin.getDefault().getPreferenceStore();
    }

    protected static void setChecked(boolean checked) {
        getPrefs().setValue(IAnyEditConstants.SHOW_WHITESPACE, checked);
    }

    protected static boolean isTrailingOnly() {
        return getPrefs().getBoolean(IAnyEditConstants.SHOW_TRAILING_ONLY);
    }

    protected static boolean isTrailingShownDifferently() {
        return getPrefs().getBoolean(IAnyEditConstants.SHOW_TRAILING_DIFFERENTLY);
    }

}
