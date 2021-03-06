// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.temporal.operands;

import java.util.Iterator;
import java.util.List;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.temporal.ExtentTester;
import com.bbn.parliament.jena.graph.index.temporal.TemporalPropertyFunction;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInstant;
import com.bbn.parliament.jena.graph.index.temporal.memory.MemoryTemporalIndex;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.NiceIterator;

/** @author dkolas */
public abstract class MemoryOperatorImplementation extends TemporalPropertyFunction<MemoryTemporalIndex> {
	public MemoryOperatorImplementation(ExtentTester extentTester) {
		super(MemoryTemporalIndex.class, extentTester);
	}

	protected Iterator<Record<TemporalExtent>> wrapIterator(
		Iterator<TemporalInstant> internal) {
		return new NodeExtentIterator(getIndex(), internal);
	}

	/**
	 * This isn't exactly the most efficient way to do this, but it will do for now.
	 *
	 * @see com.bbn.parliament.jena.graph.index.temporal.TemporalPropertyFunction#estimateFirstVar(com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent)
	 */
	@Override
	public long estimateFirstVar(TemporalExtent boundExtent) {
		Iterator<Record<TemporalExtent>> result = bindFirstVar(boundExtent);
		long count = 0L;
		while (result.hasNext()) {
			count++;
			result.next();
		}
		return count;
	}

	@Override
	public long estimateSecondVar(TemporalExtent boundExtent) {
		Iterator<Record<TemporalExtent>> result = bindSecondVar(boundExtent);
		long count = 0L;
		while (result.hasNext()) {
			count++;
			result.next();
		}
		return count;
	}

	private class NodeExtentIterator implements ClosableIterator<Record<TemporalExtent>> {
		private Iterator<TemporalInstant> items;
		private MemoryTemporalIndex memIndex;
		private Iterator<Node> nodes = null;
		private TemporalExtent current = null;

		public NodeExtentIterator(MemoryTemporalIndex index,
			Iterator<TemporalInstant> internal) {
			this.memIndex = index;
			this.items = internal;
		}

		@Override
		public boolean hasNext() {
			if (current == null) {
				if (!items.hasNext()) {
					return false;
				}
				current = items.next();
				List<Node> ns = memIndex.getExtentsToNodes().get(current);
				nodes = ns.iterator();
			}
			if (!nodes.hasNext() && !items.hasNext()) {
				return false;
			}
			if (!nodes.hasNext()) {
				current = items.next();
				List<Node> ns = memIndex.getExtentsToNodes().get(current);
				nodes = ns.iterator();
			}
			return nodes.hasNext();
		}

		@Override
		public Record<TemporalExtent> next() {
			Node n = nodes.next();
			return Record.create(n, current);

			// This is weird, putting the instant in the NodeExtent instead of the
			// interval that is really associated with the node, but the
			// InclusionDeciders need it.
			// return new Record<TemporalExtent>(index.extentToNode(extent), instant);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Cannot remove on this iterator!");
		}

		@Override
		public void close() {
			NiceIterator.close(items);
		}
	}
}
