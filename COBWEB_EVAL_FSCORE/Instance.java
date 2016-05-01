
public class Instance {
	
	//private
	private  Boolean		constrained 		= false;
	private  String 		name				= "Random_Name";
	private  String[] 		attrValues			= null;
	
	public String[] getAttrValues() {
		return attrValues;
	}
	public void setAttrValues(String[] attrValues) {
		this.attrValues = attrValues;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void printInstance(){
		try{
			int n = Integer.parseInt(this.name);
			System.out.println(attrValues[0]);
		}
		catch(NumberFormatException e){
			System.out.println(this.name);
		}
		/*System.out.println(name);
		for(int i=0; i<attrValues.length; i++){
			System.out.print(attrValues[i] + " ");
		}
		System.out.println();*/
	}
	public int getNAttr() {
		return this.attrValues.length;
	}
	public void printName() {
		System.out.println(this.name);
		
	}
	public boolean isConstrained() {
		return constrained;
	}
	public void setConstrained(boolean constrained) {
		//System.out.println("Setting as Constrained");
		this.constrained = constrained;
	}
	
	
}
