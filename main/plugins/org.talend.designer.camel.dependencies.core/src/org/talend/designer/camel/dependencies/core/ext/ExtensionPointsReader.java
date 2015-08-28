package org.talend.designer.camel.dependencies.core.ext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

public class ExtensionPointsReader {

    private static final String IMPORT_PACKAGE = "importPackage"; //$NON-NLS-1$
    private static final String REQUIRE_BUNDLE = "requireBundle"; //$NON-NLS-1$
    private static final String NAME = "name"; //$NON-NLS-1$
	private static final String COMPONENT = "component"; //$NON-NLS-1$
	private static final String ATTRIBUTE_VALUE = "attributeValue"; //$NON-NLS-1$
	private static final String PREDICATE = "predicate"; //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME = "attributeName"; //$NON-NLS-1$
    private static final String PARAMETER = "parameter"; //$NON-NLS-1$
	private static final String OPTIONAL = "optional"; //$NON-NLS-1$
	private static final String COMPONENT_NAME = "componentName"; //$NON-NLS-1$

	private static final String REQUIRE_BUNDLE_EXT = "org.talend.designer.camel.dependencies.requireBundle"; //$NON-NLS-1$
	private static final String IMPORT_PACKAGE_EXT = "org.talend.designer.camel.dependencies.importPackage"; //$NON-NLS-1$
	private static final String BUNDLE_CLASSPATH_EXT = "org.talend.designer.camel.dependencies.bundleClasspath"; //$NON-NLS-1$

	public static ExtensionPointsReader INSTANCE = new ExtensionPointsReader();

    private final Map<String, Collection<ExBundleClasspath>> componentBundleClasspaths =
        new HashMap<String, Collection<ExBundleClasspath>>();
    /*
     * for languages, please check init(INode, INode, EConnectionType, String, String, String, boolean)
     * of org.talend.designer.core.ui.editor.connections.Connection, and see the EConnectionType.ROUTE_WHEN case
     */
//    String[] languages = { "constant", "el", "groovy", "header", "javaScript", "jxpath", "mvel", "ognl", "php", "property",
//            "python", "ruby", "simple", "spel", "sql", "xpath", "xquery" };
    private final Map<String, Collection<ExImportPackage>> componentImportPackages =
        new HashMap<String, Collection<ExImportPackage>>();
    private final Map<String, Collection<ExRequireBundle>> componentRequireBundles =
        new HashMap<String, Collection<ExRequireBundle>>();
    private final Collection<ExRequireBundle> requireBundlesForAll = new ArrayList<ExRequireBundle>();
    private final Collection<ExImportPackage> importPackagesForAll = new ArrayList<ExImportPackage>();

	private ExtensionPointsReader() {
        readRegisteredBundleClasspaths();
        readRegisteredImportPackages();
        readRegisteredRequireBundles();
	}

    private void readRegisteredBundleClasspaths() {
        final IConfigurationElement[] configurationElements = Platform.getExtensionRegistry()
            .getConfigurationElementsFor(BUNDLE_CLASSPATH_EXT);
        for (IConfigurationElement e : configurationElements) {
            final String cmpName = e.getAttribute(NAME);
            final ExBundleClasspath bc =
                new ExBundleClasspath(e.getAttribute(PARAMETER), Boolean.parseBoolean(e.getAttribute(OPTIONAL)));
            parsePredicates(bc, e);

            Collection<ExBundleClasspath> attributeSet = componentBundleClasspaths.get(cmpName);
            if (attributeSet == null) {
                attributeSet = new ArrayList<ExBundleClasspath>();
                componentBundleClasspaths.put(cmpName, attributeSet);
            }
            attributeSet.add(bc);
        }
    }

    private void readRegisteredImportPackages() {
        final IConfigurationElement[] configurationElements =
            Platform.getExtensionRegistry().getConfigurationElementsFor(IMPORT_PACKAGE_EXT);
        for (IConfigurationElement e : configurationElements) {
            final String name = e.getName();
            if (COMPONENT.equals(name)) {
                final String cmpName = e.getAttribute(COMPONENT_NAME);
                Collection<ExImportPackage> packageSet = componentImportPackages.get(cmpName);
                if (packageSet == null) {
                    packageSet = new ArrayList<ExImportPackage>();
                    componentImportPackages.put(cmpName, packageSet);
                }
                for (IConfigurationElement p : e.getChildren(IMPORT_PACKAGE)) {
                    packageSet.add(createImportPackageFrom(p));
                }
            } else {
                importPackagesForAll.add(createImportPackageFrom(e));
            }
        }
    }

    private ExImportPackage createImportPackageFrom(final IConfigurationElement p) {
        final ExImportPackage importPackage = new ExImportPackage(
            p.getAttribute(NAME), Boolean.parseBoolean(p.getAttribute(OPTIONAL)));
        parsePredicates(importPackage, p);
        return importPackage;
    }

    private void readRegisteredRequireBundles() {
        final IConfigurationElement[] configurationElements =
            Platform.getExtensionRegistry().getConfigurationElementsFor(REQUIRE_BUNDLE_EXT);
        for (IConfigurationElement e : configurationElements) {
            final String name = e.getName();
            if (COMPONENT.equals(name)) {
                final String cmpName = e.getAttribute(COMPONENT_NAME);
                Collection<ExRequireBundle> bundleSet = componentRequireBundles.get(cmpName);
                if (bundleSet == null) {
                    bundleSet = new ArrayList<ExRequireBundle>();
                    componentRequireBundles.put(cmpName, bundleSet);
                }
                for (IConfigurationElement b : e.getChildren(REQUIRE_BUNDLE)) {
                    bundleSet.add(createRequireBundleFrom(b));
                }
            } else {
                requireBundlesForAll.add(createRequireBundleFrom(e));
            }
        }
    }

    private ExRequireBundle createRequireBundleFrom(final IConfigurationElement b) {
        final ExRequireBundle requireBundle = new ExRequireBundle(
            b.getAttribute(NAME), Boolean.parseBoolean(b.getAttribute(OPTIONAL)));
        parsePredicates(requireBundle, b);
        return requireBundle;
    }

    private static void parsePredicates(final AbstractExPredicator<?> abstractExPredicator,
        final IConfigurationElement element) {
        for (final IConfigurationElement pe : element.getChildren(PREDICATE)) {
            abstractExPredicator.addPredicate(pe.getAttribute(ATTRIBUTE_NAME), pe.getAttribute(ATTRIBUTE_VALUE));
        }
    }

    public Collection<ExImportPackage> getImportPackagesForAll() {
        return importPackagesForAll;
    }

    public Collection<ExRequireBundle> getRequireBundlesForAll() {
        return requireBundlesForAll;
    }

    public Collection<ExBundleClasspath> getBundleClasspathsForComponent(String name) {
        return componentBundleClasspaths.get(name);
    }

    public Collection<ExImportPackage> getImportPackagesForComponent(String name) {
        return componentImportPackages.get(name);
    }

    public Collection<ExRequireBundle> getRequireBundlesForComponent(String name) {
        return componentRequireBundles.get(name);
    }

}
