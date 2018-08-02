/*
 * © 2017 EntIT Software LLC, a Micro Focus company, L.P.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hpe.adm.octane.ideplugins.intellij.ui.detail;

import com.google.inject.Inject;
import com.hpe.adm.nga.sdk.exception.OctaneException;
import com.hpe.adm.nga.sdk.metadata.FieldMetadata;
import com.hpe.adm.nga.sdk.model.StringFieldModel;
import com.hpe.adm.octane.ideplugins.intellij.ui.Constants;
import com.hpe.adm.octane.ideplugins.intellij.ui.Presenter;
import com.hpe.adm.octane.ideplugins.intellij.ui.customcomponents.BusinessErrorReportingDialog;
import com.hpe.adm.octane.ideplugins.intellij.util.ExceptionHandler;
import com.hpe.adm.octane.ideplugins.intellij.util.HtmlTextEditor;
import com.hpe.adm.octane.ideplugins.intellij.util.RestUtil;
import com.hpe.adm.octane.ideplugins.services.EntityService;
import com.hpe.adm.octane.ideplugins.services.MetadataService;
import com.hpe.adm.octane.ideplugins.services.filtering.Entity;
import com.hpe.adm.octane.ideplugins.services.model.EntityModelWrapper;
import com.hpe.adm.octane.ideplugins.services.nonentity.ImageService;
import com.hpe.adm.octane.ideplugins.services.util.Util;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import com.intellij.ui.wizard.WizardDialog;
import com.intellij.util.ui.ConfirmationDialog;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class EntityDetailPresenter implements Presenter<EntityDetailView> {

    private static final Logger logger = Logger.getInstance(EntityDetailPresenter.class.getName());

    @Inject
    private Project project;
    @Inject
    private EntityService entityService;
    @Inject
    private MetadataService metadataService;
    @Inject
    private ImageService imageService;

    private Long entityId;
    private Entity entityType;
    private EntityModelWrapper entityModelWrapper;
    private Collection<FieldMetadata> fields;

    private EntityDetailView entityDetailView;

    private boolean stateChanged = false;

    public EntityDetailPresenter() {
    }

    public EntityDetailView getView() {
        return entityDetailView;
    }

    @Override
    @Inject
    public void setView(EntityDetailView entityDetailView) {
        this.entityDetailView = entityDetailView;
        entityDetailView.setSaveSelectedPhaseButton(new SaveAction());
        entityDetailView.setRefreshEntityButton(new EntityRefreshAction());
        entityDetailView.setOpenInBrowserButton();
        entityDetailView.setupFieldsSelectButton();
        entityDetailView.setupCommentsButton();

    }

    public void setEntity(Entity entityType, Long entityId) {
        this.entityType = entityType;
        this.entityId = entityId;

        RestUtil.runInBackground(
                () -> {
                    try {
                        fields = metadataService.getVisibleFields(entityType);

                        Set<String> requestedFields = fields.stream().map(FieldMetadata::getName).collect(Collectors.toSet());
                        entityModelWrapper = new EntityModelWrapper(entityService.findEntity(this.entityType, this.entityId, requestedFields));

                        //The subtype field is absolutely necessary, yet the server sometimes has weird ideas, and doesn't return it
                        if (entityType.isSubtype()) {
                            entityModelWrapper.setValue(new StringFieldModel(DetailsViewDefaultFields.FIELD_SUBTYPE, entityType.getSubtypeName()));
                        }

                        //change relative urls with local paths to temp and download images
                        String description = Util.getUiDataFromModel(entityModelWrapper.getValue(DetailsViewDefaultFields.FIELD_DESCRIPTION));
                        description = HtmlTextEditor.removeHtmlStructure(description);
                        try {
                            description = imageService.downloadPictures(description);
                        } catch (Exception ex) {
                            ExceptionHandler exceptionHandler = new ExceptionHandler(ex, project);
                            exceptionHandler.showErrorNotification();
                            entityDetailView.setErrorMessage(ex.getMessage());
                        }
                        entityModelWrapper.setValue(new StringFieldModel(DetailsViewDefaultFields.FIELD_DESCRIPTION, description));

                        return entityModelWrapper;
                    } catch (Exception ex) {
                        ExceptionHandler exceptionHandler = new ExceptionHandler(ex, project);
                        exceptionHandler.showErrorNotification();
                        entityDetailView.setErrorMessage(ex.getMessage());
                        return null;
                    }
                },
                (entityModelWrapper) -> {
                    if (entityModelWrapper != null) {
                        entityDetailView.setEntityModel(entityModelWrapper, fields);
                    }
                    entityModelWrapper.addFieldModelChangedHandler((e) -> {
                        stateChanged = true;
                    });
                },
                null,
                null,
                "Loading entity " + entityType.name() + ": " + entityId);
    }


    private final class EntityRefreshAction extends AnAction {
        public EntityRefreshAction() {
            super("Refresh current entity", "Refresh entity details", IconLoader.findIcon(Constants.IMG_REFRESH_ICON));
        }

        public void actionPerformed(AnActionEvent e) {
            entityDetailView.doRefresh();
            setEntity(entityType, entityId);
            stateChanged = false;
        }
    }

    private final class SaveAction extends AnAction {


        public SaveAction() {
            super("Save selected phase", "Save changes to entity phase", IconLoader.findIcon("/actions/menu-saveall.png"));
            getTemplatePresentation().setEnabled(false);
        }

        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(stateChanged);

        }

        public void actionPerformed(AnActionEvent e) {
            RestUtil.runInBackground(() -> {
                try {
                    entityService.updateEntity(entityModelWrapper.getEntityModel());
                    entityDetailView.doRefresh();
                    setEntity(entityType, entityId);
                    stateChanged = false;
                } catch (OctaneException ex) {
                    BusinessErrorReportingDialog berDialog = new BusinessErrorReportingDialog(project, ex);
                    berDialog.show();

                    switch (berDialog.getExitCode()) {
                        case BusinessErrorReportingDialog.EXIT_CODE_OPEN_IN_BROWSER: {
                            entityService.openInBrowser(entityModelWrapper.getEntityModel());
                        }
                        case BusinessErrorReportingDialog.EXIT_CODE_REFRESH: {
                            entityDetailView.doRefresh();
                            setEntity(entityType, entityId);
                            stateChanged = false;
                            break;
                        }
                        case BusinessErrorReportingDialog.EXIT_CODE_BACK:
                        default:
                    }
                }

            }, null, "Failed to save entity", "Saving entity");

        }
    }


}