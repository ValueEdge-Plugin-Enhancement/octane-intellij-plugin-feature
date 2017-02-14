package com.hpe.adm.octane.ideplugins.intellij.gitcommit;

import com.hpe.adm.octane.ideplugins.intellij.PluginModule;
import com.hpe.adm.octane.ideplugins.intellij.settings.IdePluginPersistentState;
import com.hpe.adm.octane.services.EntityService;
import com.hpe.adm.octane.services.nonentity.CommitMessageService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import org.jetbrains.annotations.NotNull;

public class OctaneCheckinHandlerFactory extends CheckinHandlerFactory {

    @NotNull
    @Override
    public CheckinHandler createHandler(@NotNull CheckinProjectPanel panel, @NotNull CommitContext commitContext) {
        Project project = panel.getProject();
        PluginModule pluginModule = PluginModule.getPluginModuleForProject(project);
        CheckinHandler checkinHandler = new OctaneCheckinHandler(
                pluginModule.getInstance(IdePluginPersistentState.class),
                pluginModule.getInstance(CommitMessageService.class),
                pluginModule.getInstance(EntityService.class),
                panel
        );
        return checkinHandler;
    }
}