/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.app;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.nuxeo.ecm.webengine.ApplicationManager;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.io.BlobWriter;
import org.nuxeo.ecm.webengine.model.io.FileWriter;
import org.nuxeo.ecm.webengine.model.io.ScriptFileWriter;
import org.nuxeo.ecm.webengine.model.io.TemplateViewWriter;
import org.nuxeo.ecm.webengine.model.io.TemplateWriter;
import org.nuxeo.ecm.webengine.model.io.URLWriter;
import org.nuxeo.runtime.api.Framework;

/**
 * A composite application that aggregate all webengine deployed modules as a single JAX-RS application.
 * Should be used as the value of the <code>javax.ws.rs.Application</code> init-param of JAX-RS servlet in the web.xml file. 
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngineApplication extends Application {

    protected WebEngine engine;
    
    public WebEngineApplication() {
        engine = Framework.getLocalService(WebEngine.class);
    }
    
    @Override
    public Set<Class<?>> getClasses() {
        ApplicationManager apps = engine.getApplicationManager();
        HashSet<Class<?>> result = new HashSet<Class<?>>();
        for (Application app : apps.getApplications()) {
            result.addAll(app.getClasses());
        }
        return result;
    }
    
    @Override
    public Set<Object> getSingletons() {
        ApplicationManager apps = engine.getApplicationManager();
        HashSet<Object> result = new HashSet<Object>();
        result.add(new WebEngineExceptionMapper());
        result.add(new TemplateWriter());
        result.add(new ScriptFileWriter());
        result.add(new BlobWriter());
        result.add(new FileWriter());
        result.add(new URLWriter());
        result.add(new TemplateViewWriter());
        for (Application app : apps.getApplications()) {
            result.addAll(app.getSingletons());
        }
        return result;
    }

}