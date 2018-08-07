package ca.ucalgary.ispia.graphpatterns.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

import ca.ucalgary.ispia.graphpatterns.graph.GPHolder;
import ca.ucalgary.ispia.graphpatterns.graph.GraphPattern;
import ca.ucalgary.ispia.graphpatterns.graph.HasAttributes;
import ca.ucalgary.ispia.graphpatterns.graph.MyNode;
import ca.ucalgary.ispia.graphpatterns.graph.MyRelationship;
import ca.ucalgary.ispia.graphpatterns.graph.RelType;
import ca.ucalgary.ispia.graphpatterns.util.GPUtil;
import ca.ucalgary.ispia.graphpatterns.util.Pair;

/**
 * This is the wrapper class for generating test cases (TripleGPHolder objects).
 * @author szrrizvi
 *
 */
public class EvalTestGenWrapper {
	private GraphDatabaseService graphDb;	//The graph database interface
	private Random random;					//The random number generator
	private int totalDBNodes;				//Total number of nodes in the database

	//Parameters for the database query (first graph pattern)
	private int endSizeA, rootedA, numMexA, numVAttrsA, numEAttrsA;
	private double completeA;

	//Parameters for the access control policy (second graph pattern)
	private int endSizeB, rootedB, numMexB, numVAttrsB, numEAttrsB;
	private double completeB;

	//flags to check if parameters have been set
	private boolean paramsASet, paramsBSet;

	/**
	 * Initializes the fields
	 * @param graphDb The graph database interface
	 * @param random The pseudorandom number generator
	 */
	public EvalTestGenWrapper(GraphDatabaseService graphDb, Random random, int totalDBNodes){
		this.graphDb = graphDb;
		this.random = random;
		this.totalDBNodes = totalDBNodes;

		this.paramsASet = false;
		this.paramsBSet = false;
	}

	/**
	 * Sets the parameters for the database query (first graph pattern)
	 * @param endSize
	 * @param complete
	 * @param rooted
	 * @param p
	 * @param numMex
	 * @param numVAttrs
	 * @param numEAttrs
	 */
	public void setParamsA(int endSize, double complete, int rooted, int numMex, int numVAttrs, int numEAttrs){
		this.endSizeA = endSize;
		this.completeA = complete;
		this.rootedA = rooted;
		this.numMexA = numMex;
		this.numVAttrsA = numVAttrs;
		this.numEAttrsA = numEAttrs;

		this.paramsASet = true;
	}

	/**
	 * Sets the parameters for the access control policy (second graph pattern)
	 * @param endSize
	 * @param complete
	 * @param rooted
	 * @param p
	 * @param numMex
	 * @param numVAttrs
	 * @param numEAttrs
	 */
	public void setParamsB(int endSize, double complete, int rooted, int numMex, int numVAttrs, int numEAttrs){
		this.endSizeB = endSize;
		this.completeB = complete;
		this.rootedB = rooted;
		this.numMexB = numMex;
		this.numVAttrsB = numVAttrs;
		this.numEAttrsB = numEAttrs;

		this.paramsBSet = true;
	}



	/**
	 * Generates the test graph pattern holders objects. 
	 * @return The graph pattern holder objects for the tests
	 */
	public TripleGPHolder generateTests(){

		//Check if the parameters have been set
		if (!(paramsASet && paramsBSet)){
			return null;
		}

		SubgraphGenerator genA = null;
		SubgraphGenerator genB = null;

		boolean gpADone =false;
		boolean gpBDone = false;

		//Generate the first graph pattern holder. This is the database query.
		GPHolder gpA = null;
		while (!gpADone){
			//Keep looping until we get a non-null result.
			genA = new SubgraphGenerator(graphDb, totalDBNodes, random, endSizeA, completeA, rootedA, numMexA, numVAttrsA, numEAttrsA);
			gpA = genA.createDBBasedGP();
			if (gpA != null){
				gpADone = true;
			}
		}

		//Extract the nodes to use as the seed for the policy query.
		//Additionally, these nodes will also have an actor mapping
		Map<Node, MyNode> nodesMap = genA.extractNodes(1);

		List<MyNode> resultSchema = new ArrayList<MyNode>();
		Node seedNode = null;
		Map<String, MyNode> actMap = new HashMap<String, MyNode>();
		int counter = 1;

		//Note that nodesMap constains a single mapping
		for (Node node : nodesMap.keySet()){
			seedNode = node;
			resultSchema.add(nodesMap.get(node));
			actMap.put("act"+counter, nodesMap.get(node));
			counter++;
		}

		gpA.setResultSchema(resultSchema);
		gpA.setActMap(actMap);

		//Generate the second graph pattern holder. This is the policy.
		GPHolder gpB = null;
		while(!gpBDone){
			//Keep looping until we get a non-null result.
			genB = new SubgraphGenerator(graphDb, totalDBNodes, random, endSizeB, completeB, rootedB, numMexB, numVAttrsB, numEAttrsB);
			gpB = genB.createDBBasedGP(seedNode);
			if (gpB != null){
				gpBDone = true;
			}
		}


		//Set the actMap for gpB
		Map<String, MyNode> actMapB = new HashMap<String, MyNode>();
		for (Node node : nodesMap.keySet()){
			MyNode first = genA.findMyNode(node);
			MyNode second = genB.findMyNode(node);

			//Find the key name mapped to first
			String keyName = null;
			for (String key : actMap.keySet()){
				if (actMap.get(key).equals(first)){
					keyName = key;
				}
			}

			if (keyName == null){
				System.out.println("Couldn't map");
			} else {
				actMapB.put(keyName, second);
			}
		}
		gpB.setActMap(actMapB);

		GPHolder gpC = combineGPs(genA, genB, seedNode);

		return new TripleGPHolder(gpA, gpB, gpC);
	}	

	/**
	 * Combines two GPHolders into a single GPHolder.
	 * @param genA The source for the first GPHolder.
	 * @param genB The source for the second GPHolder.
	 * @param seeds The seeds used for overlapping the second GP with the first.
	 * @return The combined GPHolder
	 */
	private GPHolder combineGPs(SubgraphGenerator genA, SubgraphGenerator genB, Node seedNode){

		//Extract the graph patterns
		GraphPattern gpA = genA.getGPHolder().getGp();
		GraphPattern gpB = genB.getGPHolder().getGp();

		//The list of nodes for the new graph pattern
		List<MyNode> nodes = new ArrayList<MyNode> ();

		//The list of already visited nodes from the input graph patterns
		List<MyNode> seenNodes = new ArrayList<MyNode> ();

		//The mapping from the original nodes to the new nodes
		Map<MyNode, MyNode> nodesMap = new HashMap<MyNode, MyNode>();

		int nodeCount = 0;
		int relCount = 0;
		String nodePrefix = "C";
		String relPrefix = "rel";

		List<MyNode> resultSchema = new ArrayList<MyNode>();

		//For the seed, create a single node and map that node from the input GPs to the new node
		//Create the new node
		MyNode newNode = new MyNode(nodeCount, "PERSON");
		nodeCount++;

		//Add it to the nodes list
		nodes.add(newNode);

		//Find the corresponding original nodes, and map them to the new node
		MyNode temp1 = genA.findMyNode(seedNode);
		nodesMap.put(temp1, newNode);
		seenNodes.add(temp1);
		MyNode temp2 = genB.findMyNode(seedNode);
		nodesMap.put(temp2, newNode);
		seenNodes.add(temp2);

		addAttrs(temp1, newNode);
		addAttrs(temp2, newNode);
		resultSchema.add(newNode);

		//Generate the actMap for the combine GPHolder
		Map<String, MyNode> actMap = new HashMap<String, MyNode>();
		Map<String, MyNode> sourceActMap = genA.getGPHolder().getActMap();
		for (String key : sourceActMap.keySet()){
			MyNode src = sourceActMap.get(key);
			actMap.put(key, nodesMap.get(src));
		}


		//For the rest of the nodes in gpA, create a new corresponding node and mapping
		for (MyNode node : gpA.getNodes()){
			if (!seenNodes.contains(node)){
				//Create the node
				MyNode newNodeB = new MyNode(nodeCount, "PERSON");
				nodeCount++;


				//Update the mapping
				nodesMap.put(node, newNodeB);
				seenNodes.add(node);

				addAttrs(node, newNodeB);
			}
		}

		//For the rest of the nodes in gpB, create a new corresponding node and mapping
		for (MyNode node : gpB.getNodes()){
			if (!seenNodes.contains(node)){
				//Create the node
				MyNode newNodeB = new MyNode(nodeCount, "PERSON");
				nodeCount++;

				//Update the mapping
				nodesMap.put(node, newNodeB);
				seenNodes.add(node);

				addAttrs(node, newNodeB);
			}
		}

		List<MyRelationship> rels = new ArrayList<MyRelationship>();
		GraphPattern gp = new GraphPattern();

		//Create the relationships based on the mappings

		//For gpA
		for (MyRelationship rel : gpA.getAllRelationships()){
			//Get the corresponding new nodes and relationship type
			MyNode src = nodesMap.get(rel.getSource());
			MyNode tgt = nodesMap.get(rel.getTarget());
			RelType type = GPUtil.translateRelType(rel.getIdentifier());
			//Generate the relationships
			MyRelationship r = new MyRelationship(src, tgt, type, relCount);
			relCount++;

			if (relCount == 26){
				relCount = 0;
				relPrefix = relPrefix + "l";
			}

			addAttrs(rel, r);
			gp.addRelationship(r);
		}

		//For gpB
		for (MyRelationship rel : gpB.getAllRelationships()){
			//Get the corresponding new nodes
			MyNode src = nodesMap.get(rel.getSource());
			MyNode tgt = nodesMap.get(rel.getTarget());
			RelType type = GPUtil.translateRelType(rel.getIdentifier());
			//Generate the relationships
			MyRelationship r = new MyRelationship(src, tgt, type, relCount);
			relCount++;

			if (relCount == 26){
				relCount = 0;
				relPrefix = relPrefix + "l";
			}

			addAttrs(rel, r);
			gp.addRelationship(r);

		}


		//Create the mutual exclusion constraints based on teh mappings
		List<Pair<MyNode, MyNode>> mexList = new ArrayList<Pair<MyNode, MyNode>>();


		//Mutual exclusion constraints for gpA
		for(Pair<MyNode, MyNode> mex : genA.getGPHolder().getMexList()){
			Pair<MyNode, MyNode> newMex = new Pair<MyNode, MyNode>(nodesMap.get(mex.first), nodesMap.get(mex.second));
			mexList.add(newMex);
		}

		//Mutual exclusion constraints for gpB
		for(Pair<MyNode, MyNode> mex : genB.getGPHolder().getMexList()){
			Pair<MyNode, MyNode> newMex = new Pair<MyNode, MyNode>(nodesMap.get(mex.first), nodesMap.get(mex.second));
			mexList.add(newMex);
		}

		//Generate and return the GPHolder
		GPHolder gph = new GPHolder(gp, mexList, new HashMap<String, MyNode>());
		gph.setResultSchema(resultSchema);
		gph.setActMap(actMap);
		return gph;
	}

	private void addAttrs(HasAttributes source, HasAttributes target){
		Map<String, String> attrs = source.getAttributes();

		for (String key : attrs.keySet()){
			target.addAttribute(key, source.getAttribute(key));
		}
	}
}
