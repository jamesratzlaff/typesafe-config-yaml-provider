package com.jamesratzlaff.yaml.spi.impl;

import org.yaml.snakeyaml.nodes.Tag;

import com.typesafe.config.ConfigIncludeContext;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

public class BooleanTagProcessor extends AbstractTagProcessor{

	public BooleanTagProcessor() {
		super(Tag.BOOL);
	}
	
	@Override
	public ConfigValue apply(ConfigOrigin origin, String val, ConfigIncludeContext includeContext) {
		boolean booleanVal = Boolean.parseBoolean(val);
		return ConfigValueFactory.fromAnyRef(booleanVal);
	}
	
}
