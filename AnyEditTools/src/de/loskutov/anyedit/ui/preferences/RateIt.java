/*******************************************************************************
 * Copyright (c) 2008 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD License
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/bsd-license.php
 * Contributor:  Andrei Loskutov - initial API and implementation
 *******************************************************************************/

package de.loskutov.anyedit.ui.preferences;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.osgi.util.tracker.ServiceTracker;

import de.loskutov.anyedit.AnyEditToolsPlugin;

/**
 * @author Andrei
 */
public class RateIt {

    private static boolean ratingDone;

    static boolean isProxyEnabled() {
        ServiceTracker proxyTracker = new ServiceTracker(AnyEditToolsPlugin.getDefault()
                .getBundle().getBundleContext(), IProxyService.class.getName(), null);
        proxyTracker.open();
        IProxyService proxyService = (IProxyService) proxyTracker.getService();
        IProxyData proxyData = proxyService.getProxyDataForHost(
                "www.eclipseplugincentral.com", IProxyData.HTTP_PROXY_TYPE);
        proxyTracker.close();
        return proxyData != null && proxyService.isProxiesEnabled();
    }

    static boolean rate(int value, String comment) {
        DataOutputStream out = null;
        try {
            URL url = new URL("http://www.eclipseplugincentral.com/Web_Links.html");
            URLConnection urlConn = url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setUseCaches(false);
            urlConn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            out = new DataOutputStream(urlConn.getOutputStream());
            String content = "rating=" + value + "&ratinglid=104&ratinguser=outside"
                    + "&req=addrating";
            if (comment != null && comment.trim().length() > 0) {
                content += "&ratingcomments="
                        + URLEncoder.encode(comment.trim(), "UTF-8");
            }
            out.writeBytes(content);
            out.flush();
            out.close();
            BufferedReader input = new BufferedReader(new InputStreamReader(urlConn
                    .getInputStream()));
            // String s;
            while ((/* s = */input.readLine()) != null) {
                // just read response
                // System.out.println(s);
            }
            input.close();
            ratingDone = true;
            return true;
        } catch (IOException e) {
            AnyEditToolsPlugin.errorDialog("Failed to rate on EPIC...", e);
            return false;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    AnyEditToolsPlugin.errorDialog("Failed to close stream...", e);
                }
            }
        }
    }

    static void createTextArea(Composite defPanel) {

        Composite commonPanel = new Composite(defPanel, SWT.NONE);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.marginWidth = 0;
        commonPanel.setLayout(layout);
        commonPanel.setLayoutData(gridData);

        Label label = new Label(commonPanel, SWT.LEFT);
        label.setText("Rate:");
        label.setToolTipText("You can vote only once!");

        final Combo combo = new Combo(commonPanel, SWT.READ_ONLY | SWT.BORDER);
        final String[] items = new String[] { "Perfect", "Very good", "Good", "So so" };
        combo.setItems(items);
        combo.select(0);

        gridData = new GridData();
        gridData.widthHint = 50;
        combo.setLayoutData(gridData);
        combo.setToolTipText("You can vote only once!");

        final Button okButton = new Button(commonPanel, SWT.NONE);
        okButton.setText("Rate now!");
        okButton.setToolTipText("You can vote only once!");

        commonPanel = new Composite(defPanel, SWT.NONE);
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginWidth = 0;
        commonPanel.setLayout(layout);
        commonPanel.setLayoutData(gridData);

        String tooltip = "This comment will appear on the EPIC web page (www.eclipseplugincentral.com)";

        label = new Label(commonPanel, SWT.LEFT);
        label.setText("Rating Comment");
        label.setToolTipText(tooltip);

        final String commentText = "I like it ("
                + System.getProperty("user.name", "anonymous user") + ")";

        final Text textArea = new Text(commonPanel, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER
                | SWT.V_SCROLL | SWT.H_SCROLL);
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        textArea.setLayoutData(gridData);
        textArea.setText(items[0] + ", " + commentText);
        textArea.setToolTipText(tooltip);

        combo.addSelectionListener(new SelectionAdapter() {
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            public void widgetSelected(SelectionEvent e) {
                if (combo.getSelectionIndex() == items.length - 1) {
                    okButton.setEnabled(false);
                    textArea.setText("If you have to report any issues,\n"
                            + "please don't hesitate to mail me at:\nloskutov@gmx.de!");
                    textArea.setToolTipText(null);
                } else {
                    okButton.setEnabled(true);
                    textArea.setText(items[combo.getSelectionIndex()] + ", "
                            + commentText);
                }
            }
        });

        okButton.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                if (!combo.isEnabled()) {
                    return;
                }
                boolean success = rate(10 - combo.getSelectionIndex(), textArea.getText());
                if (success) {
                    combo.setEnabled(false);
                    textArea.setEnabled(false);
                    okButton.setEnabled(false);
                }
            }
        });

        if (ratingDone) {
            textArea.setText("Thank you for rating!");
            combo.setEnabled(false);
            textArea.setEditable(false);
            okButton.setEnabled(false);
        } else if (isProxyEnabled()) {
            textArea
                    .setText("Unfortunately,\nhttp proxy "
                            + "is enabled and therefore you can't rate AnyEdit plugin now...\n"
                            + "Please try to rate for AnyEdit plugin\nfrom another workstation,\nuse this url:\n"
                            + "www.eclipseplugincentral.com/Web_Links-index-req-ratelink-lid-104-ttitle-AnyEdit_Tools.html");
            combo.setEnabled(false);
            textArea.setEditable(false);
            okButton.setEnabled(false);
        }
    }

}
