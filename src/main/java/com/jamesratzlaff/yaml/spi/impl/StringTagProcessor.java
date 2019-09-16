package com.jamesratzlaff.yaml.spi.impl;

import org.yaml.snakeyaml.nodes.Tag;

public class StringTagProcessor extends AbstractTagProcessor{
	
	public StringTagProcessor() {
		super(Tag.STR);
	}

}
