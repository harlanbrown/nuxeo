/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.jdbc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for the registration of a QueryMaker.
 */
@XObject(value = "queryMaker")
public class QueryMakerDescriptor {

    private static final Log log = LogFactory.getLog(QueryMakerDescriptor.class);

    @SuppressWarnings("unchecked")
    @XNode("")
    public void setQueryMaker(String klass) {
        Class<?> qm;
        try {
            qm = Class.forName(klass.trim());
        } catch (ClassNotFoundException e) {
            log.error("No such QueryMaker class: " + klass);
            return;
        }
        if (!QueryMaker.class.isAssignableFrom(qm)) {
            log.error("No such QueryMaker class: " + klass);
            return;
        }
        queryMaker = (Class<? extends QueryMaker>) qm;
    }

    public Class<? extends QueryMaker> queryMaker;

}