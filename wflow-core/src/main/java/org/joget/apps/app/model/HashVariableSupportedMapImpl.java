package org.joget.apps.app.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.plugin.base.HashVariableSupportedMap;
import org.joget.workflow.model.WorkflowAssignment;

public class HashVariableSupportedMapImpl<K,V> extends HashVariableSupportedMap<K,V> {
    protected AppDefinition appDef;
    protected WorkflowAssignment assignment;
    
    public HashVariableSupportedMapImpl(Map<K,V> initialValues) {
        super(initialValues);
        
        if (initialValues instanceof HashVariableSupportedMapImpl) {
            this.assignment = ((HashVariableSupportedMapImpl) initialValues).assignment;
            this.appDef = ((HashVariableSupportedMapImpl) initialValues).appDef;
        } else {
            this.assignment = AppUtil.getCurrentAssignment();
            this.appDef = AppUtil.getCurrentAppDefinition();
        }
        AppUtil.resetCurrentAssignment(); //reset the current assignment after use it
    }
    
    protected HashVariableSupportedMapImpl(AppDefinition appDef, WorkflowAssignment assignment, Map<K,V> initialValues) {
        super(initialValues);
        this.appDef = appDef;
        this.assignment = assignment;
    }
    
    @Override
    public Object clone() {
        this.isInternal = true;
        HashVariableSupportedMapImpl<K,V> clone = new HashVariableSupportedMapImpl<>(appDef, assignment, (HashMap<K,V>) ((HashMap) this.initialMap).clone());
        clone.putAll(this);
        this.isInternal = null; //reset it after clone
        return clone;
    }

    @Override
    protected Object getProcessedValue(V value) {
        Object newValue = null;
        if (value instanceof Map && !(value instanceof HashVariableSupportedMapImpl)) {
            newValue = new HashVariableSupportedMapImpl(appDef, assignment, (Map) value);
            if (this.isInternal != null && !this.isInternal) { //if this child map is created for external, set it for external too
                ((HashVariableSupportedMap) newValue).setInternal(false);
            }
        } else if (value instanceof Object[]) {
            Object[] objArr = (Object[]) value;
            if (objArr.length > 0 && objArr[0] instanceof Map && !(objArr[0] instanceof HashVariableSupportedMapImpl)) {
                Collection arr = new ArrayList();
                for (Object v : objArr) {
                    HashVariableSupportedMap m = new HashVariableSupportedMapImpl(appDef, assignment, (Map) v);
                    if (this.isInternal != null && !this.isInternal) { //if this child map is created for external, set it for external too
                        m.setInternal(false);
                    }
                    arr.add(m);
                }
                newValue = arr.toArray(new HashVariableSupportedMapImpl[0]);
            } else {
                newValue = value;
            }
        } else if (value instanceof String) {
            newValue = AppUtil.processHashVariable((String) value, assignment, null, null, appDef, true);
        } else if (value instanceof FormRowSet) {
            for (FormRow r : (FormRowSet) value) {
                for (String k : r.stringPropertyNames()) {
                    String t = r.getProperty(k);
                    if (AppUtil.containsHashVariable(t)) {
                        r.setProperty(k, AppUtil.processHashVariable(t, assignment, null, null, appDef, true));
                    }
                }
            }
            newValue = value;
        } else {
            newValue = value;
        }
        return newValue;
    }
}
