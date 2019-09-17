package com.typesafe.config.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.jamesratzlaff.util.SubstitutableValue;
import com.jamesratzlaff.util.SubstitutableValues;
import com.typesafe.config.ConfigIncludeContext;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

public class ConfigImplementationsAccessor {

	public static ConfigValue toConfigReferenceOrStringValue(ConfigOrigin origin,
			SubstitutableValue substitutableValue) {
		ConfigValue result = null;
		if (substitutableValue != null) {
			CharSequence value = substitutableValue.getValue();
			if (substitutableValue.isPlaceholder()) {
				Path p = value != null ? toPath(value) : null;
				SubstitutionExpression expr = new SubstitutionExpression(p, substitutableValue.isOptional());
				ConfigReference cr = new ConfigReference(origin, expr);
				result = cr;
			} else {
				result = ConfigValueFactory.fromAnyRef(value != null ? value.toString() : null);
				if(origin!=null) {
					result=result.withOrigin(origin);
				}
			}
		}
		return result;
	}

	public static SimpleConfigObject toSimpleConfigObject(ConfigOrigin origin, Iterable<Entry<String, ConfigValue>> entries) {
		List<Entry<String,ConfigValue>> asList= StreamSupport.stream(entries.spliterator(), false).collect(Collectors.toList());
		Map<String,AbstractConfigValue> asMap = new LinkedHashMap<String,AbstractConfigValue>(asList.size());
		for(int i=0;i<asList.size();i++) {
			Entry<String,ConfigValue> entry = asList.get(i);
			asMap.put(entry.getKey(), (AbstractConfigValue)entry.getValue());
		}
		return toSimpleConfigObject(origin, asMap);
	}
	private static ConfigConcatenation toConfigConcat(ConfigOrigin origin, List<ConfigValue> values) {
		return new ConfigConcatenation(origin, values==null?Collections.emptyList():values.stream().map(item->(AbstractConfigValue)item).collect(Collectors.toList()));
	}
	public static SimpleConfigObject toSimpleConfigObject(ConfigOrigin origin,Map<String,AbstractConfigValue> map,ResolveStatus resolveStatus, boolean withFallbacks) {
		return new SimpleConfigObject(origin, map,resolveStatus,withFallbacks);
	}
	public static SimpleConfigObject toSimpleConfigObject(ConfigOrigin origin, Map<String,AbstractConfigValue> map) {
		return new SimpleConfigObject(origin, map);
	}

	public static ConfigValue toConcatenationValueOrString(ConfigOrigin origin, CharSequence cs) {
		SubstitutableValues sv = cs != null ? new SubstitutableValues(cs.toString()) : null;
		ConfigValue toReturn = null;
		if (sv != null && sv.size() == 1) {
			SubstitutableValue val = sv.get(0);
			ConfigValue cv = toConfigReferenceOrStringValue(origin, val);
			toReturn = cv;
		}
		if(toReturn==null) {
			List<ConfigValue> asList = toConfigConcatenationValuesList(origin, sv);
			ConfigConcatenation concat = toConfigConcat(origin, asList);
			toReturn=concat;
		}
		return toReturn;
	}
	
	public static ConfigObject include(ConfigIncludeContext includeContext, String name) {
		return new SimpleIncluder(null).include(includeContext, name);
	}
	
	

	public static List<ConfigValue> toConfigConcatenationValuesList(ConfigOrigin origin, List<SubstitutableValue> cs) {
		List<ConfigValue> result = cs != null ? new ArrayList<ConfigValue>(cs.size()) : null;
		if (cs != null) {
			for (int i = 0; i < cs.size(); i++) {
				SubstitutableValue sv = cs.get(i);
				ConfigValue cv = toConfigReferenceOrStringValue(origin, sv);
				result.add(cv);
			}
		}
		return result;
	}

	private static Path toPath(CharSequence value) {
		Path path = null;
		if (value != null) {
			path = PathParser.parsePath(value.toString());
		}
		return path;
	}

}
