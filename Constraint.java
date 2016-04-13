
public class Constraint {
	private int id;
	private String label;
	private String object;
	private String closeObject;
	private String farObject;
	
	public Constraint(int i, String[] constraintArtifacts) {
		// TODO Auto-generated constructor stub
		this.setId(i);
		this.setLabel(constraintArtifacts[0]);
		this.setObject(constraintArtifacts[1]);
		this.setCloseObject(constraintArtifacts[2]);
		this.setFarObject(constraintArtifacts[3]);
		System.out.println("Constraint Created : R - " + this.getObject() + " " + this.getCloseObject() + " " +this.getFarObject());
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getObject() {
		return object;
	}
	public void setObject(String object) {
		this.object = object;
	}
	public String getCloseObject() {
		return closeObject;
	}
	public void setCloseObject(String closeObject) {
		this.closeObject = closeObject;
	}
	public String getFarObject() {
		return farObject;
	}
	public void setFarObject(String farObject) {
		this.farObject = farObject;
	}
	public void printConstraint() {
		// TODO Auto-generated method stub
		System.out.println(this.object + " " + this.closeObject + " " + this.farObject);
	}
	
	
	
}
