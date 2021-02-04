package com.bbn.parliament.jena.graph.index.spatial.geosparql.function;

import java.util.List;

import com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes.GeoSPARQLLiteral;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionEnv;
import org.locationtech.jts.geom.Geometry;

/** @author rbattle */
public class Difference extends DoubleGeometrySpatialFunction {
	/** {@inheritDoc} */
	@Override
	protected NodeValue exec(Geometry g1, Geometry g2, GeoSPARQLLiteral datatype,
			Binding binding, List<NodeValue> evalArgs, String uri, FunctionEnv env) {
		Geometry toReturn = g1.difference(g2);
		toReturn.setUserData(g1.getUserData());
		return makeNodeValue(toReturn, datatype);
	}

	/** {@inheritDoc} */
	@Override
	protected String[] getRestOfArgumentTypes() {
		return new String[] { };
	}
}
