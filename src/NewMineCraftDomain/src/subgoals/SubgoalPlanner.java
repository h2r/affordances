package subgoals;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

import burlap.oomdp.core.State;
import burlap.oomdp.logicalexpressions.LogicalExpression;


public class SubgoalPlanner {

	public SubgoalPlanner() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a graph out of a list of subgoals
	 * @param kb
	 * @return
	 */
	private Node generateGraph(ArrayList<Subgoal> kb) {
		HashMap<LogicalExpression,Node> nodes = new HashMap<LogicalExpression,Node>();
		
		// Initialize Root of tree (based on final goal)
		Node root = new Node(kb.get(0).getPost(), null);
		nodes.put(kb.get(0).getPost(), root);
		
		// Create a node for each propositional function
		for (int i = 0; i < kb.size(); i++) {
			LogicalExpression pre = kb.get(i).getPre();
			LogicalExpression post = kb.get(i).getPost();
			
			Node postNode = new Node(post, null);
			Node preNode = new Node(pre, null);
			System.out.println("Post Node: " + post);
			System.out.println("Pre Node: " + pre);
			if (!nodes.containsKey(post)) {
				nodes.put(post, postNode);
			}
			
			if (!nodes.containsKey(preNode)) {
				nodes.put(pre, preNode);
			}
		}

		// Add edges between the nodes to form a tree of LogicalExpressions
		for (int i = 0; i < kb.size(); i++) {
			Subgoal edge = kb.get(i);
			
			LogicalExpression edgeStart = edge.getPre();
			LogicalExpression edgeEnd = edge.getPost();
			
			Node startNode = nodes.get(edgeStart);
			Node endNode = nodes.get(edgeEnd);
			
			if (startNode != null) {
				startNode.setParent(endNode);				
				endNode.addChild(startNode);
			}
						
		}
		
		return root;
	}
	
	/**
	 * Performs a BFS on a graph of subgoals to find a high-level plan from the start to the goal condition
	 * @param root
	 * @param initialState
	 * @return
	 */
	private Node initialStateSubGoalBFS(Node root, State initialState) {
		ArrayDeque<Node> nodeQueue = new ArrayDeque<Node>();
		
		nodeQueue.add(root);
		Node curr = null;
		while (!nodeQueue.isEmpty()) {
			curr = nodeQueue.poll();
			if (curr.getLogicalExpression().evaluateIn(initialState)) {
				return curr;
			}
			if (curr.getChildren() != null) {
				nodeQueue.addAll(curr.getChildren());
			}
		}
		
		return curr;
	}
	
}
