package ca.ucalgary.ispia.graphpatterns.tests;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionTerminatedException;

import ca.ucalgary.ispia.graphpatterns.gpchecker.GPCheckerFC;
import ca.ucalgary.ispia.graphpatterns.graph.EvalTestGenerator;
import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.util.GPUtil;
import ca.ucalgary.ispia.graphpatterns.util.Translator;

/**
 * This class runs the evaluation tests. 
 * @author szrrizvi
 *
 */
public class EvalTestRunner {

	private GraphDatabaseService graphDb;

	/**
	 * Constructor. Initialize the graph database service (graphDb)
	 * @param graphDb
	 */
	public EvalTestRunner(GraphDatabaseService graphDb){
		this.graphDb = graphDb;
	}

	public void debugging(int numFiles, String folder) throws Exception{

		for (int i = 0; i < numFiles; i++){
			//Read the test cases from the file
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(folder+"/tests"+i+".ser"));
			List<TripleGPHolder> temp = (List<TripleGPHolder>) ois.readObject();
			ois.close();
		
			for (int j = 0; j < temp.size(); j++){
				TripleGPHolder test = temp.get(j);
				System.out.println(Translator.translateToCypher(test.dbQeury));
				
				//System.out.println((test.policy));
				
			}
			
		}
		
		//for (int i = 0; i < numFiles; i++){
		//Read the test cases from the file
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(folder+"/tests"+3+".ser"));
		List<TripleGPHolder> temp = (List<TripleGPHolder>) ois.readObject();
		ois.close();

		//GPUtil.PrintStats(temp);
		//for (int i = 0; i < temp.size(); i++){
		TripleGPHolder test = temp.get(0);
		String q = "Profile \n" +
				"MATCH (Cb:PERSON) -[rela : RelD]->(Cd:PERSON)\n" + 
				"MATCH (Ce:PERSON) -[reld : RelD]->(Cb:PERSON)\n" + 
				"MATCH (Ca:PERSON) -[relg : RelC]->(Cd:PERSON)\n" +
				"MATCH (Cd:PERSON) -[relb : RelG]->(Cb:PERSON)\n" + 
				"MATCH (Cd:PERSON) -[relc : RelA]->(Ce:PERSON)\n" +
				"MATCH (Cf:PERSON) -[relm : RelB]->(Cg:PERSON)\n" + 
				"MATCH (Cg:PERSON) -[reli : RelA]->(Cf:PERSON)\n" + 
				"MATCH (Ch:PERSON) -[relj : RelB]->(Cf:PERSON)\n" +
				"MATCH (Ca:PERSON) -[relh : RelG]->(Ce:PERSON)\n" +
				"MATCH (Ci:PERSON) -[relk : RelB]->(Cf:PERSON)\n" +
				"MATCH (Ca:PERSON) -[rele : RelB]->(Cb:PERSON)\n" +
				"MATCH (Ca:PERSON) -[relf : RelG]->(Cc:PERSON)\n" +
				"MATCH (Ca:PERSON) -[rell : RelF]->(Cf:PERSON)\n" +
				"WHERE Cb.`country of birth father`=\"United-States\" AND Ca.`country of birth self`=\"United-States\" AND Cc.`major occupation code`=\"Not in universe\" AND Cg.`id`=26892 AND Ci.`hispanic origin`=\"All other\" AND Ci.`migration code-move within reg`=\"Nonmover\" AND Ci.`own business or self employed`=0 AND relb.`weight`=2 AND relk.`weight`=7 AND rele.`weight`=5 AND relf.`weight`=4 AND rela.`weight`=3\n" + 
				"RETURN distinct Ca";

				Neo4jQueries nq = new Neo4jQueries(this.graphDb);
		nq.setDebug(true);
		nq.runQuery(q);
		//}

		//}

		/*for (TripleGPHolder test :  temp){
			System.out.println(test.dbQeury);
			System.out.println(test.policy);
			System.out.println(test.combined);

			System.out.println(Translator.translateToCypher(test.combined));
			executeTests(test, true, true);
			//runCypherQuery(Translator.translateToCypher(test.combined));
			return;
			//GPCheckerFC gpFC = new GPCheckerFC(graphDb, test.dbQeury);
			//gpFC.check();
		}*/

	}

	/**
	 * Runs the warmup tests.
	 * @param The number of warmup tests to run
	 */
	public void warmup(int numTests){

		//Create the domains for the parameters in generation
		int[] endSize = {5, 15};
		int[] numMex = {0,1};
		int[] numC = {0, 5};

		//Initialize PRGN with a seed
		Random rand = new Random(274185);

		//Randomly generate the test cases, and run them
		for (int i = 0; i < numTests; i++){

			//Obtain the parameter values for the query gp
			int esA = endSize[rand.nextInt(2)];
			int nmA = numMex[rand.nextInt(2)];
			int ncA = numC[rand.nextInt(2)];

			//Obtain the parameter values for the policy gp
			int esB = endSize[rand.nextInt(2)];
			int nmB = numMex[rand.nextInt(2)];
			int ncB = numC[rand.nextInt(2)];

			//Generate the test case
			EvalTestGenWrapper etw = new EvalTestGenWrapper(graphDb, rand, 1);
			//etw.setParamsA(esA, 0.5d, 2, 0.01f, nmA, ncA, ncA);
			etw.setParamsA(2, 0.1d, 0, 0.001f, 0, 1, 0);
			etw.setParamsB(esB, 0.1d, 2, 0.01f, nmB, ncB, ncB);
			TripleGPHolder test = etw.generateTests();

			//Randomly decide which algorithm(s) to run.
			//Must run at least one of the algorithms.
			boolean twoStep = rand.nextBoolean();
			boolean comb = true;
			if (twoStep){
				comb = rand.nextBoolean();
			}

			//Run the test case
			executeTests(test, true, true);
		}
	}

	public int writeDiffTests(Random rand, String folder) throws Exception{
		int count = 0;

		//Initialize the domain for the parameters
		int[] numMex = {0, 1, 2};
		int[] numAttr = {1, 2, 4};


		//Iterate throught the 7 profiles
		for (int profile = 0; profile < 7; profile++){

			List<TripleGPHolder> tests = new ArrayList<TripleGPHolder>();

			int eq = 0, ep = 0;
			double compQ = 0.5d, compP = 0.5d;

			if (profile == 0){
				eq = 1;
				ep = 5;
			} else if (profile == 1){
				eq = 1;
				ep = 7;
			} else if (profile == 2){
				eq = 1;
				ep = 10;
				compP = 0.25d;
			}  else if (profile == 3){
				eq = 5;
				ep = 5;
			}  else if (profile == 4){
				eq = 5;
				ep = 7;
			}  else if (profile == 5){
				eq = 7;
				ep = 5;
			}   else if (profile == 6){
				eq = 7;
				ep = 7;
			}



			//Generate 1000 cases for the profile, and add them to the list
			for (int idx = 0; idx < 1000; idx++){
				
				int mq = numMex[rand.nextInt(numMex.length)];
				int aq = numAttr[rand.nextInt(numAttr.length)];
				int rq = numAttr[rand.nextInt(numAttr.length)];

				int mp = numMex[rand.nextInt(numMex.length)];
				int ap = numAttr[rand.nextInt(numAttr.length)];
				int rp = numAttr[rand.nextInt(numAttr.length)];

				
				EvalTestGenWrapper etw = new EvalTestGenWrapper(graphDb, rand, 1);
				etw.setParamsA(eq, compQ, 0, 0.01f, mq, aq, rq);
				etw.setParamsB(ep, compP, 1, 0.01f, mp, ap, rp);

				TripleGPHolder test = etw.generateTests();
				tests.add(test);	
			}

			//Save the list in a new file
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(folder +"/tests" + count + ".ser"));
			oos.writeObject(tests);
			oos.flush();
			oos.close();

			count++;
		}

		//Return the number of files created
		return count;
	}


	/**
	 * Creates the 50 cases for each parameter combination.
	 * Store the 50 cases as a list in a serialized file. Creates a separate
	 * file for each parameter value combination.
	 * @param rand The PRGN
	 * @param folder The name of the folder to store the files in
	 * @return The number of test files created
	 * @throws Exception
	 */
	public int writeTests(Random rand, String folder) throws Exception{

		int count = 0;
		//Initialize the domain for the parameters
		int[] endSize = {1, 5, 7, 10, 15, 20};
		//double[] complete = {0.5d};//, 0.20d};
		int[] numMex = {0, 5, 10};
		int[] numAttr = {0, 5, 7};

		//Iterate through the parameters (query params on top, then the policy params)
		for (int eq = 0; eq < endSize.length; eq++){
			//for (int cq = 0; cq < complete.length; cq++){
			//for (int mq = 0; mq < numMex.length; mq++){
			//for (int aq = 0; aq < numAttr.length; aq++){
			for (int ep = 1; ep < endSize.length; ep++){
				//for (int cp = 0; cp < complete.length; cp++){
				//for (int mp = 0; mp < numMex.length; mp++){
				//for (int ap = 0; ap < numAttr.length; ap++){

				List<TripleGPHolder> tests = new ArrayList<TripleGPHolder>();

				double cq = 0.0d;
				int mq = 0;
				int aq = 1;
				if (eq > 0 && eq <= 2){
					cq = 0.5d;
					aq = numAttr[rand.nextInt(2)];
				} else if (eq > 2) {
					cq = 0.25d;
					mq = numMex[rand.nextInt(numMex.length)];
					aq = numAttr[rand.nextInt(numAttr.length)];
				}

				if (aq == 0){
					aq = 1;
				}

				double cp = 0.0d;
				int mp = 0;
				int ap = 0;
				if (ep <= 2){
					cp = 0.5d;
					ap = numAttr[rand.nextInt(2)];
				} else if (ep > 2) {
					cp = 0.25d;
					mp = numMex[rand.nextInt(numMex.length)];
					ap = numAttr[rand.nextInt(numAttr.length)];
				}

				//Generate 50 cases for the profile, and add them to the list
				for (int idx = 0; idx < 50; idx++){
					EvalTestGenWrapper etw = new EvalTestGenWrapper(graphDb, rand, 1);
					etw.setParamsA(endSize[eq], cq, 0, 0.01f, mq, aq, aq);
					etw.setParamsB(endSize[ep], cp, 2, 0.01f, mp, ap, ap);

					TripleGPHolder test = etw.generateTests();
					tests.add(test);	
				}

				//Save the list in a new file
				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(folder +"/tests" + count + ".ser"));
				oos.writeObject(tests);
				oos.flush();
				oos.close();

				count++;
			}
		}
		//}
		//}
		//}
		//}
		//}
		//}

		//Return the number of files created
		return count;
	}

	/**
	 * Reads the test from the files, and executes them. 
	 * Assumed: At least one of the flags is true.
	 * @param numFiles The number of test files in the folder.
	 * @param folder The name of the folder containing test files.
	 * @param twoStep Flag. If true, run the twoStep algorithm, else don't run it.
	 * @param comb Flag. If true, run the combined algorithm, else don't run it.
	 * @throws Exception
	 */
	public void runTests(int numFiles, String folder, boolean twoStep, boolean comb) throws Exception{

		for (int i = 0; i < numFiles; i++){
			//Read the test cases from the file
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(folder+"/tests"+i+".ser"));
			List<TripleGPHolder> tests = (List<TripleGPHolder>) ois.readObject();
			ois.close();
			//Execute the tests
			executeTests(tests, twoStep, comb);
		}
	}

	/**
	 * Runs the graph pattern analys for the tests.
	 * @param numFiles The number of test files in the folder.
	 * @param folder The name of the folder containing test files.
	 * @throws Exception
	 */
	public void analyzeTests(int numFiles, String folder) throws Exception{

		for (int i = 0; i < numFiles; i++){
			//Read the test cases from the file
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(folder+"/tests"+i+".ser"));
			List<TripleGPHolder> tests = (List<TripleGPHolder>) ois.readObject();
			ois.close();
			//Analyze the stats
			GPUtil.PrintStats(tests);

		}
	}

	/**
	 * Executes the given test. The flags specify which algorithm(s) to run.
	 * Assumed: At least one of the flags is true.
	 * @param test The target test.
	 * @param twoStep Flag. If true, run the twoStep algorithm, else don't run it.
	 * @param comb Flag. If true, run the combined algorithm, else don't run it.
	 */
	public void executeTests(TripleGPHolder test, boolean twoStep, boolean comb){
		//Add the test to a list, and invoke the overloading method.
		List<TripleGPHolder> tests = new ArrayList<TripleGPHolder>();
		tests.add(test);
		executeTests(tests, twoStep, comb);
	}

	/**
	 * Executes the given list of tests. The flags specify which algorithm(s) to run.
	 * Assumed: At least one of the flags is true.
	 * @param tests The tests to execute.
	 * @param twoStep Flag. If true, run the twoStep algorithm, else don't run it.
	 * @param comb Flag. If true, run the combined algorithm, else don't run it.
	 */
	public void executeTests(List<TripleGPHolder> tests, boolean twoStep, boolean comb){
		for (TripleGPHolder test : tests){	//Iterate through the tests

			//Initialize the variables
			GPHolder dbQuery = test.dbQeury;
			GPHolder policy = test.policy;
			GPHolder combined = test.combined;

			Terminator term;
			long start, end, time, startB, endB, timeB;
			time = 0l;
			timeB = 0l;

			//Run the twoStep algorithm if specified
			if (twoStep){
				TwoStepEval tse = new TwoStepEval();

				//Set a 6 second kill switch
				term = new Terminator(tse);
				term.terminateAfter(6000l);
				//Run the algorithm and record the time.
				start = System.nanoTime();
				List<Map<MyNode, Node>> filtered = tse.check(graphDb, dbQuery, policy);
				end = System.nanoTime();

				//Make sure the terminator is killed
				term.nullifyObj();
				term.stop();

				time = end - start;
				//Print the performance time
				if (tse.unfiltered != null){
					System.out.print("Unfiltered size=" + tse.unfiltered.size() + ", ");
				}
				System.out.print("twoStep time=" + time + ", ");

			}


			//Run the combined algorithm if specified
			if (comb){
				GPCheckerFC gpFC = new GPCheckerFC(graphDb, combined);
				//Set a 6 second kill switch
				term = new Terminator(gpFC);
				term.terminateAfter(6000l);
				//Run the algorithm and record the time
				startB = System.nanoTime();
				List<Map<MyNode, Node>> result = gpFC.check();
				endB = System.nanoTime();
				//Make sure the terminator is killed
				term.nullifyObj();
				term.stop();

				timeB = endB - startB;
				//Print the performance time

				if (result != null){
					System.out.print("Result size=" + result.size() + ", ");
				}

				System.out.print("Combined time=" + timeB + ", ");
			}

			//If both types of algorithms were ran, compare the performances.
			if (twoStep && comb){
				//Flags to specify if the algorithms completed (under 6 seconds). 
				boolean tsFail = false;
				boolean combFail = false;

				if (time >= 6000000000l){
					tsFail= true;	
				}
				if (timeB >= 6000000000l){
					combFail = true;
				}


				if (!tsFail && !combFail){	//If both algorithms completed
					if (time > timeB){
						System.out.print("Case=0, ");	//TwoStep performed slower than combined
					} else {
						System.out.print("Case=1, ");	//TwoStep performed faster than combined
					}
				} else if(tsFail && !combFail) {
					System.out.print("Case=2, ");		//TwoStep didn't complete, but combined completed.
				} else if (!tsFail && combFail) {
					System.out.print("Case=3, ");		//TwoStep completed, but combined didn't.
				} else {
					System.out.print("Case=4, ");		//Neither of the algorithms completed.
				}
			} else {
				System.out.println();
			}

			runCypherQuery(Translator.translateToCypher(combined));
		}
	}

	private void runCypherQuery(String query){

		TerminatorCypher term = null;
		try (Transaction tx = graphDb.beginTx()){

			long start = 0l, end = 0l;

			term = new TerminatorCypher(tx);
			term.terminateAfter(6000l);

			start = System.nanoTime();
			Result result = graphDb.execute(query);

			boolean done = false;
			int count = 0;
			while(result.hasNext() && !done){
				Map<String, Object> res = result.next();

				for (String key : res.keySet()){
					res.get(key);
				}
				count++;

				long currTime = System.nanoTime();
				if ((currTime - start) > 6000000000l){
					done = true;
				}
			}
			end = System.nanoTime();

			term.nullifyTx();
			term.stop();
			result.close();

			long timeC = end - start;
			System.out.println("Cypher Time=" + timeC +", CypherResult=" + count);
			//System.out.println(count);

			tx.success();
		} catch (TransactionTerminatedException e){
			term.stop();
			System.out.println("Transaction lasted more than 6 seconds");
		}
	}


	/**
	 * Inner classes, used for killing the evaluation algorithms. 
	 * @author szrrizvi
	 *
	 */
	public class Terminator {

		private Killable obj;
		private ExecutorService service;

		/**
		 * Constructor. 
		 * @param obj The object to kill
		 */
		Terminator( Killable obj ) {
			this.obj = obj;
			service = null;
		}

		/**
		 * Removes the reference to the object
		 */
		public void nullifyObj(){
			this.obj = null;
		}

		/**
		 * Terminates the process after the specified time.
		 * @param millis The specified time to terminate the process.
		 */
		public void terminateAfter( final long millis ) {
			service = Executors.newSingleThreadExecutor();
			service.submit( new Runnable() {
				@Override
				public void run() {

					//Make the threat sleep for the specified time
					long startTime = System.currentTimeMillis();
					do {
						try {
							Thread.sleep( millis );
						} catch ( InterruptedException ignored ){
							return;
						}
					}
					while ((System.currentTimeMillis() - startTime) < millis );

					//Kill the process if we still have reference to the object
					if (obj != null) {
						obj.kill();
					}

				}
			}
					);
		}

		//Stops this service.
		public void stop(){
			service.shutdownNow();
		}
	}

	public class TerminatorCypher {

		private Transaction tx;
		private ExecutorService service;

		TerminatorCypher( Transaction tx ) {
			this.tx = tx;
			service = null;
		}

		public void nullifyTx(){
			this.tx = null;
		}

		public void terminateAfter( final long millis ) {
			service = Executors.newSingleThreadExecutor();
			service.submit( new Runnable() {
				@Override
				public void run() {
					long startTime = System.currentTimeMillis();
					do {
						try {
							Thread.sleep( millis );
						} catch ( InterruptedException ignored ){
							return;
						}
					}
					while ( (System.currentTimeMillis() - startTime) < millis );
					// START SNIPPET: terminateTx


					if (tx != null) {
						tx.terminate();
					}
				}
			}
					);
		}

		public void stop(){
			service.shutdownNow();
		}
	}
}