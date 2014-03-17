package org.jenkinsci.plugins.utils;

import org.kohsuke.stapler.DataBoundConstructor;


public class PublishOptional {
    private final String teams;
    private final String users;
    private final boolean sendNotifications;

    @DataBoundConstructor
    public PublishOptional(String teams, String users, boolean sendNotifications) {
        this.teams = teams;
        this.users = users;
        this.sendNotifications= sendNotifications;
    }

    public String getTeams() {
        return teams;
    }

    public String getUsers() {
        return users;
    }
    
    public boolean getSendNotifications(){
    	return sendNotifications;
    }
}