package com.jamesratzlaff.typesafe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

		public Comment(URL resource, int lineNo, int firstCommentTagCharOffset, BitSet innerBlankLines,
				int numberOfLinesIncludingBlanks, int charOffsetOfProceedingNonWhiteSpaceChar,
				List<String> commentLines) {
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
		}

		public Comment copy() {
			return new Comment(resource, lineNo, firstCommentTagCharOffset, innerBlankLines,
					numberOfLinesIncludingBlanks, charOffsetOfProceedingNonWhiteSpaceChar,
					new ArrayList<String>(commentLines));
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
			return "Comment [resource=" + resource + ", lineNo=" + lineNo + ", firstCommentTagCharOffset="
					+ firstCommentTagCharOffset + ", commentLines=" + commentLines + ", innerBlankLines="
					+ innerBlankLines + ", numberOfLinesIncludingBlanks=" + numberOfLinesIncludingBlanks
					+ ", charOffsetOfProceedingNonWhiteSpaceChar=" + charOffsetOfProceedingNonWhiteSpaceChar + "]";
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

	public static List<Comment> readComments(URL url) {
		List<CommentTracker> commentsTrackers = Collections.emptyList();
		if (url != null) {
			try (Stream<String> lines = lines(url)) {
				commentsTrackers = readComments(lines.iterator());
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		List<Comment> cmnts = commentsTrackers.stream().map(ct -> ct.toComment(url)).sorted()
				.collect(Collectors.toList());
		return cmnts;
	}

	private static List<CommentTracker> readComments(Iterator<String> iterator) {
		int lineNo = 0;
		CommentTracker commentTracker = new CommentTracker();
		ArrayList<CommentTracker> commentTrackers = new ArrayList<CommentTracker>();
		while (iterator.hasNext()) {
			String line = iterator.next();
			boolean completed = commentTracker.process(line, lineNo);
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
		return is != null ? lines(new InputStreamReader(is)) : Stream.empty();
	}

	private static Stream<String> lines(Reader r) {
		return r != null ? lines(new BufferedReader(r)) : Stream.empty();
	}

	private static Stream<String> lines(BufferedReader br) {
		return br != null ? br.lines() : Stream.empty();
	}

	static class CommentTracker {
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
					getInnerLineIndex() + 1, getNonCommentCharOffset(), getCommentLines());
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

		/**
		 * 
		 * @param line
		 * @param lineNo
		 * @return {@code true} if this is done processing
		 */
		public boolean process(String line, int lineNo) {
			boolean truth = false;
			if (line.isBlank()) {
				if (firstCommentLineNo != -1) {
					innerBlankLines.set(innerLineIndex, true);
				}
			} else {
				Pattern toUse = firstCommentLineNo == -1 ? COMMENT : COMMENT_CONTINUED;
				Matcher m = toUse.matcher(line);
				if (m.matches()) {
					if (firstCommentLineNo == -1) {
						String nonCommentData = m.group(1);
						String commentData = m.group(2);
						firstCommentLineNo = lineNo;
						firstCommentTagOffset = nonCommentData.length();
						this.commentLines.add(commentData);
						this.innerLineIndex = 0;
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

}
