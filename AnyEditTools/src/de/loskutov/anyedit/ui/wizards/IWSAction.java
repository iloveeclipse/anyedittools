/**
 *
 */
package de.loskutov.anyedit.ui.wizards;

import java.util.List;

/**
 * @author Andrei
 *
 */
public interface IWSAction {
    void setWorkingSets(List/* <IWorkingSet> */workingSets);
    void run();
}
