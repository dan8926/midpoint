/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

import java.io.Serializable;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Almost the same as Runnable but this one can throw CommonException and is serializable.
 */
@Experimental
@FunctionalInterface
public interface CheckedRunnable extends Serializable {
    void run() throws CommonException;
}
