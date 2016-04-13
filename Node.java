import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Node {
	public HashMap<String, Integer>[] attrCounts;
	private ArrayList<Node> children;
	private ArrayList<Instance> instances;
	
	private ArrayList<Integer> commonAttributes;
	
	private double nAttr;
	private double nInstances;
	private double nChildren;
	private int level;
	private String label;
	
	Node(int nAttr){
		this.nAttr = nAttr;
		this.nInstances = 0;
		this.nChildren = 0;
		
		this.attrCounts = new HashMap[nAttr];
		for(int i=0; i<this.nAttr; i++){
			this.attrCounts[i] = new HashMap<String, Integer>();
		}
		this.children = new ArrayList<Node>();
		this.instances = new ArrayList<Instance>();
		this.commonAttributes = new ArrayList<Integer>();
		//this.label = null;
		for(int i=0; i<this.nAttr; i++){
			this.commonAttributes.add(i);
		}
	}
	
	public void addInstance(Instance I){
		nInstances++;
		this.instances.add(I);
		for(int i=0; i<nAttr; i++){
			int temp = 1;
			String key = I.getAttrValues()[i];
			if(this.attrCounts[i].containsKey(key)){
				temp = this.attrCounts[i].get(key) + 1;
			}
			this.attrCounts[i].put(key, temp);
		}
	}
	
	public void removeInstance(Instance I){
		nInstances--;
		this.instances.remove(I);
		for(int i=0; i<nAttr; i++){
			String key = I.getAttrValues()[i];
			int temp = this.attrCounts[i].get(key) - 1;
			this.attrCounts[i].put(key, temp);
		}
	}
	
	public void addNode(Node N){
		if(!this.getChildren().contains(N)){
			this.nChildren++;
			this.children.add(N);
		}
	}
	
	public void removeNode(Node N){
		this.nChildren--;
		this.children.remove(N);
	}
	
	public Node copyNode(){
		Node returnNode = new Node((int)this.nAttr);
		//for(int i=0; i<nChildren; i++){
		//	returnNode.addNode(this.children.get(i));
		//}
		for(int i=0; i<nInstances; i++){
			returnNode.addInstance(this.instances.get(i));
		}
		return returnNode;
	}

	public double getNChildren() {
		// TODO Auto-generated method stub
		return this.children.size();
	}

	public ArrayList<Node> getChildren() {
		// TODO Auto-generated method stub
		return this.children;
	}

	public double calculateCU() {
		double CU = 0;
        double A = 0;
        double B = 0;        
        for(int node = 0; node< this.getNChildren(); node++){
            A = 0;
            B = 0;
            for(int attr = 0; attr < nAttr; attr++){
                for(String name: this.attrCounts[attr].keySet()){
                	String value = name.toString(); 
                    A += (this.getChildren().get(node).P_av(attr, value) * this.getChildren().get(node).P_av(attr, value));
                    B += this.P_av(attr, value) * this.P_av(attr, value);
                }
            }
            CU += P_n(this.getChildren().get(node)) * (A - B);
            //System.out.println("Node Probab : " + P_n(childNodes.get(node)));

        }
         CU = CU/(double)this.getNChildren();   
         
        return CU;
	}
    
	private double P_n(Node node) {
		return (double)node.getNInstances()/(double)this.nInstances;
	}
    
	private double P_av(int attr, String value) {
		int attrValueCount = 0;
		if(this.getAttrCounts()[attr].containsKey(value)) attrValueCount = (int) this.getAttrCounts()[attr].get(value);
		return (double)attrValueCount/(double)this.getNInstances();
	}

	private HashMap[] getAttrCounts() {
		return this.attrCounts;
	}

	public double getNInstances() {
		return this.instances.size();
	}

	public ArrayList<Instance> getInstances() {
		return this.instances;
	}
	
	public void setLevel(int lvl){
		this.level = lvl;
	}
	
	public int getLevel(){
		return this.level;
	}
	
	public void setLabel(String lbl){
		this.label = lbl;
	}
	
	public String getLabel(){
		return this.label;
	}
	
	public boolean updateCommonAttributes(){
		ArrayList<Integer> remove = new ArrayList<Integer>();
		for(int i=0; i<this.nAttr; i++){
			String commonValue = this.instances.get(0).getAttrValues()[i];
			for(int j=0; j<this.nInstances; j++){
				if(!commonValue.equals(this.instances.get(j).getAttrValues()[i])){
					remove.add(i);
					break;
				}
			}
		}
		for(int i=0; i<remove.size(); i++){
			commonAttributes.remove(remove.get(i));
		}
		boolean returnValue = false;
		if(remove.size()>0){
			returnValue = true;
		}
		return returnValue;
	}

	public boolean isConstraintEnforcable(Constraint C){
		boolean returnValue = false;
		if(this.getInstanceNames().contains(C.getCloseObject()) && this.getInstanceNames().contains(C.getFarObject())){
			returnValue = true;
		}
		return returnValue;
	}
	public boolean atConstraintLevel(Constraint C){
		boolean closeReturnValue = false;
		boolean farReturnValue = false;
		//System.out.println("Debug Mofo");
		//C.printConstraint();
		for(int i=0; i<this.getNChildren(); i++){
			if(this.getChildren().get(i).getInstanceNames().contains(C.getCloseObject())){
				if(!this.getChildren().get(i).getInstanceNames().contains(C.getFarObject())){
					closeReturnValue = true;
					break;	
				}
			}
		}
		for(int i=0; i<this.getNChildren(); i++){
			if(this.getChildren().get(i).getInstanceNames().contains(C.getFarObject())){
				if(!this.getChildren().get(i).getInstanceNames().contains(C.getCloseObject()))
				{
					farReturnValue = true;
					break;
				}
			}
		}
		
		return (closeReturnValue || farReturnValue);
	}

	public ArrayList<String> getInstanceNames() {
		// TODO Auto-generated method stub
		ArrayList<String> names = new ArrayList<String>();
		for(int i=0; i<this.getNInstances(); i++){
			names.add(this.getInstances().get(i).getName());
		}
		return names;
	}

	public void deepRemoveInstance(Instance I) {
		// TODO Auto-generated method stub
		this.removeInstance(I);
			for(int i=0; i<this.getNChildren(); i++){
				if(this.getChildren().get(i).getInstances().contains(I)){
					if(this.getChildren().get(i).getNInstances()>1){
						this.getChildren().get(i).deepRemoveInstance(I);
					}
					else{
						this.removeNode(this.getChildren().get(i));
					}
					break;
				}

		}
	}

	public String sample() {
		// TODO Auto-generated method stub
		return this.getInstanceNames().get((int)Math.floor(Math.random()*this.getNInstances()));
	}

	public String sample(String string) {
		// TODO Auto-generated method stub
		String returnValue = this.getInstanceNames().get((int)Math.floor(Math.random()*this.getNInstances()));
		if(this.getNInstances()>1){while(returnValue.equals(string)){
			returnValue = this.getInstanceNames().get((int)Math.floor(Math.random()*this.getNInstances()));
		}}
		return returnValue;
	}
}
