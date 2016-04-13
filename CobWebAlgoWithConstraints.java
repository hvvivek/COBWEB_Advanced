import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class CobWebAlgoWithConstraints {
	
	ArrayList<HashMap<ArrayList<String>, String>> 	processedLabels 	= new ArrayList<HashMap<ArrayList<String>, String>>();
	int 											levelLimit 			= 4;
	Scanner 										reader 				= new Scanner(System.in);
	double 											threshold 			= 0.05;

	
	public void classifyConstrained(Instance object, Node root, Constraint constraint) {
		//Pre-Processed Labels
		HashMap<ArrayList<String>, String> currentLabel = new HashMap<ArrayList<String>, String>();

		if(object.isConstrained()){
			//System.out.println("Working");
		}
		else{
			object.setConstrained(true);
		}
		//Add Instance to Root
		root.addInstance(object);
		boolean breakFlag = false;
		
		//Check if new Label is required
		//Check if Label already exists
		if(CobWebMain.labelMode){
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
		}
		
		//User Indicator
		if(CobWebMain.simulationMode){
			System.out.println("Classifying Constrained : " + object.getName());
		}
		
		//Encountered Leaf Node
		if(root.getNChildren() == 0){
			if(root.getNInstances()>1 ){
				//Leaf - Add New singleton Node
				Node 		copy 			= new Node(object.getNAttr());
				Node 		leaf 			= new Node(object.getNAttr());
				
				//Create a copy of Root excluding current Instance
				for(int i=0; i<root.getNInstances(); i++){
					copy.addInstance(root.getInstances().get(i));
				}
				copy.setLevel(root.getLevel()+1);
				copy.removeInstance(object);
				
				if(CobWebMain.labelMode){
					if(copy.getNInstances()>0){
						this.getNewLabel(copy);
					}
				}
				else{
					copy.setLabel(root.getLabel() + " - " + ((int)root.getNChildren()));
				}
				
				//Create a singleton Leaf
				leaf.addInstance(object);
				leaf.setLevel(root.getLevel()+1);
				if(CobWebMain.labelMode){
					leaf.setLabel(object.getName());	
				}
				else{
					leaf.setLabel(root.getLabel() + " - " + ((int)root.getNChildren()));
				}
				
				//Add to root
				root.addNode(leaf);
				if(copy.getNInstances()>0){
					root.addNode(copy);
				}
			}
			else{
				Node leaf = new Node(object.getNAttr());
				leaf.addInstance(object);
				leaf.setLevel(root.getLevel()+1);
				if(CobWebMain.labelMode){
					leaf.setLabel(object.getName());	
				}
				else{
					leaf.setLabel(root.getLabel() + " - " + ((int)root.getNChildren()));
				}
				root.addNode(leaf);
			}
		}
		else{
			
			if(root.atConstraintLevel(constraint)){
				for(int i=0; i<root.getNChildren(); i++){
					if(root.getChildren().get(i).getInstanceNames().contains(constraint.getCloseObject())){
						this.classifyNonConstrained(object, root.getChildren().get(i));
						break;
					}
				}
			}
			else{
				//Case 1: Adding to Best Node
				//Case 2: Adding to Singleton Node
				//Case 3: Adding to Merged Node (Two Best Nodes)
				//Case 4: Splitting the Best Node
				double[] CobWebCaseValues = new double[4];
				
				/////////////////////////////////////////////////////////////////////
				/////////////    Case 1: Adding to best child    ////////////////////
				
				//Find Maximum CU value and figure out the two best Nodes
				double[] ChildCUValues = new double[(int)root.getNChildren()];
				for(int child=0; child<root.getNChildren(); child++){
					root.getChildren().get(child).addInstance(object);
					ChildCUValues[child] = root.calculateCU();
					root.getChildren().get(child).removeInstance(object);
				}
				Node firstBestChoice 	= getFirstMaxNode(ChildCUValues, root);
				Node secondBestChoice 	= getSecondMaxNode(ChildCUValues, root);
				CobWebCaseValues[0] 	= getMaxCU(ChildCUValues);
				
				/////////////////////////////////////////////////////////////////////
	
				/////////////////////////////////////////////////////////////////////
				///////////    Case 1: Adding to singleton node    //////////////////
				
				Node singleton 			= new Node(object.getNAttr());
				singleton.addInstance(object);
				root.addNode(singleton);
				CobWebCaseValues[1] = root.calculateCU();
				root.removeNode(singleton);
				singleton.removeInstance(object);
				
				/////////////////////////////////////////////////////////////////////
	
				/////////////////////////////////////////////////////////////////////
				//////////////    Case 3: Adding to Merged node    //////////////////
				
				Node mergedNode 		= new Node(object.getNAttr());
				//Merging is possible only if there are more than one children in root
				if(root.getNChildren()>1){
					
					//Create Merged Node
					//Add Instances from first and second best choices
					for(int i=0; i<firstBestChoice.getNInstances(); i++){
						mergedNode.addInstance(firstBestChoice.getInstances().get(i));
					}
					for(int i=0; i<secondBestChoice.getNInstances(); i++){
						mergedNode.addInstance(secondBestChoice.getInstances().get(i));
					}    
					mergedNode.addNode(firstBestChoice);
					mergedNode.addNode(secondBestChoice);
					
					//Calculate CU with Merged Node
					root.removeNode(firstBestChoice);
					root.removeNode(secondBestChoice);
					root.addNode(mergedNode);
					mergedNode.addInstance(object);
				
					CobWebCaseValues[2] = root.calculateCU();
					
					//Reverse changes
					mergedNode.removeInstance(object);
					root.addNode(firstBestChoice);
					root.addNode(secondBestChoice);
					root.removeNode(mergedNode);
				}
				else{
					CobWebCaseValues[2] = 0;
				}
				
				/////////////////////////////////////////////////////////////////////
				
				
				/////////////////////////////////////////////////////////////////////
				//////////////    Case 3: Adding to Merged node    //////////////////
				
				Node bestSplitNode = new Node(object.getNAttr());	
				//Split is only useful if first Best Choice has at least one child
				if(firstBestChoice.getNChildren()>0){
					
					double[] splitChildCUValues = new double[(int)firstBestChoice.getNChildren() + (int)root.getNChildren() - 1];
					//Add the children of firstBestChoice
					for(int i=0; i<firstBestChoice.getNChildren(); i++){
						root.addNode(firstBestChoice.getChildren().get(i));
					}
					
					//remove First Best
					root.removeNode(firstBestChoice);
					
					for(int i=0; i<root.getNChildren(); i++){
						root.getChildren().get(i).addInstance(object);
						splitChildCUValues[i] = root.calculateCU();
						root.getChildren().get(i).removeInstance(object);
					}
					
					//Calculate CU
					CobWebCaseValues[3] = getMaxCU(splitChildCUValues);
					
					//Remove FirstBests children 
					bestSplitNode = getFirstMaxNode(splitChildCUValues, root);	
					for(int i=0; i<firstBestChoice.getNChildren(); i++){
						root.removeNode(firstBestChoice.getChildren().get(i));
					}
					
					//Add back firstBestChoice
					root.addNode(firstBestChoice);
	
				}
				else{
					CobWebCaseValues[3] = 0;
				}
				
				
				double secondBestNodeCU = 0.0;
				secondBestChoice.addInstance(object);
				secondBestNodeCU = root.calculateCU();
				secondBestChoice.removeInstance(object);
				
				System.out.println("Case 1 CU: Best Child : " + CobWebCaseValues[0]);
				System.out.println("Case 1 CU: Second Best Child : " + secondBestNodeCU);
				System.out.println("Case 2 CU: Singleton : " + CobWebCaseValues[1]);
				System.out.println("Case 3 CU: Merging : " + CobWebCaseValues[2]);
				System.out.println("Case 4 CU: Splitting : " + CobWebCaseValues[3]);
				System.out.println();
				
				
				int bestCase = getMaxCUIndex(CobWebCaseValues);
				
				Node resultNode = null;
				switch(bestCase){
					case 0:
					{
						resultNode = firstBestChoice;
						break;
					}
					case 1:
					{
						resultNode = singleton;
						break;
					}
					case 2:
					{
						resultNode = mergedNode;
						break;
					}
					case 3:
					{
						resultNode = bestSplitNode;
						break;
					}
					default:
					{
						break;
					}
				}
				
				double result = getMaxCU(CobWebCaseValues);
				boolean constraintAdded = false;
				if(resultNode.getNInstances()>0){
					//Check difference between Best Choice and Second Best
					if(		Math.abs(result - secondBestNodeCU)<threshold && 
							Math.abs(result - secondBestNodeCU)>0 && 
							secondBestNodeCU>0 && 
							secondBestChoice.getNInstances()>0
							)
					{
							System.out.println("CU Close between result and second best child ");
							if(CobWebMain.labelMode){
								System.out.println("Is " + object.getName() + " close to 1. " + resultNode.getLabel() + " or 2. " + secondBestChoice.getLabel() + "?");
								if(reader.nextLine().equals("2")){
									constraintAdded = true;
									String[] constraintString = {"R", object.getName(), secondBestChoice.sample(), resultNode.sample()};
									Constraint newConstraint = new Constraint(CobWebMain.constraints.size(), constraintString);
									CobWebMain.constraints.add(newConstraint);
									CobWebMain.enforcedConstraints.add(newConstraint);
									root.removeInstance(object);
									object.setConstrained(true);
									this.classifyConstrained(object, root, newConstraint);
								}
							}
							else{
								
								/////////////////////////////////////////////////
								String secondSample = secondBestChoice.sample();
								String rootSample =  resultNode.sample();
								if(secondSample.equals(rootSample)){
									if(resultNode.getNInstances()>1){
										while(rootSample.equals(secondSample)){
											rootSample = resultNode.sample();
										}
									}
									else if(secondBestChoice.getNInstances()>1){
										while(rootSample.equals(secondSample)){
											secondSample = secondBestChoice.sample();
										}
									}
								}
								//////////////////////////////////////////////////
								
								System.out.println("Is " + object.getName() + " close to 1. " + rootSample + " or 2. " + secondSample + "?");
								if(reader.nextLine().equals("2")){
									constraintAdded = true;
									
									
									
									String[] constraintString = {"R", object.getName(), secondSample, rootSample};
									Constraint newConstraint = new Constraint(CobWebMain.constraints.size(), constraintString);
									CobWebMain.constraints.add(newConstraint);
									CobWebMain.enforcedConstraints.add(newConstraint);
									root.removeInstance(object);
									object.setConstrained(true);
									this.classifyConstrained(object, root, newConstraint);
							}
							System.out.println();
						}
					}
					
					//Check difference between Best Choice and Split
					if(		Math.abs(result - CobWebCaseValues[3])<threshold && 
							Math.abs(result - CobWebCaseValues[3])>0  && 
							CobWebCaseValues[3]>0 && 
							bestSplitNode.getNInstances()>0
							&& (resultNode.getNInstances()>1 || bestSplitNode.getNInstances()>1)
							)
					{
							System.out.println("CU Close between result and Split Child " + result + " - " + CobWebCaseValues[3] );
							if(CobWebMain.labelMode){
								System.out.println("Is " + object.getName() + " close to 1. " + resultNode.getLabel() + " or 2. " + bestSplitNode.getLabel() + "?");
								if(reader.nextLine().equals("2")){
									constraintAdded = true;
									String[] constraintString = {"R", object.getName(), bestSplitNode.sample(), resultNode.sample()};
									Constraint newConstraint = new Constraint(CobWebMain.constraints.size(), constraintString);
									CobWebMain.constraints.add(newConstraint);
									CobWebMain.enforcedConstraints.add(newConstraint);
									root.removeInstance(object);
									object.setConstrained(true);
									this.classifyConstrained(object, root, newConstraint);
								}
							}
							else{

								/////////////////////////////////////////////////
								String secondSample = bestSplitNode.sample();
								String rootSample =  resultNode.sample();
								if(secondSample.equals(rootSample)){
									if(resultNode.getNInstances()>1){
										while(rootSample.equals(secondSample)){
											rootSample = resultNode.sample();
										}
									}
									else if(bestSplitNode.getNInstances()>1){
										while(rootSample.equals(secondSample)){
											secondSample = bestSplitNode.sample();
										}
									}
								}
								
								//////////////////////////////////////////////////
								
								System.out.println("Is " + object.getName() + " close to 1. " + rootSample + " or 2. " + secondSample + "?");
								if(reader.nextLine().equals("2")){
									System.out.println("Debug: Here");
									constraintAdded = true;
									String[] constraintString = {"R", object.getName(), secondSample, rootSample};
									Constraint newConstraint = new Constraint(CobWebMain.constraints.size(), constraintString);
									CobWebMain.constraints.add(newConstraint);
									CobWebMain.enforcedConstraints.add(newConstraint);
									root.removeInstance(object);
									object.setConstrained(true);
									this.classifyConstrained(object, root, newConstraint);
								}
							System.out.println();
						}
					}
					
				}
				
				if(!constraintAdded){
				
					switch(bestCase){
					
					//Case 1: Add to Best Child
					case 0:
					{	
						System.out.println("Adding to " + firstBestChoice.getLabel());
						this.classifyConstrained(object, firstBestChoice, constraint);
						break;
					}
					//Case 2: Add to Singleton Child
					case 1:
					{
						System.out.println("Adding to Singleton Node");
						root.addNode(singleton);
						singleton.setLevel(root.getLevel()+1);
						if(CobWebMain.labelMode){
							singleton.setLabel(object.getName());
						}
						else{
							singleton.setLabel(root.getLabel() + " - " + ((int)root.getNChildren()));
						}
						singleton.addInstance(object);
						break;
					}
					//Case 3: Add to Merged Child
					case 2:
					{
						System.out.println("Adding to Merged Node");
						
						root.addNode(mergedNode);
						mergedNode.setLevel(root.getLevel()+1);
						
						resetLevel(firstBestChoice);
						resetLevel(secondBestChoice);
						
						root.removeNode(firstBestChoice);
						root.removeNode(secondBestChoice);
						
						mergedNode.addInstance(object);
						if(CobWebMain.labelMode){
							getNewMergedLabel(mergedNode);
						}else{
							mergedNode.setLabel(root.getLabel() + " - " + ((int)root.getNChildren()));
						}
						mergedNode.removeInstance(object);
		
						this.classifyConstrained(object, mergedNode, constraint);	
						break;
					}
					
					case 3:
					{	
						System.out.println("Splitting the root node");
						for(int i=0; i<firstBestChoice.getNChildren(); i++){
							root.addNode(firstBestChoice.getChildren().get(i));
						}
						root.removeNode(firstBestChoice);
						this.classifyConstrained(object, root, constraint);
						break;
					}
					default:
						break;
					}
				}
			}
		}
	}

	public void classifyNonConstrained(Instance object, Node root) {
		//Pre-Processed Labels
		HashMap<ArrayList<String>, String> currentLabel = new HashMap<ArrayList<String>, String>();
		
		//Add Instance to Root
		root.addInstance(object);
		boolean breakFlag = false;
		
		//Check if new Label is required
		//Check if Label already exists
		if(CobWebMain.labelMode){
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
		}
		
		//User Indicator
		if(CobWebMain.simulationMode){
			System.out.println("Classifying Non Constrained : " + object.getName());
		}
		
		//Encountered Leaf Node
		if(root.getNChildren() == 0){
			if(root.getNInstances()>1 ){
				//Leaf - Add New singleton Node
				Node 		copy 			= new Node(object.getNAttr());
				Node 		leaf 			= new Node(object.getNAttr());
				
				//Create a copy of Root excluding current Instance
				for(int i=0; i<root.getNInstances(); i++){
					copy.addInstance(root.getInstances().get(i));
				}
				copy.setLevel(root.getLevel()+1);
				copy.removeInstance(object);
				
				if(CobWebMain.labelMode){
					if(copy.getNInstances()>0){
						this.getNewLabel(copy);
					}
				}
				else{
					copy.setLabel(root.getLabel() + " - " + ((int)root.getNChildren()));
				}
				
				//Create a singleton Leaf
				leaf.addInstance(object);
				leaf.setLevel(root.getLevel()+1);
				if(CobWebMain.labelMode){
					leaf.setLabel(object.getName());	
				}
				else{
					leaf.setLabel(root.getLabel() + " - " + ((int)root.getNChildren()));
				}
				
				//Add to root
				root.addNode(leaf);
				if(copy.getNInstances()>0){
					root.addNode(copy);
				}
			}
			else{
				Node leaf = new Node(object.getNAttr());
				leaf.addInstance(object);
				leaf.setLevel(root.getLevel()+1);
				if(CobWebMain.labelMode){
					leaf.setLabel(object.getName());	
				}
				else{
					leaf.setLabel(root.getLabel() + " - " + ((int)root.getNChildren()));
				}
				root.addNode(leaf);
			}
		}
		else{
			
			//Case 1: Adding to Best Node
			//Case 2: Adding to Singleton Node
			//Case 3: Adding to Merged Node (Two Best Nodes)
			//Case 4: Splitting the Best Node
			double[] CobWebCaseValues = new double[4];
			
			/////////////////////////////////////////////////////////////////////
			/////////////    Case 1: Adding to best child    ////////////////////
			
			//Find Maximum CU value and figure out the two best Nodes
			double[] ChildCUValues = new double[(int)root.getNChildren()];
			for(int child=0; child<root.getNChildren(); child++){
				root.getChildren().get(child).addInstance(object);
				ChildCUValues[child] = root.calculateCU();
				root.getChildren().get(child).removeInstance(object);
			}
			Node firstBestChoice 	= getFirstMaxNode(ChildCUValues, root);
			Node secondBestChoice 	= getSecondMaxNode(ChildCUValues, root);
			CobWebCaseValues[0] 	= getMaxCU(ChildCUValues);
			
			/////////////////////////////////////////////////////////////////////

			/////////////////////////////////////////////////////////////////////
			///////////    Case 1: Adding to singleton node    //////////////////
			
			Node singleton 			= new Node(object.getNAttr());
			singleton.addInstance(object);
			root.addNode(singleton);
			CobWebCaseValues[1] = root.calculateCU();
			root.removeNode(singleton);
			singleton.removeInstance(object);
			
			/////////////////////////////////////////////////////////////////////

			/////////////////////////////////////////////////////////////////////
			//////////////    Case 3: Adding to Merged node    //////////////////
			
			Node mergedNode 		= new Node(object.getNAttr());
			//Merging is possible only if there are more than one children in root
			if(root.getNChildren()>1){
				
				//Create Merged Node
				//Add Instances from first and second best choices
				for(int i=0; i<firstBestChoice.getNInstances(); i++){
					mergedNode.addInstance(firstBestChoice.getInstances().get(i));
				}
				for(int i=0; i<secondBestChoice.getNInstances(); i++){
					mergedNode.addInstance(secondBestChoice.getInstances().get(i));
				}    
				mergedNode.addNode(firstBestChoice);
				mergedNode.addNode(secondBestChoice);
				
				//Calculate CU with Merged Node
				root.removeNode(firstBestChoice);
				root.removeNode(secondBestChoice);
				root.addNode(mergedNode);
				mergedNode.addInstance(object);
			
				CobWebCaseValues[2] = root.calculateCU();
				
				//Reverse changes
				mergedNode.removeInstance(object);
				root.addNode(firstBestChoice);
				root.addNode(secondBestChoice);
				root.removeNode(mergedNode);
			}
			else{
				CobWebCaseValues[2] = 0;
			}
			
			/////////////////////////////////////////////////////////////////////
			
			
			/////////////////////////////////////////////////////////////////////
			//////////////  Case 4: Adding to node after split //////////////////
			
			Node bestSplitNode = new Node(object.getNAttr());	
			//Split is only useful if first Best Choice has at least one child
			if(firstBestChoice.getNChildren()>0){
				
				double[] splitChildCUValues = new double[(int)firstBestChoice.getNChildren() + (int)root.getNChildren() - 1];
				//Add the children of firstBestChoice
				for(int i=0; i<firstBestChoice.getNChildren(); i++){
					root.addNode(firstBestChoice.getChildren().get(i));
				}
				
				//remove First Best
				root.removeNode(firstBestChoice);
				
				for(int i=0; i<root.getNChildren(); i++){
					root.getChildren().get(i).addInstance(object);
					splitChildCUValues[i] = root.calculateCU();
					root.getChildren().get(i).removeInstance(object);
				}
				
				//Calculate CU
				CobWebCaseValues[3] = getMaxCU(splitChildCUValues);
				
				//Remove FirstBests children 
				bestSplitNode = getFirstMaxNode(splitChildCUValues, root);	
				for(int i=0; i<firstBestChoice.getNChildren(); i++){
					root.removeNode(firstBestChoice.getChildren().get(i));
				}
				
				//Add back firstBestChoice
				root.addNode(firstBestChoice);

			}
			else{
				CobWebCaseValues[3] = 0;
			}
			
			/////////////////////////////////////////////////////////////
			
			double secondBestNodeCU = 0.0;
			secondBestChoice.addInstance(object);
			secondBestNodeCU = root.calculateCU();
			secondBestChoice.removeInstance(object);
			
			System.out.println("Case 1 CU: Best Child : " + CobWebCaseValues[0]);
			System.out.println("Case 1 CU: Second Best Child : " + secondBestNodeCU);
			System.out.println("Case 2 CU: Singleton : " + CobWebCaseValues[1]);
			System.out.println("Case 3 CU: Merging : " + CobWebCaseValues[2]);
			System.out.println("Case 4 CU: Splitting : " + CobWebCaseValues[3]);
			System.out.println();
			
			
			int bestCase = getMaxCUIndex(CobWebCaseValues);
			
			Node resultNode = null;
			switch(bestCase){
				case 0:
				{
					resultNode = firstBestChoice;
					break;
				}
				case 1:
				{
					resultNode = singleton;
					break;
				}
				case 2:
				{
					resultNode = mergedNode;
					break;
				}
				case 3:
				{
					resultNode = bestSplitNode;
					break;
				}
				default:
				{
					break;
				}
			}
			
			double result = getMaxCU(CobWebCaseValues);
			boolean constraintAdded = false;
			if(resultNode.getNInstances()>0){
				//Check difference between Best Choice and Second Best
				if(		Math.abs(result - secondBestNodeCU)<threshold && 
						Math.abs(result - secondBestNodeCU)>0 && 
						secondBestNodeCU>0 && 
						secondBestChoice.getNInstances()>0
						)
				{
						System.out.println("CU Close between result and second best child ");
						if(CobWebMain.labelMode){
							System.out.println("Is " + object.getName() + " close to 1. " + resultNode.getLabel() + " or 2. " + secondBestChoice.getLabel() + "?");
							if(reader.nextLine().equals("2")){
								constraintAdded = true;
								String[] constraintString = {"R", object.getName(), secondBestChoice.sample(), resultNode.sample()};
								Constraint newConstraint = new Constraint(CobWebMain.constraints.size(), constraintString);
								CobWebMain.constraints.add(newConstraint);
								CobWebMain.enforcedConstraints.add(newConstraint);
								root.removeInstance(object);
								classifyConstrained(object, root, newConstraint);
							}
						}
						else{
							System.out.println("Is " + object.getName() + " close to 1. " + resultNode.getInstanceNames().get(0) + " or 2. " + secondBestChoice.sample() + "?");
							if(reader.nextLine().equals("2")){
								constraintAdded = true;
								String[] constraintString = {"R", object.getName(), secondBestChoice.sample(), resultNode.sample()};
								Constraint newConstraint = new Constraint(CobWebMain.constraints.size(), constraintString);
								CobWebMain.constraints.add(newConstraint);
								CobWebMain.enforcedConstraints.add(newConstraint);
								root.removeInstance(object);
								classifyConstrained(object, root, newConstraint);
						}
						System.out.println();
					}
				}
				
				//Check difference between Best Choice and Split
				if(		Math.abs(result - CobWebCaseValues[3])<threshold && 
						Math.abs(result - CobWebCaseValues[3])>0  && 
						CobWebCaseValues[3]>0 && 
						bestSplitNode.getNInstances()>1
						)
				{
						System.out.println("CU Close between result and Split Child " + result + " - " + CobWebCaseValues[3] + " =  "+ Math.abs(result - CobWebCaseValues[3]));
						if(CobWebMain.labelMode){
							System.out.println("Is " + object.getName() + " close to 1. " + resultNode.getLabel() + " or 2. " + bestSplitNode.getLabel() + "?");
							if(reader.nextLine().equals("2")){
								constraintAdded = true;
								String[] constraintString = {"R", object.getName(), bestSplitNode.sample(), resultNode.sample()};
								Constraint newConstraint = new Constraint(CobWebMain.constraints.size(), constraintString);
								CobWebMain.constraints.add(newConstraint);
								CobWebMain.enforcedConstraints.add(newConstraint);
								root.removeInstance(object);
								classifyConstrained(object, root, newConstraint);
							}
						}
						else{
								/////////////////////////////////////////////////
								String secondSample = bestSplitNode.sample();
								String rootSample =  resultNode.sample();
								if(secondSample.equals(rootSample)){
									if(resultNode.getNInstances()>1){
										while(rootSample.equals(secondSample)){
											rootSample = resultNode.sample();
										}
									}
									else if(bestSplitNode.getNInstances()>1){
										while(rootSample.equals(secondSample)){
											secondSample = bestSplitNode.sample();
										}
									}
								}
								
								//////////////////////////////////////////////////
			
							System.out.println("Is " + object.getName() + " close to 1. " + rootSample + " or 2. " + secondSample + "?");
			
							if(reader.nextLine().equals("2")){
								constraintAdded = true;
								String[] constraintString = {"R", object.getName(), secondSample, rootSample};
								Constraint newConstraint = new Constraint(CobWebMain.constraints.size(), constraintString);
								CobWebMain.constraints.add(newConstraint);
								CobWebMain.enforcedConstraints.add(newConstraint);
								root.removeInstance(object);
								classifyConstrained(object, root, newConstraint);
							}
						System.out.println();
					}
				}
				
			}
			
			if(!constraintAdded){
				switch(bestCase){
				
				//Case 1: Add to Best Child
				case 0:
				{	
					System.out.println("Adding to " + firstBestChoice.getLabel());
					this.classifyNonConstrained(object, firstBestChoice);
					break;
				}
				//Case 2: Add to Singleton Child
				case 1:
				{
					System.out.println("Adding to Singleton Node");
					root.addNode(singleton);
					singleton.setLevel(root.getLevel()+1);
					if(CobWebMain.labelMode){
						singleton.setLabel(object.getName());
					}
					else{
						singleton.setLabel(root.getLabel() + " - " + ((int)root.getNChildren()));
					}
					singleton.addInstance(object);
					break;
				}
				//Case 3: Add to Merged Child
				case 2:
				{
					System.out.println("Adding to Merged Node");
					
					root.addNode(mergedNode);
					mergedNode.setLevel(root.getLevel()+1);
					
					resetLevel(firstBestChoice);
					resetLevel(secondBestChoice);
					
					root.removeNode(firstBestChoice);
					root.removeNode(secondBestChoice);
					
					mergedNode.addInstance(object);
					if(CobWebMain.labelMode){
						getNewMergedLabel(mergedNode);
					}else{
						mergedNode.setLabel(root.getLabel() + " - " + ((int)root.getNChildren()));
					}
					mergedNode.removeInstance(object);
	
					this.classifyNonConstrained(object, mergedNode);	
					break;
				}
				
				case 3:
				{	
					System.out.println("Splitting the root node");
					for(int i=0; i<firstBestChoice.getNChildren(); i++){
						root.addNode(firstBestChoice.getChildren().get(i));
					}
					root.removeNode(firstBestChoice);
					this.classifyNonConstrained(object, root);
					break;
				}
				default:
					break;
				}
			}
		}

	}
	private void getNewMergedLabel(Node node) {
		// TODO Auto-generated method stub
		System.out.println("Merged Node:");
		for(int i=0;i<node.getNInstances(); i++){
			System.out.print((i+1) + ". ");
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
			System.out.print((i+1) + ". ");
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
