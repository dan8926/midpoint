/*
 * Copyright (c) 2011 Evolveum
 * 
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.ucf.impl;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConfigurationProperty;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.identityconnectors.framework.api.ConnectorKey;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.object.ObjectResolver;
import com.evolveum.midpoint.common.object.ObjectTypeUtil;
import com.evolveum.midpoint.common.object.ResourceTypeUtil;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.provisioning.ucf.api.ObjectNotFoundException;
import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.util.ClasspathUrlFinder;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

/**
 * Currently the only implementation of the UCF Connector Manager API interface.
 * 
 * It is hardcoded to ICF now.
 * 
 * This class holds a list of all known ICF connectors in the system.
 * 
 * @author Radovan Semancik
 */
@Component
public class ConnectorFactoryIcfImpl implements ConnectorFactory {

	public static final String ICF_FRAMEWORK_URI = "http://midpoint.evolveum.com/xml/ns/connector/icf-1";
	
	// This usually refers to WEB-INF/lib/icf-connectors
	private static final String BUNDLE_PATH = "../../lib/icf-connectors";
	private static final String BUNDLE_PREFIX = "org.identityconnectors";
	private static final String BUNDLE_SUFFIX = ".jar";
	private static final String ICF_CONFIGURATION_NAMESPACE_PREFIX = ICF_FRAMEWORK_URI + "/bundle/";
	private static final Trace LOGGER = TraceManager.getTrace(ConnectorFactoryIcfImpl.class);
	private static final String CONNECTOR_IDENTIFIER_SEPARATOR = "/";
	private ConnectorInfoManager localConnectorInfoManager;
	private Map<String, ConnectorInfo> connectors;

	public ConnectorFactoryIcfImpl() {
	}

	/**
	 * Initialize the ICF implementation. Look for all connector bundles,
	 * get basic information about them and keep that in memory.
	 */
	@PostConstruct
	public void initialize() {
		Set<URL> bundleURLs = listBundleJars();

		connectors = new HashMap<String, ConnectorInfo>();
		List<ConnectorInfo> connectorInfos = getLocalConnectorInfoManager().getConnectorInfos();
		for (ConnectorInfo connectorInfo : connectorInfos) {
			ConnectorKey key = connectorInfo.getConnectorKey();
			String mapKey = keyToNamespaceSuffix(key);
			connectors.put(mapKey, connectorInfo);
		}
	}

	/**
	 * Creates new connector instance.
	 * 
	 * It will initialize the connector by taking the XML Resource definition,
	 * transforming it to the ICF configuration and applying that to the 
	 * new connector instance.
	 * @throws ObjectNotFoundException 
	 * 
	 */
	@Override
	public ConnectorInstance createConnectorInstance(ConnectorType connectorType, String namespace) throws ObjectNotFoundException {
		
		ConnectorInfo cinfo = getConnectorInfo(connectorType);
		
		if (cinfo==null) {
			throw new ObjectNotFoundException("The connector "+ObjectTypeUtil.toShortString(connectorType)+" was not found");
		}
		
		// Create new midPoint ConnectorInstance and pass it the ICF connector facade
		ConnectorInstanceIcfImpl connectorImpl = new ConnectorInstanceIcfImpl(cinfo, connectorType, namespace);

		return connectorImpl;
	}

	/**
	 * Returns a list XML representation of the ICF connectors.
	 */
	@Override
	public Set<ConnectorType> listConnectors() {
		Set<ConnectorType> connectorTypes = new HashSet<ConnectorType>();
		for (Map.Entry<String, ConnectorInfo> e : connectors.entrySet()) {
			ConnectorInfo cinfo = e.getValue();
			ConnectorType connectorType = convertToConnectorType(cinfo);
			connectorTypes.add(connectorType);
		}
		return connectorTypes;
	}

	/**
	 * Converts ICF ConnectorInfo into a midPoint XML connector representation.
	 * 
	 * TODO: schema transformation
	 */
	private ConnectorType convertToConnectorType(ConnectorInfo cinfo) {
		ConnectorType connectorType = new ConnectorType();
		ConnectorKey key = cinfo.getConnectorKey();
		String stringID = keyToNamespaceSuffix(key);
		connectorType.setName("ICF " + key.getConnectorName());
		connectorType.setFramework(ICF_FRAMEWORK_URI);
		connectorType.setConnectorType(key.getConnectorName());
		connectorType.setNamespace(ICF_CONFIGURATION_NAMESPACE_PREFIX + stringID);
		connectorType.setConnectorVersion(key.getBundleVersion());
		connectorType.setConnectorBundle(key.getBundleName());
		return connectorType;
	}

	/**
	 * Converts ICF connector key to a simple string.
	 * 
	 * The string may be used as an OID.
	 */
	private String keyToNamespaceSuffix(ConnectorKey key) {
		StringBuilder sb = new StringBuilder();
		sb.append(key.getBundleName());
		sb.append(CONNECTOR_IDENTIFIER_SEPARATOR);
		// Don't include version. It is lesser evil.
//		sb.append(key.getBundleVersion());
//		sb.append(CONNECTOR_IDENTIFIER_SEPARATOR);
		sb.append(key.getConnectorName());
		return sb.toString();
	}


	/**
	 * Returns ICF connector info manager that manages local connectors.
	 * The manager will be created if it does not exist yet.
	 * 
	 * @return ICF connector info manager that manages local connectors
	 */
	private ConnectorInfoManager getLocalConnectorInfoManager() {
		if (null == localConnectorInfoManager) {
			Set<URL> bundleUrls = listBundleJars();
			ConnectorInfoManagerFactory factory = ConnectorInfoManagerFactory.getInstance();
			localConnectorInfoManager = factory.getLocalManager(bundleUrls.toArray(new URL[0]));
		}
		return localConnectorInfoManager;
	}

	/**
	 * Lists all ICF connector bundles, either in BUNDLE_PATH or in current classpath.
	 * 
	 * @return set of connector bundle URLs.
	 */
	private Set<URL> listBundleJars() {
		Set<URL> bundleURLs = new HashSet<URL>();

		// Look for connectors in the BUNDLE_PATH folder

		File icfFolder = null;
		try {
			icfFolder = new File(new File(this.getClass().getClassLoader().getResource("com").toURI()),
					BUNDLE_PATH);
		} catch (URISyntaxException ex) {
			LOGGER.debug("Couldn't find icf-connectors folder, reason: " + ex.getMessage());
		}

		// Take only those that start with BUNDLE_PREFIX and end with BUNDLE_SUFFIX

		final FileFilter fileFilter = new FileFilter() {

			@Override
			public boolean accept(File file) {
				if (!file.exists() || file.isDirectory()) {
					return false;
				}
				String fileName = file.getName();
				if (fileName.startsWith(BUNDLE_PREFIX) && fileName.endsWith(BUNDLE_SUFFIX)) {
					return true;
				}
				return false;
			}
		};

		if (icfFolder == null || !icfFolder.exists() || !icfFolder.isDirectory()) {
			// cannot find icfFolder, therefore we are going to seach classpath
			// this is useful in tests
			URL[] resourceURLs = ClasspathUrlFinder.findClassPaths();
			for (int j = 0; j < resourceURLs.length; j++) {
				URL bundleUrl = resourceURLs[j];
				if ("file".equals(bundleUrl.getProtocol())) {
					File file = new File(bundleUrl.getFile());
					if (fileFilter.accept(file)) {
						bundleURLs.add(bundleUrl);
					}
				}
			}
		} else {
			// Looking in icfFolder
			File[] connectors = icfFolder.listFiles(fileFilter);
			for (File file : connectors) {
				try {
					bundleURLs.add(file.toURI().toURL());
				} catch (MalformedURLException ex) {
					LOGGER.debug("Couldn't transform file path " + file.getAbsolutePath()
							+ " to URL, reason: " + ex.getMessage());
				}
			}
		}

		if (LOGGER.isDebugEnabled()) {
			for (URL u : bundleURLs) {
				LOGGER.debug("Bundle URL: {}", u);
			}
		}

		return bundleURLs;
	}

	private ConnectorInfo getConnectorInfo(ConnectorType connectorType) throws ObjectNotFoundException {
		if (!ICF_FRAMEWORK_URI.equals(connectorType.getFramework())) {
			throw new ObjectNotFoundException("Requested connector for framework "+connectorType.getFramework()+" cannot be found in framework "+ICF_FRAMEWORK_URI);
		}
		ConnectorKey key = getConnectorKey(connectorType);
		return getLocalConnectorInfoManager().findConnectorInfo(key);
	}
	
	private ConnectorKey getConnectorKey(ConnectorType connectorType) {
		return new ConnectorKey(connectorType.getConnectorBundle(),
				connectorType.getConnectorVersion(),connectorType.getConnectorType());
	}
	
}
