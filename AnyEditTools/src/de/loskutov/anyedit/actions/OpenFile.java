/*****************************************************************************************
 * Copyright (c) 2009 Andrey Loskutov. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributor: Andrey Loskutov -
 * initial API and implementation
 ****************************************************************************************/

package de.loskutov.anyedit.actions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

import de.loskutov.anyedit.AnyEditToolsPlugin;
import de.loskutov.anyedit.IOpenEditorParticipant;
import de.loskutov.anyedit.ui.editor.AbstractEditor;

public class OpenFile extends AbstractOpenAction {

    static WeakReference<List<IOpenEditorParticipant>> weakRef = new WeakReference<List<IOpenEditorParticipant>>(null);

    public OpenFile() {
        super();
    }

    @Override
    protected void handleAction(IDocument doc, ISelectionProvider selectionProvider,
            IEditorInput currentInput) {
        List<IOpenEditorParticipant> participants = getParticipants();
        IFile file;
        try {
            file = guessFile(participants, doc, selectionProvider, currentInput);
        } catch (OperationCanceledException e) {
            return;
        }

        IEditorPart editorPart = openEditor(participants, doc, selectionProvider,
                currentInput, file);

        if (editorPart != null) {
            goToLine(participants, doc, selectionProvider, editorPart);
        }
    }

    private IFile guessFile(List<IOpenEditorParticipant> participants, IDocument doc,
            ISelectionProvider selectionProvider, IEditorInput currentInput) {
        for (int i = 0; i < participants.size(); i++) {
            IOpenEditorParticipant participant = participants.get(i);
            IFile file;
            try {
                file = participant.guessFile(doc, selectionProvider, currentInput,
                        getViewPart());
            } catch (OperationCanceledException e) {
                // forward
                throw e;
            } catch (Throwable e) {
                AnyEditToolsPlugin.logError("Error with '" + participant
                        + "' in guessFile() call", e);
                continue;
            }
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    private IEditorPart openEditor(List<IOpenEditorParticipant> participants,
            IDocument doc, ISelectionProvider selectionProvider,
            IEditorInput currentInput, IFile file) {

        for (int i = 0; i < participants.size(); i++) {
            IOpenEditorParticipant participant = participants.get(i);
            IEditorPart part = null;
            try {
                part = participant.openEditor(doc, selectionProvider, currentInput, file);
            } catch (OperationCanceledException e) {
                return null;
            } catch (Throwable e) {
                AnyEditToolsPlugin.logError("Error with '" + participant
                        + "' in openEditor() call", e);
                continue;
            }
            if (part != null) {
                return part;
            }
        }
        return null;
    }

    private void goToLine(List<IOpenEditorParticipant> participants, IDocument doc,
            ISelectionProvider selectionProvider, IEditorPart editorPart) {

        for (int i = 0; i < participants.size(); i++) {
            IOpenEditorParticipant participant = participants.get(i);
            int line = -1;
            try {
                line = participant.guessLine(doc, selectionProvider, editorPart);
            } catch (OperationCanceledException e) {
                return;
            } catch (Throwable e) {
                AnyEditToolsPlugin.logError("Error with '" + participant
                        + "' in guessLine() call", e);
                continue;
            }
            if (line >= 0) {
                if (line >= 0) {
                    AbstractEditor aeditor = new AbstractEditor(editorPart);
                    aeditor.selectAndReveal(line);
                }
                return;
            }
        }
    }

    private static List<IOpenEditorParticipant> getParticipants() {
        List<IOpenEditorParticipant> participants = weakRef.get();
        if(participants == null){
            participants = new ArrayList<IOpenEditorParticipant>();
            weakRef = new WeakReference<List<IOpenEditorParticipant>>(participants);
        } else {
            return participants;
        }

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(AnyEditToolsPlugin.getId()
                + ".openEditorParticipants");
        if (point == null) {
            return participants;
        }
        IExtension[] extensions = point.getExtensions();
        for (int i = 0; i < extensions.length; i++) {
            IConfigurationElement[] elements = extensions[i].getConfigurationElements();
            for (int j = 0; j < elements.length; j++) {
                Object object;
                try {
                    object = elements[j].createExecutableExtension("class");
                } catch (Throwable e) {
                    AnyEditToolsPlugin.logError("Bad 'IOpenEditorParticipant': "
                            + elements[j].getAttribute("class"), e);
                    continue;
                }
                if (object instanceof IOpenEditorParticipant) {
                    participants.add((IOpenEditorParticipant) object);
                }
            }
        }
        if (participants.size() <= 1) {
            return participants;
        }
        Collections.sort(participants, new Comparator<IOpenEditorParticipant>() {
            @Override
            public int compare(IOpenEditorParticipant o1, IOpenEditorParticipant o2) {
                if (o1 == null || o2 == null) {
                    return 0;
                }
                // inverted order
                return getPrio(o2).compareTo(getPrio(o1));
            }
        });
        return participants;
    }

    private static Integer getPrio(IOpenEditorParticipant participant) {
        int prio = participant.getPriority();
        prio = prio > IOpenEditorParticipant.PRIO_HIGH ? IOpenEditorParticipant.PRIO_HIGH
                : prio < IOpenEditorParticipant.PRIO_LOW ? IOpenEditorParticipant.PRIO_LOW
                        : prio;
        return Integer.valueOf(prio);
    }

}
