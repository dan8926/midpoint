/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import java.util.Map;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition.Scope;
import com.evolveum.axiom.api.AxiomValueIdentifier;


interface IdentifierSpaceHolder {

    void register(AxiomName space, Scope scope, AxiomValueIdentifier key, ValueContext<?> context);

    public ValueContext<?> lookup(AxiomName space, AxiomValueIdentifier key);

    Map<AxiomValueIdentifier, ValueContext<?>> space(AxiomName space);
}
