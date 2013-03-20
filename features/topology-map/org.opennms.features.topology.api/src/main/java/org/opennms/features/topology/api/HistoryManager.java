package org.opennms.features.topology.api;

public interface HistoryManager {

    public void applyHistory(String fragmentId, GraphContainer container);
    public String create(GraphContainer container);
	void onBind(HistoryOperation operation);
	void onUnbind(HistoryOperation operation);
    
}
