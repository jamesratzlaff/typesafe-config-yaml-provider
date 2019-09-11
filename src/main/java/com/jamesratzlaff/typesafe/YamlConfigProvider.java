package com.jamesratzlaff.typesafe;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.typesafe.config.ConfigFormat;
import com.typesafe.config.ConfigIncludeContext;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;
import com.typesafe.config.impl.AbstractConfigProvider;
import com.typesafe.config.impl.SimpleConfigFormat;

public class YamlConfigProvider extends AbstractConfigProvider {
	public static final ConfigFormat YAML = new SimpleConfigFormat("yml","yaml").withMimeTypes("application/x-yaml","text/yaml");
	
	
	public YamlConfigProvider() {
		super(YamlConfigProvider.YAML);
	}

	public ConfigValue rawParseValue(Reader reader, ConfigOrigin origin, ConfigParseOptions finalOptions,
			ConfigIncludeContext includeContext) throws IOException {
		Yaml yaml = new Yaml();
		Iterable<Object> obj = yaml.loadAll(reader);
		System.out.println(origin);
		
		System.out.println(YAML.acceptContent());
		ConfigObject co = ConfigValueFactory.fromMap((Map<String,Object>) obj);
		return co;
	}

}
