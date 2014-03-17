package org.jenkinsci.plugins.utils;

public class Param{
	
	private String	name;
	private String value;
	private String type;
	
	public Param(){
	}
	
	public Param(String n,String v, String t){
		this.name=n;
		this.value=v;
		this.type=t;
	}
	
	public void setName(String n){
		this.name=n;
	}
	
	public String getName(){
		return name;
	}
	
	public void setValue(String v){
		this.value=v;
	}
	
	public String getValue(){
		return value;
	}
	
	public void setType(String t){
		this.type=t;
	}
	
	public String getType(){
		return type;
	}
}