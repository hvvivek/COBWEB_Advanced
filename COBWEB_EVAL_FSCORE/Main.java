import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

	public static void main(String[] args) throws FileNotFoundException {
		// TODO Auto-generated method stub
		Scanner reader = null;
		ArrayList<Node> Clusters = new ArrayList<Node>();
		ArrayList<Node> Classes = new ArrayList<Node>();
		System.out.println("Path for Cluster List (Default: data/clusters.txt):");
		Scanner input = new Scanner(System.in);
		String path = input.nextLine();
		File inputFile = null;
		String defaultPath = "data/clusters.txt";
		double count = 0;
		if(!path.equals("")){
			inputFile = new File(path);
		}
		else{
			inputFile = new File(defaultPath);
		}
		reader = new Scanner(inputFile);
		Node node = null;
		while(reader.hasNext()){
			String nextLine = reader.nextLine();
			if(nextLine.equals("Node:")){
				if(node!=null && node.getNInstances()>0){
					Clusters.add(node);
				}
				node = new Node(1);
			}
			else if(!nextLine.equals("")){
				Instance I = new Instance();
				String[] attrs = new String[1];
				attrs[0] = nextLine;
				I.setAttrValues(attrs);
				I.setName(nextLine);
				node.addInstance(I);
			}
		}
		
		for(int i=0; i<Clusters.size(); i++){
			System.out.println("Cluster:");
			for(int j=0; j<Clusters.get(i).getNInstances(); j++){
				Clusters.get(i).getInstances().get(j).printInstance();
			}
			System.out.println();
		}
		
		System.out.println("Path for Actual Class List (Default: data/classes.txt):");
		input = new Scanner(System.in);
		path = input.nextLine();
		inputFile = null;
		defaultPath = "data/classes.txt";
		if(!path.equals("")){
			inputFile = new File(path);
		}
		else{
			inputFile = new File(defaultPath);
		}
		reader = new Scanner(inputFile);
		
		while(reader.hasNext()){
			count++;

			String nextLine = reader.nextLine();
			Instance I = new Instance();
			String[] 	attrValues 				= null;
			if(nextLine.contains(",")){
				attrValues 				= nextLine.split(",");
			}
			else{
				attrValues 				= nextLine.split("\\s+");
			}
			String[] attrs = new String[1];
			attrs[0] = attrValues[attrValues.length-1];
			Node classNode = getNode(Classes, attrValues[attrValues.length-1]);
			I.setAttrValues(attrs);
			I.setName(attrValues[0]);
			classNode.addInstance(I);
		}
		

		for(int i=0; i<Classes.size(); i++){
			System.out.println("Classes:");
			for(int j=0; j<Classes.get(i).getNInstances(); j++){
				Classes.get(i).getInstances().get(j).printInstance();
			}
			System.out.println();
		}
		System.out.println("Total Number of Instances: " + count);
		
		double FScoreGlobal = 0;
		for(int i=0; i<Classes.size(); i++){
			double FScoreLocal = 0;
			for(int j=0; j<Clusters.size(); j++){
				double fscore = 2 * R(Classes.get(i), Clusters.get(j)) * P(Classes.get(i), Clusters.get(j)) / (R(Classes.get(i), Clusters.get(j)) + P(Classes.get(i), Clusters.get(j))) ;
				if(fscore > FScoreLocal) FScoreLocal = fscore;
			}
			FScoreGlobal += (Classes.get(i).getNInstances()/count) * FScoreLocal;
		}
		
		System.out.println(FScoreGlobal);
	}

	private static double P(Node C, Node S) {
		// TODO Auto-generated method stub
		double n_i = S.getNInstances();
		double n_ri = 0;
		for(int i=0; i<C.getNInstances(); i++){
			if(S.getInstanceNames().contains(C.getInstances().get(i).getName())){
				n_ri++;
			}
		}
		return n_ri/n_i;
	}

	private static double R(Node C, Node S) {
		// TODO Auto-generated method stub
		double n_r = C.getNInstances();
		double n_ri = 0;
		for(int i=0; i<C.getNInstances(); i++){
			if(S.getInstanceNames().contains(C.getInstances().get(i).getName())){
				n_ri++;
			}
		}
		return n_ri/n_r;
	}

	private static Node getNode(ArrayList<Node> classes, String string) {
		// TODO Auto-generated method stub
		Node returnNode = null;
		int nodeID = Integer.parseInt(string);
		for(int i=0; i<classes.size(); i++){
			if(classes.get(i).getID() == nodeID){
				returnNode = classes.get(i);
			}
		}
		if(returnNode == null){
			returnNode = new Node(1);
			returnNode.setID(nodeID);
			classes.add(returnNode);
		}
		return returnNode;
	}

}
