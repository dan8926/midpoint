/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.factory.panel.SearchFilterTypeModel;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.search.BasicSearchFilterModel;
import com.evolveum.midpoint.web.component.search.SearchPropertiesConfigPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * @author honchar
 */
public class SearchFilterConfigurationPanel<O extends ObjectType> extends BasePanel<SearchFilterType> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SearchFilterConfigurationPanel.class);

    private static final String ID_ACE_EDITOR_FIELD = "aceEditorField";
    private static final String ID_CONFIGURE_BUTTON = "configureButton";

    Class<O> filterType;

    public SearchFilterConfigurationPanel(String id, IModel<SearchFilterType> model, Class<O> filterType){
        super(id, model);
        this.filterType = filterType;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        AceEditorPanel aceEditorField = new AceEditorPanel(ID_ACE_EDITOR_FIELD, null, new SearchFilterTypeModel(getModel(), getPageBase()), 10);
        aceEditorField.setOutputMarkupId(true);
        add(aceEditorField);

        AjaxLink<Void> searchConfigurationButton = new AjaxLink<Void>(ID_CONFIGURE_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                searchConfigurationPerformed(target);
            }
        };
        searchConfigurationButton.setOutputMarkupId(true);
        add(searchConfigurationButton);

    }

    private void searchConfigurationPerformed(AjaxRequestTarget target){
        SearchPropertiesConfigPanel<O> configPanel = new SearchPropertiesConfigPanel<O>(getPageBase().getMainPopupBodyId(),
                new BasicSearchFilterModel<O>(getModel(), filterType, getPageBase()), filterType){
            private static final long serialVersionUID = 1L;

            @Override
            protected void filterConfiguredPerformed(ObjectFilter configuredFilter, AjaxRequestTarget target){
                getPageBase().hideMainPopup(target);

                try {
                    if (configuredFilter == null) {
                        return;
                    }
                    SearchFilterConfigurationPanel.this.getModel().setObject(getPageBase().getPrismContext().getQueryConverter().createSearchFilterType(configuredFilter));
                    target.add(getAceEditorPanel());
                } catch (SchemaException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Cannot serialize filter", e);
                }
            }
        };
        getPageBase().showMainPopup(configPanel, target);
    }

    private AceEditorPanel getAceEditorPanel(){
        return (AceEditorPanel) get(ID_ACE_EDITOR_FIELD);
    }
}
