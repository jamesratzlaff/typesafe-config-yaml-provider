package com.jamesratzlaff.typesafe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import com.jamesratzlaff.typesafe.CommentReader.Comment;
import com.typesafe.config.ConfigIncludeContext;
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
	private final Node rootNode;
	private List<Comment> comments;
	private ConfigValue result;
	
	public YamlConfigObjConverter(ConfigOrigin configOrigin, ConfigIncludeContext includeContext, Node rootNode) {
		this.configOrigin=configOrigin;
		this.includeContext=includeContext;
		this.rootNode=rootNode;
		if(this.configOrigin!=null) {
			this.comments=CommentReader.readComments(this.configOrigin.url());
		} else {
			this.comments=Collections.emptyList();
		}
	}
	
	public List<Comment> getComments(){
		if(this.comments==null) {
			if(this.configOrigin!=null) {
				List<Comment> unfiltered = CommentReader.readComments(this.configOrigin.url());
				List<ScalarNode> scalarNodes = getAllScalarNodes();
				this.comments=new ArrayList<Comment>(unfiltered.stream().filter(comment->commentIsNotInScalarNode(comment, scalarNodes)).collect(Collectors.toList()));
			}
		}
		return this.comments;
	}
	
	private static boolean commentIsNotInScalarNode(Comment comment, Iterable<ScalarNode> nodes) {
		return StreamSupport.stream(nodes.spliterator(), false).noneMatch(node->commentIsInScalarNode(comment, node));
	}
	
	
	private static boolean commentIsInScalarNode(Comment comment, ScalarNode node) {
		Mark start = node.getStartMark();
		Mark end = node.getEndMark();
		int startLine = start.getLine();
		int endLine = end.getLine();
		int startColumn=start.getColumn();
		int endColumn=end.getColumn();
		return ((comment.getLineNo()>startLine||(comment.getLineNo()==startLine&&comment.getFirstCommentTagCharOffset()>=startColumn))&&
				(comment.getEndLine()<endLine||(comment.getEndLine()+1==endLine&&comment.getCharOffsetOfProceedingNonWhiteSpaceChar()<=endColumn)));
	}
	
	private List<ScalarNode> getAllScalarNodes(){
		return getScalarNodes(new ArrayList<ScalarNode>(), this.rootNode);
	}
	
	private List<ScalarNode> getScalarNodes(List<ScalarNode> result, NodeTuple tuple){
		if(result==null) {
			result=new ArrayList<ScalarNode>();
		}
		getScalarNodes(result, tuple.getKeyNode());
		getScalarNodes(result, tuple.getValueNode());
		return result;
	}
	
	private List<ScalarNode> getScalarNodes(List<ScalarNode> result, Node node){
		if(result==null) {
			result=new ArrayList<ScalarNode>();
		}
		if(node instanceof MappingNode) {
			MappingNode asMappingNode = (MappingNode)node;
			List<NodeTuple> kvps = asMappingNode.getValue();
			for(int i=0;i<kvps.size();i++) {
				getScalarNodes(result,kvps.get(i));
			}
		} else if(node instanceof SequenceNode) {
		SequenceNode asSequenceNode = (SequenceNode)node;
			List<Node> nodes = asSequenceNode.getValue();
			for(int i=0;i<nodes.size();i++) {
				getScalarNodes(result, nodes.get(i));
			}
		}else if(node instanceof ScalarNode) {
			result.add((ScalarNode)node);
		}
		return result;
	}
	
	
}
