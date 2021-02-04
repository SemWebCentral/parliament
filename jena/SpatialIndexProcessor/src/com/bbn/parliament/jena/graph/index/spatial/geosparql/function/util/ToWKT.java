package com.bbn.parliament.jena.graph.index.spatial.geosparql.function.util;

import java.util.List;

import com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes.WKTLiteral;
import com.hp.hpl.jena.query.QueryBuildException;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.expr.ExprList;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionBase;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

/** @author rbattle */
public class ToWKT extends FunctionBase {
	@Override
	public NodeValue exec(List<NodeValue> args) {
		String wkt = args.get(0).getString();

		Geometry g;
		try {
			g = new WKTReader().read(wkt);
		} catch (ParseException e) {
			return NodeValue.nvNothing;
		}

		if (args.size() == 2) {
			int srid = args.get(1).getInteger().intValue();
			g.setSRID(srid);
			g.setUserData("EPSG:" + srid);
		}
		WKTLiteral lit = new WKTLiteral();
		String value = lit.unparse(g);
		return NodeValue.makeNode(ResourceFactory.createTypedLiteral(value, lit).asNode());
	}

	@Override
	public void checkBuild(String uri, ExprList args) {
		if (args.size() < 1) {
			throw new QueryBuildException("No arguments passed to " + uri);
		} else if (args.size() > 2) {
			throw new QueryBuildException("Too many arguments to " + uri);
		}
	}
}
