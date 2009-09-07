/**
 *
 */
package de.loskutov.anyedit.jdt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.workingsets.WorkingSetModel;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.ui.wizards.IWSAction;

/**
 * Thic class is a dirty way to activate imported working sets into the Package Explorer
 *
 * @author Andrei
 */
public class SelectWorkingSetsAction extends Action implements IWSAction {

    private IWorkingSet[] workingSets;

    public SelectWorkingSetsAction() {
        super();
    }

    public void run() {
        if (workingSets == null || workingSets.length == 0) {
            return;
        }
        try {
            IViewPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                    .getActivePage().showView(JavaUI.ID_PACKAGES);
            if (!(part instanceof PackageExplorerPart)) {
                return;
            }
            PackageExplorerPart viewPart = (PackageExplorerPart) part;
            // Eclipse 3.3: ViewActionGroup.SHOW_WORKING_SETS, value is 2
            // XXX Eclipse 3.4: PackageExplorerPart.WORKING_SETS_AS_ROOTS
            int showWS = 2;
            if (viewPart.getRootMode() != showWS) {
                viewPart.rootModeChanged(showWS);
            }
            WorkingSetModel workingSetModel = viewPart.getWorkingSetModel();
            IWorkingSet[] active = workingSetModel.getActiveWorkingSets();
            List all = new ArrayList();
            for (int i = 0; i < active.length; i++) {
                all.add(active[i]);
            }
            IWorkingSet[] existing = workingSetModel.getAllWorkingSets();
            for (int i = 0; i < workingSets.length; i++) {
                IWorkingSet set = workingSets[i];
                set = lookupName(existing, set);
                if (set != null && !all.contains(set)) {
                    all.add(set);
                }
            }
            if (all.size() > 0) {
                workingSetModel.setActiveWorkingSets((IWorkingSet[]) all
                        .toArray(new IWorkingSet[0]));
                TreeViewer viewer = viewPart.getTreeViewer();
                viewer.getControl().setRedraw(false);
                viewer.refresh();
                viewer.getControl().setRedraw(true);
            }
        } catch (PartInitException e) {
            AnyEditToolsPlugin.logError(null, e);
        }
    }

    private IWorkingSet lookupName(IWorkingSet[] all, IWorkingSet set) {
        String name = set.getName();
        if (name == null) {
            return null;
        }
        for (int i = 0; i < all.length; i++) {
            if (name.equals(all[i].getName())) {
                return all[i];
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.loskutov.anyedit.ui.wizards.IWSAction#setWorkingSets(java.util.List)
     */
    public void setWorkingSets(List sets) {
        this.workingSets = (IWorkingSet[]) sets.toArray(new IWorkingSet[0]);
    }
}
