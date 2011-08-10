// ============================================================================
//
// Copyright (C) 2006-2011 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.camel.spring.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.talend.camel.core.model.camelProperties.CamelProcessItem;
import org.talend.camel.core.model.camelProperties.CamelPropertiesFactory;
import org.talend.camel.designer.ui.editor.CamelMultiPageTalendEditor;
import org.talend.camel.designer.ui.editor.CamelProcessEditorInput;
import org.talend.camel.designer.util.CamelRepositoryNodeType;
import org.talend.camel.designer.util.ECamelCoreImage;
import org.talend.commons.exception.LoginException;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.runtime.exception.ExceptionHandler;
import org.talend.commons.ui.runtime.image.ImageProvider;
import org.talend.commons.utils.VersionUtils;
import org.talend.core.CorePlugin;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.model.properties.PropertiesFactory;
import org.talend.core.model.properties.Property;
import org.talend.core.model.relationship.RelationshipItemBuilder;
import org.talend.designer.camel.spring.core.CamelSpringParser;
import org.talend.designer.camel.spring.ui.listeners.SpringParserListener;
import org.talend.designer.core.DesignerPlugin;
import org.talend.designer.core.model.utils.emf.talendfile.ProcessType;
import org.talend.designer.core.model.utils.emf.talendfile.TalendFileFactory;
import org.talend.repository.RepositoryWorkUnit;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.IRepositoryNode;
import org.talend.repository.model.RepositoryNodeUtilities;
import org.talend.repository.ui.views.IRepositoryView;

/**
 * Wizard for the creation of a new project. <br/>
 * 
 * $Id: ImportSpringXMLWizard.java 52559 2010-12-13 04:14:06Z $
 * 
 */
public class ImportSpringXMLWizard extends Wizard {

    /** Main page. */
    private ImportSpringXMLWizardPage mainPage;

    /** Created project. */
    private CamelProcessItem processItem;

    private Property property;

    private IPath path;

    private IProxyRepositoryFactory repositoryFactory;

    private String springXMLPath;

    private IPath destinationPath;
    
    private boolean created;
    /**
     * Constructs a new NewProjectWizard.
     * 
     * @param author Project author.
     * @param server
     * @param password
     */
    public ImportSpringXMLWizard(IPath path) {
        super();
        this.path = path;
        this.created = false;
        initialProcess();
        this.repositoryFactory = DesignerPlugin.getDefault().getRepositoryService().getProxyRepositoryFactory();
        this.setDefaultPageImageDescriptor(ImageProvider.getImageDesc(ECamelCoreImage.ROUTES_WIZ));
        this.setHelpAvailable(false);
        this.setNeedsProgressMonitor(true);
        
    }

    /**
     * @see org.eclipse.jface.wizard.Wizard#addPages()
     */
    @Override
    public void addPages() {
        mainPage = new ImportSpringXMLWizardPage(property, path);
        addPage(mainPage);
        setWindowTitle("Import Routes From Spring XML");
    }

    /**
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
       
        try {
            getContainer().run(false, false, new IRunnableWithProgress() {

                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Import Spring XML...", 3);
                    monitor.worked(1);
                    try {
                        createNewItem();
                        monitor.worked(1);
                        parsrXMLFile();
                        openEditor();
                        monitor.done();
                    } catch (Exception e) {
                        monitor.done();
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e1) {
            ExceptionHandler.process(e1);
            MessageDialog
                    .openError(getShell(), "Error", "Import Spring XML failed, details: \n" + handle(e1));
            return false;
        } catch (InterruptedException e1) {
            ExceptionHandler.process(e1);
            MessageDialog.openError(getShell(), "Error", "Import Spring XML failed, details: " + e1.getMessage());
            return false;
        }
       
        return processItem != null;
    }

    private String handle(InvocationTargetException e) {
        Throwable targetException = e.getTargetException();
        String message = targetException.getMessage();
        if(message == null){
            return targetException.toString();
        }
        if(message.length() < 500){
            return message;
        }else{
            message = message.substring(0, 200) + "......";
        }
        return message;
    }

    private void createNewItem() {
        
        if(created){
            return;
        }
        
        springXMLPath = mainPage.getXMLPath();
        destinationPath = mainPage.getDestinationPath();
        property.setId(repositoryFactory.getNextId());
        ProcessType process = TalendFileFactory.eINSTANCE.createProcessType();
        processItem.setProcess(process);
        RepositoryWorkUnit<Object> workUnit = new RepositoryWorkUnit<Object>(ImportSpringXMLWizard.this
                .getWindowTitle(), this) {
            @Override
            protected void run() throws LoginException, PersistenceException {
                repositoryFactory.create(processItem, destinationPath);
                RelationshipItemBuilder.getInstance().addOrUpdateItem(processItem);
            }
        };
        workUnit.setAvoidUnloadResources(true);
        repositoryFactory.executeRepositoryWorkUnit(workUnit);
        created = true;
    }
    
    @Override
    public boolean performCancel() {
        deleteItem();
        return true;
    }

    /**
     * Delete created item if import fails.
     */
    private void deleteItem() {
        
        if(!created){
            return;
        }
        
        getViewPart().refresh(CamelRepositoryNodeType.repositoryRoutesType);
        IRepositoryNode repositoryNode = RepositoryNodeUtilities.getRepositoryNode(processItem.getProperty().getId(), false);
        try {
            repositoryFactory.deleteObjectPhysical(repositoryNode.getObject());
            getViewPart().refresh(repositoryNode.getObjectType());
        } catch (PersistenceException pe) {
            // ignore
        }

    }

    private void initialProcess() {
        this.property = PropertiesFactory.eINSTANCE.createProperty();
        this.property.setAuthor(((RepositoryContext) CorePlugin.getContext().getProperty(Context.REPOSITORY_CONTEXT_KEY))
                .getUser());
        this.property.setVersion(VersionUtils.DEFAULT_VERSION);
        this.property.setStatusCode(""); //$NON-NLS-1$
        this.processItem = CamelPropertiesFactory.eINSTANCE.createCamelProcessItem();
        this.processItem.setProperty(property);
        
    }

    /**
     * Open editor. DOC LiXP Comment method "openEditor".
     * 
     * @throws PersistenceException
     * @throws PartInitException
     */
    private void openEditor() throws PersistenceException, PartInitException {
        repositoryFactory.save(processItem, true);
        CamelProcessEditorInput fileEditorInput;
        // Set readonly to false since created job will always be editable.
        fileEditorInput = new CamelProcessEditorInput(processItem, true, true, false);
        fileEditorInput.setView(getViewPart());
        IRepositoryNode repositoryNode = RepositoryNodeUtilities.getRepositoryNode(fileEditorInput.getItem().getProperty()
                .getId(), false);
        fileEditorInput.setRepositoryNode(repositoryNode);
        IWorkbenchPage page = getViewPart().getViewSite().getPage();
        page.openEditor(fileEditorInput, CamelMultiPageTalendEditor.ID, true);

    }

    /**
     * 
     * DOC LiXP Comment method "parseAndImportContent".
     * 
     * @throws Exception
     */
    private void parsrXMLFile() throws Exception {
        CamelSpringParser parser = new CamelSpringParser();
        parser.addListener(new SpringParserListener(processItem));
        parser.startParse(springXMLPath);
    }

    /**
     * 
     * Returns the repository view..
     * 
     * @return - the repository biew
     */
    public IRepositoryView getViewPart() {
        IViewPart viewPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                .findView(IRepositoryView.VIEW_ID);
        return (IRepositoryView) viewPart;
    }

    /**
     * Getter for project.
     * 
     * @return the project
     */
    public CamelProcessItem getProcess() {
        return this.processItem;
    }

    /*
     * 
     */
    public String getSpringXMLPath() {
        return springXMLPath;
    }
}
