package com.jamesratzlaff.typesafe;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.reader.UnicodeReader;

import com.jamesratzlaff.typesafe.CommentReader.Comment;
import com.typesafe.config.ConfigIncludeContext;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigValue;

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
	
	public YamlConfigObjConverter(ConfigOrigin configOrigin, ConfigIncludeContext includeContext, List<Node> rootNodes, List<Comment> comments) {
		this.configOrigin=configOrigin;
		this.includeContext=includeContext;

		List<Node> toUse = rootNodes==null?configOrigin!=null?getRootNodes(configOrigin.url()):null:null;
		this.rootNodes=toUse!=null?toUse:Collections.emptyList();
		if(comments==null) {
			if(this.configOrigin!=null) {
				comments=CommentReader.readComment(this.configOrigin.url(), this.rootNodes);
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
		Comment associatedComment = getCommentAssociatedWithNode(n);
		ConfigOrigin co = getConfigOrigin().withLineNumber(n.getStartMark().getLine());
		if(associatedComment!=null) {
			co=co.withComments(associatedComment.getCommentLines());
		}
		cv=(T)cv.withOrigin(co);
		return cv;
	}
	
	protected ConfigObject convert(MappingNode node) {
		return null;
	}
	
	protected ConfigList convert(SequenceNode node) {
		return null;
	}
	
	protected ConfigValue convert(ScalarNode node) {
		return null;
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

	
	
}
