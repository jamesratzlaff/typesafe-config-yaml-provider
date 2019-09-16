package com.jamesratzlaff.yaml.spi.impl.service;

import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;

import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;

import com.jamesratzlaff.yaml.spi.TagProcessor;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigValue;

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
	}
	
	public ServiceLoader<TagProcessor> getServiceLoader(){
		return this.tagProcessors;
	}
	
	private Provider<TagProcessor> getProvider(Tag tag){
		if(tag==null) {
			tag=Tag.NULL;
		}
		Tag toUse = tag;
		return getServiceLoader().stream().filter(tProcessor->tProcessor.get().isCompatible(toUse)).findFirst().orElse(null);
	}
	
	public TagProcessor getTagProcessor(Tag tag) {
		if(tag==null) {
			tag=Tag.NULL;
		}
		Provider<TagProcessor> provider = getProvider(tag);
		if(provider!=null) {
			return provider.get();
		}
		return null;
	}
	
	public TagProcessor getTagProcessor(Node n) {
		return getTagProcessor(n!=null?n.getTag():null);
	}
	
	
	public ConfigValue getConfigValue(ConfigOrigin origin, Node n) {
		TagProcessor tp = getTagProcessor(n);
		return tp.apply(origin, n);
	}
	
	
	
	
	
}
