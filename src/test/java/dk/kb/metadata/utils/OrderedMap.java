package dk.kb.metadata.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderedMap {

	private final Map<String, String> content;
	private final List<String> order;
	
	public OrderedMap() {
		content = new HashMap<String, String>();
		order = new ArrayList<String>();
	}
	
	public void put(String key, String value) {
		order.add(key);
		content.put(key, value);
	}
	
	public boolean hasKey(String key) {
		return content.containsKey(key) && order.contains(key);
	}
	
	public String getValue(String key) {
		return content.get(key);
	}
	
	public Integer getIndex(String key) {
		return order.indexOf(key);
	}
}
