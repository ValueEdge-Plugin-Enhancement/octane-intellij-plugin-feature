package com.hpe.adm.octane.ideplugins.intellij.settings;

import com.hpe.adm.octane.ideplugins.intellij.PluginModule;
import com.hpe.adm.octane.ideplugins.intellij.ui.components.ConnectionSettingsComponent;
import com.hpe.adm.octane.ideplugins.intellij.util.Constants;
import com.hpe.adm.octane.ideplugins.services.TestService;
import com.hpe.adm.octane.ideplugins.services.connection.ConnectionSettings;
import com.hpe.adm.octane.ideplugins.services.connection.ConnectionSettingsProvider;
import com.hpe.adm.octane.ideplugins.services.exception.ServiceException;
import com.hpe.adm.octane.ideplugins.services.util.UrlParser;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.hpe.adm.octane.ideplugins.services.util.UrlParser.resolveConnectionSettings;

public class ConnectionSettingsConfigurable implements SearchableConfigurable {

    private static final String NAME = "Octane";

    //@Inject is not supported here, this class is instantiated by intellij
    private ConnectionSettingsProvider connectionSettingsProvider = PluginModule.getInstance(ConnectionSettingsProvider.class);
    private ConnectionSettingsComponent connectionSettingsView = new ConnectionSettingsComponent();
    private TestService testService = PluginModule.getInstance(TestService.class);

    @NotNull
    @Override
    public String getId() {
        return "settings.octane";
    }

    @Nullable
    @Override
    public Runnable enableSearch(String option) {
        return null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return "settings.octane";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        ConnectionSettings connectionSettings = connectionSettingsProvider.getConnectionSettings();

        //Setting the base url will fire the even handler in the view, this will set the shared space and workspace fields
        connectionSettingsView.setServerUrl(UrlParser.createUrlFromConnectionSettings(connectionSettings));
        connectionSettingsView.setUserName(connectionSettings.getUserName());
        connectionSettingsView.setPassword(connectionSettings.getPassword());

        connectionSettingsView.setTestConnectionActionListener(event -> {
            //Clear previous message
            connectionSettingsView.setConnectionStatusLoading();
            new SwingWorker() {
                @Override
                protected Void doInBackground() throws Exception {
                    testConnection();
                    return null;
                }
            }.execute();
        });

        return connectionSettingsView.getComponent();
    }

    @Override
    public boolean isModified() {

        ConnectionSettings currentConnectionSettings = connectionSettingsProvider.getConnectionSettings();

        ConnectionSettings viewConnectionSettings;
        try {
            viewConnectionSettings = resolveConnectionSettings(
                    connectionSettingsView.getServerUrl(),
                    connectionSettingsView.getUserName(),
                    connectionSettingsView.getPassword());
        } catch (ServiceException e) {
            viewConnectionSettings = new ConnectionSettings();
        }

        return !viewConnectionSettings.equals(currentConnectionSettings);
    }

    @Override
    public void apply() throws ConfigurationException {
        //If the connection settings are empty then save them, only way to clear and save
        if(isViewConnectionSettingsEmpty()){
            connectionSettingsProvider.setConnectionSettings(new ConnectionSettings());
            return;
        }

        ConnectionSettings newConnectionSettings = testConnection();
        //apply if valid
        if(newConnectionSettings != null){
            connectionSettingsProvider.setConnectionSettings(newConnectionSettings);
            //remove the hash and remove extra stuff if successful
            connectionSettingsView.setServerUrl(UrlParser.createUrlFromConnectionSettings(newConnectionSettings));
            connectionSettingsView.setConnectionStatusSuccess();
        }
    }

    /**
     * Test the connection with the given info from the view, sets error labels
     * @return ConnectionSettings if valid, null otherwise
     */
    private ConnectionSettings testConnection(){
        ConnectionSettings newConnectionSettings;

        // Validation that does not require connection to the server,
        // only this one shows and example for a correct message
        try {
            newConnectionSettings = getConnectionSettingsFromView();
        } catch (ServiceException ex){

            final StringBuilder errorMessageBuilder = new StringBuilder();

            errorMessageBuilder.append(ex.getMessage());
            errorMessageBuilder.append("<br>");
            errorMessageBuilder.append(Constants.CORRECT_URL_FORMAT_MESSAGE);

            SwingUtilities.invokeLater(() ->  connectionSettingsView.setConnectionStatusError(errorMessageBuilder.toString()));

            return null;
        }

        //Validation of username and password
        try {
            validateUsernameAndPassword();
        } catch (ServiceException ex) {
            SwingUtilities.invokeLater(() ->  connectionSettingsView.setConnectionStatusError(ex.getMessage()));

            return null;
        }

        //This will attempt a connection
        try {
            testService.testConnection(newConnectionSettings);
            SwingUtilities.invokeLater(connectionSettingsView::setConnectionStatusSuccess);
        } catch (ServiceException ex){
            SwingUtilities.invokeLater(() ->  connectionSettingsView.setConnectionStatusError(ex.getMessage()));

            return null;
        }

        //it's valid! yay
        return newConnectionSettings;
    }


    private ConnectionSettings getConnectionSettingsFromView() throws ServiceException{
        //Parse server url
        ConnectionSettings connectionSettings = UrlParser.resolveConnectionSettings(
                connectionSettingsView.getServerUrl(),
                connectionSettingsView.getUserName(),
                connectionSettingsView.getPassword());

        return connectionSettings;
    }

    private boolean isViewConnectionSettingsEmpty(){
        return StringUtils.isEmpty(connectionSettingsView.getServerUrl()) &&
                StringUtils.isEmpty(connectionSettingsView.getUserName()) &&
                StringUtils.isEmpty(connectionSettingsView.getPassword());
    }

    private void validateUsernameAndPassword() throws ServiceException {
        StringBuilder errorMessageBuilder = new StringBuilder();
        if(StringUtils.isEmpty(connectionSettingsView.getUserName())){
            errorMessageBuilder.append("Username cannot be blank.");
        }
        if(errorMessageBuilder.length() != 0){
            errorMessageBuilder.append(" ");
        }
        if(StringUtils.isEmpty(connectionSettingsView.getPassword())){
            errorMessageBuilder.append("Password cannot be blank.");
        }

        if(errorMessageBuilder.length() != 0){
            throw new ServiceException(errorMessageBuilder.toString());
        }
    }

    @Override
    public void reset() {
        ConnectionSettings connectionSettings = connectionSettingsProvider.getConnectionSettings();
        connectionSettingsView.setServerUrl(UrlParser.createUrlFromConnectionSettings(connectionSettings));
        connectionSettingsView.setUserName(connectionSettings.getUserName());
        connectionSettingsView.setPassword(connectionSettings.getPassword());
    }

    @Override
    public void disposeUIResources() {
        connectionSettingsView = null;
    }

}
