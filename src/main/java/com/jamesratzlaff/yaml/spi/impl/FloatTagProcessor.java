package com.jamesratzlaff.yaml.spi.impl;

import java.math.BigDecimal;

import org.yaml.snakeyaml.nodes.Tag;

import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

public class FloatTagProcessor extends AbstractTagProcessor{
	private static final BigDecimal MIN_FLOAT=BigDecimal.valueOf(Float.MIN_VALUE);
	private static final BigDecimal MAX_FLOAT=BigDecimal.valueOf(Float.MAX_VALUE);
	private static final BigDecimal MIN_DOUBLE=BigDecimal.valueOf(Double.MIN_VALUE);
	private static final BigDecimal MAX_DOUBLE=BigDecimal.valueOf(Double.MAX_VALUE);
	
	public FloatTagProcessor() {
		super(Tag.FLOAT);
	}
	
	
	public ConfigValue apply(ConfigOrigin origin, String value) {
		BigDecimal floatVal = new BigDecimal(value);
		return ConfigValueFactory.fromAnyRef(floatVal);
	}
	
}
