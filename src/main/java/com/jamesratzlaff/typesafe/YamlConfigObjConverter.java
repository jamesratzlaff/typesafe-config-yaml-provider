package com.jamesratzlaff.typesafe;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.reader.UnicodeReader;

import com.jamesratzlaff.typesafe.CommentReader.Comment;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigIncludeContext;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;
import com.typesafe.config.impl.ConfigImplementationsAccessor;

/**
 * 
 * @author jamesratzlaff
 *
 */
public class YamlConfigObjConverter {

	private final ConfigOrigin configOrigin;
	private final ConfigIncludeContext includeContext;
	private final List<Node> rootNodes;
	private final List<Comment> comments;
	private ConfigValue confValue;
	public YamlConfigObjConverter(ConfigOrigin configOrigin, ConfigIncludeContext includeContext, Reader reader) {
		this(configOrigin, includeContext, reader,null);
	}
	public YamlConfigObjConverter(ConfigOrigin configOrigin, ConfigIncludeContext includeContext, List<Node> rootNodes, List<Comment> comments) {
		this.configOrigin=configOrigin;
		this.includeContext=includeContext;

		List<Node> toUse = rootNodes==null?configOrigin!=null?getRootNodes(configOrigin.url()):null:rootNodes;
		this.rootNodes=toUse!=null?toUse:Collections.emptyList();
		if(comments==null) {
			if(this.configOrigin!=null) {
				comments=CommentReader.readComments(this.configOrigin.url(), this.rootNodes);
			}
		}
		if(comments==null) {
			comments=Collections.emptyList();
		}
		this.comments=comments;
	}
	
	public YamlConfigObjConverter(ConfigOrigin configOrigin, ConfigIncludeContext includeContext, Reader reader, List<Comment> comments) {
		this(configOrigin,includeContext,getRootNodes(reader),comments);
	}
	
	public List<Comment> getComments(){
		return this.comments;
	}
	
	/**
	 * @return the configOrigin
	 */
	public ConfigOrigin getConfigOrigin() {
		return configOrigin;
	}

	/**
	 * @return the includeContext
	 */
	public ConfigIncludeContext getIncludeContext() {
		return includeContext;
	}

	/**
	 * @return the rootNodes
	 */
	public List<Node> getRootNodes() {
		return rootNodes;
	}

	/**
	 * @return the confValue
	 */
	public ConfigValue getConfValue() {
		if(this.confValue==null) {
			List<Node> rootNodes = getRootNodes();
			if(rootNodes.size()==1) {
				this.confValue=convert(rootNodes.get(0));
			} else if(rootNodes.size()>1) {
				List<ConfigValue> configValues = rootNodes.stream().map(node->(ConfigValue)convert(node)).collect(Collectors.toList());
				ConfigList asConfigList = ConfigValueFactory.fromIterable(configValues).withOrigin(getConfigOrigin());
				this.confValue=ConfigFactory.empty().withValue("_", asConfigList).root();
			}			
		}
		return confValue;
	}
	
	@SuppressWarnings("unchecked")
	protected <T extends ConfigValue> T convert(Node node) {
		T val = null;
		if(node instanceof MappingNode) {
			val=(T)convert((MappingNode)node);	
		} else if(node instanceof SequenceNode) {
			val=(T)convert((SequenceNode)node);
		} else if(node instanceof ScalarNode) {
			val=(T)convert((ScalarNode)node);
		}
		if(val!=null) {
			val=applyOrigin(node, val);
		}
		return val;
	}
	
	protected Comment getCommentAssociatedWithNode(Node n) {
		return null;
	}
	
	private ConfigOrigin createOrigin(Node n) {
		Comment associatedComment = getCommentAssociatedWithNode(n);
		ConfigOrigin co = getConfigOrigin().withLineNumber(n.getStartMark().getLine());
		if(associatedComment!=null) {
			co=co.withComments(associatedComment.getCommentLines());
		}
		return co;
	}
	
	@SuppressWarnings("unchecked")
	protected <T extends ConfigValue> T applyOrigin(Node n, T cv) {
		ConfigOrigin co = createOrigin(n);
		cv=(T)cv.withOrigin(co);
		return cv;
	}
	
	
	private static final Pattern ARRAY_ENTRY_PATTERN = Pattern.compile("^([^\\[]+)[\\[]([0-9]+)\\]$");
	protected ConfigTuple convert(NodeTuple tuple, Map<String, List<ConfigValue>> decArrays) {
		ScalarNode keyNode = (ScalarNode)tuple.getKeyNode();
		String key = keyNode.getValue();
		ConfigValue value = convert(tuple.getValueNode());
		
		return new ConfigTuple(key, value);
	}
	
	private static Map<String, Integer> getDecArrayWithMaxVal(List<NodeTuple> tuples){
		Map<String,Integer> reso = new LinkedHashMap<String,Integer>();
		for(int i=0;i<tuples.size();i++) {
			NodeTuple tNode = tuples.get(i);
			String key = ((ScalarNode)tNode.getKeyNode()).getValue();
			Matcher m = ARRAY_ENTRY_PATTERN.matcher(key);
			if(m.matches()) {
				String mapKey = m.group(1);
				int index = Integer.parseInt(m.group(2));
				Integer existing = reso.get(mapKey);
				if(existing==null) {
					existing=index;
				}
			}
		}
		return reso;
		
	}
	
	public static class IndexedKey {
		private final String key;
		private final int index;
		
		private IndexedKey(String key, int index) {
			this.key=key;
			this.index=index;
		}
		
		/**
		 * @return the key
		 */
		public String getKey() {
			return key;
		}

		/**
		 * @return the index
		 */
		public int getIndex() {
			return index;
		}

		public static IndexedKey parse(String key) {
			Matcher m = ARRAY_ENTRY_PATTERN.matcher(key);
			if(m.matches()) {
				return new IndexedKey(m.group(1), Integer.parseInt(m.group(2)));
			}
			return null;
		}
		
		public static IndexedKey parse(ScalarNode sn) {
			return parse(sn.getValue());
		}
		
		public static IndexedKey parse(NodeTuple tuple) {
			return parse(((ScalarNode)tuple.getKeyNode()));
		}

		@Override
		public int hashCode() {
			return Objects.hash(index, key);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof IndexedKey)) {
				return false;
			}
			IndexedKey other = (IndexedKey) obj;
			return index == other.index && Objects.equals(key, other.key);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("IndexedKey [key=");
			builder.append(key);
			builder.append(", index=");
			builder.append(index);
			builder.append("]");
			return builder.toString();
		}
		
	}
	
	protected ConfigTuple convert(NodeTuple tuple) {
		ScalarNode keyNode = (ScalarNode)tuple.getKeyNode();
		String key = keyNode.getValue();
		ConfigValue value = convert(tuple.getValueNode());
		return new ConfigTuple(key, value);
	}
	
	protected Iterable<Entry<String,ConfigValue>> convert(List<NodeTuple> tuples) {
		return tuples.stream().map(this::convert).collect(Collectors.toList());
	}
	
	protected ConfigObject convert(MappingNode node) {
		ConfigOrigin origin = createOrigin(node);
		Iterable<Entry<String,ConfigValue>> asIterable = convert(node.getValue());
		ConfigObject co = ConfigImplementationsAccessor.toSimpleConfigObject(origin, asIterable);
		return co;
	}
	
	protected ConfigList convert(SequenceNode node) {
		List<Node> nodes = node.getValue();
		List<ConfigValue> asValues = new ArrayList<ConfigValue>(nodes.size());
		for(int i=0;i<nodes.size();i++) {
			Node n = nodes.get(i);
			ConfigValue cv = convert(n);
			asValues.add(cv);
		}
		return ConfigValueFactory.fromIterable(asValues);
	}
	
	protected ConfigValue convert(ScalarNode node) {
		Object value = node.getValue();
		return ConfigValueFactory.fromAnyRef(value);
	}

	public static List<Node> getRootNodes(URL url){
		List<Node> result = Collections.emptyList();
		try(InputStream is = url.openStream()){
			Reader r = new UnicodeReader(is);
			result = getRootNodes(r);
		} catch(IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static List<Node> getRootNodes(Reader reader) {
		Yaml y = new Yaml();
		Iterable<Node> nodes = reader!=null?y.composeAll(reader):Collections.emptyList();
		return new ArrayList<Node>(StreamSupport.stream(nodes.spliterator(), false).collect(Collectors.toList()));
	}

	
	private static class ConfigTuple implements Serializable,Entry<String, ConfigValue> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 4337047017588306220L;
		private final String key;
		private ConfigValue value;
		
		public ConfigTuple(String key) {
			this.key=key;
		}
		public ConfigTuple(String key, ConfigValue value) {
			this(key);
			setValue(value);
		}
		
		public String getKey() {
			return this.key;
		}
		
		public ConfigValue getValue() {
			return this.value;
		}
		
		
		
		public ConfigValue setValue(ConfigValue value) {
			ConfigValue old = getValue();
			this.value=value;
			return old;
		}
		@Override
		public int hashCode() {
			return Objects.hash(key, value);
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof ConfigTuple)) {
				return false;
			}
			ConfigTuple other = (ConfigTuple) obj;
			return Objects.equals(key, other.key) && Objects.equals(value, other.value);
		}
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ConfigTuple [key=");
			builder.append(key);
			builder.append(", value=");
			builder.append(value);
			builder.append("]");
			return builder.toString();
		}
		
		
		
		
		
	}
}
