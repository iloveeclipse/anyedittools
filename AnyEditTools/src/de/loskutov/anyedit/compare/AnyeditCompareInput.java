/*******************************************************************************
 * Copyright (c) 2009 Andrey Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.compare;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.progress.UIJob;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.util.EclipseUtils;

/**
 * @author Andrey
 */
/*implements IContentChangeNotifier*/
public class AnyeditCompareInput extends CompareEditorInput  {


    static class ExclusiveJobRule implements ISchedulingRule {

        private final Object id;

        public ExclusiveJobRule(Object id) {
            this.id = id;
        }

        @Override
        public boolean contains(ISchedulingRule rule) {
            return rule instanceof ExclusiveJobRule && ((ExclusiveJobRule) rule).id == id;
        }

        @Override
        public boolean isConflicting(ISchedulingRule rule) {
            return contains(rule);
        }

    }


    // org.eclipse.compare.internal.CompareEditor.CONFIRM_SAVE_PROPERTY;
    private static final String CONFIRM_SAVE_PROPERTY = "org.eclipse.compare.internal.CONFIRM_SAVE_PROPERTY";
    private StreamContent left;
    private StreamContent right;
    private Object differences;
    /**
     * allow "no diff" result to keep the editor open
     */
    private boolean createNoDiffNode;

    public AnyeditCompareInput(StreamContent left, StreamContent right) {
        super(new CompareConfiguration());
        this.left = left;
        this.right = right;
        getCompareConfiguration().setProperty(CONFIRM_SAVE_PROPERTY, Boolean.FALSE);
    }


    @Override
    public Object getAdapter(Class adapter) {
        if(IFile.class == adapter) {
            Object object = EclipseUtils.getIFile(left, false);
            if(object != null) {
                return object;
            }
            return EclipseUtils.getIFile(right, false);
        }
        return super.getAdapter(adapter);
    }

    @Override
    protected Object prepareInput(IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException {
        if (right == null || left == null) {
            return null;
        }
        try {
            initTitle();
            left.init(this);
            right.init(this);
            Differencer differencer = new Differencer();
            String message = "Comparing " + left.getName() + " with " + right.getName();
            monitor.beginTask(message, 30);
            IProgressMonitor sub = new SubProgressMonitor(monitor, 10);
            try {
                sub.beginTask(message, 100);

                differences = differencer.findDifferences(false, sub, null, null, left, right);
                if(differences == null && createNoDiffNode) {
                    differences = new DiffNode(left, right);
                }
                return differences;
            } finally {
                sub.done();
            }
        } catch (OperationCanceledException e) {
            left.dispose();
            right.dispose();
            throw new InterruptedException(e.getMessage());
        } finally {
            monitor.done();
        }
    }

    @Override
    public void saveChanges(IProgressMonitor monitor) throws CoreException {
        super.saveChanges(monitor);
        if (differences instanceof DiffNode) {
            try {
                boolean result = commit(monitor, (DiffNode) differences);
                // let the UI re-compare here on changed inputs
                if(result){
                    reuseEditor();
                }
            } finally {
                setDirty(false);
            }
        }
    }

    @Override
    public Object getCompareResult() {
        Object compareResult = super.getCompareResult();
        if(compareResult == null){
            // dispose???
        }
        return compareResult;
    }

    void reuseEditor() {
        UIJob job = new UIJob("Updating differences"){
            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                if(monitor.isCanceled() || left.isDisposed() || right.isDisposed()){
                    return Status.CANCEL_STATUS;
                }

                // This causes too much flicker:
                //                AnyeditCompareInput input = new AnyeditCompareInput(left.recreate(), right
                //                        .recreate());
                //                if(monitor.isCanceled()){
                //                    input.internalDispose();
                //                    return Status.CANCEL_STATUS;
                //                }
                //                CompareUI.reuseCompareEditor(input, (IReusableEditor) getWorkbenchPart());

                AnyeditCompareInput input = AnyeditCompareInput.this;
                // allow "no diff" result to keep the editor open
                createNoDiffNode = true;
                try {
                    StreamContent old_left = left;
                    left = old_left.recreate();
                    old_left.dispose();
                    StreamContent old_right = right;
                    right = old_right.recreate();
                    old_right.dispose();


                    // calls prepareInput(monitor);
                    input.run(monitor);
                    if(differences != null){
                        CompareViewerSwitchingPane pane = getInputPane();
                        if(pane != null){
                            Viewer viewer = pane.getViewer();
                            if (viewer instanceof TextMergeViewer) {
                                viewer.setInput(differences);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    // ignore, we are interrupted
                    return Status.CANCEL_STATUS;
                } catch (InvocationTargetException e) {
                    AnyEditToolsPlugin.errorDialog("Error during diff of " + getName(), e);
                    return Status.CANCEL_STATUS;
                } finally {
                    createNoDiffNode = false;
                }
                return Status.OK_STATUS;
            }
            @Override
            public boolean belongsTo(Object family) {
                return AnyeditCompareInput.this == family;
            }

        };
        job.setPriority(Job.SHORT);
        job.setUser(true);
        job.setRule(new ExclusiveJobRule(this));
        Job[] jobs = Job.getJobManager().find(this);
        if(jobs.length > 0){
            for (int i = 0; i < jobs.length; i++) {
                jobs[i].cancel();
            }
        }
        jobs = Job.getJobManager().find(this);
        if(jobs.length > 0) {
            job.schedule(1000);
        } else {
            job.schedule(500);
        }
    }

    public CompareViewerSwitchingPane getInputPane() {
        try {
            Field field = CompareEditorInput.class.getDeclaredField("fContentInputPane");
            field.setAccessible(true);
            Object object = field.get(this);
            if(object instanceof CompareViewerSwitchingPane) {
                return (CompareViewerSwitchingPane) object;
            }
        } catch (Throwable e) {
            // ignore
        }
        return null;
    }

    private boolean commit(IProgressMonitor monitor, ICompareInput diffNode) {
        boolean okLeft = commitNode(monitor, diffNode.getLeft());
        boolean okRight = commitNode(monitor, diffNode.getRight());
        return okLeft || okRight;
    }

    private boolean commitNode(IProgressMonitor monitor, ITypedElement element) {
        if(element instanceof StreamContent) {
            StreamContent content = (StreamContent) element;
            if(content.isDirty()) {
                try {
                    return content.commitChanges(monitor);
                } catch (CoreException e) {
                    AnyEditToolsPlugin.errorDialog("Error during save of " + content.getName(), e);
                }
            }
        }
        return false;
    }

    private void initTitle() {
        CompareConfiguration cc = getCompareConfiguration();
        String nameLeft = left.getName();
        String nameRight = right.getName();
        if(nameLeft.equals(nameRight)){
            nameLeft = left.getFullName();
            nameRight = right.getFullName();
        }

        cc.setLeftLabel(nameLeft);
        cc.setLeftImage(left.getImage());

        cc.setRightLabel(nameRight);
        cc.setRightImage(right.getImage());

        cc.setLeftEditable(true);
        cc.setRightEditable(true);
        setTitle("Compare (" + nameLeft + " - " + nameRight + ")");
    }

    @Override
    protected void handleDispose() {
        internalDispose();
        super.handleDispose();
    }


    public void internalDispose() {
        left.dispose();
        right.dispose();
        differences = null;
    }

}
