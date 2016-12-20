package com.hpe.adm.octane.ideplugins.intellij.gitcommit;

import com.hpe.adm.octane.ideplugins.intellij.PluginModule;
import com.hpe.adm.octane.ideplugins.services.nonentity.CommitMessageService;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Created by dulaut on 11/22/2016.
 */
public class OctaneCheckinHandlerFactory extends CheckinHandlerFactory {

    @NotNull
    @Override
    public CheckinHandler createHandler(@NotNull CheckinProjectPanel panel, @NotNull CommitContext commitContext) {
        CheckinHandler checkinHandler = new OctaneCheckinHandler(
                PluginModule.getInstance(panel.getProject(), CommitMessageService.class),
                panel
        );
        return checkinHandler;
    }
}
