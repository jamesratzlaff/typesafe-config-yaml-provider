package com.jamesratzlaff.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SubstitutableValues implements List<SubstitutableValue>, CharSequence, Serializable, Cloneable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8419565131992809947L;
	private final List<SubstitutableValue> delegate;

	public SubstitutableValues(String value) {
		this(TokenUtils.getJoinableTokens(value));
	}

	public SubstitutableValues(List<? extends CharSequence> values) {
		if (values == null) {
			values = Collections.emptyList();
		}
		this.delegate = new ArrayList<SubstitutableValue>(values.size());
		this.delegate.addAll(values.stream()
				.map(value -> (value != null && value instanceof SubstitutableValue) ? (SubstitutableValue) value
						: new SubstitutableValue(value))
				.collect(Collectors.toList()));

	}

	public void forEach(Consumer<? super SubstitutableValue> action) {
		delegate.forEach(action);
	}

	public int size() {
		return delegate.size();
	}

	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	public boolean contains(Object o) {
		return delegate.contains(o);
	}

	public Iterator<SubstitutableValue> iterator() {
		return delegate.iterator();
	}

	public Object[] toArray() {
		return delegate.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return delegate.toArray(a);
	}

	public boolean add(SubstitutableValue e) {
		return delegate.add(e);
	}

	public boolean remove(Object o) {
		return delegate.remove(o);
	}

	public boolean containsAll(Collection<?> c) {
		return delegate.containsAll(c);
	}

	public boolean addAll(Collection<? extends SubstitutableValue> c) {
		return delegate.addAll(c);
	}

	public boolean addAll(int index, Collection<? extends SubstitutableValue> c) {
		return delegate.addAll(index, c);
	}

	public boolean removeAll(Collection<?> c) {
		return delegate.removeAll(c);
	}

	public <T> T[] toArray(IntFunction<T[]> generator) {
		return delegate.toArray(generator);
	}

	public boolean retainAll(Collection<?> c) {
		return delegate.retainAll(c);
	}

	public void replaceAll(UnaryOperator<SubstitutableValue> operator) {
		delegate.replaceAll(operator);
	}

	public void sort(Comparator<? super SubstitutableValue> c) {
		delegate.sort(c);
	}

	public void clear() {
		delegate.clear();
	}

	public boolean equals(Object o) {
		return delegate.equals(o);
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public SubstitutableValue get(int index) {
		return delegate.get(index);
	}

	public boolean removeIf(Predicate<? super SubstitutableValue> filter) {
		return delegate.removeIf(filter);
	}

	public SubstitutableValue set(int index, SubstitutableValue element) {
		return delegate.set(index, element);
	}

	public void add(int index, SubstitutableValue element) {
		delegate.add(index, element);
	}

	public SubstitutableValue remove(int index) {
		return delegate.remove(index);
	}

	public int indexOf(Object o) {
		return delegate.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return delegate.lastIndexOf(o);
	}

	public ListIterator<SubstitutableValue> listIterator() {
		return delegate.listIterator();
	}

	public ListIterator<SubstitutableValue> listIterator(int index) {
		return delegate.listIterator(index);
	}

	public List<SubstitutableValue> subList(int fromIndex, int toIndex) {
		return delegate.subList(fromIndex, toIndex);
	}

	public Spliterator<SubstitutableValue> spliterator() {
		return delegate.spliterator();
	}

	public Stream<SubstitutableValue> stream() {
		return delegate.stream();
	}

	public Stream<SubstitutableValue> parallelStream() {
		return delegate.parallelStream();
	}

	@Override
	public int length() {
		return this.delegate.stream().mapToInt(val -> val.length()).sum();
	}

	@Override
	public char charAt(int index) {
		return toString().charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return toString().subSequence(start, end);
	}

	@Override
	public String toString() {
		return String.join("", delegate);
	}

	public SubstitutableValues clone() {
		List<SubstitutableValue> d = new ArrayList<SubstitutableValue>(this.delegate.size());
		for(int i=0;i<this.delegate.size();i++) {
			SubstitutableValue current = this.delegate.get(i);
			d.add(current.clone());
		}
		return new SubstitutableValues(d);
	}

}
