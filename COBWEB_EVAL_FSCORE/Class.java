import java.util.ArrayList;

public class Class {
	private ArrayList<Instance> instances = new ArrayList<Instance>();
	private int classID;
	
	Class(int ID){
		this.setClassID(ID);
	}
	public void addInstance(Instance I){
		this.instances.add(I);
	}
	public void removeInstance(Instance I){
		this.instances.remove(I);
	}
	public double getSize(){
		return instances.size();
	}
	public boolean contains(Instance I){
		boolean returnValue = false;
		if(this.instances.contains(I)) returnValue = true;
		return returnValue;
	}
	public int getClassID() {
		return classID;
	}
	public void setClassID(int classID) {
		this.classID = classID;
	}
}
