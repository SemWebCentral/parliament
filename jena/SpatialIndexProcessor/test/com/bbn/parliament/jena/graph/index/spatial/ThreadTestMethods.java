package com.bbn.parliament.jena.graph.index.spatial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.joseki.client.CloseableQueryExec;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public class ThreadTestMethods extends SpatialTestDataset {
	private static final Logger LOG = LoggerFactory.getLogger(ThreadTestMethods.class);

	public ThreadTestMethods(Properties factoryProperties) {
		super(factoryProperties);
	}

	// Call from @BeforeEach
	public void addData() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");
	}

	private static final String SIMPLE_QUERY = ""
		+ "SELECT ?a\n"
		+ "WHERE {\n"
		+ "?polygon a gml:Polygon .\n"
		+ "?polygon gml:exterior ?ext .\n"
		+ "?ext a gml:LinearRing .\n"
		+ "?ext gml:posList \"34.8448761696609 33 34.8448761696609 35.9148048779863 34.8448761696609 37 40 37 40 33 34.8448761696609 33\" .\n"
		+ "?a rcc:nonTangentialProperPart ?polygon .\n"
		+ "}";

	public void testSimpleQuery() {
		startThreadTest(SIMPLE_QUERY, 4, 10);
	}

	private static final String CIRCLE_3_EXTENTS_QUERY = ""
		+ "SELECT DISTINCT\n"
		+ "  ?building1 ?building2 ?building3\n"
		+ "WHERE {\n"
		+ "  ?circle a gml:Circle ;\n"
		+ "     gml:radius \"0.1\"^^xsd:double .\n"
		+ "  (?sreg1 ?sreg2 ?sreg3) rcc:part ?circle .\n"
		+ "  ?building1 a example:SpatialThing ;\n"
		+ "     georss:where ?sreg1 .\n"
		+ "  ?building2 a example:SpatialThing ;\n"
		+ "     georss:where ?sreg2 .\n"
		+ "  ?building3 a example:SpatialThing ;\n"
		+ "     georss:where ?sreg3 .\n"
		+ "  FILTER (?sreg1 != ?sreg2 &&\n"
		+ "     ?sreg1 != ?sreg3 &&\n"
		+ "     ?sreg2 != ?sreg3\n"
		+ "  )\n"
		+ "}";

	public void testCircle3Extents() {
		startThreadTest(CIRCLE_3_EXTENTS_QUERY, 6, 20);
	}

	private static final String OPTIONAL_PART_QUERY = ""
		+ "SELECT DISTINCT\n"
		+ "  ?x ?y\n"
		+ "WHERE {\n"
		+ "  ?poly1 a gml:Polygon ;\n"
		+ "    gml:exterior [\n"
		+ "      a gml:LinearRing ;\n"
		+ "      gml:posList \"34.90 36.0 34.845 36.0 34.845 35.8 34.9 35.8 34.9 36.0\"\n"
		+ "    ] .\n"
		+ "  ?x rcc:part ?poly1 .\n"
		+ "  OPTIONAL {\n"
		+ "    ?poly2 a gml:Polygon ;\n"
		+ "       gml:exterior ?poly2ext .\n"
		+ "    ?poly2ext a gml:LinearRing ;\n"
		+ "       gml:posList \"34.90 36.0 34.845 36.0 34.845 35.8 34.9 35.8 34.9 36.0\" .\n"
		+ "    ?y rcc:part ?poly2 .\n"
		+ "  }\n"
		+ "}";

	public void testOptionalPart() {
		startThreadTest(OPTIONAL_PART_QUERY, 9, 50);
	}

	private void startThreadTest(String query, int amount, int poolSize) {
		LOG.debug("Starting test");
		LOG.debug("Index size {}", getIndex().size());
		long maxTimeOut = 450000 * 1000;
		int corePoolSize = poolSize;
		int maximumSize = corePoolSize + 1;
		long keepAliveTime = 5000L;

		List<ThreadQuery> threads = new ArrayList<>(corePoolSize);

		ThreadPoolExecutor tpe = new ThreadPoolExecutor(
			corePoolSize + 1,
			maximumSize,
			keepAliveTime,
			TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<Runnable>());

		Dataset ds = getDataset();
		for (int i = 0; i < corePoolSize; i++) {
			ThreadQuery tq = new ThreadQuery(query, ds);
			threads.add(tq);
			tpe.execute(tq);
		}

		final AtomicBoolean finished = new AtomicBoolean(false);

		long startTime = System.currentTimeMillis();
		while (!finished.get()) {
			if (System.currentTimeMillis() - startTime > maxTimeOut) {
				fail("Did not finish");
				return;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}

			boolean isfinished = true;
			// LOG.info("Threads size: {}", threads.size());

			for (ThreadQuery tq : threads) {
				if (null == tq) {
					LOG.info("null..wtf");
					continue;
				}
				if (tq.isFinished()) {
					assertEquals(amount, tq.getCount(), String.format("Thread #%1$d invalid count", tq.getThreadId()));
				}
				isfinished = (tq.isFinished()) && isfinished;

				if (!isfinished) {
					// LOG.info(tq.getThreadId() + " not finished");
					continue;
				}
			}
			finished.set(isfinished);
		}
		tpe.shutdown();

		assertTrue(finished.get());
	}

	private static class ThreadQuery implements Runnable {
		private AtomicLong id;
		private final AtomicBoolean finished;
		private final AtomicInteger count;
		private final String queryStr;
		private final Dataset ds;

		public ThreadQuery(String query, Dataset dataset) {
			id = null;
			finished = new AtomicBoolean(false);
			count = new AtomicInteger(0);
			queryStr = query;
			ds = dataset;
		}

		/** {@inheritDoc} */
		@Override
		public void run() {
			id = new AtomicLong(Thread.currentThread().getId());
			LOG.debug("{} Start", getThreadId());

			try (CloseableQueryExec qExec = SpatialTestDataset.performQuery(ds, queryStr)) {
				ResultSet rs = qExec.execSelect();
				assertTrue(rs.hasNext());
				while (rs.hasNext()) {
					QuerySolution qs = rs.next();
					LOG.debug("{} {}", getThreadId(), qs);
					count.incrementAndGet();
				}
			} catch (RuntimeException ex) {
				LOG.error("wtf?", ex);
			} finally {
				finished.set(true);
			}
			LOG.debug("{} Read: {} results", getThreadId(), count.get());
		}

		public long getThreadId() {
			return id.get();
		}

		public boolean isFinished() {
			return finished.get();
		}

		public int getCount() {
			return count.get();
		}
	}
}
