package com.jamesratzlaff.typesafe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.reader.UnicodeReader;

/**
 * Aggregates comment data for use with attaching it so some configuration item (since snakeyaml ignores comments).
 * @author jamesratzlaff
 * 
 *
 */
public class CommentReader {

	private static final Pattern COMMENT = Pattern.compile("^([^#]*)#(.*)\\s*$");
	private static final Pattern COMMENT_CONTINUED = Pattern.compile("^\\s*#(.*)\\s*$");
	private static final Pattern NON_COMMENT = Pattern.compile("^(\\s*)[^#].*$");

	@SuppressWarnings("serial")
	public static class Comment implements Serializable, Cloneable, Comparable<Comment> {
		private final URL resource;
		private final int lineNo;
		private final int firstCommentTagCharOffset;
		private final List<String> commentLines;
		private final BitSet innerBlankLines;
		private final int numberOfLinesIncludingBlanks;
		private final int charOffsetOfProceedingNonWhiteSpaceChar;
		private final boolean inline;

		public Comment(URL resource, int lineNo, int firstCommentTagCharOffset, BitSet innerBlankLines,
				int numberOfLinesIncludingBlanks, int charOffsetOfProceedingNonWhiteSpaceChar,
				List<String> commentLines, boolean inline) {
			if (commentLines == null) {
				commentLines = new ArrayList<String>(0);
			}
			this.resource = resource;
			this.lineNo = lineNo;
			this.firstCommentTagCharOffset = firstCommentTagCharOffset;
			this.innerBlankLines = innerBlankLines == null ? new BitSet(0) : innerBlankLines;
			this.numberOfLinesIncludingBlanks = numberOfLinesIncludingBlanks;
			this.charOffsetOfProceedingNonWhiteSpaceChar = charOffsetOfProceedingNonWhiteSpaceChar;
			this.commentLines = Collections.unmodifiableList(commentLines);
			this.inline=inline;
		}
		
		public int getLineOfFirstNonWhiteSpaceChar() {
			return lineNo+numberOfLinesIncludingBlanks+1;
		}

		/**
		 * @return the innerBlankLines
		 */
		public BitSet getInnerBlankLines() {
			return innerBlankLines;
		}

		/**
		 * @return the inline
		 */
		public boolean isInline() {
			return inline;
		}

		public Comment copy() {
			return new Comment(resource, lineNo, firstCommentTagCharOffset, innerBlankLines,
					numberOfLinesIncludingBlanks, charOffsetOfProceedingNonWhiteSpaceChar,
					new ArrayList<String>(commentLines), inline);
		}

		public int getEndLine() {
			return this.lineNo + numberOfLinesIncludingBlanks;
		}

		@Override
		public int hashCode() {
			return Objects.hash(charOffsetOfProceedingNonWhiteSpaceChar, commentLines, firstCommentTagCharOffset,
					lineNo, numberOfLinesIncludingBlanks, resource);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof Comment)) {
				return false;
			}
			Comment other = (Comment) obj;
			return charOffsetOfProceedingNonWhiteSpaceChar == other.charOffsetOfProceedingNonWhiteSpaceChar
					&& Objects.equals(commentLines, other.commentLines)
					&& firstCommentTagCharOffset == other.firstCommentTagCharOffset && lineNo == other.lineNo
					&& numberOfLinesIncludingBlanks == other.numberOfLinesIncludingBlanks
					&& Objects.equals(resource, other.resource);
		}

		public Comment clone() {
			Comment cl = null;
			try {
				cl = (Comment) super.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			return cl;
		}
		
		public boolean isBlankLine(int fileLine) {
			return this.innerBlankLines.get(getCommentObjectLineNo(fileLine));
		}
		
		public String getLine(int fileLineNo) {
			int myLine = getCommentObjectLineNo(fileLineNo);
			return getMyLine(myLine);
		}
		
		private String getMyLine(int myLine) {
			if(myLine>=this.getNumberOfLinesIncludingBlanks()) {
				return null;
			}
			int commentLine = getCommentLinesIndex(myLine)-1;
			if(commentLine>-1) {
				return commentLines.get(commentLine);
			} else {
				return null;
			}
		}
		
		public List<String> getLines(){
			List<String> strs = new ArrayList<String>(this.getNumberOfLinesIncludingBlanks());
			for(int i=0;i<this.getNumberOfLinesIncludingBlanks();i++) {
				String myLine = getMyLine(i);
				if(myLine!=null) {
					strs.add(myLine);
				}
			}
			return strs;
		}
		
		private int getCommentLinesIndex(int myLine) {
			int commentLine=-1;
			for(int i=0;i<myLine+1;i++) {
				if(!this.innerBlankLines.get(i)) {
					commentLine+=1;
				}
			}
			if(this.innerBlankLines.get(myLine)) {
				return -1;
			}
			return commentLine;
		}
		
		private int getCommentObjectLineNo(int fileLine) {
			return fileLine-this.lineNo;
		}

//		private int getNextCommentStartAfter(int fileLine, int index) {
//			if(this.commentLines.isEmpty()) {
//				return -1;
//			}
//			if()
//			int nextCommentChar = !this.commentLines.isEmpty() ? this.commentLines.get(0).indexOf('#', index) : -1;
//			if (nextCommentChar > -1) {
//				nextCommentChar += firstCommentTagCharOffset;
//			}
//			return nextCommentChar;
//		}
//
//		/**
//		 * 
//		 * @param index - the char index of the line that this exists in
//		 * @return {@code null} if there isn't a comment tage after the given
//		 *         {@code index}, otherwise a new instance of a {@link Comment} other
//		 *         than {@link #getFirstCommentTagCharOffset()} and the first line of
//		 *         the comment lines, it will have all the same values as this instance
//		 */
//		public Comment withCommentTagAfterIndex(int index) {
//			int nextCommentTag = getNextCommentStartAfter(index);
//			Comment newInst = null;
//			if(nextCommentTag>-1) {
//				List<String> commentsToUse = new ArrayList<String>(this.commentLines.size());
//				commentsToUse.add(this.commentLines.get(0).substring(nextCommentTag-firstCommentTagCharOffset));
//				if(this.commentLines.size()>1) {
//					commentsToUse.addAll(this.commentLines.subList(1, this.commentLines.size()));
//				}
//				return new Comment(resource, lineNo, nextCommentTag, innerBlankLines, index, nextCommentTag, commentsToUse)
//			}
//			return newInst;
//		}

		/**
		 * @return the resource
		 */
		public URL getResource() {
			return resource;
		}

		/**
		 * @return the lineNo
		 */
		public int getLineNo() {
			return lineNo;
		}

		/**
		 * @return the firstCommentTagCharOffset
		 */
		public int getFirstCommentTagCharOffset() {
			return firstCommentTagCharOffset;
		}

		/**
		 * @return the commentLines
		 */
		public List<String> getCommentLines() {
			return commentLines;
		}

		/**
		 * @return the proceedingWhiteSpaceLines
		 */
		public int getNumberOfLinesIncludingBlanks() {
			return numberOfLinesIncludingBlanks;
		}

		/**
		 * @return the charOffsetOfProceedingNonWhiteSpaceChar
		 */
		public int getCharOffsetOfProceedingNonWhiteSpaceChar() {
			return charOffsetOfProceedingNonWhiteSpaceChar;
		}

		/**
		 * @return the commentcomparator
		 */
		public static Comparator<Comment> getCommentcomparator() {
			return commentComparator;
		}

		private static final Comparator<URL> resourceComparator = Comparator.nullsLast((a, b) -> {
			return a.toExternalForm().compareTo(b.toExternalForm());
		});

		public static final Comparator<Comment> commentComparator = Comparator
				.nullsLast(Comparator.comparing(Comment::getResource, resourceComparator)
						.thenComparingInt(Comment::getLineNo).thenComparingInt(Comment::getFirstCommentTagCharOffset));

		@Override
		public int compareTo(Comment o) {
			return commentComparator.compare(this, o);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Comment [resource=");
			builder.append(resource);
			builder.append(", lineNo=");
			builder.append(lineNo);
			builder.append(", firstCommentTagCharOffset=");
			builder.append(firstCommentTagCharOffset);
			builder.append(", commentLines=");
			builder.append(commentLines);
			builder.append(", innerBlankLines=");
			builder.append(innerBlankLines);
			builder.append(", numberOfLinesIncludingBlanks=");
			builder.append(numberOfLinesIncludingBlanks);
			builder.append(", charOffsetOfProceedingNonWhiteSpaceChar=");
			builder.append(charOffsetOfProceedingNonWhiteSpaceChar);
			builder.append("]");
			return builder.toString();
		}

		private static String prefixString(String prefix, String toPrefix) {
			StringBuilder sb = new StringBuilder();
			int octothorpIndex = toPrefix.indexOf('#');
			if (octothorpIndex > -1) {
				while (toPrefix.codePointAt(octothorpIndex) == "#".codePointAt(0)) {
					octothorpIndex += 1;
				}
			} else {
				octothorpIndex = 0;
			}
			Charset codePointCharSet = Charset.forName("UTF-32");
			String toPrefixPrefix = toPrefix.substring(0, octothorpIndex);
			toPrefix = toPrefix.substring(octothorpIndex);
			try {

				CharsetDecoder decoder = codePointCharSet.newDecoder();
				CharBuffer cb = decoder.decode(ByteBuffer.wrap(toPrefix.getBytes(codePointCharSet)));

				System.out.println(new String("\t##".getBytes()) + cb);
			} catch (CharacterCodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			List<Integer> asInts = new ArrayList<Integer>();
			toPrefixPrefix.codePoints().forEach(asInts::add);
			prefix.codePoints().forEach(asInts::add);
			toPrefix.codePoints().forEach(asInts::add);
			System.out.println(asInts);
			asInts.stream().mapToInt(Integer::intValue).forEach(codePoint -> sb.appendCodePoint(codePoint));

			return sb.toString();
		}

	}

	public static List<Comment> readComments(Path path) {
		return readComments(path != null ? path.toUri() : null);
	}

	public static List<Comment> readComments(URI uri) {
		if (uri != null) {
			URL asURL = null;
			try {
				asURL = uri.toURL();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			return readComments(asURL);
		} else {
			return Collections.emptyList();
		}

	}
	
	public static List<Comment> readComments(URL url, List<Node> nodes){
		List<ScalarNode> toUse = Collections.emptyList();
		if(nodes==null) {
			if(url!=null) {
				toUse = new ArrayList<ScalarNode>();
				nodes = YamlConfigObjConverter.getRootNodes(url);
				for(int i=0;i<nodes.size();i++) {
					Node current = nodes.get(i);
					getScalarNodes(toUse, current);
				}
			}
		}
		List<ScalarNode> scalarNodes = toUse;
		List<CommentTracker> commentsTrackers = Collections.emptyList();
		if (url != null) {
			try (Stream<String> lines = lines(url)) {
				commentsTrackers = readComments(lines.iterator(),scalarNodes);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		List<Comment> cmnts = commentsTrackers.stream().map(ct -> ct.toComment(url)).sorted()
				.collect(Collectors.toList());
		return cmnts;
	}

	public static List<Comment> readComments(URL url) {
		return readComments(url, null);
		
	}

	private static List<CommentTracker> readComments(Iterator<String> iterator,List<ScalarNode> scalarNodes) {
		int lineNo = 0;
		CommentTracker commentTracker = new CommentTracker();
		ArrayList<CommentTracker> commentTrackers = new ArrayList<CommentTracker>();
		while (iterator.hasNext()) {
			String line = iterator.next();
			boolean completed =false;
			if(commentTracker.getCommentLines().size()==1&&commentTracker.getFirstCommentLineNo()!=-1&&commentTracker.isInline()) {
				if(!line.isBlank()&&!COMMENT_CONTINUED.matcher(line).matches()) {
					for(int i=0;i<line.length();i++) {
						int currentChar=line.charAt(i);
						if(!Character.isWhitespace(currentChar)) {
							commentTracker.nonCommentCharOffset=i;
							break;
						}
					}
					completed=true;
					if(completed) {
						commentTrackers.add(commentTracker);
						commentTracker = new CommentTracker();
						completed=false;
					}
				}
			}
			if(!completed) {
				completed = commentTracker.process(line, lineNo,scalarNodes);
			}
			
			if (completed) {
				commentTrackers.add(commentTracker);
				commentTracker = new CommentTracker();
			}
			lineNo++;
		}
		if (commentTracker.getFirstCommentLineNo() != -1) {
			commentTrackers.add(commentTracker);
		}
		return commentTrackers;
	}

	private static Stream<String> lines(URL url) throws IOException {
		return url != null ? lines(url.openStream()) : Stream.empty();
	}

	private static Stream<String> lines(InputStream is) {
		return is != null ? lines(new UnicodeReader(is)) : Stream.empty();
	}

	private static Stream<String> lines(Reader r) {
		return r != null ? lines(new BufferedReader(r)) : Stream.empty();
	}

	private static Stream<String> lines(BufferedReader br) {
		return br != null ? br.lines() : Stream.empty();
	}

	static class CommentTracker {
		private boolean inline=false;
		private int firstCommentLineNo = -1;
		private int firstCommentTagOffset = -1;
		private BitSet innerBlankLines = new BitSet();
		private int nonCommentCharOffset = -1;
		private int innerLineIndex = -1;
		private ArrayList<String> commentLines = new ArrayList<String>();

		public CommentTracker() {

		}

		public Comment toComment(URL resource) {
			return new Comment(resource, getFirstCommentLineNo(), getFirstCommentTagOffset(), getInnerBlankLines(),
					getInnerLineIndex() + 1, getNonCommentCharOffset(), getCommentLines(),isInline());
		}

		/**
		 * @return the innerLineIndex
		 */
		public int getInnerLineIndex() {
			return innerLineIndex;
		}

		/**
		 * @param innerLineIndex the innerLineIndex to set
		 */
		public void setInnerLineIndex(int innerLineIndex) {
			this.innerLineIndex = innerLineIndex;
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
		
		
		public static final Comparator<Mark> markComparator = Comparator.nullsLast(Comparator.comparingInt(Mark::getLine)).thenComparing(Mark::getIndex);
		private static final Comparator<ScalarNode> scalarNodeComparator = Comparator.nullsLast(Comparator.comparing(ScalarNode::getStartMark, markComparator));
		private static boolean ScalarNodeContains(ScalarNode node, int line, int offset) {
			Mark start = node.getStartMark();
			Mark end = node.getEndMark();
			int startLine = start.getLine();
			int endLine = end.getLine();
			int startColumn=start.getColumn();
			int endColumn=end.getColumn();
			return ((line>startLine||(line==startLine&&offset>=startColumn))&&
					(line<endLine||(line==endLine&&offset<endColumn)));
		}
		
		private static List<ScalarNode> getScalarNodesInLineNumber(int line, List<ScalarNode> nodes) {
			return nodes.stream().filter(node->node.getStartMark().getLine()<=line&&node.getEndMark().getLine()>=line).sorted(scalarNodeComparator).collect(Collectors.toList());
		}
		private static ScalarNode getScalarNodeInLineNumber(int line, List<ScalarNode> nodes) {
			List<ScalarNode> foundNodes = getScalarNodesInLineNumber(line, nodes);
			if(!foundNodes.isEmpty()) {
				return foundNodes.get(foundNodes.size()-1);
			}
			return null;
		}
		
		/**
		 * 
		 * @param line
		 * @param lineNo
		 * @return {@code true} if this is done processing
		 */
		public boolean process(String line, int lineNo, List<ScalarNode> scalarNodes) {
			if(line.contains("server-addr")) {
				System.out.println("hmm");
			}
			boolean truth = false;
			ScalarNode containingNode = firstCommentLineNo==-1?getScalarNodeInLineNumber(lineNo, scalarNodes):null;
			if(containingNode!=null) {
				if(lineNo<containingNode.getEndMark().getLine()) {
					return false;
				}
			}
			if (line.isBlank()) {
				if (firstCommentLineNo != -1) {
					innerBlankLines.set(innerLineIndex, true);
				}
			} else {
				Pattern toUse = firstCommentLineNo == -1 ? COMMENT : COMMENT_CONTINUED;
				Matcher m = toUse.matcher(line);
				if(containingNode!=null) {
					if(containingNode.getEndMark().getLine()==lineNo) {
						int endMarkCol = containingNode.getEndMark().getColumn();
						try {
						m=m.region(endMarkCol, line.length());
						}catch(IndexOutOfBoundsException e) {
							e.printStackTrace();
						}
					}
				}
				if (m.matches()) {
					if (firstCommentLineNo == -1) {
						this.innerLineIndex = 0;
						String nonCommentData = m.group(1);
						String commentData = m.group(2);
						firstCommentTagOffset = m.start(2);
						firstCommentLineNo = lineNo;
						isInline(line);
						this.commentLines.add(commentData);
						
					} else {
						String commentData = m.group(1);
						this.commentLines.add(commentData);
					}
				} else if (firstCommentLineNo != -1) {
					toUse = NON_COMMENT;
					m = toUse.matcher(line);
					if (m.matches()) {
						truth = true;
						this.nonCommentCharOffset = m.group(1).length();
					}
				}
			}
			if (!truth && firstCommentLineNo != -1) {
				innerLineIndex += 1;
			}
			return truth;
		}
		private static final IntPredicate isWhiteSpace = Character::isWhitespace;
		private static final IntPredicate isNotWhiteSpace=isWhiteSpace.negate();
		private boolean isInline(String line) {
			if(this.innerLineIndex==0) {
				this.inline=line.codePoints().limit(this.firstCommentTagOffset).anyMatch(isNotWhiteSpace);
			}
			return this.inline;
		}
		
		public boolean isInline() {
			return this.inline;
		}

		/**
		 * @return the firstCommentLineNo
		 */
		public int getFirstCommentLineNo() {
			return firstCommentLineNo;
		}

		/**
		 * @param firstCommentLineNo the firstCommentLineNo to set
		 */
		public void setFirstCommentLineNo(int firstCommentLineNo) {
			this.firstCommentLineNo = firstCommentLineNo;
		}

		/**
		 * @return the firstCommentTagOffset
		 */
		public int getFirstCommentTagOffset() {
			return firstCommentTagOffset;
		}

		/**
		 * @param firstCommentTagOffset the firstCommentTagOffset to set
		 */
		public void setFirstCommentTagOffset(int firstCommentTagOffset) {
			this.firstCommentTagOffset = firstCommentTagOffset;
		}

		/**
		 * @return the innerBlankLines
		 */
		public BitSet getInnerBlankLines() {
			return innerBlankLines;
		}

		/**
		 * @param innerBlankLines the innerBlankLines to set
		 */
		public void setInnerBlankLines(BitSet innerBlankLines) {
			this.innerBlankLines = innerBlankLines;
		}

		/**
		 * @return the nonCommentCharOffset
		 */
		public int getNonCommentCharOffset() {
			return nonCommentCharOffset;
		}

		/**
		 * @param nonCommentCharOffset the nonCommentCharOffset to set
		 */
		public void setNonCommentCharOffset(int nonCommentCharOffset) {
			this.nonCommentCharOffset = nonCommentCharOffset;
		}

		/**
		 * @return the commentLines
		 */
		public ArrayList<String> getCommentLines() {
			return commentLines;
		}

		/**
		 * @param commentLines the commentLines to set
		 */
		public void setCommentLines(ArrayList<String> commentLines) {
			this.commentLines = commentLines;
		}

		public void reset() {
			this.firstCommentLineNo = -1;
			this.firstCommentTagOffset = -1;
			this.innerBlankLines = new BitSet();
			this.nonCommentCharOffset = -1;
			this.innerLineIndex = 0;
			this.commentLines = new ArrayList<String>();
		}

	}
	
	
	
	
	
	static List<ScalarNode> getScalarNodes(Node node){
		if(node==null) {
			return Collections.emptyList();
		}
		return getScalarNodes(new ArrayList<ScalarNode>(), node);
	}
	
	static List<ScalarNode> getScalarNodes(List<ScalarNode> result, Node node){
		if(result==null) {
			result=new ArrayList<ScalarNode>();
		}
		if(node==null) {
			return Collections.emptyList();
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
	static List<ScalarNode> getScalarNodes(List<ScalarNode> result, NodeTuple tuple){
		if(result==null) {
			result=new ArrayList<ScalarNode>();
		}
		getScalarNodes(result, tuple!=null?tuple.getKeyNode():null);
		getScalarNodes(result, tuple!=null?tuple.getValueNode():null);
		return result;
	}

}
