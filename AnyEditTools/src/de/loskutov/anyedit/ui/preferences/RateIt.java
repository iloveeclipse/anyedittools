/*******************************************************************************
 * Copyright (c) 2009 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.anyedit.ui.preferences;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import de.loskutov.anyedit.AnyEditToolsPlugin;

/**
 * @author Andrei
 */
public class RateIt {

    static void createTextArea(Composite defPanel) {
        Composite commonPanel = new Composite(defPanel, SWT.NONE);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginWidth = 0;
        commonPanel.setLayout(layout);
        commonPanel.setLayoutData(gridData);
        Link label = new Link(commonPanel, SWT.NONE);
        label.setFont(JFaceResources.getBannerFont());
        label.setText(" - <a>visit homepage</a>");
        label.setToolTipText("You need just a sense of humor!");
        label.addListener (SWT.Selection, new Listener () {
            public void handleEvent(Event event) {
                handleUrlClick("http://andrei.gmxhome.de/anyedit");
            }
        });

        label = new Link(commonPanel, SWT.NONE);
        label.setFont(JFaceResources.getBannerFont());
        label.setText(" - <a>report issue or feature request</a>");
        label.setToolTipText("You need a valid google account at google.com!");
        label.addListener (SWT.Selection, new Listener () {
            public void handleEvent(Event event) {
                handleUrlClick("http://code.google.com/a/eclipselabs.org/p/anyedittools/issues/list");
            }
        });

        label = new Link(commonPanel, SWT.NONE);
        label.setFont(JFaceResources.getBannerFont());
        label.setText(" - <a>add to your Ohloh software stack</a>");
        label.setToolTipText("You need a valid Ohloh account at ohloh.net!");
        label.addListener (SWT.Selection, new Listener () {
            public void handleEvent(Event event) {
                handleUrlClick("http://www.ohloh.net/p/anyedittools");
            }
        });

        label = new Link(commonPanel, SWT.NONE);
        label.setFont(JFaceResources.getBannerFont());
        label.setText(" - <a>add to your favorites at Eclipse MarketPlace</a>");
        label.setToolTipText("You need a valid bugzilla account at Eclipse.org!");
        label.addListener (SWT.Selection, new Listener () {
            public void handleEvent(Event event) {
                handleUrlClick("http://marketplace.eclipse.org/content/anyedit-tools");
            }
        });
    }

    private static void handleUrlClick(final String urlStr) {
        try {
            IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
            IWebBrowser externalBrowser = support.getExternalBrowser();
            if(externalBrowser != null){
                externalBrowser.openURL(new URL(urlStr));
            } else {
                IWebBrowser browser = support.createBrowser(urlStr);
                if(browser != null){
                    browser.openURL(new URL(urlStr));
                }
            }
        } catch (PartInitException e) {
            AnyEditToolsPlugin.logError("Failed to open url " + urlStr, e);
        } catch (MalformedURLException e) {
            AnyEditToolsPlugin.logError("Failed to open url " + urlStr, e);
        }
    }

}
