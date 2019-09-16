package com.jamesratzlaff.yaml.spi;

import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

public interface TagProcessor {

	boolean isCompatible(Tag t);

	default ConfigValue apply(Node node) {
		if (node == null) {
			return ConfigValueFactory.fromAnyRef(null);
		}
		if (isCompatible(node.getTag())) {
			if (node instanceof ScalarNode) {
				return apply(((ScalarNode) node).getValue());
			}
			return null;
		}
		throw new RuntimeException("The given node " + node + " with tag " + node.getTag()
				+ " is not compatible with this node processor");
	}

	ConfigValue apply(String strValue);

}
