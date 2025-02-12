/*!
 *
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 *
 * Copyright (c) 2002-2023 Hitachi Vantara. All rights reserved.
 *
 */

package org.pentaho.platform.web.http.api.resources;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.pentaho.platform.api.scheduler2.CronJobTrigger;
import org.pentaho.platform.api.scheduler2.Job.JobState;
import org.pentaho.platform.api.scheduler2.SimpleJobTrigger;

@XmlRootElement
public class JobScheduleRequest implements Serializable {

  private static final long serialVersionUID = -485489832281790257L;

  public static final int SUNDAY = 0;

  public static final int MONDAY = 1;

  public static final int TUESDAY = 2;

  public static final int WEDNESDAY = 3;

  public static final int THURSDAY = 4;

  public static final int FRIDAY = 5;

  public static final int SATURDAY = 6;

  public static final int JANUARY = 0;

  public static final int FEBRUARY = 1;

  public static final int MARCH = 2;

  public static final int APRIL = 3;

  public static final int MAY = 4;

  public static final int JUNE = 5;

  public static final int JULY = 6;

  public static final int AUGUST = 7;

  public static final int SEPTEMBER = 8;

  public static final int OCTOBER = 9;

  public static final int NOVEMBER = 10;

  public static final int DECEMBER = 11;

  public static final int LAST_WEEK_OF_MONTH = 4;

  String jobName;

  String jobId;

  JobState jobState;

  String inputFile;

  String outputFile;

  String actionClass;

  CronJobTrigger cronJobTrigger;

  ComplexJobTriggerProxy complexJobTrigger;

  SimpleJobTrigger simpleJobTrigger;

  ArrayList<JobScheduleParam> jobParameters = new ArrayList<JobScheduleParam>();

  Map<String, String> pdiParameters;

  long duration;

  String timeZone;

  protected String runSafeMode;
  protected String gatheringMetrics;
  protected String logLevel;

  public String getInputFile() {
    return inputFile;
  }

  public void setInputFile( String file ) {
    this.inputFile = file;
  }

  public String getOutputFile() {
    return outputFile;
  }

  public void setOutputFile( String file ) {
    this.outputFile = file;
  }

  public CronJobTrigger getCronJobTrigger() {
    return cronJobTrigger;
  }

  public void setCronJobTrigger( CronJobTrigger jobTrigger ) {
    if ( jobTrigger != null ) {
      setComplexJobTrigger( null );
      setSimpleJobTrigger( null );
    }
    this.cronJobTrigger = jobTrigger;
  }

  public ComplexJobTriggerProxy getComplexJobTrigger() {
    return complexJobTrigger;
  }

  public void setComplexJobTrigger( ComplexJobTriggerProxy jobTrigger ) {
    if ( jobTrigger != null ) {
      setCronJobTrigger( null );
      setSimpleJobTrigger( null );
    }
    this.complexJobTrigger = jobTrigger;
  }

  public SimpleJobTrigger getSimpleJobTrigger() {
    return simpleJobTrigger;
  }

  public void setSimpleJobTrigger( SimpleJobTrigger jobTrigger ) {
    if ( jobTrigger != null ) {
      setCronJobTrigger( null );
      setComplexJobTrigger( null );
    }
    this.simpleJobTrigger = jobTrigger;
  }

  public ArrayList<JobScheduleParam> getJobParameters() {
    return jobParameters;
  }

  public void setJobParameters( ArrayList<JobScheduleParam> jobParameters ) {
    if ( jobParameters != this.jobParameters ) {
      this.jobParameters.clear();
      if ( jobParameters != null ) {
        this.jobParameters.addAll( jobParameters );
      }
    }
  }

  public Map<String, String> getPdiParameters() {
    return pdiParameters;
  }

  public void setPdiParameters( Map<String, String> pdiParameters ) {
    this.pdiParameters = pdiParameters;
  }

  public String getJobName() {
    return jobName;
  }

  public void setJobName( String jobName ) {
    this.jobName = jobName;
  }

  public JobState getJobState() {
    return jobState;
  }

  public void setJobState( JobState jobState ) {
    this.jobState = jobState;
  }

  public String getActionClass() {
    return actionClass;
  }

  public void setActionClass( String actionClass ) {
    this.actionClass = actionClass;
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration( long duration ) {
    this.duration = duration;
  }

  public String getTimeZone() {
    return timeZone;
  }

  public void setTimeZone( String timeZone ) {
    this.timeZone = timeZone;
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId( String jobId ) {
    this.jobId = jobId;
  }

  public String getRunSafeMode() {
    return runSafeMode;
  }

  public void setRunSafeMode( String runSafeMode ) {
    this.runSafeMode = runSafeMode;
  }

  public String getGatheringMetrics() {
    return gatheringMetrics;
  }

  public void setGatheringMetrics( String gatheringMetrics ) {
    this.gatheringMetrics = gatheringMetrics;
  }

  public String getLogLevel() {
    return logLevel;
  }

  public void setLogLevel( String logLevel ) {
    this.logLevel = logLevel;
  }
}
