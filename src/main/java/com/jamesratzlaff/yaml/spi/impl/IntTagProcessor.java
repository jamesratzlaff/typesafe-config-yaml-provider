package com.jamesratzlaff.yaml.spi.impl;

import java.math.BigInteger;

import org.yaml.snakeyaml.nodes.Tag;

import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

public class IntTagProcessor extends AbstractTagProcessor{
	private static final BigInteger MAX_INT = BigInteger.valueOf(Integer.MAX_VALUE);
	private static final BigInteger MIN_INT = BigInteger.valueOf(Integer.MIN_VALUE);
	private static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
	private static final BigInteger MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);
	
	
	public IntTagProcessor() {
		super(Tag.INT);
	}

	@Override
	public ConfigValue apply(String strValue) {
		BigInteger intVal = new BigInteger(strValue);
		if(intVal.compareTo(MAX_INT)<=0&&intVal.compareTo(MIN_INT)>=0) {
			return ConfigValueFactory.fromAnyRef(intVal.intValue());
		} else if(intVal.compareTo(MAX_LONG)<=0&&intVal.compareTo(MIN_LONG)>=0) {
			return ConfigValueFactory.fromAnyRef(intVal.longValue());
		}
		return ConfigValueFactory.fromAnyRef(intVal);
	}
	
	
}
