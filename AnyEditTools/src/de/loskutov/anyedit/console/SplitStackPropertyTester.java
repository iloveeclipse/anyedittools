package de.loskutov.anyedit.console;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleView;

public class SplitStackPropertyTester extends PropertyTester {

    @Override
    public boolean test(Object receiver, String property, Object[] args,
            Object expectedValue) {
        if ("isJStackConsole".equals(property)
                && receiver instanceof IWorkbenchPart) {
            return isJStackConsole((IWorkbenchPart) receiver, args,
                    expectedValue);
        }
        return false;
    }

    private boolean isJStackConsole(IWorkbenchPart part, Object[] args,
            Object expectedValue) {
        if (!(part instanceof IConsoleView)) {
            return false;
        }
        IConsoleView consoleView = (IConsoleView) part;
        IConsole console = consoleView.getConsole();
        // no dependency to JDT (option)
        return console != null && console.getClass().getSimpleName().equals("JavaStackTraceConsole");
    }

}
