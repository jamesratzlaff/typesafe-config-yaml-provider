package com.jamesratzlaff.yaml.spi;

import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

public interface TagProcessor {

	boolean isCompatible(Tag t);
	default ConfigValue apply(Node node) {
		return apply(null, node);
	}
	default ConfigValue apply(ConfigOrigin origin, Node node) {
		if (node == null) {
			return ConfigValueFactory.fromAnyRef(null);
		}
		if (isCompatible(node.getTag())) {
			if (node instanceof ScalarNode) {
				return apply(origin, ((ScalarNode) node).getValue());
			}
			return null;
		}
		throw new RuntimeException("The given node " + node + " with tag " + node.getTag()
				+ " is not compatible with this node processor");
	}

	default ConfigValue apply(String strValue) {
		return apply(null, strValue);
	}
	
	ConfigValue apply(ConfigOrigin origin, String strValue);

}
