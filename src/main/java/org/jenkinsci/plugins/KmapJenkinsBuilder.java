package org.jenkinsci.plugins;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;
import org.jenkinsci.plugins.utils.PublishOptional;
import javax.servlet.ServletException;
import java.io.IOException;
import org.jenkinsci.plugins.utils.KmapConnection;
import org.jenkinsci.plugins.utils.Param;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link KmapJenkinsBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked. 
 *
 * @author Kohsuke Kawaguchi
 */



public class KmapJenkinsBuilder extends Builder {

    private String	username;
    private String	password;
    private String	kmapClient;
    private String	categories;
    private PublishOptional publishOptional;
    private String	filePath;
    private String appName;
    private String bundle;
    private String version;
    private String description;
    private String iconPath;
   

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public KmapJenkinsBuilder(String username, String password, String kmapClient, String categories, String teams, 
    		String users, boolean sendNotifications, PublishOptional publishOptional, 
    		String filePath, String appName, String bundle, String version, String description, String iconPath) {
        this.username = username;
        this.password = password;
        this.kmapClient = kmapClient;
        this.categories = categories;
        this.publishOptional= publishOptional;
        this.filePath = filePath;
        this.appName = appName;
        this.bundle = bundle;
        this.version = version;
        this.description= description;
        this.iconPath=iconPath;
//        Map<Object, String> usersOptional=new HashMap<Object, String>();
//        usersOptional.put('users',users);
//        this.publishOptional.put('usersOptional',usersOptional);
        //this.teams = teams;
        //this.users = users;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getUsername() {
        return username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public String getKmapClient(){
    	return kmapClient;
    }
    
    public String getCategories(){
    	return categories;
    }
    
    public PublishOptional getPublishOptional(){
    	return publishOptional;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public String getAppName() {
        return appName;
    }
    
    public String getBundle() {
        return bundle;
    }
    
    public String getVersion() {
        return version;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getIconPath() {
    	return iconPath;
    }
    
    
    private String replaceEnvVars(EnvVars env, String param){
    	Set<String>setKeys = env.keySet();
    	Iterator<String> it = setKeys.iterator();
    	while(it.hasNext()){
    		String var_name = it.next();
    		//Pattern p = Pattern.compile("(\\$"+var_name+"[^a-zA-Z0-9_])|(\\$\\{"+var_name+"\\})|(\\$"+var_name+"$)");
    		Pattern p = Pattern.compile("(\\$\\{"+var_name+"\\})");
    		Matcher m = p.matcher(param);
    		param = m.replaceAll(env.get(var_name));
    	}
    	return param;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        // This is where you 'build' the project.
    	EnvVars env = null;
    	try{
    		env = build.getEnvironment(listener);
    	}catch(Exception e){
    		listener.getLogger().println("Kmap Plugin --> "+e.getMessage());
    	}
    	
        String username = replaceEnvVars(env,this.username);
        String password = replaceEnvVars(env,this.password);
        String kmapClient = replaceEnvVars(env,this.kmapClient);
        String categories = replaceEnvVars(env,this.categories);
        String filePath = replaceEnvVars(env,this.filePath);
        String appName = replaceEnvVars(env,this.appName);
        String bundle = replaceEnvVars(env,this.bundle);
        String version = replaceEnvVars(env,this.version);
        String description = replaceEnvVars(env,this.description);
        String iconPath = replaceEnvVars(env,this.iconPath);
        
        listener.getLogger().println("Kmap Plugin --> username: "+username);
        listener.getLogger().println("Kmap Plugin --> password: ***********");//"+password);
        listener.getLogger().println("Kmap Plugin --> kmapUrl: "+kmapClient);
        listener.getLogger().println("Kmap Plugin --> categories: "+categories);
        listener.getLogger().println("Kmap Plugin --> filePath: "+filePath);
        listener.getLogger().println("Kmap Plugin --> appName: "+appName);
        listener.getLogger().println("Kmap Plugin --> bundle: "+bundle);
        listener.getLogger().println("Kmap Plugin --> version: "+version);
        listener.getLogger().println("Kmap Plugin --> description: "+description);
        listener.getLogger().println("Kmap Plugin --> iconPath: "+iconPath);

    	Map<String,String> params=new HashMap<String, String>();
		params.put("user", username);
		params.put("pass", password);
	    KmapConnection kmapConnection = new KmapConnection(kmapClient,params);
	    Map<String,String> response=kmapConnection.requestGet(kmapClient+"ci/validateAccess",null);
	    if(response.get("message").contains("User can access to Kmap-Client")){
		    List<Param> data = new ArrayList<Param>();
		    data.add(new Param("categories",categories, "String"));
		    data.add(new Param("appName",appName, "String"));
	    	response=kmapConnection.requestPost(kmapClient+"ci/validateCategories",data, false, launcher);
	    	if(!response.get("type").equals("error") && !response.get("type").equals("exception") ){
	    		List<Param> lastData = new ArrayList<Param>();
	    		if(publishOptional != null){
	    			listener.getLogger().println("Kmap Plugin --> publish: true");
	    			String teamList = replaceEnvVars(env,publishOptional.getTeams());
	    			listener.getLogger().println("Kmap Plugin --> groups: "+teamList);
	    			if (teamList!=null && !teamList.equals("")){
	        		    List<Param> data2 = new ArrayList<Param>();
	        		    data2.add(new Param("teams",teamList, "String"));
    	    			response=kmapConnection.requestPost(kmapClient+"ci/validateTeams",data2, false, launcher);
    	    			if(response.get("type").equals("error") || response.get("type").equals("exception")){
    	    				listener.getLogger().println("Kmap Plugin --> "+response.get("type")+": "+response.get("message"));
    	    				return false;
    	    			}
    	    			lastData.add(new Param("teams",teamList, "String"));
    	    		}
	    			String userList = replaceEnvVars(env,publishOptional.getUsers());
	    			listener.getLogger().println("Kmap Plugin --> users: "+userList);
	    			if (userList!=null && !userList.equals("")){
	        		    List<Param> data3 = new ArrayList<Param>();
	        		    data3.add(new Param("users",userList, "String"));
    	    			response=kmapConnection.requestPost(kmapClient+"ci/validateUsers",data3, false, launcher);
    	    			if(response.get("type").equals("error") || response.get("type").equals("exception")){
    	    				listener.getLogger().println("Kmap Plugin --> "+response.get("type")+": "+response.get("message"));
    	    				return false;
    	    			}
    	    			lastData.add(new Param("users",userList, "String"));
    	    		}
	    			String sendNotifications;
	    			if(publishOptional.getSendNotifications()){
	    				sendNotifications="true";
	    			}else{
	    				sendNotifications="false";
	    			}
	    			listener.getLogger().println("Kmap Plugin --> NotifyUsers: "+sendNotifications);
	    			
	    			List<Param> createData = new ArrayList<Param>();
	    			createData.add(new Param("appName",appName, "String"));
	    			createData.add(new Param("description",description, "String"));
	    			createData.add(new Param("categories",categories, "String"));
	    			createData.add(new Param("fileApp",filePath, "File"));
	    			createData.add(new Param("bundle",bundle, "String"));
	    			createData.add(new Param("friendlyVersion",version, "String"));
	    			if(iconPath != null && !iconPath.equals("")){
	    				createData.add(new Param("icon",iconPath, "File"));
	    			}
	    			response=kmapConnection.requestPost(kmapClient+"ci/createApp",createData, true, launcher);
		    		listener.getLogger().println("Kmap Plugin --> "+response.get("type")+": "+response.get("message"));
		    		if(response.get("type").equals("error") || response.get("type").equals("exception")){
		    			return false;
		    		}else{
		    	        JSONObject json = (JSONObject) JSONSerializer.toJSON( response.get("message") );
		    			lastData.add(new Param("appId",json.getString("id"), "String"));
		    			lastData.add(new Param("sendNotifications",sendNotifications, "String"));
		    			response=kmapConnection.requestPost(kmapClient+"ci/publishApp",lastData, true, launcher);
			    		listener.getLogger().println("Kmap Plugin --> "+response.get("type")+": "+response.get("message"));
			    		if(response.get("type").equals("error") || response.get("type").equals("exception")){
			    			return false;
			    		}else{
			    			return true;
			    		}
		    		}
	    		}else{
	    			List<Param> createData = new ArrayList<Param>();
	    			createData.add(new Param("appName",appName, "String"));
	    			createData.add(new Param("description",description, "String"));
	    			createData.add(new Param("categories",categories, "String"));
	    			createData.add(new Param("fileApp",filePath, "File"));
	    			createData.add(new Param("bundle",bundle, "String"));
	    			createData.add(new Param("friendlyVersion",version, "String"));
	    			if(iconPath != null && !iconPath.equals("")){
	    				createData.add(new Param("icon",iconPath, "File"));
	    			}
	    			response=kmapConnection.requestPost(kmapClient+"ci/createApp",createData, true, launcher);
		    		listener.getLogger().println("Kmap Plugin --> "+response.get("type")+": "+response.get("message"));
		    		if(response.get("type").equals("error") || response.get("type").equals("exception")){
		    			return false;
		    		}else{
		    			return true;
		    		}
	    		}
	    	}else{
	    		listener.getLogger().println("Kmap Plugin --> "+response.get("type")+": "+response.get("message"));
				return false;
	    	}
	    }else{
			listener.getLogger().println("Kmap Plugin --> "+response.get("type")+": "+response.get("message"));
			return false;
	    }

        // This also shows how you can consult the global configuration of the builder
//        if (getDescriptor().getUseFrench() && !getDescriptor().getUseSpanish())
//            listener.getLogger().println("Bonjour, "+name+"!");
//        else if (getDescriptor().getUseSpanish())
//        	listener.getLogger().println("Hola, "+name+"!");
//        else
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link KmapJenkinsBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/KmapJenkinsBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private String	username;
        private String	password;
        private String	kmapClient;

        public FormValidation doCheckUsername(@QueryParameter String value)
                throws IOException, ServletException {
            if (value==null || value.equals("")){
                return FormValidation.error("Mandatory field");
            }
            return FormValidation.ok();
        }
        
        public FormValidation doCheckPassword(@QueryParameter String value)
                throws IOException, ServletException {
            if (value==null || value.equals("")){
                return FormValidation.error("Mandatory field");
            }
            return FormValidation.ok();
        }
        
        public FormValidation doCheckKmapClient(@QueryParameter String value)
                throws IOException, ServletException {
            if (value==null || value.equals("")){
                return FormValidation.error("Mandatory field");
            }else if(!value.substring(value.length()-12).equals("kmap-client/")){
            	return FormValidation.error("The URL must finish with 'kmap-client/'");
            }
            return FormValidation.ok();
        }
        
        public FormValidation doCheckCategories(@QueryParameter String value)
                throws IOException, ServletException {
            if (value==null || value.equals("")){
                return FormValidation.error("This field is Mandatory");
            }
            return FormValidation.ok();
        }
        
        public FormValidation doCheckBundle(@QueryParameter String value)
		      throws IOException, ServletException {
		  if (value==null || value.equals("")){
		      return FormValidation.warning("Click on the help button for detailed information.");
		  }
		  return FormValidation.ok();
		}
        
        public FormValidation doCheckFilePath(@QueryParameter String value)
                throws IOException, ServletException {
            if (value==null || value.equals("")){
                return FormValidation.error("Mandatory field");
            }
            return FormValidation.ok();
        }
        
        public FormValidation doCheckAppName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value==null || value.equals("")){
                return FormValidation.error("Mandatory field");
            }
            return FormValidation.ok();
        }
        
        public FormValidation doCheckVersion(@QueryParameter String value)
                throws IOException, ServletException {
            if (value==null || value.equals("")){
                return FormValidation.error("Mandatory field");
            }
            return FormValidation.ok();
        }
       
        
        public FormValidation doTestConnection(@QueryParameter("username") String user, 
                @QueryParameter("password") String pass, @QueryParameter("kmapClient") String url, @QueryParameter("categories") String cats, @QueryParameter("appName") String appN,
                @QueryParameter("teams") String teamList,@QueryParameter("users") String userList, @QueryParameter("publishOptional") boolean publish) throws IOException, ServletException {
        	    
    		Map<String,String> params=new HashMap<String, String>();
    		params.put("user", user);
    		params.put("pass", pass);
    	    KmapConnection kmapConnection = new KmapConnection(url,params);
    	    
    	    Map<String,String> response=kmapConnection.requestGet(url+"ci/validateAccess",null);
    	    
    	    if(response.get("message").contains("User can access to Kmap-Client")){
    		    List<Param> data = new ArrayList<Param>();
    		    data.add(new Param("categories",cats, "String"));
    	    	response=kmapConnection.requestPost(url+"ci/validateCategories",data, false, null);
    	    	if(!response.get("type").equals("error") && !response.get("type").equals("exception") ){
    	    		String type = response.get("type");
    	    		if(publish){
    	    			if (teamList!=null && !teamList.equals("")){
    	        		    List<Param> data2 = new ArrayList<Param>();
    	        		    data2.add(new Param("teams",teamList, "String"));
        	    			response=kmapConnection.requestPost(url+"ci/validateTeams",data2, false, null);
        	    			if(response.get("type").equals("error") || response.get("type").equals("exception")){
        	    				return FormValidation.error(response.get("type")+": "+response.get("message"));
        	    			}
        	    		}
    	    			if (userList!=null && !userList.equals("")){
    	        		    List<Param> data3 = new ArrayList<Param>();
    	        		    data3.add(new Param("users",userList, "String"));
        	    			response=kmapConnection.requestPost(url+"ci/validateUsers",data3, false, null);
        	    			if(response.get("type").equals("error") || response.get("type").equals("exception")){
        	    				return FormValidation.error(response.get("type")+": "+response.get("message"));
        	    			}
        	    		}
    	    			return FormValidation.ok("Connection and Parameters OK");
    	    		}else{
    	    			return FormValidation.ok("Connection and Parameters OK");
    	    		}
    	    	}else{
    	    		return FormValidation.error(response.get("type")+": "+response.get("message"));
    	    	}
    	    }else{
    	    	return FormValidation.error(response.get("type")+": "+response.get("message"));
    	    }
        }
        
        
        public FormValidation doTestConnectionGlobal(@QueryParameter("username") String user, 
                @QueryParameter("password") String pass, @QueryParameter("kmapClient") String url) throws IOException, ServletException {
        	    
    		Map<String,String> params=new HashMap<String, String>();
    		params.put("user", user);
    		params.put("pass", pass);
    	    KmapConnection kmapConnection = new KmapConnection(url,params);
    	    
    	    Map<String,String> response=kmapConnection.requestGet(url+"ci/validateAccess",null);
    	    
    	    if(response.get("message").contains("User can access to Kmap-Client")){
    	    	return FormValidation.ok("Connection and Parameters OK");
    	    }else{
    	    	return FormValidation.error(response.get("type")+": "+response.get("message"));
    	    }
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Kmap Plugin";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            username = formData.getString("username");
            password = formData.getString("password");
            kmapClient = formData.getString("kmapClient");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        public String getUsername() {
            return username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public String getKmapClient(){
        	return kmapClient;
        }
        public String getVersion(){
        	return "b${BUILD_NUMBER}_r${SVN_REVISION}";
        }
        public String getFilePath(){
        	return "${WORKSPACE}/path/to/file.extension";
        }
    }
}

