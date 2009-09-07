package de.loskutov.anyedit.actions.internal;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.IWorkbenchWindow;

public interface IDirtyWorkaround extends IAction {
    void copyStateAndDispose(IContributionItem oldItem);
    /**
     * Performs the 'convert spaces' action before the editor buffer is saved
     */
    void runBeforeSave();

    IWorkbenchWindow getWindow();
}
