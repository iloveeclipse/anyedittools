/*******************************************************************************
 * Copyright (c) 2009 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.actions.compare;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.progress.UIJob;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.compare.ContentWrapper;
import de.loskutov.anyedit.compare.ExternalFileStreamContent;
import de.loskutov.anyedit.compare.FileStreamContent;
import de.loskutov.anyedit.compare.StreamContent;
import de.loskutov.anyedit.compare.TextStreamContent;
import de.loskutov.anyedit.ui.editor.AbstractEditor;
import de.loskutov.anyedit.util.EclipseUtils;

/**
 * @author Andrey
 */
public class CompareWithEditorAction extends CompareWithAction {

    private static final String COMPARE_EDITOR_ID = "org.eclipse.compare.CompareEditor";

    public CompareWithEditorAction() {
        super();
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        // restore disabled state and let us re-test enablement
        action.setEnabled(true);
        super.selectionChanged(action, selection);
        if (action.isEnabled()) {
            Object[] elements = new EditorsContentProvider(editor, selectedContent).getElements(null);
            action.setEnabled(elements.length > 0);
        }
    }

    @Override
    protected StreamContent createRightContent(StreamContent left) throws CoreException {
        // WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider();
        ListDialog dialog = createSelectionDialog(editor, selectedContent,
                "Select an opened editor to compare with current file:");

        int result = dialog.open();
        if (result == Window.OK) {
            Object[] objects = dialog.getResult();
            if (objects != null && objects.length == 1
                    && objects[0] instanceof IEditorReference) {
                AbstractEditor editor1 = new AbstractEditor(((IEditorReference) objects[0])
                        .getEditor(true));
                ContentWrapper content = ContentWrapper.create(editor1);
                if(content == null){
                    return null;
                }

                IDocument document = editor1.getDocument();
                if(document != null) {
                    return new TextStreamContent(content, editor1);
                }

                if (content.getIFile() != null) {
                    return new FileStreamContent(content);
                }

                if (content.getFile() != null) {
                    return new ExternalFileStreamContent(content);
                }

            }
        }
        return null;
    }

    public static ListDialog createSelectionDialog(AbstractEditor myEditor, ContentWrapper selectedContent, String message) {
        EditorsContentProvider contentProvider = new EditorsContentProvider(myEditor, selectedContent);
        ILabelProvider labelProvider = new EditorsLabelProvider(contentProvider);

        class MyListDialog extends ListDialog {
            public MyListDialog(Shell parent) {
                super(parent);
                setShellStyle(getShellStyle() | SWT.RESIZE);
            }
        }
        ListDialog dialog = new MyListDialog(AnyEditToolsPlugin.getShell());
        dialog.setContentProvider(contentProvider);
        dialog.setLabelProvider(labelProvider);
        dialog.setTitle("Select editor");
        dialog.setMessage(message);
        dialog.setInput(new byte[0]);
        dialog.setDialogBoundsSettings(AnyEditToolsPlugin.getDefault()
                .getDialogSettings(),
                Dialog.DIALOG_PERSISTLOCATION | Dialog.DIALOG_PERSISTSIZE);
        return dialog;
    }

    static  final class EditorsLabelProvider extends LabelProvider {
        private final EditorsContentProvider contentProvider;

        // TODO WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider(); ???

        public EditorsLabelProvider(EditorsContentProvider contentProvider) {
            this.contentProvider = contentProvider;
        }

        @Override
        public Image getImage(Object element) {
            if (element instanceof IEditorReference) {
                IEditorReference reference = (IEditorReference) element;
                return reference.getTitleImage();
            }
            return super.getImage(element);
        }

        @Override
        public String getText(Object element) {
            if (element instanceof IEditorReference) {
                IEditorReference ref = (IEditorReference) element;
                if(contentProvider.isDuplicated(ref)) {
                    return getFullTitle(ref);
                }
                return ref.getTitle();
            }
            return super.getText(element);
        }

        String getFullTitle(IEditorReference ref) {
            String tip = ref.getTitleToolTip();
            String title = ref.getTitle();
            if(tip != null && tip.endsWith(title)) {
                tip = tip.substring(0, tip.lastIndexOf(title));
            }
            return title + " - " + tip;
        }
    }

    /**
     * This provider must not be re-used for more then one execution, as it creates cache
     * of editors
     * @author Andrey
     */
    public static final class EditorsContentProvider implements IStructuredContentProvider {

        private final AbstractEditor myEditor;
        private final IEditorReference[] references;
        private static boolean editorsInitialized;
        private static boolean jobStarted;

        public EditorsContentProvider(AbstractEditor myEditor, ContentWrapper selectedContent) {
            super();
            this.myEditor = myEditor;
            IEditorReference[] editorReferences = EclipseUtils.getEditors();
            boolean initEditor;
            if(!editorsInitialized && editorReferences.length > 3){
                // spawn a job to init editors for the next time menu is shown
                initEditors(editorReferences);
                initEditor = false;
            } else {
                initEditor = true;
            }
            List<IEditorReference> refs = new ArrayList<IEditorReference>();
            for (int i = 0; i < editorReferences.length; i++) {
                IEditorReference reference = editorReferences[i];
                if(COMPARE_EDITOR_ID.equals(reference.getId())){
                    continue;
                }

                // if we are called from navigator (not from editor menu)
                if(myEditor == null || myEditor.getPart() == null){
                    // if navigator context menu has no valid selection
                    if(selectedContent == null) {
                        continue;
                    }
                    AbstractEditor abstractEditor = new AbstractEditor(reference.getEditor(initEditor));

                    File file = selectedContent.getFile();
                    File anotherFile = abstractEditor.getFile();
                    if(file != null && file.equals(anotherFile)){
                        // same file as selection
                        continue;
                    }
                    refs.add(editorReferences[i]);
                    continue;
                }

                // here we was called from the editor menu
                AbstractEditor abstractEditor = new AbstractEditor(reference.getEditor(initEditor));
                if (abstractEditor.getPart() == null || sameEditor(abstractEditor)) {
                    continue;
                }
                refs.add(editorReferences[i]);
            }
            references = refs.toArray(new IEditorReference[refs.size()]);
        }

        private synchronized static void initEditors(final IEditorReference[] editorReferences) {
            Job initJob = new UIJob("Initializing editor parts"){
                @Override
                public IStatus runInUIThread(IProgressMonitor monitor) {
                    monitor.beginTask("Initializing editor parts", editorReferences.length);
                    for (int i = 0; i < editorReferences.length; i++) {
                        if(monitor.isCanceled()){
                            jobStarted = false;
                            return Status.CANCEL_STATUS;
                        }
                        editorReferences[i].getEditor(true);
                        monitor.internalWorked(1);
                    }
                    editorsInitialized = true;
                    monitor.done();
                    return Status.OK_STATUS;
                }
            };
            initJob.setPriority(Job.DECORATE);
            initJob.setSystem(true);
            if(!jobStarted) {
                jobStarted = true;
                initJob.schedule(5000);
            }
        }

        private boolean sameEditor(AbstractEditor abstractEditor) {
            if(myEditor == null){
                return false;
            }
            File myFile = myEditor.getFile();
            File another = abstractEditor.getFile();
            return myFile != null && myFile.equals(another);
        }

        private boolean similarEditor(IEditorReference reference) {
            if(myEditor == null) {
                return false;
            }
            IWorkbenchPart part = myEditor.getPart();
            if(part == null) {
                return false;
            }

            String title = part.getTitle();
            return title!= null && title.equals(reference.getTitle());
        }

        @Override
        public Object[] getElements(Object inputElement) {
            return references;
        }

        @Override
        public void dispose() {
            // ignore
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // ignore
        }

        public boolean isDuplicated(IEditorReference ref) {
            String title = ref.getTitle();
            if(title == null || title.length() == 0) {
                return false;
            }
            IEditorReference[] elements = (IEditorReference[]) getElements(null);
            boolean seen = false;
            for (int i = 0; i < elements.length; i++) {
                if(title.equals(elements[i].getTitle())){
                    if(seen || similarEditor(elements[i])) {
                        return true;
                    }
                    seen = true;
                }
            }
            return false;
        }
    }

}
