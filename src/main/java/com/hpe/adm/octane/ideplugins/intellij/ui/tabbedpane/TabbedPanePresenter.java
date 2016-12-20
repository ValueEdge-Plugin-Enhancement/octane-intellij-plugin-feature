package com.hpe.adm.octane.ideplugins.intellij.ui.tabbedpane;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.hpe.adm.octane.ideplugins.intellij.settings.IdePluginPersistentState;
import com.hpe.adm.octane.ideplugins.intellij.ui.Presenter;
import com.hpe.adm.octane.ideplugins.intellij.ui.detail.EntityDetailPresenter;
import com.hpe.adm.octane.ideplugins.intellij.ui.entityicon.EntityIconFactory;
import com.hpe.adm.octane.ideplugins.intellij.ui.treetable.EntityTreeTablePresenter;
import com.hpe.adm.octane.ideplugins.intellij.util.Constants;
import com.hpe.adm.octane.ideplugins.services.filtering.Entity;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.TabsListener;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class TabbedPanePresenter implements Presenter<TabbedPaneView> {

    private static EntityIconFactory entityIconFactory = new EntityIconFactory(20, 20, 10, Color.WHITE);

    @Inject
    TabbedPaneView tabbedPaneView;

    @Inject
    private Provider<EntityDetailPresenter> entityDetailPresenterProvider;

    @Inject
    private Provider<EntityTreeTablePresenter> entityTreeTablePresenterProvider;

    private BiMap<DetailTabKey, TabInfo> detailTabInfo = HashBiMap.create();

    @Inject
    private IdePluginPersistentState idePluginPersistentState;

    public EntityTreeTablePresenter openMyWorkTab() {
        EntityTreeTablePresenter presenter = entityTreeTablePresenterProvider.get();
        Icon myWorkIcon = IconLoader.findIcon(Constants.IMG_MYWORK);
        tabbedPaneView.addTab(Constants.TAB_MY_WORK_TITLE, null, myWorkIcon, presenter.getView().getComponent(), false);
        return presenter;
    }

    public void openDetailTab(DetailTabKey tabKey) {
        openDetailTab(tabKey.getEntityType(), tabKey.getEntityId(), tabKey.getEntityName());
    }

    public void openDetailTab(Entity entityType, Long entityId, String entityName) {
        EntityDetailPresenter presenter = entityDetailPresenterProvider.get();
        presenter.setEntity(entityType, entityId);
        DetailTabKey tabKey = new DetailTabKey(entityId, entityName, entityType);

        ImageIcon tabIcon = new ImageIcon(entityIconFactory.getIconAsImage(entityType));

        TabInfo tabInfo = tabbedPaneView.addTab(
                String.valueOf(entityId),
                entityName,
                tabIcon,
                presenter.getView().getComponent());

        detailTabInfo.put(tabKey, tabInfo);

        //saveDetailTabsToPersistentState();
    }

    public TabbedPaneView getView() {
        return tabbedPaneView;
    }

    @Override
    @Inject
    public void setView(TabbedPaneView tabbedPaneView) {
        this.tabbedPaneView = tabbedPaneView;

        //open test entity tree view
        EntityTreeTablePresenter presenter = openMyWorkTab();
        initHandlers(presenter);

        loadDetailTabsFromPersistentState();
    }

    private void initHandlers(EntityTreeTablePresenter presenter){

        //TODO atoth: only save once at the end
        tabbedPaneView.addTabsListener(new TabsListener() {
            @Override
            public void selectionChanged(TabInfo oldSelection, TabInfo newSelection) {}

            @Override
            public void beforeSelectionChanged(TabInfo oldSelection, TabInfo newSelection) {}

            @Override
            public void tabRemoved(TabInfo tabToRemove) {
                saveDetailTabsToPersistentState();
            }

            @Override
            public void tabsMoved() {
                saveDetailTabsToPersistentState();
            }
        });

        presenter.addEntityClickHandler((mouseEvent, entityType, entityId, model) -> {

            //Need the name for the tab tooltip
            String entityName = model.getValue("name").getValue().toString();
            DetailTabKey tabKey = new DetailTabKey(entityId, entityName, entityType);

            //double click
            if(SwingUtilities.isLeftMouseButton(mouseEvent) && mouseEvent.getClickCount() == 2){
                if(isDetailTabAlreadyOpen(tabKey)){
                    selectDetailTab(tabKey);
                } else {
                    openDetailTab(entityType, entityId, entityName);
                    selectDetailTab(tabKey);
                }
            }

            //Middle click
            else if(SwingUtilities.isMiddleMouseButton(mouseEvent)){
                if(!isDetailTabAlreadyOpen(tabKey)){
                    openDetailTab(entityType, entityId, entityName);
                }
            }
        });

        presenter.addEntityKeyHandler((event, selectedEntityType, selectedEntityId, model) -> {
            if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                //Need the name for the tab tooltip
                String entityName = model.getValue("name").getValue().toString();
                DetailTabKey tabKey = new DetailTabKey(selectedEntityId, entityName, selectedEntityType);

                if(isDetailTabAlreadyOpen(tabKey)){
                    selectDetailTab(tabKey);
                } else {
                    openDetailTab(selectedEntityType, selectedEntityId, entityName);
                    selectDetailTab(tabKey);
                }
            }
        });
    }

    private void selectDetailTab(DetailTabKey tabKey){
        tabbedPaneView.selectTabWithTabInfo(detailTabInfo.get(tabKey), false);
    }

    private boolean isDetailTabAlreadyOpen(DetailTabKey tabKey){
        return detailTabInfo.containsKey(tabKey) && tabbedPaneView.hasTabWithTabInfo(detailTabInfo.get(tabKey));
    }

    private void loadDetailTabsFromPersistentState(){
        JSONObject jsonObject =  idePluginPersistentState.loadState(IdePluginPersistentState.Key.OPEN_TABS);
        if(jsonObject == null) return;

        JSONArray jsonArray = jsonObject.getJSONArray("openDetailTabs");
        for(int i = 0; i<jsonArray.length(); i++){
            JSONObject obj = jsonArray.getJSONObject(i);
            openDetailTab(DetailTabKey.fromJsonObject(obj));
        }
    }

    private void saveDetailTabsToPersistentState(){
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        tabbedPaneView.getTabInfos()
                .stream()
                .filter(detailTabInfo::containsValue)
                .forEach(tabInfo -> {
                    jsonArray.put(DetailTabKey.toJsonObject(detailTabInfo.inverse().get(tabInfo)));
                });

        jsonObject.put("openDetailTabs", jsonArray);
        idePluginPersistentState.saveState(IdePluginPersistentState.Key.OPEN_TABS, jsonObject);
    }

}

