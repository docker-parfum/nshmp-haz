package org.opensha.eq.forecast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.opensha.data.DataUtils.validateWeight;

import java.util.Iterator;
import java.util.List;

import org.opensha.eq.fault.scaling.MagScalingType;
import org.opensha.geo.Location;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * Add comments here
 *
 * @author Peter Powers
 */
public class FaultSourceSet implements SourceSet<FaultSource> {

	private final List<FaultSource> sources;
	private final String name;
	private final double weight;
	private final MagScalingType msrType;
	
	// NOTE we're holding onto weight for reference, however, MFD
	// rates will have already been scaled in place. The weight value
	// may come in handy when trying to put together individual
	// logic tree branches.
	//
	// NOTE msrType is currently not exposed
	
	private FaultSourceSet(String name, double weight, MagScalingType msrType,
		List<FaultSource> sources) {
		this.name = name;
		this.weight = weight;
		this.msrType = msrType;
		this.sources = sources;
	}
	
	@Override
	public Iterable<FaultSource> locationIterable(Location loc) {
		// TODO
		return null;
	}	
	
	@Override
	public Iterator<FaultSource> iterator() {
		return sources.iterator();
	}

	@Override
	public String name() {
		return name;
	}
	
	@Override
	public double weight() {
		return weight;
	}

	@Override
	public int size() {
		return sources.size();
	}

	@Override
	public SourceType type() {
		return SourceType.FAULT;
	}


	static class Builder {

		// build() may only be called once
		// use Doubles to ensure fields are initially null

		static final String ID = "FaultSourceSet.Builder";
		boolean built = false;

		String name;
		Double weight;
		MagScalingType magScaling;
		List<FaultSource> sources = Lists.newArrayList();

		Builder name(String name) {
			checkArgument(!Strings.nullToEmpty(name).trim().isEmpty(),
				"Name may not be empty or null");
			this.name = name;
			return this;
		}
		
		Builder weight(double weight) {
			this.weight = validateWeight(weight);
			return this;
		}

		Builder magScaling(MagScalingType magScaling) {
			this.magScaling = checkNotNull(magScaling, "MagScalingType is null");
			return this;
		}
		
		Builder source(FaultSource source) {
			sources.add(checkNotNull(source, "FaultSource is null"));
			return this;
		}

		void validateState(String mssgID) {
			checkState(!built, "This %s instance as already been used", mssgID);
			checkState(name != null, "%s name not set", mssgID);
			checkState(weight != null, "%s weight not set", mssgID);
			checkState(magScaling != null, "%s mag-scaling relation not set", mssgID);
			built = true;
		}

		FaultSourceSet buildFaultSet() {
			validateState(ID);
			return new FaultSourceSet(name, weight, magScaling, sources);
		}
	}

}
