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

package com.hpe.adm.octane.ideplugins.intellij.ui.detail.html;

import javax.swing.*;

public class SwingHtmlPanel extends HtmlPanel {

    private JEditorPane editorPane;

    public SwingHtmlPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        add(editorPane);
    }

    @Override
    public void setHtmlContent(String htmlContent) {
        editorPane.setText(htmlContent);
    }

    @Override
    public String getHtmlContent() {
        return editorPane.getText();
    }

}