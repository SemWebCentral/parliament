// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2018, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.union;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.bbn.parliament.jena.graph.KbGraph;
import com.bbn.parliament.jena.graph.OptimizationMethod;
import com.bbn.parliament.jena.joseki.client.RDFFormat;
import com.bbn.parliament.jni.Config;
import com.bbn.parliament.jni.KbInstance;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/** @author dkolas */
public class UnionGraphTestNotJUnit {
	private static final String ONT_RSRC = "univ-bench.owl";
	private static final File INPUT_DATA_FILE = new File(
		System.getProperty("test.data.path"), "gendata-80.zip");
	private static final File KB_DATA_DIR = new File("./union-test-kb-data");

	private static final String QUERY1 = "" +
		"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
		"PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#>" +
		"SELECT ?x ?y ?z WHERE {" +
		"	?x a ub:GraduateStudent ." +
		"	?y a ub:University ." +
		"	?z a ub:Department ." +
		"	?x ub:memberOf ?z ." +
		"	?z ub:subOrganizationOf ?y ." +
		"	?x ub:undergraduateDegreeFrom ?y" +
		"}";
	private static final String QUERY2 = "" +
		"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
		"PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#>" +
		"SELECT ?x ?y1 ?y2 ?y3 WHERE {" +
		"	?x a ub:Professor;" +
		"		ub:worksFor <http://www.Department0.University0.edu>;" +
		"		ub:name ?y1;" +
		"		ub:emailAddress ?y2;" +
		"		ub:telephone ?y3." +
		"}";
	private static final String QUERY3 = "" +
		"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
		"PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#>" +
		"SELECT ?x ?y ?z WHERE {" +
		"	?x a ub:Student ." +
		"	?y a ub:Faculty ." +
		"	?z a ub:Course ." +
		"	?x ub:advisor ?y ." +
		"	?y ub:teacherOf ?z ." +
		"	?x ub:takesCourse ?z ." +
		"}";
	private static final String QUERY4 = "" +
		"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
		"PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#>" +
		"SELECT ?x ?y WHERE {" +
		"	?x a ub:GraduateStudent ;" +
		"		ub:name ?y ." +
		"}";
	private static final String[] QUERYS = { QUERY1, QUERY2, QUERY3, QUERY4 };

	public static void main(String[] args) throws IOException {
		try (
			KbGraph bigGraph = createGraph(KB_DATA_DIR, "big");
			KbGraph graph0 = createGraph(KB_DATA_DIR, "0");
			KbGraph graph1 = createGraph(KB_DATA_DIR, "1");
			KbGraph graph2 = createGraph(KB_DATA_DIR, "2");
		) {
			Model bigModel = createModel(bigGraph);

			Node graph0Name = Node.createURI("http://example.org/graph0");
			Model model0 = createModel(graph0);

			Node graph1Name = Node.createURI("http://example.org/graph1");
			Model model1 = createModel(graph1);

			Node graph2Name = Node.createURI("http://example.org/graph2");
			Model model2 = createModel(graph2);

			Node innerUnionName = Node.createURI("http://example.org/innner-union-graph");
			KbUnionGraph innerUnionGraph = new KbUnionGraph(graph0, graph0Name, graph1, graph1Name);

			KbUnionGraph unionGraph = new KbUnionGraph(innerUnionGraph, innerUnionName, graph2, graph2Name);
			Model unionModel = ModelFactory.createModelForGraph(unionGraph);

			//Load data
			loadUniversityData(bigModel, model0, 0);
			loadUniversityData(bigModel, model1, 1);
			loadUniversityData(bigModel, model2, 2);

			//unionGraph.setFiltering(false);
			for (int i = 0; i < QUERYS.length; ++i) {
				System.out.format("Running query #%1$d...%n", i);
				timeCount("big", QUERYS[i], bigModel);
				timeCount("union", QUERYS[i], unionModel);
			}
		}
	}

	private static KbGraph createGraph(File rootDir, String relativeDirectory) {
		File dir = new File(rootDir, relativeDirectory);
		dir.mkdirs();
		Config config = Config.readFromFile();
		config.m_kbDirectoryPath = dir.getPath();
		KbInstance.deleteKb(config, null);
		return new KbGraph(config, relativeDirectory, OptimizationMethod.DefaultOptimization);
	}

	private static Model createModel(KbGraph g) throws IOException {
		Model m = ModelFactory.createModelForGraph(g);
		RDFFormat rdfFmt = RDFFormat.parseFilename(ONT_RSRC);
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try (InputStream strm = cl.getResourceAsStream(ONT_RSRC)) {
			if (strm == null) {
				throw new FileNotFoundException("Unable to load resource " + ONT_RSRC);
			}
			m.read(strm, null, rdfFmt.toString());
		}
		return m;
	}

	private static void loadUniversityData(Model m1, Model m2, int univNum) throws IOException {
		System.out.format("Loading university #%1$d:%n", univNum);
		Pattern pattern = Pattern.compile(
			String.format("University%1$d_.*", univNum), Pattern.CASE_INSENSITIVE);
		try (ZipFile zipFile = new ZipFile(INPUT_DATA_FILE)) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry zipEntry = entries.nextElement();
				if (!zipEntry.isDirectory() && pattern.matcher(zipEntry.getName()).matches()) {
					Model tmpModel = ModelFactory.createDefaultModel();
					RDFFormat rdfFmt = RDFFormat.parseFilename(zipEntry.getName());
					try (InputStream strm = zipFile.getInputStream(zipEntry)) {
						tmpModel.read(strm, null, rdfFmt.toString());
					}
					m1.add(tmpModel);
					m2.add(tmpModel);
					System.out.format("Loaded file '%1$s'%n", zipEntry.getName());
				}
			}
		}
	}

	private static void timeCount(String modelName, String query, Model model) {
		long now = System.currentTimeMillis();
		ResultSet results = QueryExecutionFactory.create(query, model).execSelect();
		int count = 0;
		while (results.hasNext()) {
			results.next();
			++count;
		}
		long duration = System.currentTimeMillis() - now;
		System.out.format("%1$s [%2$d] results in %3$d ms%n", modelName, count, duration);
	}
}
