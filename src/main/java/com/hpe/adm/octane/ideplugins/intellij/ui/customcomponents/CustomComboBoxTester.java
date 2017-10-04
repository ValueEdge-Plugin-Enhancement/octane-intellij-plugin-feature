/*
 * Copyright 2017 Hewlett-Packard Enterprise Development Company, L.P.
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

package com.hpe.adm.octane.ideplugins.intellij.ui.customcomponents;

import com.google.inject.Inject;
import com.hpe.adm.nga.sdk.model.EntityModel;
import com.hpe.adm.octane.ideplugins.services.EntityService;
import com.hpe.adm.octane.ideplugins.services.exception.ServiceException;
import com.hpe.adm.octane.ideplugins.services.filtering.Entity;
import com.hpe.adm.octane.ideplugins.services.util.Util;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

public class CustomComboBoxTester extends JFrame {

    @Inject
    private EntityService entityService;

    public CustomComboBoxTester() {
        super("Demo program for custom combobox");
        setLayout(new FlowLayout());

        PhaseComboBox customCombobox = new PhaseComboBox();
        customCombobox.setPreferredSize(new Dimension(120, 30));
        customCombobox.setEditable(true);
        Long entityId = 1072L;

        Collection<EntityModel> phaseList = null;
        try {
            EntityModel testEntityModel = entityService.findEntity(Entity.DEFECT, entityId);
            String currentPhaseId = Util.getUiDataFromModel(testEntityModel.getValue("phase"), "id");
            phaseList = entityService.findPossibleTransitionFromCurrentPhase(Entity.DEFECT, currentPhaseId);
        } catch (ServiceException e) {
            e.printStackTrace();
        }

        customCombobox.addItems(phaseList);

        add(customCombobox);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 100);
        setLocationRelativeTo(null);    // center on screen
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new CustomComboBoxTester().setVisible(true);
            }
        });
    }
}