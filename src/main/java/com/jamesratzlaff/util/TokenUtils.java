package com.jamesratzlaff.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class TokenUtils {

	private static final String opChars = "%`\"{[(<";
	private static final String clChars = "%`\"}])>";
	private static final String qtChars = "`\"";
	private static final String esc = "\\";

	public static List<Range> getPlaceHolderRanges(String cs){
		Map<Character,List<Range>> map = getIndexOfBalancedCharacter(cs);
		List<Range> openCurly = map.get(SubstitutableValue.placeholderStart.charAt(SubstitutableValue.placeholderStart.length()-1));
		if(openCurly==null) {
			openCurly=Collections.emptyList();
		}
		int indexDelta = SubstitutableValue.placeholderStart.length()-1;//the length of the token minus the single char len
		ArrayList<Range> shiftedRanges = new ArrayList<Range>();
		for(int i=0;i<openCurly.size();i++) {
			Range openCurlyRange = openCurly.get(i);
			int openCurlyStart = openCurlyRange.open();
			int expectedPlaceHolderTokenIndex = openCurlyStart-indexDelta;
			if(expectedPlaceHolderTokenIndex>-1&&expectedPlaceHolderTokenIndex<cs.length()-1) {
				if(cs.indexOf(SubstitutableValue.placeholderStart,expectedPlaceHolderTokenIndex)==expectedPlaceHolderTokenIndex) {
					shiftedRanges.add(openCurlyRange.withOpenIndex(expectedPlaceHolderTokenIndex));
				}
			}
		}
		return shiftedRanges;
		
	}
	
	public static List<CharSequence> getJoinableTokens(String cs) {
		if("${_.0.spring.cloud.nacos.config.ext-bonfig.0.derf}".equals(cs)) {
			System.out.println("whoop");
		}
		List<Range> ranges = Range.fillIn(Range.removeRangesContainedByOthers(getPlaceHolderRanges(cs)),cs.length());
		return ranges.stream().map(range->range.subSequence(cs)).collect(Collectors.toList());
	}
	
	
	/**
	 * 
	 * @param str a {@link CharSequence} to index characters in
	 * @return a {@link Map}{@link Character &lt;Character,}{@link List}{@link Range
	 *         Range&gt;&gt;} that maps to opening character to their respective
	 *         {@link Range ranges}
	 */
	public static Map<Character, List<Range>> getIndexOfBalancedCharacter(CharSequence str) {
		return getIndexOfBalancedCharacter(str, 0);
	}

	static Map<Character, List<Range>> getIndexOfBalancedCharacter(CharSequence str, int fromIndex) {
		return getIndexOfBalancedCharacter(str, opChars, clChars, fromIndex);
	}

	static Map<Character, List<Range>> getIndexOfBalancedCharacter(CharSequence str, String openChars,
			String closeChars, int fromIndex) {
		return getIndexOfBalancedCharacter(str, openChars, closeChars, qtChars, fromIndex);
	}

	static Map<Character, List<Range>> getIndexOfBalancedCharacter(CharSequence str, String openChars,
			String closeChars, String literalChars, int fromIndex) {
		return getIndexOfBalancedCharacter(str, openChars, closeChars, literalChars, esc, fromIndex);
	}

	private static Map<Character, List<Range>> getIndexOfBalancedCharacter(CharSequence str, String openChars,
			String closeChars, String literalChars, String escape, int fromIndex) {
		Map<Character, List<Range>> resoMap = new LinkedHashMap<Character, List<Range>>();
		Stack<Character> charStack = new Stack<Character>();
		Stack<Integer> openIndex = new Stack<Integer>();
		char inCurrentLiteral = '\0';
		for (int i = fromIndex; i < str.length(); i++) {
			char current = str.charAt(i);
			if (opChars.indexOf(current) != -1) {
				if (!isEscaped(str, i, escape)) {
					if (inCurrentLiteral == '\0') {
						charStack.push(current);
						openIndex.push(i);
						if (literalChars.indexOf(current) != -1) {
							inCurrentLiteral = current;
						}
						continue;
					}
				}
			}

			if (clChars.indexOf(current) != -1 && !isEscaped(str, i, escape)) {
				Character openChar = null;
				if (inCurrentLiteral != '\0') {
					if (current == inCurrentLiteral) {
						openChar = charStack.pop();
						inCurrentLiteral = '\0';
					}
				} else {
					openChar = charStack.pop();
				}
				if (openChar != null) {
					int start = openIndex.pop();
					List<Range> ranges = resoMap.get(openChar);
					if (ranges == null) {
						ranges = new ArrayList<Range>();
						resoMap.put(openChar, ranges);
					}
					ranges.add(new Range(start, i));
				}
			}
		}
		return resoMap;
	}

	/**
	 * This class is used to represent the index in which an opening character was
	 * found and the index of its balancing close character
	 * 
	 * @author jamesratzlaff
	 *
	 */
	static class Range implements Comparable<Range>, Serializable, Cloneable {

		private static final long serialVersionUID = -5055004322404887078L;
		private final int open;
		private final int close;

		public Range(int open, int close) {
			int realOpen = Math.min(open, close);
			int realClose = Math.max(open, close);
			this.open = realOpen;
			this.close = realClose;
		}

		public boolean contains(Range other) {
			return (contains(other.open) && contains(other.close))
					|| (contains(other.open) && other.close <= this.close)
					|| (contains(other.close) && other.open >= this.open);
		}
		
		public Range breed(int index) {
			
			if(index<this.open) {
				return new Range(index,this.open-1);
			} else if(index>this.close) {
				return new Range(this.close+1,index);
			}else{
				return this.clone();
			}
			
		}
		
		public int open() {
			return this.open;
		}
		
		public int close() {
			return this.close;
		}
		
		public int leftOverlap(int index) {
			return this.close-index;
		}
		
		public int leftOverlap(Range range) {
			return leftOverlap(range.open);
		}
		
		public int rightOverlap(int index) {
			return this.open-index;
		}
		
		public boolean isLeftOf(int index) {
			return this.close<index;
		}
		
		public boolean isRightOf(int index) {
			return this.open>index;
		}
		
		public boolean isLeftOf(Range range) {
			return isLeftOf(range.open);
		}
		
		public boolean isRightOf(Range range) {
			return isRightOf(range.close);
		}
		
		public int rightOverlap(Range range) {
			return rightOverlap(range.close);
		}
		
		public boolean isLeftAndAdjacentTo(int index) {
			return isLeftOf(index)&&(this.close+1)==index;
		}
		
		public boolean isLeftAndAdjacentTo(Range range) {
			return isLeftAndAdjacentTo(range.open);
		}
		
		public boolean isRightAndAdjacentTo(int index) {
			return isRightOf(index)&&(this.open-1)==index;
		}
		
		public boolean isRightAndAdjacentTo(Range range) {
			return isRightAndAdjacentTo(range.close);
		}
		 
		public Range breed(Range other) {
			Range r = this.clone();
			if(other==null||other.equals(this)||this.contains(other)) {
				return r;
			} else if(other.contains(this)) {
				r=other.clone();
			} else if(this.isLeftOf(other)) {
				r=new Range(this.close+1,other.open-1);
			} else if(this.isRightOf(other)) {
				r=new Range(other.close+1,this.open-1);
			}
			return r;
			
		}
		public static List<Range> removeRangesContainedByOthers(Iterable<Range> ranges){
			List<Range> asList = StreamSupport.stream(ranges.spliterator(), false).sorted().collect(Collectors.toList());
			List<Range> reduced = new ArrayList<Range>(asList.stream().filter(range->!isContainedInAny(range, ranges)).collect(Collectors.toList()));
			Collections.sort(reduced);
			return reduced;
		}
		public static List<Range> fillIn(Iterable<Range> ranges, int len){
			List<Range> reduced = removeRangesContainedByOthers(ranges);
			Collections.sort(reduced);
			List<Range> result = new ArrayList<Range>();
			Range previous = null;
			for(int i=0;i<reduced.size();i++) {
				Range current=reduced.get(i);
				if(i==0&&current.open>0) {
					previous=current.breed(0);
					result.add(current.breed(0));
				}
				if(previous!=null) {
					if(!previous.isLeftAndAdjacentTo(current)) {
						result.add(previous.breed(current));
					}
				}
				
				result.add(current);
				previous=current;
			}
			int start=result.isEmpty()?0:result.get(result.size()-1).close+1;
			int end=len-1;
			if(start<end) {
				result.add(new Range(start,end));
			}
			
			return result;
			
		}
		
		public static boolean isContainedInAny(Range range, Iterable<Range> ranges) {
			return StreamSupport.stream(ranges.spliterator(), false).anyMatch(r->r.contains(range));
		}
		
		/**
		 * This method is like {@link #contains(int)} but includes {@code open} and {@code close}
		 * @param index
		 * @return 
		 */
		public boolean has(int index) {
			return close>=index && index>=open;
		}

		/**
		 * 
		 * @param index the value to check to see if it is in range
		 * @return {@code true} if the value is in between open (exclusive) and close
		 *         (exclusive), otherwise {@code false}
		 */
		public boolean contains(int index) {
			return close > index && index > open;
		}

		public int length() {
			return length(false);
		}
		
		/**
		 * 
		 * @param exclusive if {@code true} the resulting value will be the internal
		 *                  length (everything in between open and close) otherwise open
		 *                  and close are taken into consideration
		 * @return the length
		 */
		public int length(boolean exclusive) {
			
			int len = ((close - open)+1) + (exclusive?-2:0);
			if(len<0) {
				len=0;
			}
			return len;
		}

		public Range deltaOpen(int delta) {
			if (delta != 0) {
				return new Range(open + delta, close);
			}
			return this;
		}

		/**
		 * 
		 * @param cs the CharSequence to create a subSequenceFrom
		 * @return the value resulting from calling
		 *         {@link #subSequence(CharSequence, boolean) subSequence(cs, false)}
		 * @see #subSequence(CharSequence, boolean)
		 */
		public CharSequence subSequence(CharSequence cs) {
			return subSequence(cs, false);
		}

		/**
		 * 
		 * @param cs        the CharSequence to create a subSequenceFrom
		 * @param exclusive if {@code true} the chars at {@code this.open} and
		 *                  {@code this.close} will not be included in the resulting
		 *                  subsequence
		 * @return a subsequence of characters
		 */
		public CharSequence subSequence(CharSequence cs, boolean exclusive) {
			CharSequence sub = null;
			if (cs != null) {
				int start = exclusive ? this.open + 1 : this.open;
				int end = exclusive ? this.close : this.close+1;
				sub = cs.subSequence(start,end);
			}
			return sub;
		}

		public Range withOpenIndex(int open) {
			if (open != this.open) {
				return new Range(open, close);
			}
			return this;
		}

		public Range deltaClose(int delta) {
			if (delta != 0) {
				return new Range(open, close + delta);
			}
			return this;
		}

		public Range withCloseIndex(int close) {
			if (close != this.close) {
				return new Range(open, close);
			}
			return this;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + close;
			result = prime * result + open;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Range other = (Range) obj;
			if (close != other.close)
				return false;
			if (open != other.open)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Range [open=" + open + ", close=" + close + "]";
		}

		@Override
		public int compareTo(Range o) {
			if (o == null) {
				return -1;
			}
			if (this.equals(o)) {
				return 0;
			}
			int cmp = 0;
			if (cmp == 0) {
				cmp = Integer.compare(this.open, o.open);
			}
			if (cmp == 0) {
				cmp = Integer.compare(o.length(), this.length());
			}

			return cmp;
		}
		
		public Range clone() {
			Range c = null;
			try {
				c=(Range)super.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			return c;
		}

	}

	private static boolean isEscaped(CharSequence str, int index, String escapeSequence) {
		boolean isEscaped = false;
		while (index > -1 && index < str.length() && index - escapeSequence.length() > -1) {
			CharSequence sub = str.subSequence(index - escapeSequence.length(), index);
			if (sub.toString().equals(escapeSequence)) {
				isEscaped = !isEscaped;
				index -= escapeSequence.length();
			} else {
				break;
			}
		}
		return isEscaped;
	}


}
