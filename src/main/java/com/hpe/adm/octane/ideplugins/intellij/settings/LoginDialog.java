package com.hpe.adm.octane.ideplugins.intellij.settings;

import com.hpe.adm.octane.ideplugins.intellij.ui.customcomponents.LoadingWidget;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.sun.javafx.application.PlatformImpl;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


public class LoginDialog extends DialogWrapper {

    private static final Logger logger = Logger.getInstance(LoginDialog.class.getName());

    public static final String TITLE = "Login to ALM Octane";
    private String loginPageUrl;
    private boolean wasClosed = false;
    private boolean shouldUseJavaFxBrowser;
    private JPanel browserPanel;

    public LoginDialog(Project project, String loginPageUrl) {
        this(project, loginPageUrl, true);
    }

    public LoginDialog(Project project, String loginPageUrl, boolean shouldUseJavaFxBrowser) {
        super(project, false, IdeModalityType.PROJECT);
        this.loginPageUrl = loginPageUrl;
        this.shouldUseJavaFxBrowser = shouldUseJavaFxBrowser;
        setTitle(TITLE);
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {

        logger.debug("Showing login page for login url: " + loginPageUrl);

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout(0, 10));
        contentPane.setPreferredSize(new Dimension(800, 600));

        JLabel lblOpenSystemBrowser = new JLabel("<html>If the page below does not display correctly, <a href=\\\"\\\">click here to use your system default browser.</a></html>");
        lblOpenSystemBrowser.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        contentPane.add(lblOpenSystemBrowser, BorderLayout.NORTH);

        lblOpenSystemBrowser.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(loginPageUrl));
                    JLabel lblSystemBrowser = new JLabel("Opening login page system browser, waiting for session...");
                    if(browserPanel != null) {
                        browserPanel.setVisible(false);
                    }
                    contentPane.add(lblSystemBrowser, BorderLayout.CENTER);
                    contentPane.setPreferredSize(new Dimension(800, -1));
                    LoginDialog.this.pack();
                    LoginDialog.this.centerRelativeToParent();

                } catch (URISyntaxException | IOException ex) {
                    logger.error("Failed to open system browser, " + ex);
                }
            }
        });

        // We need to check if the jvm running IntelliJ has java fx installed in it, on linux, openjdk can be installed without java fx
        // TODO: need to do this for the description and the comments as well
        boolean isJavaFxAvailable;
        try {
            this.getClass().getClassLoader().loadClass("javafx.embed.swing.JFXPanel");
            isJavaFxAvailable = true;

        } catch (ClassNotFoundException e) {

            isJavaFxAvailable = false;
        }

        if (isJavaFxAvailable && shouldUseJavaFxBrowser) {

            browserPanel = new JPanel(new BorderLayout());
            browserPanel.setBorder(new LineBorder(UIManager.getColor("Separator.foreground")));
            contentPane.add(browserPanel, BorderLayout.CENTER);

            LoadingWidget loadingWidget = new LoadingWidget("Loading login page...");
            JFXPanel jfxPanel = new JFXPanel();

            PlatformImpl.setImplicitExit(false);
            PlatformImpl.runAndWait(() -> {

                Group root = new Group();
                Stage stage = new Stage();
                stage.setResizable(true);

                Scene scene = new Scene(root, jfxPanel.getWidth(), jfxPanel.getHeight());
                stage.setScene(scene);

                // Set up the embedded browser:
                WebView browser = new WebView();
                WebEngine webEngine = browser.getEngine();

                // process page loading
                webEngine.getLoadWorker().stateProperty().addListener(
                        (ov, oldState, newState) ->
                                SwingUtilities.invokeLater(() -> {
                                    browserPanel.remove(jfxPanel);
                                    browserPanel.remove(loadingWidget);

                                    if (State.SCHEDULED == newState || State.RUNNING == newState) {
                                        browserPanel.add(loadingWidget, BorderLayout.CENTER);
                                    } else {
                                        browserPanel.add(jfxPanel, BorderLayout.CENTER);
                                    }

                                    contentPane.repaint();
                                    contentPane.revalidate();
                                })
                );

                webEngine.load(loginPageUrl);
                root.getChildren().add(browser);

                jfxPanel.setScene(scene);
            });

        } else {
            contentPane.setPreferredSize(new Dimension(800, -1));
            pack();
            centerRelativeToParent();
        }

        return contentPane;
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[0];
    }

    @Override
    protected void dispose() {
        wasClosed = true;
        super.dispose();
    }

    public boolean wasClosed() {
        return wasClosed;
    }

}