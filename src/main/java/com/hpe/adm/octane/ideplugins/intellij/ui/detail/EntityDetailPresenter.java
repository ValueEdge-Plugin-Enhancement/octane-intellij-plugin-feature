package com.hpe.adm.octane.ideplugins.intellij.ui.detail;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.hpe.adm.nga.sdk.exception.OctaneException;
import com.hpe.adm.nga.sdk.model.EntityModel;
import com.hpe.adm.nga.sdk.model.ReferenceFieldModel;
import com.hpe.adm.octane.ideplugins.intellij.ui.Presenter;
import com.hpe.adm.octane.services.util.Util;
import com.hpe.adm.octane.services.util.Constants;
import com.hpe.adm.octane.ideplugins.intellij.util.RestUtil;
import com.hpe.adm.octane.services.CommentService;
import com.hpe.adm.octane.services.EntityService;
import com.hpe.adm.octane.services.exception.ServiceException;
import com.hpe.adm.octane.services.filtering.Entity;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import com.intellij.util.ui.ConfirmationDialog;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;

import static com.hpe.adm.octane.services.filtering.Entity.*;

public class EntityDetailPresenter implements Presenter<EntityDetailView> {

    @Inject
    private EntityService entityService;
    @Inject
    private CommentService commentService;
    @Inject
    private Project project;

    private EntityDetailView entityDetailView;
    private Entity entityType;
    private Long entityId;
    private EntityModel entityModel;
    private Logger logger = Logger.getInstance("EntityDetailPresenter");
    private final String GO_TO_BROWSER_DIALOG_MESSAGE = "\nThe plugin does not support editing.\n" + "You can edit the field in the browser. Do you what to do this now?";


    public EntityDetailPresenter() {
    }

    public EntityDetailView getView() {
        return entityDetailView;
    }

    @Override
    @Inject
    public void setView(EntityDetailView entityDetailView) {
        this.entityDetailView = entityDetailView;
    }

    public void setEntity(Entity entityType, Long entityId) {
        this.entityType = entityType;
        this.entityId = entityId;

        RestUtil.runInBackground(
                () -> {
                    try {
                        return entityService.findEntity(entityType, entityId);
                    } catch (ServiceException ex) {
                        entityDetailView.setErrorMessage(ex.getMessage());
                        return null;
                    }
                },
                (entityModel) -> {
                    if (entityModel != null) {
                        this.entityModel = entityModel;
                        entityDetailView.setEntityModel(entityModel);
                        entityDetailView.setSaveSelectedPhaseButton(new SaveSelectedPhaseAction());
                        entityDetailView.setRefreshEntityButton(new EntityRefreshAction());
                        if (entityType != MANUAL_TEST_RUN && entityType != TEST_SUITE_RUN) {
                            setPossibleTransitions(entityModel);
                            entityDetailView.setPhaseInHeader(true);
                        } else {
                            entityDetailView.removeSaveSelectedPhaseButton();
                            entityDetailView.setPhaseInHeader(false);
                        }
                        if (entityType.getSubtypeOf() == WORK_ITEM || entityType.getSubtypeOf() == TEST) {
                            setComments(entityModel);
                            addSendNewCommentAction(entityModel);
                        } else {
                            entityDetailView.removeToggleOnButtonForComments();
                        }
                        //Title goes to browser
                        entityDetailView.setEntityNameClickHandler(() -> entityService.openInBrowser(entityModel));
                    }
                },
                null,
                null,
                "Loading entity " + entityType.name() + ": " + entityId);


    }

    private void setPossibleTransitions(EntityModel entityModel) {
        Collection<EntityModel> result = new HashSet<>();
        RestUtil.runInBackground(() -> {
            Long currentPhaseId = Long.valueOf(Util.getUiDataFromModel(entityModel.getValue("phase"), "id"));
            return entityService.findPossibleTransitionFromCurrentPhase(Entity.getEntityType(entityModel), currentPhaseId);
        }, (possibleTransitions) -> {
            if (possibleTransitions.isEmpty()) {
                possibleTransitions.add(new EntityModel("target_phase", "No transition"));
                entityDetailView.setPossiblePhasesForEntity(possibleTransitions);
                entityDetailView.removeSaveSelectedPhaseButton();
            } else {
                entityDetailView.setPossiblePhasesForEntity(possibleTransitions);
            }
        }, null, "Failed to get possible transitions", "fetching possible transitions");
    }

    private void setComments(EntityModel entityModel) {
        Collection<EntityModel> result = new HashSet<>();
        RestUtil.runInBackground(() -> commentService.getComments(entityModel), (comments) -> entityDetailView.setComments(comments), null, "Failed to get possible comments", "fetching comments");
    }

    private final class EntityRefreshAction extends AnAction {
        public EntityRefreshAction() {
            super("Refresh current entity", "this will refresh the current entity", IconLoader.findIcon(Constants.IMG_REFRESH_ICON));
        }

        public void actionPerformed(AnActionEvent e) {
            entityDetailView.doRefresh();
            setEntity(entityType, entityId);
        }
    }

    private final class SaveSelectedPhaseAction extends AnAction {
        public SaveSelectedPhaseAction() {
            super("Save selected phase", "this will save the new phase entity", IconLoader.findIcon("/actions/menu-saveall.png"));
        }

        public void actionPerformed(AnActionEvent e) {
            RestUtil.runInBackground(() -> {
                EntityModel selectedTransition = entityDetailView.getSelectedTransition();
                return (ReferenceFieldModel) selectedTransition.getValue("target_phase");
            }, (nextPhase) -> {
                try {
                    entityService.updateEntityPhase(entityDetailView.getEntityModel(), nextPhase);
                } catch (OctaneException ex) {
                    if (ex.getMessage().contains("400")) {
                        String errorMessage = "Failed to change phase";
                        try {
                            JsonParser jsonParser = new JsonParser();
                            JsonObject jsonObject = (JsonObject) jsonParser.parse(ex.getMessage().substring(ex.getMessage().indexOf("{")));
                            errorMessage = jsonObject.get("description_translated").getAsString();
                        } catch (Exception e1) {
                            logger.debug("Failed to get JSON message from Octane Server" + e1.getMessage());
                        }
                        ConfirmationDialog dialog = new ConfirmationDialog(
                                project,
                                "Server message: " + errorMessage + GO_TO_BROWSER_DIALOG_MESSAGE,
                                "Business rule violation",
                                null, VcsShowConfirmationOption.STATIC_SHOW_CONFIRMATION) {
                            @Override
                            public void setDoNotAskOption(@Nullable DoNotAskOption doNotAsk) {
                                super.setDoNotAskOption(null);
                            }
                        };
                        if (dialog.showAndGet()) {
                            entityService.openInBrowser(entityModel);
                        }
                    }
                }
                entityDetailView.doRefresh();
                setEntity(entityType, entityId);
            }, null, "Failed to move to next phase", "Moving to next phase");

        }
    }

    public void addSendNewCommentAction(EntityModel entityModel) {
        entityDetailView.addSendNewCommentAction(e -> {
            commentService.postComment(entityModel, entityDetailView.getCommentMessageBoxText());
            entityDetailView.setCommentMessageBoxText("");
            setComments(entityModel);
        });
    }

}
