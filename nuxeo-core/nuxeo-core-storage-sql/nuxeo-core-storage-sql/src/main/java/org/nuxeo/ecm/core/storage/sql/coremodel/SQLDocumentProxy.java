/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.util.Collection;
import java.util.Collections;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.lifecycle.LifeCycle;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.DocumentVersionProxy;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.ecm.core.versioning.DocumentVersion;

/**
 * A proxy extends {@link SQLDocumentVersion}.
 *
 * @author Florent Guillaume
 */
public class SQLDocumentProxy extends SQLDocumentVersion implements
        DocumentVersionProxy {

    private final Node proxyNode;

    private SQLDocumentVersion version;

    protected SQLDocumentProxy(Node proxyNode, Node versionNode,
            ComplexType type, SQLSession session, boolean readonly)
            throws DocumentException {
        super(versionNode, type, session, readonly);
        version = new SQLDocumentVersion(versionNode, type, session, true);
        this.proxyNode = proxyNode;
    }

    /*
     * ----- DocumentVersionProxy -----
     */

    public Document getTargetDocument() {
        return version;
    }

    public void setTargetDocument(Document document, String label) {
        try {
            Node node = ((SQLDocument) document).getHierarchyNode();
            Document versionDocument = session.getVersionByLabel(node, label);
            Node versionNode = ((SQLDocument) versionDocument).getHierarchyNode();
            super.setHierarchyNode(versionNode);
            version = new SQLDocumentVersion(versionNode, version.getType(), session, true);
            proxyNode.setSingleProperty(Model.PROXY_TARGET_PROP, versionNode.getId());
            proxyNode.setSingleProperty(Model.PROXY_VERSIONABLE_PROP, node.getId());
        } catch (DocumentException e) {
            throw new UnsupportedOperationException();
        } catch (StorageException e) {
            throw new UnsupportedOperationException();
        }
    }

    public DocumentVersion getTargetVersion() {
        return version;
    }

    // API unused
    public void updateToBaseVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getAllowedStateTransitions() {
        try {
            LifeCycleService service = NXCore.getLifeCycleService();
            if (service == null) {
                throw new LifeCycleException("LifeCycleService not available");
            }
            LifeCycle lifeCycle = service.getLifeCycleFor(this);
            if (lifeCycle == null) {
                return Collections.emptyList();
            }
            return lifeCycle.getAllowedStateTransitionsFrom(getCurrentLifeCycleState());
        } catch (LifeCycleException e) {
            return super.getAllowedStateTransitions();
        }
    }

    /*
     * ----- proxy-specific overrides -----
     */

    @Override
    public boolean isProxy() {
        return true;
    }

    @Override
    public boolean isVersion() {
        return false;
    }

    @Override
    protected Node getHierarchyNode() {
        return proxyNode;
    }

    @Override
    public Document getSourceDocument() {
        return version;
    }

    @Override
    public String getPath() throws DocumentException {
        return session.getPath(proxyNode);
    }

    @Override
    public Document getParent() throws DocumentException {
        return session.getParent(proxyNode);
    }

    /*
     * ----- equals/hashcode -----
     */

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof SQLDocumentProxy) {
            return equals((SQLDocumentProxy) other);
        }
        return false;
    }

    private boolean equals(SQLDocumentProxy other) {
        return proxyNode.getId() == other.proxyNode.getId();
    }

    @Override
    public int hashCode() {
        return proxyNode.getId().hashCode();
    }
}