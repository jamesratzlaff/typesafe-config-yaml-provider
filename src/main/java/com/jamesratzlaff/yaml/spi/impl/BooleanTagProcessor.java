package com.jamesratzlaff.yaml.spi.impl;

import org.yaml.snakeyaml.nodes.Tag;

import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

public class BooleanTagProcessor extends AbstractTagProcessor{

	public BooleanTagProcessor() {
		super(Tag.BOOL);
	}
	
	@Override
	public ConfigValue apply(String val) {
		boolean booleanVal = Boolean.parseBoolean(val);
		return ConfigValueFactory.fromAnyRef(booleanVal);
	}
	
}
