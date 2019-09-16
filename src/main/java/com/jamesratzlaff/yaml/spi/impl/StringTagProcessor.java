package com.jamesratzlaff.yaml.spi.impl;

import org.yaml.snakeyaml.nodes.Tag;

import com.typesafe.config.ConfigIncludeContext;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.impl.ConfigImplementationsAccessor;

public class StringTagProcessor extends AbstractTagProcessor{
	
	public StringTagProcessor() {
		super(Tag.STR);
	}

	@Override
	public ConfigValue apply(ConfigOrigin origin, String strValue, ConfigIncludeContext includeContext) {
		return ConfigImplementationsAccessor.toConcatenationValueOrString(origin, strValue);
	}

}
