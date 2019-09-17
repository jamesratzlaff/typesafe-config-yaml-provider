package com.jamesratzlaff.yaml.spi.impl.service;

import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

import com.jamesratzlaff.yaml.spi.TagProcessor;
import com.typesafe.config.ConfigIncludeContext;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

public class TagProcessorService {
	private static final TagProcessorService INSTANCE;
	static {
		INSTANCE=new TagProcessorService();
	}
	
	public static TagProcessorService getInstance() {
		return INSTANCE;
	}
	
	private final ServiceLoader<TagProcessor> tagProcessors;
	
	private TagProcessorService() {
		tagProcessors=ServiceLoader.load(TagProcessor.class);
		tagProcessors.reload();
	}
	
	public ServiceLoader<TagProcessor> getServiceLoader(){
		return this.tagProcessors;
	}
	
	private TagProcessor getProvider(Tag tag){
		if(tag==null) {
			tag=Tag.NULL;
		}
		Tag toUse = tag;
		return StreamSupport.stream(getServiceLoader().spliterator(),false).filter(tProcessor->tProcessor.isCompatible(toUse)).findFirst().orElse(null);
	}
	
	public TagProcessor getTagProcessor(Tag tag) {
		if(tag==null) {
			tag=Tag.NULL;
		}
		TagProcessor provider = getProvider(tag);
		return provider;
	}
	
	public TagProcessor getTagProcessor(Node n) {
		return getTagProcessor(n!=null?n.getTag():null);
	}
	
	
	public ConfigValue getConfigValue(ConfigOrigin origin, Node n, ConfigIncludeContext includer) {
		if(n==null) {
			return ConfigValueFactory.fromAnyRef(null);
		}
		TagProcessor tp = getTagProcessor(n);
		
		if(tp!=null) {
			return tp.apply(origin, n,includer);
		} else if(n instanceof ScalarNode) {
			System.out.println("unknown tag "+n.getTag());
			return ConfigValueFactory.fromAnyRef(((ScalarNode)n).getValue());
		}
		return null;
	}
	
	
	
	
	
}
