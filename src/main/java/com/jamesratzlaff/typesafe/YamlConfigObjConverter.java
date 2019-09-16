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
import com.jamesratzlaff.yaml.spi.impl.service.TagProcessorService;
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
		this(configOrigin, includeContext, reader, null);
	}

	public YamlConfigObjConverter(ConfigOrigin configOrigin, ConfigIncludeContext includeContext, List<Node> rootNodes,
			List<Comment> comments) {
		this.configOrigin = configOrigin;
		this.includeContext = includeContext;

		List<Node> toUse = rootNodes == null ? configOrigin != null ? getRootNodes(configOrigin.url()) : null
				: rootNodes;
		this.rootNodes = toUse != null ? toUse : Collections.emptyList();
		if (comments == null) {
			if (this.configOrigin != null) {
				comments = CommentReader.readComments(this.configOrigin.url(), this.rootNodes);
			}
		}
		if (comments == null) {
			comments = Collections.emptyList();
		}
		this.comments = comments;
	}

	public YamlConfigObjConverter(ConfigOrigin configOrigin, ConfigIncludeContext includeContext, Reader reader,
			List<Comment> comments) {
		this(configOrigin, includeContext, getRootNodes(reader), comments);
	}

	public List<Comment> getComments() {
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
		if (this.confValue == null) {
			List<Node> rootNodes = getRootNodes();
			if (rootNodes.size() == 1) {
				this.confValue = convert(rootNodes.get(0));
			} else if (rootNodes.size() > 1) {
				List<ConfigValue> configValues = rootNodes.stream().map(node -> (ConfigValue) convert(node))
						.collect(Collectors.toList());
				ConfigList asConfigList = ConfigValueFactory.fromIterable(configValues).withOrigin(getConfigOrigin());
				this.confValue = ConfigFactory.empty().withValue("_", asConfigList).root();
			}
		}
		return confValue;
	}

	@SuppressWarnings("unchecked")
	protected <T extends ConfigValue> T convert(Node node) {
		T val = null;
		if (node instanceof MappingNode) {
			val = (T) convert((MappingNode) node);
		} else if (node instanceof SequenceNode) {
			val = (T) convert((SequenceNode) node);
		} else if (node instanceof ScalarNode) {
			val = (T) convert((ScalarNode) node);
		}
		if (val != null) {
			val = applyOrigin(node, val);
		}
		return val;
	}

	protected List<Comment> getCommentAssociatedWithNode(Node n) {
		return getCommentForNode(n);
	}

	private ConfigOrigin createOrigin(Node n) {
		List<Comment> associatedComments = getCommentAssociatedWithNode(n);
		ConfigOrigin co = getConfigOrigin().withLineNumber(n.getStartMark().getLine());
		if (associatedComments != null && !associatedComments.isEmpty()) {
			List<String> commentLines = associatedComments.stream()
					.flatMap(associatedComment -> associatedComment.getLines().stream()).collect(Collectors.toList());
			co = co.withComments(commentLines);
		}
		return co;
	}

	@SuppressWarnings("unchecked")
	protected <T extends ConfigValue> T applyOrigin(Node n, T cv) {
		ConfigOrigin co = createOrigin(n);
		cv = (T) cv.withOrigin(co);
		return cv;
	}

	private static final Pattern ARRAY_ENTRY_PATTERN = Pattern.compile("^([^\\[]+)[\\[]([0-9]+)\\]$");

	protected ConfigTuple convert(NodeTuple tuple, Map<String, List<ConfigValue>> decArrays) {
		ScalarNode keyNode = (ScalarNode) tuple.getKeyNode();
		String key = keyNode.getValue();
		ConfigValue value = convert(tuple.getValueNode());
		
		return new ConfigTuple(key, value);
	}

	private Map<String, ConfigValue> toMap(List<NodeTuple> tuples){
		Map<String,ConfigValue> asMap = new LinkedHashMap<String,ConfigValue>(tuples.size());
		for(int i=0;i<tuples.size();i++) {
			NodeTuple tuple = tuples.get(i);
			String key = ((ScalarNode)tuple.getKeyNode()).getValue();
			ConfigValue value = convert(tuple.getValueNode());
			asMap.put(key, value);
		}
		asMap=toNormalizedMap(asMap);
		return asMap;
	}
	
	private static Map<String, ConfigValue> toNormalizedMap(Map<String,ConfigValue> nonNormalizedMap){
		Map<String,Object> normalized = new LinkedHashMap<String,Object>(nonNormalizedMap.size());
		Map<String,List<ConfigValue>> arrayIndexedMap = getArrayIndexedNodesMapAsListMap(nonNormalizedMap);
		List<String> keys = new ArrayList<String>(nonNormalizedMap.keySet());
		for(String key : keys) {
			Object value = nonNormalizedMap.remove(key);
			Matcher m = ARRAY_ENTRY_PATTERN.matcher(key);
			if(m.matches()) {
				key=m.group(1);
				
				int index = Integer.parseInt(m.group(2));
				List<ConfigValue> associatedList = arrayIndexedMap.get(key);
				if(!normalized.containsKey(key)) {
					normalized.put(key, associatedList);
				}
				associatedList.set(index, (ConfigValue)value);
			} else {
				normalized.put(key, value);
			}
		}
		
		for(String key: arrayIndexedMap.keySet()) {
			@SuppressWarnings("unchecked")
			List<ConfigValue> asList = (List<ConfigValue>) normalized.get(key);
			normalized.replace(key, ConfigValueFactory.fromIterable(asList));
		}
		keys = new ArrayList<String>(normalized.keySet());
		Map<String,ConfigValue> resultMap = new LinkedHashMap<String,ConfigValue>(normalized.size());
		for(String key : keys) {
			ConfigValue normalizedValue = (ConfigValue)normalized.remove(key);  
			resultMap.put(key, normalizedValue);
		}
		return resultMap;
	}
	
	private static Map<String, List<ConfigValue>> getArrayIndexedNodesMapAsListMap(Map<String,ConfigValue> tuples) {
		Map<String, Integer> maxNodeMap = getDecArrayWithMaxVal(tuples);
		Map<String, List<ConfigValue>> listedNodeMap = new LinkedHashMap<String, List<ConfigValue>>(maxNodeMap.size());
		for (String nodeName : maxNodeMap.keySet()) {
			Integer size = maxNodeMap.get(nodeName);
			List<ConfigValue> list = createFilledList(size.intValue());
			listedNodeMap.put(nodeName, list);
		}
		return listedNodeMap;
	}

	private static List<ConfigValue> createFilledList(int size) {
		List<ConfigValue> list = new ArrayList<ConfigValue>(size);
		for (int i = 0; i < size; i++) {
			list.add(null);
		}
		return list;
	}
	private static Map<String, Integer> getDecArrayWithMaxVal(Map<String,?> nonNormalizedMap){
		Map<String, Integer> reso = new LinkedHashMap<String, Integer>();
		for (String key : nonNormalizedMap.keySet()) {
			Matcher m = ARRAY_ENTRY_PATTERN.matcher(key);
			if (m.matches()) {
				String mapKey = m.group(1);
				int index = Integer.parseInt(m.group(2))+1;
				Integer existing = reso.get(mapKey);
				if (existing == null) {
					existing = index;
				}
				int max = Math.max(existing, index);
				if (max >= existing.intValue()) {
					if(!reso.containsKey(mapKey)) {
						reso.put(mapKey, max);
					} else {
						reso.replace(mapKey, max);
					}
				}
			}
		}
		return reso;
	}


	protected Iterable<Entry<String, ConfigValue>> convert(List<NodeTuple> tuples) {
		return toMap(tuples).entrySet();
	}

	protected List<Comment> getCommentForNode(Node n) {
		List<Comment> comments = new ArrayList<Comment>(2);
		Comment prefixed = getComments().stream()
				.filter(comment -> comment.getLineOfFirstNonWhiteSpaceChar() == n.getStartMark().getLine()).findFirst()
				.orElse(null);
		if (prefixed != null) {
			comments.add(prefixed);
		}
		Comment inline = getComments().stream()
				.filter(comment -> comment.getLineNo() == n.getEndMark().getLine() && comment.isInline()).findFirst()
				.orElse(null);
		if (inline != null) {
			comments.add(inline);
		}
		return comments;
	}

	protected ConfigObject convert(MappingNode node) {
		ConfigOrigin origin = createOrigin(node);
		Iterable<Entry<String, ConfigValue>> asIterable = convert(node.getValue());
		ConfigObject co = ConfigImplementationsAccessor.toSimpleConfigObject(origin, asIterable);
		return co;
	}

	protected ConfigList convert(SequenceNode node) {
		List<Node> nodes = node.getValue();
		List<ConfigValue> asValues = new ArrayList<ConfigValue>(nodes.size());
		for (int i = 0; i < nodes.size(); i++) {
			Node n = nodes.get(i);
			ConfigValue cv = convert(n);
			asValues.add(cv);
		}
		return ConfigValueFactory.fromIterable(asValues);
	}

	protected ConfigValue convert(ScalarNode node) {
		return TagProcessorService.getInstance().getConfigValue(node);
	}

	public static List<Node> getRootNodes(URL url) {
		List<Node> result = Collections.emptyList();
		try (InputStream is = url.openStream()) {
			Reader r = new UnicodeReader(is);
			result = getRootNodes(r);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static List<Node> getRootNodes(Reader reader) {
		Yaml y = new Yaml();
		Iterable<Node> nodes = reader != null ? y.composeAll(reader) : Collections.emptyList();
		return new ArrayList<Node>(StreamSupport.stream(nodes.spliterator(), false).collect(Collectors.toList()));
	}

	private static class ConfigTuple implements Serializable, Entry<String, ConfigValue> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 4337047017588306220L;
		private final String key;
		private ConfigValue value;

		public ConfigTuple(String key) {
			this.key = key;
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
			this.value = value;
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
