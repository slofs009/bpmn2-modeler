/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 *  All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 *
 * @author Bob Brodt
 ******************************************************************************/

package org.eclipse.bpmn2.modeler.ui.adapters.properties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.DataAssociation;
import org.eclipse.bpmn2.DataStore;
import org.eclipse.bpmn2.DocumentRoot;
import org.eclipse.bpmn2.Event;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.ItemAwareElement;
import org.eclipse.bpmn2.Property;
import org.eclipse.bpmn2.modeler.core.ModelHandler;
import org.eclipse.bpmn2.modeler.core.utils.ModelUtil;
import org.eclipse.bpmn2.modeler.ui.adapters.Bpmn2FeatureDescriptor;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;

/**
 * @author Bob Brodt
 *
 */
public class DataAssociationPropertiesAdapter extends Bpmn2ExtendedPropertiesAdapter {

	/**
	 * @param adapterFactory
	 * @param object
	 */
	public DataAssociationPropertiesAdapter(AdapterFactory adapterFactory, EObject object) {
		super(adapterFactory, object);

    	EStructuralFeature ref;
    	
    	ref = Bpmn2Package.eINSTANCE.getDataAssociation_SourceRef();
    	setFeatureDescriptor(ref, new SourceTargetFeatureDescriptor(adapterFactory,object,ref));
    	ref = Bpmn2Package.eINSTANCE.getDataAssociation_TargetRef();
    	setFeatureDescriptor(ref, new SourceTargetFeatureDescriptor(adapterFactory,object,ref));
	}

	public class SourceTargetFeatureDescriptor extends Bpmn2FeatureDescriptor {

		public SourceTargetFeatureDescriptor(AdapterFactory adapterFactory, EObject object, EStructuralFeature feature) {
			super(adapterFactory, object, feature);
		}
		
			@Override
    		public Collection getChoiceOfValues(Object context) {
				List<EObject> values = new ArrayList<EObject>();
				// search for all Properties and DataStores
				// Properties are contained in the nearest enclosing Process or Event;
				// DataStores are contained in the DocumentRoot
    			EObject object = context instanceof EObject ? (EObject)context : this.object;
    			values.addAll( ModelUtil.collectAncestorObjects(object, "properties", new Class[] {Process.class}) );
    			values.addAll( ModelUtil.collectAncestorObjects(object, "properties", new Class[] {Event.class}) );
    			values.addAll( ModelUtil.collectAncestorObjects(object, "dataStore", new Class[] {DocumentRoot.class}) );
    			return values;
    		}
			
			@Override
			public EObject createValue(EObject context) {
				EObject object = context instanceof EObject ? (EObject)context : this.object;
				// what kind of object should we create? Property or DataStore?
				EClass eClass = null;
				if (ModelUtil.findNearestAncestor(object, new Class[] {Process.class, Event.class}) != null)
					// nearest ancestor is a Process or Event, so new object will be a Property
					eClass = Bpmn2Package.eINSTANCE.getProperty();
				else if(ModelUtil.findNearestAncestor(object, new Class[] {DocumentRoot.class}) != null)
					eClass = Bpmn2Package.eINSTANCE.getDataStore();
				
				if (eClass!=null) {
					try {
						ModelHandler mh = ModelHandler.getInstance(object);
						return mh.create(eClass);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				return null;
			}
			
			@Override
			public void setValue(EObject context, final Object value) {
				final DataAssociation association = context instanceof DataAssociation ?
						(DataAssociation)context :
						(DataAssociation)this.object;

				EObject container = null;
				EStructuralFeature containerFeature = null;
				if (value instanceof Property) {
					if (((Property)value).eContainer()==null) {
						// this Property isn't owned by anything yet - figure out who the owner is
						container = ModelUtil.findNearestAncestor(association, new Class[] {Event.class});
						if (container==null)
							container = ModelUtil.findNearestAncestor(association, new Class[] {Process.class});
						containerFeature = container.eClass().getEStructuralFeature("properties");
					}
				}
				else if (value instanceof DataStore) {
					if (((DataStore)value).eContainer()==null) {
						// this DataStore isn't owned by anything yet - figure out who the owner is
						container = ModelUtil.findNearestAncestor(association, new Class[] {DocumentRoot.class});
						containerFeature = container.eClass().getEStructuralFeature("dataStore");
					}
				}

				final EObject c = container;
				final EStructuralFeature cf = containerFeature;
				
				TransactionalEditingDomain editingDomain = getEditingDomain(association);
				if (feature == Bpmn2Package.eINSTANCE.getDataAssociation_SourceRef()) {
					if (association.getSourceRef().size()==0) {
						if (editingDomain == null) {
							if (c!=null) {
								if (c.eGet(cf) instanceof List)
									((List)c.eGet(cf)).add(value);
								else
									c.eSet(cf, value);
							}
							association.getSourceRef().add((ItemAwareElement)value);
						} else {
							editingDomain.getCommandStack().execute(new RecordingCommand(editingDomain) {
								@Override
								protected void doExecute() {
									if (c!=null) {
										if (c.eGet(cf) instanceof List)
											((List)c.eGet(cf)).add(value);
										else
											c.eSet(cf, value);
									}
									association.getSourceRef().add((ItemAwareElement)value);
								}
							});
						}
					}
					else {
						if (editingDomain == null) {
							if (c!=null) {
								if (c.eGet(cf) instanceof List)
									((List)c.eGet(cf)).add(value);
								else
									c.eSet(cf, value);
							}
							association.getSourceRef().set(0,(ItemAwareElement)value);
						} else {
							editingDomain.getCommandStack().execute(new RecordingCommand(editingDomain) {
								@Override
								protected void doExecute() {
									if (c!=null) {
										if (c.eGet(cf) instanceof List)
											((List)c.eGet(cf)).add(value);
										else
											c.eSet(cf, value);
									}
									association.getSourceRef().set(0,(ItemAwareElement)value);
								}
							});
						}
					}
				}
				else {
					if (editingDomain == null) {
						if (c!=null) {
							if (c.eGet(cf) instanceof List)
								((List)c.eGet(cf)).add(value);
							else
								c.eSet(cf, value);
						}
						association.setTargetRef((ItemAwareElement)value);
					} else {
						editingDomain.getCommandStack().execute(new RecordingCommand(editingDomain) {
							@Override
							protected void doExecute() {
								if (c!=null) {
									if (c.eGet(cf) instanceof List)
										((List)c.eGet(cf)).add(value);
									else
										c.eSet(cf, value);
								}
								association.setTargetRef((ItemAwareElement)value);
							}
						});
					}
				}
			}
		
	}
}