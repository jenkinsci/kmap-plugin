package org.jenkinsci.plugins.utils;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.RequestEntity;
//import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import hudson.Launcher;
import hudson.remoting.Callable;
import java.io.IOException;

public class KmapConnection {
	
	HttpClient client;
	
	public KmapConnection(String url, Map<String,String> params){
		client = new HttpClient();
	    client.getParams().setParameter("http.useragent", "Jenkins Plugin");
	    
	    try{
		    PostMethod post = new PostMethod(url+"j_spring_security_check");
	        NameValuePair[] data = {
	          new NameValuePair("j_username", params.get("user")),
	          new NameValuePair("j_password", params.get("pass"))
	        };
	        post.setRequestBody(data);
	        int statusCode =client.executeMethod(post);
	        String response = "";
	        if (statusCode == 301 || statusCode == 302 || statusCode == 307) {
	        	GetMethod get = new GetMethod(post.getResponseHeader("Location").getValue());
	        	post.releaseConnection();
	        	client.executeMethod(get);
	//		      Cookie[] cookies = client.getState().getCookies();
	//		      for (int i = 0; i < cookies.length; i++) {
	//		        Cookie cookie = cookies[i];
	//		        response=response+("Cookie: " + cookie.getName() +", Value: " + cookie.getValue() +", IsPersistent?: " + cookie.isPersistent() +
	//		          ", Expiry Date: " + cookie.getExpiryDate() +", Comment: " + cookie.getComment());
	//
	////		        cookie.setValue("My own value");
	//		      }
	//			byte[] responseBody = get.getResponseBody();
	//			response=response + new String(responseBody);
		        get.releaseConnection();
	        }else{
	        	post.releaseConnection();
	        }
	    }catch(Exception e){
	    	e.printStackTrace();
	    }
	    
	}
	
	public Map requestPost(String url, List<Param> data, boolean multiPart, Launcher launcher){
		Map<String,String> result=new HashMap<String, String>();
		try{
			if (data==null){
				data = new ArrayList<Param>();
			}
		    PostMethod post = new PostMethod(url);
	        if(multiPart){	        	
	            Part[] parts =  new Part[data.size()];
		        for(int i=0; i<data.size(); i++ ){
		        	Param param= data.get(i);
		        	if(param.getType().equals("String")){
		        		String value;
		        		if(param.getValue()==null)
		        			value="";
		        		else{
		        			value=param.getValue();
		        		}
		        		parts[i]=new StringPart(param.getName(),value);
		        	}else{ // File type
		        		// Get a "channel" to the build machine and run the task there
		        		Map<String,Object> fileMap = launcher.getChannel().call( new MyTask(param.getValue()) );
		        		String error = (String)fileMap.get("error");
		        		if(error == null){
			        		String prefix = (String)fileMap.get("prefix");
			        		String suffix = (String)fileMap.get("suffix");
			        		byte[] dataFile = (byte[])fileMap.get("data");
			        		File f = File.createTempFile(prefix,"."+suffix);
			        		
			        		FileOutputStream fos = new FileOutputStream(f);
			        		fos.write(dataFile);
			        		fos.close();

			        		//f.delete();   //No borrar el file hasta que se haya hecho la request o enviaras un file Vacio
			        		
			        		parts[i]=new FilePart(param.getName(),f);
		        		}else{
		        			throw new Exception(error);
		        		}
		        	}
		        }
		        post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));
	        }else{
		        List<NameValuePair> dataAux = new ArrayList<NameValuePair>();
		        while(!data.isEmpty()){
		        	Param param= data.get(0);
	        		String value;
	        		if(param.getValue()==null)
	        			value="";
	        		else{
	        			value=param.getValue();
	        		}
		        	NameValuePair parametro  = new NameValuePair(param.getName(),value);
		        	dataAux.add(parametro);
		        	data.remove(0);
		        }
		        NameValuePair nvPair[] =  new NameValuePair[dataAux.size()];
		        for(int i=0; i<dataAux.size(); i++ ){
		        	nvPair[i]=dataAux.get(i);
		        }
		        post.setRequestBody(nvPair);
	        }
	        
	        int statusCode =client.executeMethod(post);
	        byte[] responseBody = post.getResponseBody();
	        String response=new String(responseBody);
	        post.releaseConnection();
	        
	        result.put("status", ""+statusCode);
	        JSONObject json = (JSONObject) JSONSerializer.toJSON( response );
	        String type=(String)json.names().get(0);
	        result.put("type", type);
	        result.put("message", json.getString(type));
			return result;
		}catch(Exception e){
			result.put("type", "exception");
			result.put("message", e.getMessage());
	    	return result;
	    }
	}
	
	public Map requestGet(String url,  List<Param> data){
		Map<String,String> result=new HashMap<String, String>();
		try{
			if (data==null){
				data = new ArrayList<Param>();
			}
			if(data.size() > 0){
				url=url+"?";
				boolean first=true;
		        while(!data.isEmpty()){
		        	if(first){
		        		first=false;
		        	}else{
		        		url=url+"&";
		        	}
		        	Param param=data.get(0);
	        		String value;
	        		if(param.getValue()==null)
	        			value="";
	        		else{
	        			value=param.getValue();
	        		}
		        	url=url+param.getName()+"="+value;
		        	data.remove(0);
		        }
			}
		    GetMethod get = new GetMethod(url);
	        int statusCode =client.executeMethod(get);
	        byte[] responseBody = get.getResponseBody();
	        String response=new String(responseBody);
	        get.releaseConnection();
	        
	        result.put("status", ""+statusCode);
	        JSONObject json = (JSONObject) JSONSerializer.toJSON( response );
	        String type=(String)json.names().get(0);
	        result.put("type", type);
	        result.put("message", json.getString(type));
			return result;
		}catch(Exception e){
			result.put("type", "exception");
			result.put("message", e.getMessage());
	    	return result;
	    }
	}
	
	
	
	

	// Define what should be run on the slave for this build
	private static class MyTask implements Callable<Map<String,Object>, IOException>{	
		private static final long serialVersionUID = 1L;
		private String path;
		
		public MyTask(String path){
			this.path=path;
		}
		
	    public Map<String,Object> call() throws IOException {
	        // This code will run on the build slave
	    	try {
	    		File f = new File(path);
	    	    InputStream in = new FileInputStream(f);
	    	    byte[] bytes = new byte[(int)f.length()];
	    	    in.read(bytes);
	    	    in.close();
	    	    
	    	    String fullPath = f.getAbsolutePath();
	    	    int dot = fullPath.lastIndexOf(".");
	    	    int sep = fullPath.lastIndexOf(System.getProperty("file.separator"));
	    	    String suffix = fullPath.substring(dot + 1);
	    	    String prefix = fullPath.substring(sep + 1, dot);
	    	    
	    	    Map<String, Object> mp=new HashMap<String, Object>();
	    	    mp.put("prefix", prefix);
	    	    mp.put("suffix", suffix);
	    	    mp.put("data", bytes);
	    	    return mp;

	    	} catch (IOException ioe) {
	     	    Map<String,Object> mp=new HashMap<String, Object>();
	    	    mp.put("error", "Error reading File in Slave. Error: "+ioe.getMessage());
	    	    return mp;
	    	}
        }
    };
}