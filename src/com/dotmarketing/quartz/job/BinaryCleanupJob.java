/**
 * 
 */
package com.dotmarketing.quartz.job;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.dotcms.repackage.commons_io.org.apache.commons.io.FileUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;

/**
 * This job will clean up the binary folder created under the binary directory.  It will cleanup files older then 12 hours by default.
 * This can be over ridden via the property BINARY_CLEANUP_FILE_LIFE_HOURS 
 * The DotScheduler will also look for BINARY_CLEANUP_JOB_CRON_EXPRESSION to see if it should start the job or not. 
 * @author BayLogic
 * @since 
 * http://jira.dotmarketing.net/browse/DOTCMS-1073
 */
public class BinaryCleanupJob implements Job {
	
	/* (non-Javadoc)
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
	
	
	public BinaryCleanupJob() {
		
	}
	
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		
		int hours = Config.getIntProperty("BINARY_CLEANUP_FILE_LIFE_HOURS",-1);
		if(hours < 0){
			hours = 12;
		}
		Calendar c = Calendar.getInstance();
		c.add(Calendar.HOUR_OF_DAY, -hours);
		Date dDate = c.getTime();
		File tempDir = null;
		try {
			tempDir = getTempBinaryDir();
		} catch (IOException e) {
			Logger.error(this,"Unable to get tempory file holder exiting job", e);
			return;
		}
		if(!tempDir.exists()){
			Logger.info(this,"Tempory Binary Directory "+ tempDir.getPath() + " not found exiting job");
			return;
		}
		File[] files = tempDir.listFiles();
		for (File file : files) {
			if(file.isDirectory()){
				boolean deleteFolder = true;
				try {
					List<File> rFiles = FileUtil.listFilesRecursively(tempDir);
					for (File rFile : rFiles) {
						if(FileUtils.isFileOlder(rFile, dDate)){
							deleteFolder = false;
							break;
						}
					}
					if(deleteFolder){
						FileUtil.deltree(file);
					}
				} catch (FileNotFoundException e) {
					Logger.error(this, "Temp dir not found unable to list files for " + file.getPath(),e);
					continue;
				}
			}else{
				if(FileUtils.isFileOlder(file, dDate)){
					file.delete();
				}
			}
		}
		
	}

	private File getTempBinaryDir() throws IOException{
		String binaryPath = APILocator.getFileAPI().getRealAssetPathTmpBinary();
		return new File(binaryPath);
	}
}
