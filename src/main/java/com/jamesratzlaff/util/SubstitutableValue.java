package com.jamesratzlaff.util;

import java.io.Serializable;
/**
 * 
 * @author jamesratzlaff
 *
 */
public class SubstitutableValue implements CharSequence, Serializable, Cloneable {

	public static final String placeholderStart = "${";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1816643473773575634L;
	private final boolean isPlaceholder;
	private final boolean isOptional;
	private final CharSequence value;
	
	public SubstitutableValue(CharSequence value) {
		this(isPlaceHolder(value),isPlaceHolder(value)?value.subSequence(placeholderStart.length(), value.length()-1):value);
	}
	
	private static boolean isPlaceHolder(CharSequence value) {
		if(value==null) {
			return false;
		}
		String asString = value.toString();
		return asString.startsWith(placeholderStart)&&asString.endsWith("}");
	}
	
	public SubstitutableValue(boolean isPlaceHolder, CharSequence value) {
		this(isPlaceHolder,value!=null&&value.charAt(0)=='?',value!=null?value.charAt(0)=='?'?value.subSequence(1, value.length()):value:value);
	}
	public SubstitutableValue(boolean isPlaceHolder, boolean isOptional, CharSequence value) {
		this.isPlaceholder=isPlaceHolder;
		this.isOptional=isOptional;
		this.value=value;
	}
	
	
	@Override
	public int length() {
		int len = (isPlaceholder?2:0)+(isOptional?1:0)+value.length();
		return len;
	}
	@Override
	public char charAt(int index) {
		return toString().charAt(0);
	}
	@Override
	public CharSequence subSequence(int start, int end) {
		return toString().subSequence(start, end);
	}
	
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(isPlaceholder) {
			sb.append(SubstitutableValue.placeholderStart);
			
		}
		if(isOptional) {
			sb.append('?');
		}
		sb.append(value);
		if(isPlaceholder) {
			sb.append('}');
		}
		return sb.toString();
	}
	

	public SubstitutableValue clone() {
		SubstitutableValue clone = null;
		try {
			clone=(SubstitutableValue)super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return clone;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isOptional ? 1231 : 1237);
		result = prime * result + (isPlaceholder ? 1231 : 1237);
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		SubstitutableValue other = (SubstitutableValue) obj;
		if (isOptional != other.isOptional)
			return false;
		if (isPlaceholder != other.isPlaceholder)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	
	
	
	
}
