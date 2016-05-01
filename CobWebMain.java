import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.*;

public class CobWebMain {
	
	static 			boolean 					areNamesAvailable 			= false;
	static 			boolean 					isFreezed 					= false;
	static 			boolean 					simulationMode 				= true;
	static			boolean						randomize					= false;
	static			boolean						labelMode					= true;
	
	static 			int 						freezeLimit 				= 0;
	static 			int 						correctLabel 				= 0;
	
	static			double[] 					weights 					= null;
	static			int[]						categoriesLevel1			= new int[101];
	static			int[]						categoriesLevel2			= new int[101];
	static			int[]						categoriesLevel3			= new int[101];
	static			int[]						categoriesLevel4			= new int[101];
	
	static 			Graph 						graph						= null;
	
	static 			ArrayList<Instance> 		instances 					= null;
	static 			ArrayList<Constraint> 		constraints 				= null;
	static 			ArrayList<Constraint> 		enforcedConstraints 		= null;
	static 			ArrayList<Constraint> 		unenforcedConstraints 		= null;
	
	static 			File 						inputFile 					= null;
	
	static 			CobWebAlgoWithConstraints 	algo 						= null;
	
	static			Scanner						reader						= null;
	static			Scanner						input						= null;

	static 			String 						styleSheet 					=
																		        "node {" +
																		        "	fill-color: black;"
																		        + 	"text-size: 18;" +"}" +
																		        "node.marked {" +
																		        "	fill-color: red;" +
																		        "}"+
																		        "node.constrained {" +
																		        "	fill-color: green;" +
																		        "}";
	static			String						labelPrompt					=
																				"Do you want to run COBWEB in labeling mode?";
	static			String						constraintDefault			=
																				"data/constraints.txt";
	static			String						constraintPrompt			=
																				"Input File Path (for Constraints) from current folder? (Empty will take in (data/constraints.txt)): ";
	static 			String						datasetDefault				=
																				"data/input.txt";
	static			String						datasetPrompt				=
																				"Input File Path (for Dataset) from current folder? (Empty will take in (data/input.txt)): ";
	static 			String						namePrompt					= 
																				"Are names available in datasheet (First Column)? Y/N";
	static			String						userInput					= "";
	static			String						randomizePrompt				=
																				"Do you want to randomize order or processing for COBWEB? Y/N";
	static			String						sampleSizePrompt			=
																				"Set manual sample size: (Integer, If left blank COBWEB will be run for full dataset)";
	
	public static void main(String[] args) throws FileNotFoundException, InterruptedException {
			CobWebMain.init();
			CobWebMain.getInputFile(datasetPrompt, datasetDefault);
			
			//Check if names are in the dataset
			userInput = "N";
			CobWebMain.getUserInput(namePrompt, userInput);
			if(userInput.charAt(0) == 'Y'){
				areNamesAvailable = true;
			}
			
			int 				count 		= 0;
			Node 				root 		= null;
			
			//Read the dataset into Instances
			while(reader.hasNextLine()){
				Instance 	tempInstance 			= new Instance();
				String 		instanceString 			= reader.nextLine();
				String[] 	attrValues 				= null;
				if(instanceString.contains(",")){
					attrValues 				= instanceString.split(",");
				}
				else{
					attrValues 				= instanceString.split("\\s+");
				}
				
				//////////////////////////////////////////////////////////////
				//////////////////      OBSOLETE     /////////////////////////
				if(weights == null){
					weights = new double[attrValues.length];
					for(int i=0; i<attrValues.length; i++){
						weights[i] = 1;
					}
				}
				/////////////////////////////////////////////////////////////
				/////////////////////////////////////////////////////////////
				
				if(areNamesAvailable){
					tempInstance.setName(attrValues[0]);
					tempInstance.setAttrValues(Arrays.copyOfRange(attrValues, 1, attrValues.length));
						
				}
				else{
					tempInstance.setName(Integer.toString(count));
					tempInstance.setAttrValues(Arrays.copyOf(attrValues, attrValues.length));
				}
				
				//Set Root
				if(root == null){
					root = new Node(tempInstance.getNAttr());
					root.setLabel("Root");
				}
				
				CobWebMain.addInstanceToList(tempInstance);
				count++;
			}
			
			//Read Constraints into runtime
			count = 0;
			CobWebMain.getInputFile(constraintPrompt, constraintDefault);
			while(reader.hasNext()){
				Constraint tempConstraint = null;
				String constraintString = reader.nextLine();
				System.out.println("constraintString: " + constraintString);
				if(constraintString.contains(",")){
					tempConstraint = new Constraint(count, constraintString.split(","));
				}
				else{
					tempConstraint = new Constraint(count, constraintString.split("//s+"));
				}
				//CobWebMain.getInstanceByName(tempConstraint.getObject(), root).setConstrained(true);
				CobWebMain.getInstanceByName(tempConstraint.getObject(), instances).setConstrained(true);

				CobWebMain.addConstraintToList(tempConstraint);
				count++;
			}
			
			////////////////////////////////////////////////////////////
			///////////    Get COBWEB attributes    ////////////////////
			userInput = "Y";
			CobWebMain.getUserInput(randomizePrompt, userInput);
			if(userInput.charAt(0) == 'Y'){
				randomize = true;
			}
			else if(userInput.charAt(0) == 'N'){
				randomize = false;
			}
			
			userInput = "Y";
			CobWebMain.getUserInput(labelPrompt, userInput);
			if(userInput.charAt(0) == 'Y'){
				labelMode = true;
			}
			else if(userInput.charAt(0) == 'N'){
				labelMode = false;
			}
			
			int nInstances = instances.size();
			CobWebMain.getUserInput(sampleSizePrompt, nInstances);
			///////////////////////////////////////////////////////////
			
			///////////////////////////////////////////////////////////
			//////////////  	   Start COBWEB	    ///////////////////
			for(int instance=0; instance<nInstances; instance++){
				Instance object 			= null;
				Constraint constraint 		= null;

				if(randomize){
					object = instances.get((int) Math.floor(Math.random()*instances.size()));
				}
				else{
					object = instances.get(0);
				}
				
				if(simulationMode){
					System.out.println("The New Object is: " + object.getName());
					System.out.println();
				}
				
				if(object.isConstrained()){
					//Get corresponding constraint
					for(int j=0; j<constraints.size(); j++){
						if(constraints.get(j).getObject().equals(object.getName())){
							constraint = constraints.get(j);
							break;
						}
					}
					
					//Check if Constraint is enforcable
					if(root.isConstraintEnforcable(constraint)){
						//If it is classify with constraint
						algo.classifyConstrained(object, root, constraint);
						CobWebMain.enforcedConstraints.add(constraint);
					}
					else{
						//If it isnt classify unconstrained and add to unenforcedConstraints
						algo.classifyNonConstrained(object, root);	
						CobWebMain.unenforcedConstraints.add(constraint);
					}
				}
				else{
					//Classify Unconstrained
					algo.classifyNonConstrained(object, root);	
				}
				
				//Check Constraints previously enforced if they still hold true
				for(int enforcedConstraint=0; enforcedConstraint<enforcedConstraints.size(); enforcedConstraint++){
					Constraint EnforcedConstraint =  enforcedConstraints.get(enforcedConstraint);
					checkEnforcedConstraints(root, EnforcedConstraint);
				}
				
				//Check if you can enforce a previously unenforced constraint
				ArrayList<Constraint> UnenforcedToEnforcedList = new ArrayList<Constraint>();
				for(int unenforcedConstraint=0; unenforcedConstraint<unenforcedConstraints.size(); unenforcedConstraint++){
					Constraint UnenforcedConstraint =  unenforcedConstraints.get(unenforcedConstraint);
					if(root.isConstraintEnforcable(UnenforcedConstraint)){
						UnenforcedToEnforcedList.add(UnenforcedConstraint);
						enforcedConstraints.add(UnenforcedConstraint);
						checkEnforcedConstraints(root, UnenforcedConstraint);
					}
				}
				//Remove from unenforced list if a constraint changes state
				if(UnenforcedToEnforcedList.size()>0){
					unenforcedConstraints.removeAll(UnenforcedToEnforcedList);
					UnenforcedToEnforcedList.clear();
				}

				
				
				//Refresh Graph
				CobWebMain.refreshGraph(root);
				CobWebMain.countLevel1(root, instance);
				CobWebMain.countLevel2(root, instance);
				CobWebMain.countLevel3(root, instance);
				CobWebMain.countLevel4(root, instance);
				
				for(org.graphstream.graph.Node node: graph.getNodeSet()){
					node.setAttribute("ui.label", node.getId());
				}
				
				instances.remove(object);
				
				//if(simulationMode){
				//	System.out.println();
				//	waitForUser();
				//}
			}
			
			System.out.println();
			printLevel(root, 4, 0);
			
			System.out.println(Arrays.toString(categoriesLevel1));
			System.out.println(Arrays.toString(categoriesLevel2));
			System.out.println(Arrays.toString(categoriesLevel3));
			System.out.println(Arrays.toString(categoriesLevel4));
	}
	
	private static Instance getInstanceByName(String object, ArrayList<Instance> root) {
		Instance I = null;
		System.out.println(object);
		for(int i=0; i<root.size(); i++){
			System.out.println(root.get(i).getName() + " - " + object);
			if(root.get(i).getName().equals(object)){
				I = root.get(i);
			}
		}
		return I;
	}

	private static void countLevel1(Node root, int instance) {
		// TODO Auto-generated method stub
		if(root.getLevel() == 1){
			categoriesLevel1[instance] = categoriesLevel1[instance]+1;
		}
		else if(root.getNChildren()>0){
			for(int i=0; i<root.getNChildren(); i++){
				countLevel1(root.getChildren().get(i), instance);
			}
		}
	}
	
	private static void countLevel2(Node root, int instance) {
		// TODO Auto-generated method stub
		if(root.getLevel() == 2){
			categoriesLevel2[instance] = categoriesLevel2[instance]+1;
		}
		else if(root.getNChildren()>0){
			for(int i=0; i<root.getNChildren(); i++){
				countLevel2(root.getChildren().get(i), instance);
			}
		}
	}
	
	private static void countLevel3(Node root, int instance) {
		// TODO Auto-generated method stub
		if(root.getLevel() == 3){
			categoriesLevel3[instance] = categoriesLevel3[instance]+1;
		}
		else if(root.getNChildren()>0){
			for(int i=0; i<root.getNChildren(); i++){
				countLevel3(root.getChildren().get(i), instance);
			}
		}
	}
	
	private static void countLevel4(Node root, int instance) {
		// TODO Auto-generated method stub
		if(root.getLevel() == 4){
			categoriesLevel4[instance] = categoriesLevel4[instance]+1;
		}
		else if(root.getNChildren()>0){
			for(int i=0; i<root.getNChildren(); i++){
				countLevel4(root.getChildren().get(i), instance);
			}
		}
	}

	private static void refreshGraph(Node root) {
		//Redraw Graph
		root.setLabel("Root");
		resetGraph();
		graph.addAttribute("ui.stylesheet", styleSheet);
		if(labelMode){
			CobWebMain.displayLevel(root, 3, 0);
		}
		else{
			CobWebMain.displayLevelWithoutLabels(root, 3, 0);
		}
	}

	private static void addConstraintToList(Constraint tempConstraint) {
		//Add constraints to runtime list
		constraints.add(tempConstraint);
	}

	private static void getUserInput(String prompt, int result) {
		//Automated User Prompt - Integer Result
		System.out.println(prompt);
		String strResult = input.nextLine();
		if(!strResult.equals("")){
			result = Integer.parseInt(strResult);
		}
	}

	private static void addInstanceToList(Instance tempInstance) {
		//Adds instances to processed dataset
		instances.add(tempInstance);		
	}

	private static void getUserInput(String prompt, String result) {
		//Automated User Prompt - String Result
		System.out.println(prompt);
		userInput = input.nextLine();
	}

	private static void init() {
		//Variable Initializations
		instances 				= new ArrayList<Instance>();
		constraints 			= new ArrayList<Constraint>();
		enforcedConstraints 	= new ArrayList<Constraint>();
		unenforcedConstraints 	= new ArrayList<Constraint>();
		input 					= new Scanner(System.in);
		algo 					= new CobWebAlgoWithConstraints();
		graph 					= new SingleGraph("Heirarchy");
		
		//Set Graph and Display
		graph.setStrict(false);
		graph.setAutoCreate(true);
		graph.addAttribute("ui.stylesheet", styleSheet);
		graph.display();
		
		for(int i=0; i<101; i++){
			categoriesLevel1[i] = 0;
			categoriesLevel2[i] = 0;
			categoriesLevel3[i] = 0;
			categoriesLevel4[i] = 0;
		}

	}

	private static void getInputFile(String prompt, String defaultPath) throws FileNotFoundException {
		//Get DataSet File
		System.out.println(prompt);
		Scanner input = new Scanner(System.in);
		String path = input.nextLine();
		if(!path.equals("")){
			inputFile = new File(path);
		}
		else{
			inputFile = new File(defaultPath);
		}
		reader = new Scanner(inputFile);
	}

	private static void resetGraph() {
		graph.clear();
		graph.addAttribute("ui.stylesheet", styleSheet);

	}

	private static void printLevel(Node root, int noOfLevels, int currentLevel) {
		// TODO Auto-generated method stub
		//System.out.println("Level " + currentLevel + ": " + root.getNInstances());
		
		if(currentLevel != noOfLevels){
			System.out.println("Node:");
			for(int i=0; i<root.getNInstances(); i++){
				root.getInstances().get(i).printInstance();
			}
			System.out.println();
			for(int i=0; i<root.getNChildren(); i++){
				if(root.getChildren().get(i).getNChildren()>0){
					printLevel(root.getChildren().get(i), noOfLevels, currentLevel+1);	
				}
				else{
					printLevel(root.getChildren().get(i), noOfLevels, noOfLevels);	
				}
			}
		}
		if(currentLevel == noOfLevels){
			//System.out.println("Category:");
			System.out.println("Node:");
			for(int i=0; i<root.getNInstances(); i++){
				root.getInstances().get(i).printInstance();
			}
			System.out.println();
		}			
	}
		


	private static void checkEnforcedConstraints(Node root, Constraint C) {
		if(root.atConstraintLevel(C)){
			boolean flag = true;
			for(int node = 0; node < root.getNChildren(); node++){
				if(root.getChildren().get(node).getInstanceNames().contains(C.getObject())){
					if(root.getChildren().get(node).getInstanceNames().contains(C.getFarObject())){
						flag = false;
						Instance I = getInstanceByName(C.getObject(), root);
						root.deepRemoveInstance(I);
						algo.classifyConstrained(I, root, C);
					}
				}
			}
		}
		else{
			for(int i=0; i<root.getNChildren(); i++){
				if(root.getChildren().get(i).isConstraintEnforcable(C)){
					checkEnforcedConstraints(root.getChildren().get(i), C);
					break;
				}
			}
		}
	}


	private static Instance getInstanceByName(String object, Node root) {
		Instance I = null;
		System.out.println(object);
		for(int i=0; i<root.getNInstances(); i++){
			System.out.println(root.getInstances().get(i).getName() + " - " + object);
			if(root.getInstances().get(i).getName().equals(object)){
				I = root.getInstances().get(i);
			}
		}
		return I;
	}
	
	private static void waitForUser() {
		Scanner userInput = new Scanner(System.in);
		System.out.println("1. Continue Simulation Or 2. Automate to End : ");
		int inputCode = userInput.nextInt();
		if(inputCode == 2){
			simulationMode = false;
		}
		
	}
	
	
	private static void displayLevel(Node root, int noOfLevels, int currentLevel) {
		if(currentLevel != noOfLevels){
			for(int i=0; i<root.getNChildren(); i++){
				graph.addEdge(root.getLabel() + " - " + root.getChildren().get(i).getLabel(), root.getLabel(), root.getChildren().get(i).getLabel());
				if(root.getChildren().get(i).getNChildren()>0){
					displayLevel(root.getChildren().get(i), noOfLevels, currentLevel+1);	
				}
				else{
					displayLevel(root.getChildren().get(i), noOfLevels, noOfLevels);	
				}
			}
		}
		if(currentLevel == noOfLevels){
			for(int i=0; i<root.getNInstances(); i++){
				graph.addEdge(root.getLabel() + " - " + root.getInstances().get(i).getName(), root.getLabel(), root.getInstances().get(i).getName());
				if(root.getInstances().get(i).isConstrained()){
					graph.getNode(root.getInstances().get(i).getName()).setAttribute("ui.class", "constrained");
				}
				else{
					graph.getNode(root.getInstances().get(i).getName()).setAttribute("ui.class", "marked");
				}

			}
		}
	}
	
	private static void displayLevelWithoutLabels(Node root, int noOfLevels, int currentLevel) {
		if(currentLevel != noOfLevels){
			for(int i=0; i<root.getNChildren(); i++){
				root.getChildren().get(i).setLabel(root.getLabel() + "-" + (i+1));
				graph.addEdge(root.getLabel() + " - " + root.getChildren().get(i).getLabel(), root.getLabel(), root.getChildren().get(i).getLabel());
				if(root.getChildren().get(i).getNChildren()>0){
					displayLevelWithoutLabels(root.getChildren().get(i), noOfLevels, currentLevel+1);	
				}
				else{
					displayLevelWithoutLabels(root.getChildren().get(i), noOfLevels, noOfLevels);	
				}
			}
		}
		if(currentLevel == noOfLevels){
			for(int i=0; i<root.getNInstances(); i++){
				graph.addEdge(root.getLabel() + " - " + root.getInstances().get(i).getName(), root.getLabel(), root.getInstances().get(i).getName());
				if(root.getInstances().get(i).isConstrained()){
					//System.out.println("Marking Constrained");
					graph.getNode(root.getInstances().get(i).getName()).setAttribute("ui.class", "constrained");
				}
				else{
					graph.getNode(root.getInstances().get(i).getName()).setAttribute("ui.class", "marked");
				}

			}
		}
	}
	
}
