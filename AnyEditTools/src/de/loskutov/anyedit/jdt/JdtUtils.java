/*******************************************************************************
 * Copyright (c) 2005 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.jdt;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;

/**
 * @author Andrei
 *
 */
public final class JdtUtils {

    /**
     *
     */
    private JdtUtils() {
        super();
    }

    /**
     * @param typeName
     * @throws OperationCanceledException
     *             if user doesnt select founded types
     */
    public static int searchAndOpenType(String typeName)
            throws OperationCanceledException {
        IJavaSearchScope fScope = SearchEngine.createWorkspaceScope();
        IType type = null;
        try {
            IType[] types = getTypeForName(typeName, fScope, null);
            if (types != null && types.length > 0) {
                if (types[0] != null) {
                    type = types[0];
                } else {
                    return 2;
                }
            }
        } catch (JavaModelException e) {
            AnyEditToolsPlugin.logError(null, e);
        }
        if (type == null) {
            return 0;
        }
        try {
            IEditorPart part = JavaUI.openInEditor(type);
            JavaUI.revealInEditor(part, (IJavaElement)type);
        } catch (CoreException x) {
            AnyEditToolsPlugin.errorDialog("'Open type' operation failed", x);
            return 0;
        }
        return 1;
    }

    /**
     * Finds a type by the simple name.
     * see org.eclipse.jdt.internal.corext.codemanipulation.AddImportsOperation
     * @return null, if no types was found, empty array if more then one type was found,
     * or only one element, if single match exists
     */
    private static IType[] getTypeForName(String simpleTypeName,
            final IJavaSearchScope searchScope, IProgressMonitor monitor)
            throws JavaModelException {
        final IType[] result = new IType[1];
        final TypeFactory fFactory = new TypeFactory();
        TypeNameRequestor requestor = new TypeNameRequestor() {
            boolean done;
            boolean found;
            public void acceptType(int modifiers, char[] packageName,
                    char[] simpleTypeName1, char[][] enclosingTypeNames, String path) {
                if(done){
                    return;
                }
                IType type = fFactory.create(packageName, simpleTypeName1,
                        enclosingTypeNames, modifiers, path, searchScope, found);
                if (type != null) {
                    if(found){
                        // more then one match => we do not need anymore
                        done = true;
                        result[0] = null;
                    } else {
                        found = true;
                        result[0] = type;
                    }
                }
            }
        };
        new SearchEngine().searchAllTypeNames(null, SearchPattern.R_EXACT_MATCH, simpleTypeName.toCharArray(),
                SearchPattern.R_EXACT_MATCH, IJavaSearchConstants.TYPE, searchScope,
                requestor, IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, monitor);

        return result;
    }

    /**
     * Returns true if the given project is accessible and it has a java nature, otherwise
     * false.
     *
     * @param project
     *            IProject
     * @return boolean
     */
    private static boolean hasJavaNature(IProject project) {
        try {
            return project.hasNature(JavaCore.NATURE_ID);
        } catch (CoreException e) {
            // project does not exist or is not open
        }
        return false;
    }

    private static IJavaProject getJavaProject(IProject project) {
        if (project == null) {
            return null;
        }
        return JavaCore.create(project);
    }

    public static int getTabWidth(IFile file) {
        int tabWidth = -1;

        IProject project = file.getProject();
        tabWidth = getJavaProjectTabWidth(tabWidth, project);

        if (tabWidth < 0) {
            tabWidth = IAnyEditConstants.DEFAULT_TAB_WIDTH;
        }
        return tabWidth;
    }

    private static int getJavaProjectTabWidth(int tabWidth, IProject project) {
        IJavaProject javaProject = null;
        if (project != null && hasJavaNature(project)) {
            javaProject = getJavaProject(project);
        }
        String tabOption;
        if (javaProject == null || JavaCore.getJavaCore() == null) {
            tabOption = JavaCore
                    .getOption(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE);
        } else {
            tabOption = javaProject.getOption(
                    DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, true);
        }
        if (tabOption != null) {
            try {
                tabWidth = Integer.parseInt(tabOption);
            } catch (NumberFormatException e) {
                // TODO: handle exception
            }
        }
        return tabWidth;
    }

    /**
     * @param currentInput class file input
     * @return may return null
     */
    public static IProject getProjectForClass(IEditorInput currentInput) {
        IProject project = null;
        Object adapter = currentInput.getAdapter(IClassFile.class);
        if (adapter instanceof IClassFile) {
            IClassFile classFile = (IClassFile) adapter;
            IJavaProject javaProject = classFile.getJavaProject();
            if (javaProject != null) {
                project = javaProject.getProject();
            }
        }
        return project;
    }

    public static IProject getProject(ILaunchConfiguration config) {
        IProject project = null;
        // no external config file...
        // try to find java project, if exist for this configuration
        IJavaProject javaProject = null;
        try {
            javaProject = JavaRuntime.getJavaProject(config);
        } catch (CoreException e) {
            AnyEditToolsPlugin.logError(null, e);
        }
        if (javaProject != null) {
            project = javaProject.getProject();
        }
        return project;
    }

}
