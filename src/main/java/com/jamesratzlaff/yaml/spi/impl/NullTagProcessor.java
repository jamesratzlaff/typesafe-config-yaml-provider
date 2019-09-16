package com.jamesratzlaff.yaml.spi.impl;

import org.yaml.snakeyaml.nodes.Tag;

import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

public class NullTagProcessor extends AbstractTagProcessor{

	public NullTagProcessor() {
		super(Tag.NULL);
	}

	@Override
	public ConfigValue apply(ConfigOrigin origin, String strValue) {
		return ConfigValueFactory.fromAnyRef(null);
	}

}
