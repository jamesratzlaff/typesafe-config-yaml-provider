package com.jamesratzlaff.typesafe;

import java.io.IOException;
import java.io.Reader;

import com.typesafe.config.ConfigFormat;
import com.typesafe.config.ConfigIncludeContext;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.impl.AbstractConfigProvider;
import com.typesafe.config.impl.SimpleConfigFormat;

public class YamlConfigProvider extends AbstractConfigProvider {
	public static final ConfigFormat YAML = new SimpleConfigFormat("yml","yaml").withMimeTypes("application/x-yaml","text/yaml");
	
	
	public YamlConfigProvider() {
		super(YamlConfigProvider.YAML);
	}

	public ConfigValue rawParseValue(Reader reader, ConfigOrigin origin, ConfigParseOptions finalOptions,
			ConfigIncludeContext includeContext) throws IOException {
		YamlConfigObjConverter converter = new YamlConfigObjConverter(origin, includeContext, reader);
		return converter.getConfValue();
	}
	
	
}
