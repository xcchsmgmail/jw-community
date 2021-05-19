package org.joget.workflow.model.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.joget.commons.spring.model.AbstractSpringDao;
import org.joget.workflow.model.WorkflowProcessLink;

public class WorkflowProcessLinkDao extends AbstractSpringDao {

    public final static String ENTITY_NAME="WorkflowProcessLink";
    public final static String HISTORY_ENTITY_NAME="WorkflowProcessLinkHistory";
    
    public void addWorkflowProcessLink(String parentProcessId, String processInstanceId){
        WorkflowProcessLink wfProcessLink = new WorkflowProcessLink();
        WorkflowProcessLink parentWfProcessLink = getWorkflowProcessLink(parentProcessId);
        wfProcessLink.setParentProcessId(parentProcessId);

        if (parentWfProcessLink != null) {
            wfProcessLink.setOriginProcessId(parentWfProcessLink.getOriginProcessId());
        } else {
            wfProcessLink.setOriginProcessId(parentProcessId);
        }

        wfProcessLink.setProcessId(processInstanceId);

        addWorkflowProcessLink(wfProcessLink);
    }

    public void addWorkflowProcessLink(WorkflowProcessLink wfProcessLink){
        saveOrUpdate(ENTITY_NAME, wfProcessLink);
    }
    
    public void addWorkflowProcessLinkHistory(WorkflowProcessLink wfProcessLink){
        if (getWorkflowProcessLinkHistory(wfProcessLink.getProcessId()) == null) {
            WorkflowProcessLink history = new WorkflowProcessLink();
            history.setProcessId(wfProcessLink.getProcessId());
            history.setOriginProcessId(wfProcessLink.getOriginProcessId());
            history.setParentProcessId(wfProcessLink.getParentProcessId());

            saveOrUpdate(HISTORY_ENTITY_NAME, history);

            try {
                wfProcessLink = getWorkflowProcessLink(wfProcessLink.getProcessId());
                if (wfProcessLink != null) {
                    delete(wfProcessLink);
                }
            } catch (Exception e) {
                //ignore
            }
        }
    }

    public WorkflowProcessLink getWorkflowProcessLink(String processId){
        return (WorkflowProcessLink) find(ENTITY_NAME, processId);
    }

    public void delete(WorkflowProcessLink wfProcessLink) {
        super.delete(ENTITY_NAME, wfProcessLink);
    }
    
    public Map<String, Collection<String>> getOriginalIds(Collection<String> ids) {
        Map<String, Collection<String>> originalIds = new HashMap<String, Collection<String>>();
        Collection<String> existIds = new ArrayList<String>();
        
        if (ids.size() > 0) {
            String conditions = "";
            Collection<WorkflowProcessLink> links = null;
            Collection<String> values = null;
            
            int i = 0;
            for (String id : ids) {
                if (i % 1000 == 0) {
                    values = new ArrayList<String>();
                    conditions = "where e.processId in (";
                }
                
                conditions += "?,";
                values.add(id);
                
                if (i % 1000 == 999 || i == ids.size() -1) {
                    conditions = conditions.substring(0, conditions.length() - 1) + ")";
                    links = super.find(ENTITY_NAME, conditions, values.toArray(new String[0]), null, null, null, null);
                    
                    for (WorkflowProcessLink link : links) {
                        String orgId = link.getOriginProcessId();
                        String pid = link.getProcessId();

                        Collection<String> pIds = originalIds.get(orgId);
                        if (pIds == null) {
                            pIds = new ArrayList<String>();
                        }
                        pIds.add(pid);
                        existIds.add(pid);

                        originalIds.put(orgId, pIds);
                    }
                }
                i++;
            }
            
            // for those does not has link
            for (String id : ids) {
                if (!existIds.contains(id)) {
                    Collection<String> pIds = originalIds.get(id);
                    if (pIds == null) {
                        pIds = new ArrayList<String>();
                    }
                    pIds.add(id);
                    existIds.add(id);
                    originalIds.put(id, pIds);
                }
            }
        }
        
        return originalIds;
    }
    
    public Collection<WorkflowProcessLink> getLinks(String processId) {
        Collection<WorkflowProcessLink> links = new ArrayList<WorkflowProcessLink>();
        WorkflowProcessLink processLink = getWorkflowProcessLink(processId);
        if (processLink == null) {
            processLink = getWorkflowProcessLinkHistory(processId);
        }
        String conditions = "where e.originProcessId = ?";
        if (processLink != null) {
            processId = processLink.getOriginProcessId();
        }
        WorkflowProcessLink origin = new WorkflowProcessLink();
        origin.setProcessId(processId);
        origin.setParentProcessId(processId);
        origin.setOriginProcessId(processId);
        links.add(origin);
        
        Collection<WorkflowProcessLink> temp = super.find(ENTITY_NAME, conditions, new String[]{processId}, null, null, null, null);
        if (temp != null && !temp.isEmpty()) {
            links.addAll(temp);
        }
        return links;
    }
    
    public WorkflowProcessLink getWorkflowProcessLinkHistory(String processId){
        return (WorkflowProcessLink) find(HISTORY_ENTITY_NAME, processId);
    }

    public void deleteHistory(WorkflowProcessLink wfProcessLink) {
        super.delete(HISTORY_ENTITY_NAME, wfProcessLink);
    }
    
    public Collection<WorkflowProcessLink> getHistoryLinks(String processId) {
        Collection<WorkflowProcessLink> links = new ArrayList<WorkflowProcessLink>();
        WorkflowProcessLink processLink = getWorkflowProcessLinkHistory(processId);
        String conditions = "where e.originProcessId = ?";
        if (processLink != null) {
            processId = processLink.getOriginProcessId();
        }
        WorkflowProcessLink origin = new WorkflowProcessLink();
        origin.setProcessId(processId);
        origin.setParentProcessId(processId);
        origin.setOriginProcessId(processId);
        links.add(origin);
        
        Collection<WorkflowProcessLink> temp = super.find(HISTORY_ENTITY_NAME, conditions, new String[]{processId}, null, null, null, null);
        if (temp != null && !temp.isEmpty()) {
            links.addAll(temp);
        }
        return links;
    }
    
    public void migrateCompletedProcessLinks() {
        String conditions = " where e.processId not in (select p.processId from SharkProcess as p join p.state s where s.name like ?)";
        Collection<WorkflowProcessLink> temp = super.find(ENTITY_NAME, conditions, new String[]{"open.%"}, null, null, null, null);
        for (WorkflowProcessLink l : temp) {
            addWorkflowProcessLinkHistory(l);
        }
    }
}
