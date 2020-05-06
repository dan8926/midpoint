/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerValueWrapper;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * @author katka
 *
 */
public class PrismContainerHeaderPanel<C extends Containerable> extends ItemHeaderPanel<PrismContainerValue<C>, PrismContainer<C>, PrismContainerDefinition<C>, PrismContainerWrapper<C>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_EXPAND_COLLAPSE_BUTTON = "expandCollapseButton";


    public PrismContainerHeaderPanel(String id, IModel<PrismContainerWrapper<C>> model) {
        super(id, model);
    }

    @Override
    protected void initButtons() {

        super.initButtons();
        initExpandCollapseButton();
            //TODO: sorting
    }


    @Override
    protected Component createTitle(IModel<String> label) {
        AjaxButton labelComponent = new AjaxButton(ID_LABEL, label) {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                onExpandClick(target);
            }
        };
        labelComponent.setOutputMarkupId(true);
        labelComponent.add(AttributeAppender.append("style", "cursor: pointer;"));
        return labelComponent;
    }


    protected void initExpandCollapseButton() {
        ToggleIconButton<?> expandCollapseButton = new ToggleIconButton<Void>(ID_EXPAND_COLLAPSE_BUTTON,
                GuiStyleConstants.CLASS_ICON_EXPAND_CONTAINER, GuiStyleConstants.CLASS_ICON_COLLAPSE_CONTAINER) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onExpandClick(target);
            }

            @Override
            public boolean isOn() {
                return PrismContainerHeaderPanel.this.getModelObject() != null && PrismContainerHeaderPanel.this.getModelObject().isExpanded();
            }
        };
        expandCollapseButton.setOutputMarkupId(true);
        add(expandCollapseButton);
    }

    protected void onExpandClick(AjaxRequestTarget target) {
    }

    @Override
    protected PrismContainerValue<C> createNewValue(PrismContainerWrapper<C> parent) {
        return parent.getItem().createNewValue();
    }

    @Override
    protected void refreshPanel(AjaxRequestTarget target) {

    }

    @Override
    protected boolean isButtonEnabled() {
        return super.isButtonEnabled() && getModelObject().isExpanded();
    }

    @Override
    protected boolean isAddButtonVisible() {
        return super.isAddButtonVisible() && getModelObject().isExpanded();
    }
}
