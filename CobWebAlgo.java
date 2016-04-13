import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class CobWebAlgo {
	ArrayList<HashMap<ArrayList<String>, String>> processedLabels = new ArrayList<HashMap<ArrayList<String>, String>>();
	int levelLimit = 4;
	Scanner reader = new Scanner(System.in);
	public void classify(Instance I, Node root){
		//Pre-Processed Labels
		HashMap<ArrayList<String>, String> currentLabel = new HashMap<ArrayList<String>, String>();
		
		root.addInstance(I);
		
		boolean breakFlag = false;
		
		//Check if new Label is required
		//Check if Label already exists
		if((root.updateCommonAttributes() || (root.getNInstances()==1 || root.getNInstances()==2)) && root.getLevel()!=0){
			for(int i=0; i<processedLabels.size(); i++){
				if(processedLabels.get(i).containsKey(root.getInstanceNames())){
					root.setLabel(processedLabels.get(i).get(root.getInstanceNames()));
					breakFlag = true;
					break;
				}
			}
			if(!breakFlag){
				getNewLabel(root);
			}
		}
			
		System.out.println("Classifying : " + I.getName());
		
		
		
		
		
		if(root.getNChildren() == 0){
			//Leaf - Add New singleton Node
			Node copy = new Node(I.getNAttr());
			Node leaf = new Node(I.getNAttr());
			
			for(int i=0; i<root.getNInstances(); i++){
				copy.addInstance(root.getInstances().get(i));
			}
			copy.removeInstance(I);
			if(copy.getNInstances()>0){this.getNewLabel(copy);}
			else{
				copy.setLabel(I.getName());
			}
			copy.setLevel(root.getLevel()+1);
			leaf.addInstance(I);
			//leaf.addInstance(I);
			leaf.setLevel(root.getLevel()+1);
			leaf.setLabel(I.getName());
			
			System.out.println("Leaf for: " + leaf.getLabel());
			root.addNode(leaf);
			root.addNode(copy);
		}
		else{
			
			System.out.println(root.getNChildren());
			
			double[] caseCU = new double[4];
			
			//Add to best child
			double[] CUValues = new double[(int)root.getNChildren()];
			for(int i=0; i<root.getNChildren(); i++){
				root.getChildren().get(i).addInstance(I);
				CUValues[i] = root.calculateCU();
				root.getChildren().get(i).removeInstance(I);
			}
			caseCU[0] = getMaxCU(CUValues);
			Node firstBest = getFirstMaxNode(CUValues, root);
			Node secondBest = getSecondMaxNode(CUValues, root);
			
			//Add singleton
			Node singleton = new Node(I.getNAttr());
			singleton.addInstance(I);
			root.addNode(singleton);
			caseCU[1] = root.calculateCU();
			root.removeNode(singleton);
			singleton.removeInstance(I);
			
			
			//Try Merging
			Node mergedNode = new Node(I.getNAttr());
			if(root.getNChildren()>1){
			for(int i=0; i<firstBest.getNInstances(); i++){
				mergedNode.addInstance(firstBest.getInstances().get(i));
			}
			for(int i=0; i<secondBest.getNInstances(); i++){
				mergedNode.addInstance(secondBest.getInstances().get(i));
			}    

			mergedNode.addNode(firstBest);
			mergedNode.addNode(secondBest);
			
			root.removeNode(firstBest);
			root.removeNode(secondBest);
			root.addNode(mergedNode);
			mergedNode.addInstance(I);
			
			caseCU[2] = root.calculateCU();
			
			mergedNode.removeInstance(I);
			root.addNode(firstBest);
			root.addNode(secondBest);
			root.removeNode(mergedNode);
			}
			else{
				caseCU[2] = 0;
			}
			
			
			//Try Splitting
			Node bestSplitNode = new Node(I.getNAttr());	
			if(firstBest.getNChildren()>0){
				double[] splitCUValues = new double[(int)firstBest.getNChildren() + (int)root.getNChildren() - 1];
				
				//Add the children of firstBest
				for(int i=0; i<firstBest.getNChildren(); i++){
					root.addNode(firstBest.getChildren().get(i));
				}
				
				//remove First Best
				root.removeNode(firstBest);
				
				for(int i=0; i<root.getNChildren(); i++){
					root.getChildren().get(i).addInstance(I);
					splitCUValues[i] = root.calculateCU();
					root.getChildren().get(i).removeInstance(I);
				}
				
				//Calculate CU
				caseCU[3] = getMaxCU(splitCUValues);
				
				//Remove FirstBests children 
				bestSplitNode = getFirstMaxNode(splitCUValues, root);	
				for(int i=0; i<firstBest.getNChildren(); i++){
					root.removeNode(firstBest.getChildren().get(i));
				}
				
				//Add back firstBest
				root.addNode(firstBest);

			}
			else{
				caseCU[3] = 0;
			}
			
			int result = getMaxCUIndex(caseCU);
			switch(result){
			
			case 0:
			{	
				System.out.println("Added to " + firstBest.getLabel());
				if(root.getLevel()<levelLimit){
					this.classify(I, firstBest);
				}
				else{
					System.out.println("Limit Level reached");
					
					firstBest.addInstance(I);
					if(firstBest.updateCommonAttributes() || firstBest.getNInstances()==2){
						getNewLabel(firstBest);
					}
					
					
				}
				break;
			}
			
			case 1:
			{
				System.out.println("Adding Singleton Node");
				root.addNode(singleton);
				singleton.setLevel(root.getLevel()+1);
				singleton.setLabel(I.getName());
				//if(root.getLevel()<levelLimit){
				//	this.classify(I, firstBest);
				//}
				//else{
					singleton.addInstance(I);
				//}	
				break;
			}
			case 2:
			{
				System.out.println("Mergin Nodes");
				root.addNode(mergedNode);
				System.out.println("Debug:" + mergedNode.getNChildren());
				mergedNode.setLevel(root.getLevel()+1);

				resetLevel(firstBest);
				resetLevel(secondBest);
				
				root.removeNode(firstBest);
				root.removeNode(secondBest);
				
				mergedNode.addInstance(I);
				getNewMergedLabel(mergedNode);
				mergedNode.removeInstance(I);

				if(mergedNode.getLevel()<levelLimit){
					this.classify(I, mergedNode);	
				}
				else{
					mergedNode.addInstance(I);
				}
				break;
			}
			
			case 3:
			{	
				System.out.println("Splitting");
				for(int i=0; i<firstBest.getNChildren(); i++){
					root.addNode(firstBest.getChildren().get(i));
				}
				root.removeNode(firstBest);
				this.classify(I, root);
				break;
			}
			default:
				break;
			}
		}
	}
	
	private void getNewMergedLabel(Node node) {
		// TODO Auto-generated method stub
		System.out.println("Merged Node:");
		for(int i=0;i<node.getNInstances(); i++){
			System.out.print(i + ". ");
			node.getInstances().get(i).printInstance();
		}
		System.out.println("New Label:");
		String inputToken = reader.nextLine();
		if(!inputToken.equals("")){node.setLabel(inputToken);}
		else{
			node.setLabel("MergedNode with " + node.getInstances().get(0).getName());
		}
		HashMap<ArrayList<String>, String> currentLabel = new HashMap<ArrayList<String>, String>();
		currentLabel.clear();
		currentLabel.put(node.getInstanceNames(), node.getLabel());
		processedLabels.add(currentLabel);
	}
	
	private void getNewLabel(Node node){
		boolean breakFlag = false;
		for(int i=0; i<processedLabels.size(); i++){
			if(processedLabels.get(i).containsKey(node.getInstanceNames())){
				node.setLabel(processedLabels.get(i).get(node.getInstanceNames()));
				breakFlag = true;
				break;
			}
		}
		if(!breakFlag){
			if(node.getNInstances() == 1){
				node.setLabel(node.getInstances().get(0).getName());
			}
			else{
		System.out.println("Label for Node: " + node.getLabel());
		System.out.println("Instances:");
		for(int i=0;i<node.getNInstances(); i++){
			System.out.print(i + ". ");
			node.getInstances().get(i).printInstance();
		}
		System.out.println("New Label (Leave Blank for no change):");
		String inputToken = reader.nextLine();
		if(!inputToken.equals(""))node.setLabel(inputToken);
			}
		HashMap<ArrayList<String>, String> currentLabel = new HashMap<ArrayList<String>, String>();
		currentLabel.clear();
		currentLabel.put(node.getInstanceNames(), node.getLabel());
		processedLabels.add(currentLabel);
			
		}
	}
	
private void getNewLabel(Node node, Instance I){
		
		System.out.println("Label for Node: " + node.getLabel());
		System.out.println("Instances:");
		for(int i=0;i<node.getNInstances(); i++){
			System.out.print(i + ". ");
			node.getInstances().get(i).printInstance();
		}
		System.out.println(((int)node.getNInstances()) + ". " + I.getName());
		System.out.println("New Label (Leave Blank for no change):");
		String inputToken = reader.nextLine();
		if(!inputToken.equals(""))node.setLabel(inputToken);
		
		HashMap<ArrayList<String>, String> currentLabel = new HashMap<ArrayList<String>, String>();
		currentLabel.clear();
		currentLabel.put(node.getInstanceNames(), node.getLabel());
		processedLabels.add(currentLabel);
	}
	private void resetLevel(Node node) {
		// TODO Auto-generated method stub
		node.setLevel(node.getLevel()+1);
		for(int i=0; i<node.getNChildren(); i++){
			resetLevel(node.getChildren().get(i));
		}
	}

	private int getMaxCUIndex(double[] C) {
		double max = -1;
		int index = 0;
		for(int i=0; i<C.length; i++){
			if(C[i]>max){
				max = C[i];
				index = i;
			}
		}
		return index;
	}

	private double getMaxCU(double[] C) {
		double max = -1;
		for(int i=0; i<C.length; i++){
			if(C[i]>max){
				max = C[i];
			}
		}
		return max;
		}

	private Node getFirstMaxNode(double[] C, Node T) {
		double max = -1;
		int index = 0;
		Node returnNode = null;
		for(int i=0; i<C.length; i++){
			if(C[i]>max){
				max = C[i];
				index = i;
			}
		}
		returnNode = T.getChildren().get(index);
		return returnNode;
	}

	private Node getSecondMaxNode(double[] C, Node T) {
		double max1 = -1;
		double max2 = -1;
		int index1 = 0;
		int index2 = 0;
		Node returnNode = null;
		for(int i=0; i<C.length; i++){
			if(C[i]>max2){
				if(C[i]>max1){
					max2 = max1;
					max1 = C[i];
					index2 = index1;
					index1 = i;
				}
				else{
					max2 = C[i];
					index2 = i;
				}
			}
		}
		returnNode = T.getChildren().get(index2);
		return returnNode;
	}
}
