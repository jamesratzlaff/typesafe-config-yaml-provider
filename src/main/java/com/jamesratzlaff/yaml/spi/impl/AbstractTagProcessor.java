package com.jamesratzlaff.yaml.spi.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.yaml.snakeyaml.nodes.Tag;

import com.jamesratzlaff.yaml.spi.TagProcessor;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

public class AbstractTagProcessor implements TagProcessor{

	private final Predicate<Tag> compatibleTags;
	protected AbstractTagProcessor(String tag, String...tags) {
		this(toTags(tag));
	}
	protected AbstractTagProcessor(Tag tag, Tag...tags) {
		this(toSet(tag,tags));
	}
	
	private AbstractTagProcessor(Set<Tag> tags) {
		this(toPredicate(Collections.unmodifiableSet(tags==null?Collections.emptySet():tags)));
	}
	
	protected AbstractTagProcessor(Predicate<Tag> tagPredicate) {
		compatibleTags=tagPredicate!=null?tagPredicate:t->true;
	}
	
	private static Predicate<Tag> toPredicate(Set<Tag> tags){
		return tag->tags==null||tags.isEmpty()||tags.contains(tag);
	}
	
	public AbstractTagProcessor withTags(Tag tag, Tag...tags) {
		return new AbstractTagProcessor(this.getCompatibleTagsPredicate().or(toPredicate(toSet(tag, tags))));
	}
	
	public AbstractTagProcessor withTags(String tag, String...tags) {
		return new AbstractTagProcessor(this.getCompatibleTagsPredicate().or(toPredicate(toTags(tag, tags))));
	}
	
	private static Set<Tag> toSet(Tag tag, Tag...tags){
		Set<Tag> asSet = new HashSet<Tag>(tags.length+1);
		asSet.add(tag);
		asSet.addAll(Arrays.asList(tags));
		return asSet;
	}
	
	private static Set<Tag> toTags(String tag, String...tags){
		List<String> all = new ArrayList<String>(tags.length+1);
		all.add(tag);
		all.addAll(Arrays.asList(tags));
		return all.stream().map(Tag::new).collect(Collectors.toSet());
	}

	@Override
	public boolean isCompatible(Tag t) {
		if(getCompatibleTagsPredicate()==null) {
			return true;
		} else {
			return getCompatibleTagsPredicate().test(t);
		}
	}
	@Override
	public ConfigValue apply(String strValue) {
		return ConfigValueFactory.fromAnyRef(strValue);
	}
	protected Predicate<Tag> getCompatibleTagsPredicate(){
		return this.compatibleTags;
	}
}
