package org.jenkinsci.plugins.lingr;

import hudson.Extension;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.tasks.Mailer;

import java.io.IOException;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.yoshiori.lingr.api.Lingr;
import org.yoshiori.lingr.api.Room;

public class LingrNotifier extends Notifier {
	protected static final Logger LOGGER = Logger.getLogger(LingrNotifier.class.getName());
	
	@DataBoundConstructor
	public LingrNotifier() {
	}
	 
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}
	
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		DescriptorImpl desc = descriptor();
		if (desc.isNotSuccess() && build.getResult() == Result.SUCCESS) return true;
		
		Lingr lingr = new Lingr(desc.getUserName(), desc.getPassword());
		lingr.createSession(desc.getAppkey());
		
		String projectName = build.getProject().getName();
		String result = build.getResult().toString();
		String url = "";
		if (desc.isIncludeURL()) {
			url = desc.getJenkinsURL();
		}
		String message = 
			String.format("%s:%s #%d - %s", result, projectName, build.number, url);
		
		lingr.say(message, new Room(desc.getRoom()));
		
		return true;
	}
	
    public static DescriptorImpl descriptor() {
        return Hudson.getInstance().getDescriptorByType(LingrNotifier.DescriptorImpl.class);
    }
	
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		private String userName;
		private String password;
		private String appkey;
		private String room;
		private boolean notSuccess;
		
		private boolean includeURL;
		private String jenkinsURL;
		
		public DescriptorImpl() {
			super(LingrNotifier.class);
			load();
		}
		
		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			// set the booleans to false as defaults
			includeURL = false;
			notSuccess = false;

			req.bindParameters(this, "lingr.");
			jenkinsURL = Mailer.descriptor().getUrl();

			save();
			return super.configure(req, formData);
		}
		
		@Override
		public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			if (jenkinsURL == null) {
				// if Jenkins URL is not configured yet, infer some default
				jenkinsURL = Functions.inferHudsonURL(req);
				save();
			}
			return super.newInstance(req, formData);
		}
		
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> arg0) {
			return true;
		}
		@Override
		public String getDisplayName() {
			return "Lingr";
		}

		public String getUserName() {
			return userName;
		}
		public void setUserName(String userName) {
			this.userName = userName;
		}
		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}
		public String getAppkey() {
			return appkey;
		}
		public void setAppkey(String appkey) {
			this.appkey = appkey;
		}
		public String getRoom() {
			return room;
		}
		public void setRoom(String room) {
			this.room = room;
		}
		public boolean isNotSuccess() {
			return notSuccess;
		}
		public void setNotSuccess(boolean notSuccess) {
			this.notSuccess = notSuccess;
		}
		public boolean isIncludeURL() {
			return includeURL;
		}
		public void setIncludeURL(boolean includeURL) {
			this.includeURL = includeURL;
		}
		public String getJenkinsURL() {
			return jenkinsURL;
		}
		public void setJenkinsURL(String jenkinsURL) {
			this.jenkinsURL = jenkinsURL;
		}
	}
}
