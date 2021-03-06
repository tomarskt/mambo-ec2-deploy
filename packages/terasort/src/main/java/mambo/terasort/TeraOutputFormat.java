/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mambo.terasort;

import java.io.File;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.InvalidJobConfException;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.OutputCommitter;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputCommitter;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.security.TokenCache;

import java.util.Random;

/**
 * An output format that writes the key and value appended together.
 */
public class TeraOutputFormat extends FileOutputFormat<Text,Text> {
  static final String FINAL_SYNC_ATTRIBUTE = "mapreduce.terasort.final.sync";
  
  static final String OUTPUT_DIRS = "terasort.output.dirs";
  
  private OutputCommitter committer = null;
  static String[] outputdirs = null;

  /**
   * Set the requirement for a final sync before the stream is closed.
   */
  static void setFinalSync(JobContext job, boolean newValue) {
    job.getConfiguration().setBoolean(FINAL_SYNC_ATTRIBUTE, newValue);
  }

  /**
   * Does the user want a final sync at close?
   */
  public static boolean getFinalSync(JobContext job) {
    return job.getConfiguration().getBoolean(FINAL_SYNC_ATTRIBUTE, false);
  }

  static class TeraRecordWriter extends RecordWriter<Text,Text> {
    private boolean finalSync = false;
    private FSDataOutputStream out;

    public TeraRecordWriter(FSDataOutputStream out,
                            JobContext job) {
      finalSync = getFinalSync(job);
      this.out = out;
    }

    public synchronized void write(Text key, 
                                   Text value) throws IOException {
      out.write(key.getBytes(), 0, key.getLength());
      out.write(value.getBytes(), 0, value.getLength());
    }
    
    public void close(TaskAttemptContext context) throws IOException {
      if (finalSync) {
        out.sync();
      }
      out.close();
    }
  }

  @Override
  public void checkOutputSpecs(JobContext job
                              ) throws InvalidJobConfException, IOException {
    // Ensure that the output directory is set
    Path outDir = getOutputPath(job, null);
    if (outDir == null) {
      throw new InvalidJobConfException("Output directory not set in JobConf.");
    }

    // get delegation token for outDir's file system
    TokenCache.obtainTokensForNamenodes(job.getCredentials(),
        new Path[] { outDir }, job.getConfiguration());
  }
  
  public static String[] getAllOutputPaths() {
      return outputdirs;
  }

  public static Path getOutputPath(JobContext job, Integer taskid) {
      
	  String name = job.getConfiguration().get(FileOutputFormat.OUTDIR);
	  /*File outputConfigFile = new File(OUTPUT_CONFIG_FILE);
	  if (outputConfigFile.exists() && taskid != null) {
	      if (outputdirs == null) {
    	      Configuration outputconf = new Configuration();
    	      Path outputConfigFilePath = new Path(outputConfigFile.getPath());
    	      outputconf.addResource(outputConfigFilePath);
    	      String pathStrings = outputconf.get(OUTPUT_DIRS);
    	      outputdirs = pathStrings.split(";");

	      }
	      name = outputdirs[taskid % outputdirs.length];
	  }*/
	  
	  if (taskid != null) {
	      if(outputdirs == null) {
	          outputdirs = job.getConfiguration().get(TeraOutputFormat.OUTPUT_DIRS).split(";");
	      }
	      name = outputdirs[taskid % outputdirs.length];
	  }
	      
	  return name == null ? null:new Path(name);
  }

  public RecordWriter<Text,Text> getRecordWriter(TaskAttemptContext job
                                                 ) throws IOException {
	
    Path file = getDefaultWorkFile(job,"");
    FileSystem fs = file.getFileSystem(job.getConfiguration());
    FSDataOutputStream fileOut = fs.create(file);
    return new TeraRecordWriter(fileOut, job);
  }
  
  /*public OutputCommitter getOutputCommitter(TaskAttemptContext context) 
      throws IOException {
    if (committer == null) {
      int partition = context.getTaskAttemptID().getTaskID().getId();
      Integer dir_num = partition % 10;
      System.out.println("mambo multout: " + getOutputPath(context).toString() + "/" + dir_num.toString());
      Path output = new Path(getOutputPath(context).toString() + "/" + dir_num.toString());
      committer = new FileOutputCommitter(output, context);
    }
    FileOutputCommitter c = (FileOutputCommitter)committer;
    System.out.println("mambo multout return committer: " + c.getWorkPath().toString());
    return committer;
  }*/
  
  public OutputCommitter getOutputCommitter(TaskAttemptContext context) 
          throws IOException {
        if (committer == null) {
          Path output = getOutputPath(context, context.getTaskAttemptID().getTaskID().getId());
          System.out.println("mambo committer: " + output.toString());
          committer = new MultipleFileOutputCommitter(output, context);
        }
        
        return committer;
      }

}
