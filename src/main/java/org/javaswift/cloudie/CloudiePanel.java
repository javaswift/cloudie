/*
 *  Copyright 2012-2013 E.Hooijmeijer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.javaswift.cloudie;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.javaswift.cloudie.login.CloudieCallbackWrapper;
import org.javaswift.cloudie.login.CredentialsStore;
import org.javaswift.cloudie.login.CredentialsStore.Credentials;
import org.javaswift.cloudie.login.LoginPanel;
import org.javaswift.cloudie.login.LoginPanel.LoginCallback;
import org.javaswift.cloudie.ops.CloudieOperations;
import org.javaswift.cloudie.ops.CloudieOperations.CloudieCallback;
import org.javaswift.cloudie.ops.CloudieOperationsImpl;
import org.javaswift.cloudie.ops.ContainerSpecification;
import org.javaswift.cloudie.preview.PreviewPanel;
import org.javaswift.cloudie.util.AsyncWrapper;
import org.javaswift.cloudie.util.DoubleClickListener;
import org.javaswift.cloudie.util.GuiTreadingUtils;
import org.javaswift.cloudie.util.LabelComponentPanel;
import org.javaswift.cloudie.util.PopupTrigger;
import org.javaswift.cloudie.util.ReflectionAction;
import org.javaswift.joss.client.impl.ClientImpl;
import org.javaswift.joss.exception.CommandException;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;

import com.beust.jcommander.ParameterException;

/**
 * CloudiePanel.
 * 
 * @author E.Hooijmeijer
 * 
 */
public class CloudiePanel extends JPanel implements CloudieOperations.CloudieCallback {

    private static final Border LR_PADDING = BorderFactory.createEmptyBorder(0, 2, 0, 2);
    private DefaultListModel containers = new DefaultListModel();
    private JList containersList = new JList(containers);
    private DefaultListModel storedObjects = new DefaultListModel();
    private JList storedObjectsList = new JList(storedObjects);
    private List<StoredObject> allStoredObjects = new ArrayList<StoredObject>();

    private JTextField searchTextField = new JTextField(16);

    private Action accountLoginAction = new ReflectionAction<CloudiePanel>("Login..", getIcon("server_connect.png"), this, "onLogin");
    private Action accountLogoutAction = new ReflectionAction<CloudiePanel>("Logout", getIcon("disconnect.png"), this, "onLogout");
    private Action accountQuitAction = new ReflectionAction<CloudiePanel>("Quit", getIcon("weather_rain.png"), this, "onQuit");

    private Action containerRefreshAction = new ReflectionAction<CloudiePanel>("Refresh", getIcon("arrow_refresh.png"), this, "onRefreshContainers");
    private Action containerCreateAction = new ReflectionAction<CloudiePanel>("Create...", getIcon("folder_add.png"), this, "onCreateContainer");
    private Action containerDeleteAction = new ReflectionAction<CloudiePanel>("Delete...", getIcon("folder_delete.png"), this, "onDeleteContainer");
    private Action containerPurgeAction = new ReflectionAction<CloudiePanel>("Purge...", getIcon("delete.png"), this, "onPurgeContainer");
    private Action containerEmptyAction = new ReflectionAction<CloudiePanel>("Empty...", getIcon("bin_empty.png"), this, "onEmptyContainer");
    private Action containerViewMetaData = new ReflectionAction<CloudiePanel>("View Metadata...", getIcon("page_gear.png"), this, "onViewMetaDataContainer");

    private Action storedObjectOpenAction = new ReflectionAction<CloudiePanel>("Open in Browser", getIcon("application_view_icons.png"), this,
            "onOpenInBrowserStoredObject");
    private Action storedObjectPreviewAction = new ReflectionAction<CloudiePanel>("Preview", getIcon("images.png"), this, "onPreviewStoredObject");
    private Action storedObjectCreateAction = new ReflectionAction<CloudiePanel>("Create...", getIcon("page_add.png"), this, "onCreateStoredObject");
    private Action storedObjectDownloadAction = new ReflectionAction<CloudiePanel>("Download...", getIcon("page_go.png"), this, "onDownloadStoredObject");
    private Action storedObjectDeleteAction = new ReflectionAction<CloudiePanel>("Delete...", getIcon("page_delete.png"), this, "onDeleteStoredObject");
    private Action storedObjectViewMetaData = new ReflectionAction<CloudiePanel>("View Metadata...", getIcon("page_gear.png"), this,
            "onViewMetaDataStoredObject");

    private Action aboutAction = new ReflectionAction<CloudiePanel>("About", getIcon("information.png"), this, "onAbout");

    private Action searchAction = new ReflectionAction<CloudiePanel>("Search", null, this, "onSearch");

    private JFrame owner;

    private CloudieOperations ops;
    private CloudieOperations.CloudieCallback callback;
    private PreviewPanel previewPanel = new PreviewPanel();
    private StatusPanel statusPanel;
    private boolean loggedIn;
    private int busyCnt;

    private CredentialsStore credentialsStore = new CredentialsStore();
    private File lastFolder = null;

    /**
     * creates Cloudie and immediately logs in using the given credentials.
     * @param login the login credentials.
     */
    public CloudiePanel(List<String> login) {
        this();
        ops.login(login.get(0), login.get(1), login.get(2), login.get(3), callback);
    }

    /**
     * creates Cloudie and immediately logs in using the given previously stored
     * profile.
     * @param profile the profile.
     */
    public CloudiePanel(String profile) {
        this();
        CredentialsStore store = new CredentialsStore();
        Credentials found = null;
        for (Credentials cr : store.getAvailableCredentials()) {
            if (cr.toString().equals(profile)) {
                found = cr;
            }
        }
        if (found == null) {
            throw new ParameterException("Unknown profile '" + profile + "'.");
        } else {
            ops.login(found.authUrl, found.tenant, found.username, String.valueOf(found.password), callback);
        }
    }

    /**
     * creates Cloudie and does not login.
     */
    public CloudiePanel() {
        super(new BorderLayout());
        //
        ops = createCloudieOperations();
        callback = GuiTreadingUtils.guiThreadSafe(CloudieCallback.class, this);
        //
        statusPanel = new StatusPanel(ops, callback);
        //
        JScrollPane left = new JScrollPane(containersList);
        //
        storedObjectsList.setMinimumSize(new Dimension(420, 320));
        JSplitPane center = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(storedObjectsList), new JScrollPane(previewPanel));
        center.setDividerLocation(450);
        //
        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, center);
        main.setDividerLocation(272);
        add(main, BorderLayout.CENTER);
        //
        add(statusPanel, BorderLayout.SOUTH);
        //
        searchTextField.setAction(searchAction);
        //
        createLists();
        //
        storedObjectsList.addMouseListener(new PopupTrigger<CloudiePanel>(createStoredObjectPopupMenu(), this, "enableDisableStoredObjectMenu"));
        storedObjectsList.addMouseListener(new DoubleClickListener(storedObjectPreviewAction));
        containersList.addMouseListener(new PopupTrigger<CloudiePanel>(createContainerPopupMenu(), this, "enableDisableContainerMenu"));
        //
        bind();
        //
        enableDisable();
    }

    public void setOwner(JFrame owner) {
        this.owner = owner;
    }

    private void createLists() {
        containersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        containersList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setBorder(LR_PADDING);
                Container c = (Container) value;
                lbl.setText(c.getName());
                lbl.setToolTipText(lbl.getText());
                return lbl;
            }
        });
        containersList.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableDisableContainerMenu();
                updateStatusPanelForContainer();
            }
        });
        //
        storedObjectsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        storedObjectsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setBorder(LR_PADDING);
                StoredObject so = (StoredObject) value;
                lbl.setText(so.getName());
                lbl.setToolTipText(lbl.getText());
                return lbl;
            }
        });
        storedObjectsList.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableDisableStoredObjectMenu();
                updateStatusPanelForStoredObject();
            }
        });
    }

    private CloudieOperations createCloudieOperations() {
        ClientImpl clientImpl = new ClientImpl();
        PoolingClientConnectionManager cm = new PoolingClientConnectionManager();
        cm.setMaxTotal(50);
        cm.setDefaultMaxPerRoute(25);
        HttpClient httpClient = new DefaultHttpClient(cm);
        clientImpl.setHttpClient(httpClient);
        CloudieOperationsImpl lops = new CloudieOperationsImpl(clientImpl);
        return AsyncWrapper.async(lops);
    }

    public static Icon getIcon(String string) {
        return new ImageIcon(CloudiePanel.class.getResource("/icons/" + string));
    }

    public boolean isContainerSelected() {
        return containersList.getSelectedIndex() >= 0;
    }

    public Container getSelectedContainer() {
        return isContainerSelected() ? (Container) containers.get(containersList.getSelectedIndex()) : null;
    }

    public boolean isSingleStoredObjectSelected() {
        return storedObjectsList.getSelectedIndices().length == 1;
    }

    public boolean isStoredObjectsSelected() {
        return storedObjectsList.getSelectedIndices().length > 0;
    }

    public List<StoredObject> getSelectedStoredObjects() {
        List<StoredObject> results = new ArrayList<StoredObject>();
        for (int idx : storedObjectsList.getSelectedIndices()) {
            results.add((StoredObject) storedObjects.get(idx));
        }
        return results;
    }

    public <A> A single(List<A> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    public void enableDisable() {
        enableDisableAccountMenu();
        enableDisableContainerMenu();
        enableDisableStoredObjectMenu();
    }

    public void enableDisableAccountMenu() {
        accountLoginAction.setEnabled(!loggedIn);
        accountLogoutAction.setEnabled(loggedIn);
        accountQuitAction.setEnabled(true);
    }

    public void enableDisableContainerMenu() {
        boolean containerSelected = isContainerSelected();
        Container selected = getSelectedContainer();
        //
        containerRefreshAction.setEnabled(loggedIn);
        containerCreateAction.setEnabled(loggedIn);
        containerDeleteAction.setEnabled(containerSelected);
        containerPurgeAction.setEnabled(containerSelected);
        containerEmptyAction.setEnabled(containerSelected);
        containerViewMetaData.setEnabled(containerSelected && selected.isInfoRetrieved() && !selected.getMetadata().isEmpty());
    }

    public void enableDisableStoredObjectMenu() {
        boolean singleObjectSelected = isSingleStoredObjectSelected();
        boolean objectsSelected = isStoredObjectsSelected();
        boolean containerSelected = isContainerSelected();
        StoredObject selected = single(getSelectedStoredObjects());
        Container selectedContainer = getSelectedContainer();
        //
        storedObjectPreviewAction.setEnabled(singleObjectSelected && selected.isInfoRetrieved());
        storedObjectCreateAction.setEnabled(containerSelected);
        storedObjectDownloadAction.setEnabled(containerSelected && objectsSelected);
        storedObjectViewMetaData.setEnabled(containerSelected && singleObjectSelected && selected.isInfoRetrieved() && !selected.getMetadata().isEmpty());
        //
        storedObjectOpenAction.setEnabled(objectsSelected && containerSelected && selectedContainer.isPublic());
        storedObjectDeleteAction.setEnabled(containerSelected && objectsSelected);
    }

    protected void updateStatusPanelForStoredObject() {
        if (isStoredObjectsSelected()) {
            statusPanel.onSelectStoredObjects(getSelectedStoredObjects());
        }
    }

    protected void updateStatusPanelForContainer() {
        if (isContainerSelected()) {
            statusPanel.onSelectContainer(getSelectedContainer());
        }
    }

    @Override
    public void onNumberOfCalls(int nrOfCalls) {
        statusPanel.onNumberOfCalls(nrOfCalls);
    }

    public void onLogin() {
        final JDialog loginDialog = new JDialog(owner, "Login");
        final LoginPanel loginPanel = new LoginPanel(new LoginCallback() {
            @Override
            public void doLogin(String authUrl, String tenant, String username, char[] pass) {
                CloudieCallback cb = GuiTreadingUtils.guiThreadSafe(CloudieCallback.class, new CloudieCallbackWrapper(callback) {
                    @Override
                    public void onLoginSuccess() {
                        loginDialog.setVisible(false);
                        super.onLoginSuccess();
                    }

                    @Override
                    public void onError(CommandException ex) {
                        JOptionPane.showMessageDialog(loginDialog, "Login Failed\n" + ex.toString(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
                ops.login(authUrl, tenant, username, new String(pass), cb);
            }
        }, credentialsStore);
        try {
            loginPanel.setOwner(loginDialog);
            loginDialog.getContentPane().add(loginPanel);
            loginDialog.setModal(true);
            loginDialog.setSize(480, 280);
            loginDialog.setResizable(false);
            loginDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            center(loginDialog);
            loginDialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    loginPanel.onCancel();
                }

                @Override
                public void windowOpened(WindowEvent e) {
                    loginPanel.onShow();
                }
            });
            loginDialog.setVisible(true);
        } finally {
            loginDialog.dispose();
        }
    }

    private void center(JDialog dialog) {
        int x = owner.getLocation().x + (owner.getWidth() - dialog.getWidth()) / 2;
        int y = owner.getLocation().y + (owner.getHeight() - dialog.getHeight()) / 2;
        dialog.setLocation(x, y);
    }

    @Override
    public void onLoginSuccess() {
        this.onNewStoredObjects();
        ops.refreshContainers(callback);
        loggedIn = true;
        enableDisable();
    }

    public void onAbout() {
        StringBuilder sb = loadResource("/about.html");
        JLabel label = new JLabel(sb.toString());
        JOptionPane.showMessageDialog(this, label, "About Cloudie", JOptionPane.INFORMATION_MESSAGE, getIcon("weather_cloudy.png"));
    }

    private StringBuilder loadResource(String resource) {
        StringBuilder sb = new StringBuilder();
        InputStream input = CloudiePanel.class.getResourceAsStream(resource);
        try {
            try {
                List<String> lines = IOUtils.readLines(input);
                for (String line : lines) {
                    sb.append(line);
                }
            } finally {
                input.close();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return sb;
    }

    public void onLogout() {
        ops.logout(callback);
    }

    @Override
    public void onLogoutSuccess() {
        this.onUpdateContainers(Collections.<Container> emptyList());
        this.onNewStoredObjects();
        statusPanel.onDeselect();
        loggedIn = false;
        enableDisable();
    }

    public void onQuit() {
        if (onClose()) {
            System.exit(0);
        }
    }

    private JPopupMenu createContainerPopupMenu() {
        JPopupMenu pop = new JPopupMenu("Container");
        pop.add(new JMenuItem(containerRefreshAction));
        pop.add(new JMenuItem(containerViewMetaData));
        pop.addSeparator();
        pop.add(new JMenuItem(containerCreateAction));
        pop.add(new JMenuItem(containerDeleteAction));
        pop.addSeparator();
        pop.add(new JMenuItem(containerEmptyAction));
        pop.addSeparator();
        pop.add(new JMenuItem(containerPurgeAction));
        return pop;
    }

    protected void onPurgeContainer() {
        Container c = getSelectedContainer();
        if (confirm("Are you sure you want to PURGE container '" + c.getName()
                + "'? This will remove the container and ALL ITS CONTENTS.\n You cannot get back what you delete!")) {
            ops.purgeContainer(c, callback);
        }
    }

    protected void onEmptyContainer() {
        Container c = getSelectedContainer();
        if (confirm("Are you sure you want to EMPTY container '" + c.getName() + "'? This will remove ALL ITS CONTENTS.\n You cannot get back what you delete!")) {
            ops.emptyContainer(c, callback);
        }
    }

    protected void onRefreshContainers() {
        int idx = containersList.getSelectedIndex();
        refreshContainers();
        containersList.setSelectedIndex(idx);
    }

    protected void onDeleteContainer() {
        Container c = getSelectedContainer();
        if (confirm("Are you sure you want to delete container '" + c.getName() + "'? You cannot get back what you delete.")) {
            ops.deleteContainer(c, callback);
        }
    }

    protected void onCreateContainer() {
        ContainerSpecification spec = doGetContainerSpec();
        ops.createContainer(spec, callback);
    }

    private ContainerSpecification doGetContainerSpec() {
        JTextField name = new JTextField();
        JCheckBox priv = new JCheckBox("private container");
        if (JOptionPane.showConfirmDialog(this, new Object[] { "Name", name, priv }, "Create Container", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            return new ContainerSpecification(name.getText(), priv.isSelected());
        }
        return null;
    }

    /**
     * @return
     */
    private JPopupMenu createStoredObjectPopupMenu() {
        JPopupMenu pop = new JPopupMenu("StoredObject");
        pop.add(new JMenuItem(storedObjectPreviewAction));
        pop.add(new JMenuItem(storedObjectOpenAction));
        pop.add(new JMenuItem(storedObjectViewMetaData));
        pop.addSeparator();
        pop.add(new JMenuItem(storedObjectCreateAction));
        pop.add(new JMenuItem(storedObjectDownloadAction));
        pop.addSeparator();
        pop.add(new JMenuItem(storedObjectDeleteAction));
        return pop;
    }

    public JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu accountMenu = new JMenu("Account");
        JMenu containerMenu = new JMenu("Container");
        JMenu storedObjectMenu = new JMenu("StoredObject");
        JMenu helpMenu = new JMenu("Help");
        accountMenu.setMnemonic('A');
        containerMenu.setMnemonic('C');
        storedObjectMenu.setMnemonic('O');
        helpMenu.setMnemonic('H');
        bar.add(accountMenu);
        bar.add(containerMenu);
        bar.add(storedObjectMenu);
        bar.add(helpMenu);
        JPanel panel = new JPanel(new FlowLayout(SwingConstants.RIGHT, 0, 0));
        JLabel label = new JLabel(getIcon("zoom.png"));
        label.setLabelFor(searchTextField);
        label.setDisplayedMnemonic('f');
        panel.add(label);
        panel.add(searchTextField);
        bar.add(panel);
        //
        accountMenu.add(new JMenuItem(accountLoginAction));
        accountMenu.add(new JMenuItem(accountLogoutAction));
        accountMenu.addSeparator();
        accountMenu.add(new JMenuItem(accountQuitAction));
        //
        containerMenu.add(new JMenuItem(containerRefreshAction));
        containerMenu.add(new JMenuItem(containerViewMetaData));
        containerMenu.addSeparator();
        containerMenu.add(new JMenuItem(containerCreateAction));
        containerMenu.add(new JMenuItem(containerDeleteAction));
        containerMenu.addSeparator();
        containerMenu.add(new JMenuItem(containerEmptyAction));
        containerMenu.addSeparator();
        containerMenu.add(new JMenuItem(containerPurgeAction));
        //
        storedObjectMenu.add(new JMenuItem(storedObjectPreviewAction));
        storedObjectMenu.add(new JMenuItem(storedObjectOpenAction));
        storedObjectMenu.add(new JMenuItem(storedObjectViewMetaData));
        storedObjectMenu.addSeparator();
        storedObjectMenu.add(new JMenuItem(storedObjectCreateAction));
        storedObjectMenu.add(new JMenuItem(storedObjectDownloadAction));
        storedObjectMenu.addSeparator();
        storedObjectMenu.add(new JMenuItem(storedObjectDeleteAction));
        //
        helpMenu.add(new JMenuItem(aboutAction));
        //
        return bar;
    }

    protected void onOpenInBrowserStoredObject() {
        Container container = getSelectedContainer();
        List<StoredObject> objects = getSelectedStoredObjects();
        if (container.isPublic()) {
            for (StoredObject obj : objects) {
                String publicURL = obj.getPublicURL();
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(new URI(publicURL));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    protected void onPreviewStoredObject() {
        StoredObject obj = single(getSelectedStoredObjects());
        if (obj.getContentLength() < 16 * 1024 * 1024) {
            previewPanel.preview(obj.getContentType(), obj.downloadObject());
        }
    }

    protected void onViewMetaDataStoredObject() {
        StoredObject obj = single(getSelectedStoredObjects());
        Map<String, Object> metadata = obj.getMetadata();
        List<LabelComponentPanel> panels = buildMetaDataPanels(metadata);
        JOptionPane.showMessageDialog(this, panels.toArray(), obj.getName() + " metadata.", JOptionPane.INFORMATION_MESSAGE);
    }

    private List<LabelComponentPanel> buildMetaDataPanels(Map<String, Object> metadata) {
        List<LabelComponentPanel> panels = new ArrayList<LabelComponentPanel>();
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            JLabel comp = new JLabel(String.valueOf(entry.getValue()));
            comp.setFont(comp.getFont().deriveFont(Font.PLAIN));
            panels.add(new LabelComponentPanel(entry.getKey(), comp));
        }
        return panels;
    }

    protected void onViewMetaDataContainer() {
        Container obj = getSelectedContainer();
        Map<String, Object> metadata = obj.getMetadata();
        List<LabelComponentPanel> panels = buildMetaDataPanels(metadata);
        JOptionPane.showMessageDialog(this, panels.toArray(), obj.getName() + " metadata.", JOptionPane.INFORMATION_MESSAGE);
    }

    protected void onDownloadStoredObject() {
        Container container = getSelectedContainer();
        List<StoredObject> obj = getSelectedStoredObjects();
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(lastFolder);
        if (obj.size() == 1) {
            chooser.setSelectedFile(new File(obj.get(0).getName()));
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        } else {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            for (StoredObject so : obj) {
                File target = selected;
                if (target.isDirectory()) {
                    target = new File(selected, so.getName());
                }
                if (target.exists()) {
                    if (confirm("File '" + target.getName() + "' already exists. Overwrite?")) {
                        doSaveStoredObject(target, container, so);
                    }
                } else {
                    doSaveStoredObject(target, container, so);
                }
            }
            lastFolder = selected.isFile() ? selected.getParentFile() : selected;
        }

    }

    private void doSaveStoredObject(File target, Container container, StoredObject obj) {
        ops.downloadStoredObject(container, obj, target, callback);
    }

    protected void onDeleteStoredObject() {
        Container container = getSelectedContainer();
        List<StoredObject> objects = getSelectedStoredObjects();
        if (objects.size() == 1) {
            doDeleteSingleObject(container, single(objects));
        } else {
            doDeleteMultipleObjects(container, objects);
        }
    }

    protected void doDeleteSingleObject(Container container, StoredObject obj) {
        if (confirm("Are you sure you want to delete '" + obj.getName() + "' from '" + container.getName() + "'? You cannot get back what you delete.")) {
            ops.deleteStoredObjects(container, Collections.singletonList(obj), callback);
        }
    }

    protected void doDeleteMultipleObjects(Container container, List<StoredObject> obj) {
        if (confirm("Are you sure you want to delete " + obj.size() + " objects from '" + container.getName() + "'? You cannot get back what you delete.")) {
            ops.deleteStoredObjects(container, obj, callback);
        }
    }

    protected void onCreateStoredObject() {
        Container container = getSelectedContainer();
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setCurrentDirectory(lastFolder);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = chooser.getSelectedFiles();
            ops.createStoredObjects(container, selectedFiles, callback);
            lastFolder = chooser.getCurrentDirectory();
        }
    }

    public void bind() {
        containersList.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                } else {
                    int idx = containersList.getSelectedIndex();
                    if (idx >= 0) {
                        refreshFiles((Container) containers.get(idx));
                    }
                }
            }
        });
        //
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable ex) {
                if (ex instanceof CommandException) {
                    showError((CommandException) ex);
                } else {
                    ex.printStackTrace();
                }
            }

        });
        //
        containersList.getInputMap().put(KeyStroke.getKeyStroke("F5"), "refresh");
        containersList.getActionMap().put("refresh", containerRefreshAction);
        //
        storedObjectsList.getInputMap().put(KeyStroke.getKeyStroke("F5"), "refresh");
        storedObjectsList.getActionMap().put("refresh", containerRefreshAction);
        //
    }

    public void refreshContainers() {
        containers.clear();
        storedObjects.clear();
        ops.refreshContainers(callback);
    }

    public void refreshFiles(Container selected) {
        storedObjects.clear();
        ops.refreshStoredObjects(selected, callback);
    }

    //
    // Callback
    //

    @Override
    public void onStart() {
        if (busyCnt == 0) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            statusPanel.onStart();
        }
        busyCnt++;
    }

    @Override
    public void onDone() {
        busyCnt--;
        if (busyCnt == 0) {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            statusPanel.onEnd();
        }
        enableDisable();
    }

    @Override
    public void onError(CommandException ex) {
        ex.printStackTrace();

        showError(ex);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void onUpdateContainers(Collection<Container> cs) {
        containers.clear();
        for (Container container : cs) {
            containers.addElement(container);
        }
        statusPanel.onDeselectContainer();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void onStoredObjectDeleted(Container container, StoredObject storedObject) {
        if (isContainerSelected() && getSelectedContainer().equals(container)) {
            int idx = storedObjects.indexOf(storedObject);
            if (idx >= 0) {
                storedObjectsList.getSelectionModel().removeIndexInterval(idx, idx);
            }
            storedObjects.removeElement(storedObject);
            allStoredObjects.remove(storedObject);
        }
    }

    public void onSearch() {
        storedObjects.clear();
        for (StoredObject storedObject : allStoredObjects) {
            if (isFilterIncluded(storedObject)) {
                storedObjects.addElement(storedObject);
            }
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void onNewStoredObjects() {
        searchTextField.setText("");
        storedObjects.clear();
        allStoredObjects.clear();
        statusPanel.onDeselectStoredObject();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void onAppendStoredObjects(Container container, int page, Collection<StoredObject> sos) {
        if (isContainerSelected() && getSelectedContainer().equals(container)) {
            allStoredObjects.addAll(sos);
            for (StoredObject storedObject : sos) {
                if (isFilterIncluded(storedObject)) {
                    storedObjects.addElement(storedObject);
                }
            }
        }
    }

    private boolean isFilterIncluded(StoredObject obj) {
        String filter = searchTextField.getText();
        if (filter.isEmpty()) {
            return true;
        } else {
            return obj.getName().contains(filter);
        }
    }

    @Override
    public void onContainerUpdate(Container container) {
        statusPanel.onSelectContainer(container);
    }

    @Override
    public void onStoredObjectUpdate(StoredObject obj) {
        statusPanel.onSelectStoredObjects(Collections.singletonList(obj));
    }

    //
    //
    //

    /**
     * @return true if the window can close.
     */
    public boolean onClose() {
        return (loggedIn && confirm("Are you sure you want to Quit this application?")) || (!loggedIn);
    }

    public boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    protected void showError(CommandException ex) {
        JOptionPane.showMessageDialog(this, ex.toString(), "Error", JOptionPane.OK_OPTION);
    }
}
