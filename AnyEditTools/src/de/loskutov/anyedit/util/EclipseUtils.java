/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/
package de.loskutov.anyedit.util;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.resources.Container;
import org.eclipse.core.internal.resources.ICoreConstants;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.internal.ide.dialogs.OpenResourceDialog;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IAnyEditConstants;
import de.loskutov.anyedit.jdt.JdtUtils;
import de.loskutov.anyedit.ui.preferences.CombinedPreferences;

/**
 * @author Andrei
 */
public final class EclipseUtils {

    private static Boolean isWindows;

    private EclipseUtils() {
        super();
    }

    /**
     * @param currentInput  may be null
     * @return project, may be null
     */
    public static IProject getProject(IEditorInput currentInput) {
        if (currentInput == null) {
            return null;
        }
        IFile file = (IFile) currentInput.getAdapter(IFile.class);
        if (file != null) {
            return file.getProject();
        }
        IProject project = null;
        if (currentInput instanceof IFileEditorInput) {
            IFileEditorInput fileInput = (IFileEditorInput) currentInput;
            IFile currentFile = fileInput.getFile();
            project = currentFile.getProject();
        } else if (isJavaInput(currentInput)) {
            // it must be a class because java source *files* are IFileEditorInput's
            project = JdtUtils.getProjectForClass(currentInput);
        }
        return project;
    }

    public static IProject getProject(IWorkbenchPart viewPart) {
        // started from console?
        if (!(viewPart instanceof IConsoleView)) {
            return null;
        }
        IConsoleView consoleView = (IConsoleView) viewPart;
        if (!(consoleView.getConsole() instanceof IConsole)) {
            return null;
        }
        IConsole pConsole = (IConsole) consoleView.getConsole();
        IProcess process = pConsole.getProcess();
        if (process == null) {
            return null;
        }

        ILaunch launch = process.getLaunch();
        if (launch == null) {
            return null;
        }

        ILaunchConfiguration config = launch.getLaunchConfiguration();
        if (config == null) {
            return null;
        }
        IProject project = null;
        IFile configFile = config.getFile();

        if (configFile != null) {
            // config file exist => should be inside project
            project = configFile.getProject();
        } else {
            // no external config file...
            // try to find java project, if exist for this configuration
            project = JdtUtils.getProject(config);
        }

        // config file doesn't exist or it is not java launch...
        // try to test for ant launch: ant launche's have labels with full path names
        if (project == null) {
            String cmdLine = process.getAttribute(IProcess.ATTR_CMDLINE);
            if (cmdLine != null && cmdLine.startsWith("ant")) {
                // XXX better path determination: parse commandline
                // search for somethings like -buildfile G:\work\JDepend4Eclipse\build.xml
                String label = process.getLabel();
                IPath ipath = new Path(label);
                IFile[] files = ResourcesPlugin.getWorkspace().getRoot()
                        .findFilesForLocation(ipath);
                if (files != null && files.length == 1) {
                    project = files[0].getProject();
                }
            }
        }

        return project;
    }

    public static IFile getResource(IProject project, IEditorInput currentInput,
            String selectedText) throws OperationCanceledException {

        IFile resource = null;

        /*
         * fast path to absolute files in windows
         */
        if (isWindows() && selectedText.length() > 3 && selectedText.indexOf(':') == 1) {
            resource = findAbsoluteFile(selectedText);
            if (resource != null) {
                return resource;
            }
        }

        String currentPath = null;
        if (currentInput instanceof IFileEditorInput) {
            currentPath = getRelativePath((IFileEditorInput) currentInput);
            if (currentPath != null && currentPath.length() == 0) {
                currentPath = null;
            }
        }

        /*
         * search througth current project and related projects
         */
        List checkedProjects = new ArrayList();
        List resultList = new ArrayList();
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        boolean useWorkspaceScope = AnyEditToolsPlugin.getDefault().getPreferenceStore()
                .getBoolean(IAnyEditConstants.USE_WORKSPACE_SCOPE_FOR_SEARCH);
        if (project != null) {
            resource = findInProject(currentPath, project, selectedText, resultList);
            if (resource != null) {
                return resource;
            }
            if (!resultList.isEmpty()) {
                return queryFile(selectedText, project);
            }
            checkedProjects.add(project);

            IProject[] projects = null;

            try {
                projects = project.getReferencedProjects();
            } catch (CoreException e) {
                AnyEditToolsPlugin.logError("File not known: " + selectedText, e);
            }

            if (projects != null) {
                resource = findInProjects(projects, currentPath, selectedText,
                        checkedProjects, resultList);
                if (resource != null) {
                    return resource;
                }
                if (!resultList.isEmpty()) {
                    if (useWorkspaceScope) {
                        return queryFile(selectedText, workspaceRoot);
                    }
                    return queryFile(selectedText, new DummyContainer(projects));
                }
            }

            projects = project.getReferencingProjects();
            resource = findInProjects(projects, currentPath, selectedText,
                    checkedProjects, resultList);
            if (resource != null) {
                return resource;
            }
            if (!resultList.isEmpty()) {
                if (useWorkspaceScope) {
                    return queryFile(selectedText, workspaceRoot);
                }
                return queryFile(selectedText, new DummyContainer(projects));
            }
        }

        /*
         * search througth all remaining projects
         */

        IProject[] projects = workspaceRoot.getProjects();
        for (int i = 0; i < projects.length; i++) {
            if (!checkedProjects.contains(projects[i])) {
                resource = findInProject(currentPath, projects[i], selectedText,
                        resultList);
            }
            if (resource != null) {
                if (!resultList.contains(resource)) {
                    resultList.add(resource);
                }
            }
        }

        if (!resultList.isEmpty()) {
            if (resultList.size() == 1) {
                return (IFile) resultList.get(0);
            }
            return queryFile(selectedText, workspaceRoot);
        }

        /*
         * is selectedText contains absolute path???
         */
        return findAbsoluteFile(selectedText);
    }

    private static IFile findAbsoluteFile(String selectedText)
            throws OperationCanceledException {
        IPath iPath = new Path(selectedText);
        File file = iPath.toFile();
        if (!file.isFile() || !file.canRead()) {
            return null;
        }
        return getIFile(iPath);
    }

    /**
     * @param iPath non null
     * @return may return null or external file, which location in workspace is null. For
     *         files located inside the root of the file system, always returns null.
     */
    public static IFile getIFile(IPath iPath) throws OperationCanceledException {
        IFile resource = getWorkspaceFile(iPath.toFile());
        if (resource != null) {
            return resource;
        }
        /*
         * this: ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(iPath);
         * doesnt' work for external files
         */
        // TODO can we just use getLocalFile(URI) instead???
        Workspace workspace = (Workspace) ResourcesPlugin.getWorkspace();
        if (iPath.segmentCount() >= ICoreConstants.MINIMUM_FILE_SEGMENT_LENGTH) {
            return (IFile) workspace.newResource(iPath, IResource.FILE);
        }
        return null;
    }

    private static IFile findInProjects(IProject[] projects, String currentPath,
            String selectedText, List checkedProjects, List resultList)
            throws OperationCanceledException {
        for (int i = 0; i < projects.length; i++) {
            IFile resource = findInProject(currentPath, projects[i], selectedText,
                    resultList);
            if (resource != null) {
                if (!resultList.contains(resource)) {
                    resultList.add(resource);
                }
            }
            checkedProjects.add(projects[i]);
        }
        if (resultList.size() == 1 && resultList.get(0) instanceof IFile) {
            return (IFile) resultList.get(0);
        }
        return null;
    }

    private static IFile findInProject(String currentPath, IContainer project,
            String selectedText, List resultList) throws OperationCanceledException {
        if (project == null || !project.isAccessible()) {
            return null;
        }

        /*
         * try to find path relative to current document
         */
        if (currentPath != null) {
            IResource resource = project.findMember(currentPath + '/' + selectedText);
            if (resource instanceof IFile) {
                return (IFile) resource;
            }
        }

        // if not found relative to current document path, try to find
        // same path in entire project
        IResource resource = project.findMember(selectedText);
        if (resource instanceof IFile) {
            return (IFile) resource;
        }

        searchForPathFragment(project, selectedText, resultList, false);
        if (!resultList.isEmpty()) {
            if (resultList.size() == 1) {
                return (IFile) resultList.get(0);
            }
        }
        return null;
    }

    /**
     * @return relative path to input file
     */
    private static String getRelativePath(IFileEditorInput currentInput) {
        IFile currentFile = currentInput.getFile();
        // remove file name
        IPath currentPath = currentFile.getFullPath().removeLastSegments(1);
        // remove project name
        currentPath = currentPath.removeFirstSegments(1);
        return currentPath.toString();
    }

    public final static IFile getWorkspaceFile() {
        try {
            IFile file = queryFile(null, ResourcesPlugin.getWorkspace().getRoot());
            return file;
        } catch (OperationCanceledException e) {
            return null;
        }
    }

    private static IFile getWorkspaceFile(File file) throws OperationCanceledException {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IPath location = new Path(file.getAbsolutePath()); // Path.fromOSString();
        IFile[] files = workspace.getRoot().findFilesForLocation(location);
        List filesList = filterNonExistentFiles(files);
        if (filesList == null || filesList.size() == 0) {
            return null;
        }
        if (filesList.size() == 1) {
            return (IFile) filesList.get(0);
        }
        return queryFile(file.getName(), workspace.getRoot());
    }

    /**
     * The caller has to disconnect the buffer after buffer is used
     * @return file buffer for given file, or null, if file is external
     */
    public static ITextFileBuffer getBuffer(IFile file) {
        IPath location = file.getLocation();
        if (location == null) {
            return null;
        }
        ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
        ITextFileBuffer fileBuffer = null;
        try {
            bufferManager.connect(file.getFullPath(), LocationKind.IFILE,
                    new NullProgressMonitor());
            fileBuffer = bufferManager
                    .getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
        } catch (CoreException e) {
            AnyEditToolsPlugin.logError(null, e);
        }
        return fileBuffer;
    }

    public static URI getURI(IEditorInput input){
        if(input == null){
            return null;
        }
        if(input instanceof IURIEditorInput) {
            IURIEditorInput editorInput = (IURIEditorInput) input;
            return editorInput.getURI();
        }
        return null;
    }

    /**
     * @return may return null
     */
    public static File getLocalFile(URI uri) {
        if (uri != null) {
            try {
                IFileStore store = EFS.getStore(uri);
                if(store != null) {
                    return store.toLocalFile(EFS.NONE,
                            new NullProgressMonitor());
                }
            } catch (CoreException e) {
                // ignore
            }
        }
        return null;
    }

    /**
     * Looks into the file returns the newline characters used in the file
     * @return given string with converted newlines, or unchanged string if something goes wrong
     */
    public static String getNewLineFromFile(IFile file) {
        ITextFileBuffer buffer = EclipseUtils.getBuffer(file);
        if(buffer != null) {
            try {
                IDocument document = buffer.getDocument();
                return getNewLineFromDocument(document);
            } finally {
                disconnectBuffer(buffer);
            }
        }
        return null;
    }

    public static String getNewLineFromDocument(IDocument document) {
        try {
            return document.getLineDelimiter(0);
        } catch (BadLocationException e) {
            AnyEditToolsPlugin.logError(null, e);
        }
        return null;
    }

    public static void disconnectBuffer(ITextFileBuffer buffer) {
        if(buffer == null) {
            return;
        }
        ITextFileBufferManager bufferManager = FileBuffers
                .getTextFileBufferManager();
        try {
            bufferManager.disconnect(buffer.getLocation(), LocationKind.IFILE,
                    new NullProgressMonitor());
        } catch (CoreException e) {
            AnyEditToolsPlugin.logError(null, e);
        }
    }

    private static List filterNonExistentFiles(IFile[] files) {
        if (files == null) {
            return null;
        }

        int length = files.length;
        ArrayList existentFiles = new ArrayList(length);
        for (int i = 0; i < length; i++) {
            if (files[i].exists()) {
                existentFiles.add(files[i]);
            }
        }
        return existentFiles;
    }

    public static int getCaretPosition(ISelectionProvider selectionProvider) {
        ISelection selection = selectionProvider.getSelection();
        int caretPos = -1;
        if (selection instanceof ITextSelection) {
            ITextSelection textSelection = (ITextSelection) selection;
            caretPos = textSelection.getOffset();
        }
        return caretPos;
    }

    public static String getSelectedText(ISelectionProvider selectionProvider) {
        if (selectionProvider == null) {
            return null;
        }
        ISelection selection = selectionProvider.getSelection();
        String text = null;
        if (selection instanceof ITextSelection) {
            ITextSelection textSelection = (ITextSelection) selection;
            text = textSelection.getText();
        }
        return text;
    }

    public static String getClipboardContent() {
        String contents;
        Clipboard cb = null;
        try {
            cb = new Clipboard(Display.getDefault());
            contents = (String) cb.getContents(TextTransfer.getInstance());
        } finally {
            if (cb != null) {
                cb.dispose();
            }
        }
        return contents;
    }

    private static boolean isJavaInput(IEditorInput editorInput) {
        if (editorInput == null) {
            return false;
        }
        return isJavaFile(editorInput.getName());
    }

    private static boolean isJavaFile(String fileName) {
        return fileName != null && fileName.endsWith("java");
    }

    public static boolean isJavaInput(IFile file) {
        if (file == null) {
            return false;
        }
        return isJavaFile(file.getName());
    }

    /**
     * @return false, if file filters are not applicable to the editor input. Return true,
     *         if at least one of filters matches the file name.
     */
    public static boolean matchFilter(IEditorPart part) {
        IEditorInput input = part.getEditorInput();
        if (input != null) {
            String name = input.getName();
            String filterPerf = getCombinedPreferences(input)
                    .getString(IAnyEditConstants.PREF_ACTIVE_FILTERS_LIST);
            String[] filters = parseList(filterPerf);
            if (matchFilter(name, filters)) {
                return true;
            }
        }
        return false;
    }

    public static CombinedPreferences getCombinedPreferences(IEditorInput input) {
        IScopeContext context = null;
        if (input != null) {
            IProject project = getProject(input);
            if (project != null) {
                context = new ProjectScope(project);
            }
        }
        CombinedPreferences combinedPreferences = new CombinedPreferences(context,
                AnyEditToolsPlugin.getDefault().getPreferenceStore());
        return combinedPreferences;
    }

    public static boolean matchFilter(String fileName, String[] filters) {
        String[] nameParts = splitFileName(fileName);
        for (int i = 0; i < filters.length; i++) {
            String[] filterParts = splitFileName(filters[i]);
            boolean firstPartOk = false;
            if (filterParts[0].equals(nameParts[0]) || "*".equals(filterParts[0])) {
                firstPartOk = true;
            }
            if (firstPartOk
                    && (filterParts[1].equals(nameParts[1]) || "*".equals(filterParts[1]))) {
                // match!
                return true;
            }
        }
        return false;
    }

    public static String[] splitFileName(String name) {
        String firstPart = name;
        String lastPart = name;
        int lastDotIdx = name.lastIndexOf('.');
        if (name.length() > 1 && lastDotIdx >= 0) {
            firstPart = name.substring(0, lastDotIdx);
            lastPart = name.substring(lastDotIdx + 1, name.length());
        }
        return new String[] { firstPart, lastPart };
    }

    /**
     * Parses the comma separated string into an array of strings
     */
    public static String[] parseList(String listString) {
        if (listString == null || listString.length() == 0) {
            return new String[] {};
        }
        List list = new ArrayList(10);
        StringTokenizer tokenizer = new StringTokenizer(listString, ",");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            list.add(token);
        }
        return (String[]) list.toArray(new String[list.size()]);
    }

    /**
     * Try to find given PathFragment somewhere in project resources tree. The matches
     * will be added to resultList
     */
    public static void searchForPathFragment(IContainer container, String pathFragment,
            List resultList, boolean searchInRoot) {

        // test directly under container root
        if (searchInRoot) {
            IResource resource = container.findMember(pathFragment);
            if (resource != null && resource instanceof IFile) {
                if (!resultList.contains(resource)) {
                    resultList.add(resource);
                }
            }
        }
        IResource[] resources = null;
        try {
            // here we could exclude derived resources
            boolean includeDerived = AnyEditToolsPlugin.getDefault().getPreferenceStore()
                    .getBoolean(IAnyEditConstants.INCLUDE_DERIVED_RESOURCES);
            if (includeDerived) {
                resources = container.members();
            } else {
                resources = container.members(IContainer.EXCLUDE_DERIVED);
            }
        } catch (CoreException e) {
            AnyEditToolsPlugin.logError(null, e);
            return;
        }
        if (resources == null) {
            return;
        }
        for (int i = 0; i < resources.length; i++) {
            if (resources[i].getType() == IResource.FOLDER) {
                // start recursion
                searchForPathFragment((IFolder) resources[i], pathFragment, resultList,
                        true);
            }
        }
    }

    /**
     * @param path may be null, see
     *   org.eclipse.ui.internal.ide.actions.OpenWorkspaceFileAction#queryFileResource()
     * @throws OperationCanceledException if user cancels the dialog
     */
    public static IFile queryFile(String path, IContainer input)
            throws OperationCanceledException {
        Shell parent = AnyEditToolsPlugin.getShell();

        MyOpenResourceDialog dialog = new MyOpenResourceDialog(parent, input,
                IResource.FILE, path);

        int resultCode = dialog.open();
        if (resultCode != IDialogConstants.OK_ID) {
            throw new OperationCanceledException();
        }

        Object[] result = dialog.getResult();
        if (result == null || result.length == 0 || result[0] instanceof IFile == false) {
            return null;
        }

        return (IFile) result[0];
    }

    public static boolean isWindows() {
        if (isWindows == null) {
            String property;
            try {
                property = System.getProperty("os.name");
                if (property != null) {
                    property = property.toLowerCase();
                    isWindows = Boolean.valueOf(property.indexOf("windows") >= 0);
                } else {
                    isWindows = Boolean.FALSE;
                }
            } catch (Exception e) {
                isWindows = Boolean.FALSE;
                AnyEditToolsPlugin.logError("System.getProperty(\"os.name\") fails:", e);
            }
        }
        return isWindows.booleanValue();
    }

    public static IEditorPart getActiveEditor() {
        IWorkbenchWindow window = AnyEditToolsPlugin.getDefault().getWorkbench()
                .getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                return page.getActiveEditor();
            }
        }
        return null;
    }

    public static IEditorReference[] getEditors() {
        IWorkbenchWindow window = AnyEditToolsPlugin.getDefault().getWorkbench()
        .getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                return page.getEditorReferences();
            }
        }
        return new IEditorReference[0];
    }

    static class DummyContainer extends Container {
        List resources;

        protected DummyContainer(IResource[] resources) {
            super(new Path(""), (Workspace) ResourcesPlugin.getWorkspace());
            this.resources = Arrays.asList(resources);
        }

        public int getType() {
            return 0;
        }

        public String getDefaultCharset(boolean checkImplicit) throws CoreException {
            return null;
        }

        public void accept(IResourceProxyVisitor visitor, int memberFlags)
                throws CoreException {
            for (int i = 0; i < resources.size(); i++) {
                ((IResource) resources.get(i)).accept(visitor, memberFlags);
            }
        }

    }

    static class MyOpenResourceDialog extends OpenResourceDialog {
        Text myPattern;

        private final String patternStr;

        public MyOpenResourceDialog(Shell parentShell, IContainer container,
                int typesMask, String patternStr) {
            super(parentShell, container, typesMask);
            if (patternStr != null) {
                patternStr = patternStr.trim();
                if (patternStr.length() != 0) {
                    patternStr = "*" + patternStr + "*";
                }
            }
            this.patternStr = patternStr;
        }

        /**
         * Hook for creating dialog area to set text on protected "Text" field from
         * dialog.
         */
        public void create() {
            super.create();
            if (patternStr != null && myPattern != null) {
                myPattern.setText(patternStr);
            }
        }

        /**
         * Hook for creating dialog area to fetch protected "Text" field from dialog.
         */
        protected Control createDialogArea(Composite parent) {
            Control c = super.createDialogArea(parent);
            Composite myDialogArea = (Composite) c;
            Control[] children = myDialogArea.getChildren();
            for (int i = 0; i < children.length; i++) {
                Control curr = children[i];
                if (curr instanceof Text) {
                    myPattern = (Text) curr;
                    break;
                }
            }
            return c;
        }
    }
}
