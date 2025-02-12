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

import com.cronutils.builder.CronBuilder;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPluginManager;
import org.pentaho.platform.api.engine.PluginBeanException;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.repository2.unified.UnifiedRepositoryException;
import org.pentaho.platform.api.scheduler2.ComplexJobTrigger;
import org.pentaho.platform.api.scheduler2.IJobTrigger;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.api.scheduler2.SimpleJobTrigger;
import org.pentaho.platform.api.util.IPdiContentProvider;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.services.exporter.ScheduleExportUtil;
import org.pentaho.platform.repository.RepositoryFilenameUtils;
import org.pentaho.platform.scheduler2.quartz.QuartzScheduler;
import org.pentaho.platform.scheduler2.recur.QualifiedDayOfWeek;
import org.pentaho.platform.scheduler2.recur.QualifiedDayOfWeek.DayOfWeek;
import org.pentaho.platform.scheduler2.recur.QualifiedDayOfWeek.DayOfWeekQualifier;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

import static com.cronutils.model.field.expression.FieldExpressionFactory.always;
import static com.cronutils.model.field.expression.FieldExpressionFactory.every;
import static com.cronutils.model.field.expression.FieldExpressionFactory.on;
import static com.cronutils.model.field.expression.FieldExpressionFactory.questionMark;

public class SchedulerResourceUtil {

  private static final Log logger = LogFactory.getLog( SchedulerResourceUtil.class );

  public static final String RESERVEDMAPKEY_LINEAGE_ID = "lineage-id";
  public static final String RESERVED_BACKGROUND_EXECUTION_ACTION_ID = ".backgroundExecution"; //$NON-NLS-1$ //$NON-NLS-2$

  public static IJobTrigger convertScheduleRequestToJobTrigger( JobScheduleRequest scheduleRequest,
                                                                IScheduler scheduler )
    throws SchedulerException, UnifiedRepositoryException {

    // Used to determine if created by a RunInBackgroundCommand
    boolean runInBackground =
      scheduleRequest.getSimpleJobTrigger() == null && scheduleRequest.getComplexJobTrigger() == null
        && scheduleRequest.getCronJobTrigger() == null;

    // add 10 seconds to the RIB to ensure execution (see PPP-3264)
    IJobTrigger jobTrigger =
      runInBackground ? new SimpleJobTrigger( new Date( System.currentTimeMillis() + 10000 ), null, 0, 0 )
        : scheduleRequest.getSimpleJobTrigger();

    if ( scheduleRequest.getSimpleJobTrigger() != null ) {
      SimpleJobTrigger simpleJobTrigger = scheduleRequest.getSimpleJobTrigger();

      if ( simpleJobTrigger.getStartTime() == null ) {
        simpleJobTrigger.setStartTime( new Date() );
      }

      jobTrigger = simpleJobTrigger;

    } else if ( scheduleRequest.getComplexJobTrigger() != null ) {

      ComplexJobTriggerProxy proxyTrigger = scheduleRequest.getComplexJobTrigger();
      String cronString = proxyTrigger.getCronString();
      ComplexJobTrigger complexJobTrigger = null;
      /**
       * We will have two options. Either it is a daily scehdule to ignore DST or any other
       * complex schedule
       */
      if(cronString != null && cronString.equals("TO_BE_GENERATED")) {
        cronString = generateCronString((int)proxyTrigger.getRepeatInterval()/86400
                ,proxyTrigger.getStartTime());
        complexJobTrigger = QuartzScheduler.createComplexTrigger( cronString );
      } else {
        complexJobTrigger = new ComplexJobTrigger();
        if ( proxyTrigger.getDaysOfWeek().length > 0 ) {
          if ( proxyTrigger.getWeeksOfMonth().length > 0 ) {
            for ( int dayOfWeek : proxyTrigger.getDaysOfWeek() ) {
              for ( int weekOfMonth : proxyTrigger.getWeeksOfMonth() ) {

                QualifiedDayOfWeek qualifiedDayOfWeek = new QualifiedDayOfWeek();
                qualifiedDayOfWeek.setDayOfWeek( DayOfWeek.values()[ dayOfWeek ] );

                if ( weekOfMonth == JobScheduleRequest.LAST_WEEK_OF_MONTH ) {
                  qualifiedDayOfWeek.setQualifier( DayOfWeekQualifier.LAST );
                } else {
                  qualifiedDayOfWeek.setQualifier( DayOfWeekQualifier.values()[ weekOfMonth ] );
                }
                complexJobTrigger.addDayOfWeekRecurrence( qualifiedDayOfWeek );
              }
            }
          } else {
            for ( int dayOfWeek : proxyTrigger.getDaysOfWeek() ) {
              complexJobTrigger.addDayOfWeekRecurrence( dayOfWeek + 1 );
            }
          }
        } else if ( proxyTrigger.getDaysOfMonth().length > 0 ) {

          for ( int dayOfMonth : proxyTrigger.getDaysOfMonth() ) {
            complexJobTrigger.addDayOfMonthRecurrence( dayOfMonth );
          }
        }

        for ( int month : proxyTrigger.getMonthsOfYear() ) {
          complexJobTrigger.addMonthlyRecurrence( month + 1 );
        }

        for ( int year : proxyTrigger.getYears() ) {
          complexJobTrigger.addYearlyRecurrence( year );
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime( proxyTrigger.getStartTime() );
        complexJobTrigger.setHourlyRecurrence( calendar.get( Calendar.HOUR_OF_DAY ) );
        complexJobTrigger.setMinuteRecurrence( calendar.get( Calendar.MINUTE ) );
      }

      complexJobTrigger.setStartTime( proxyTrigger.getStartTime() );
      complexJobTrigger.setEndTime( proxyTrigger.getEndTime() );
      complexJobTrigger.setDuration( scheduleRequest.getDuration() );
      complexJobTrigger.setUiPassParam( scheduleRequest.getComplexJobTrigger().getUiPassParam() );
      jobTrigger = complexJobTrigger;

    } else if ( scheduleRequest.getCronJobTrigger() != null ) {

      if ( scheduler instanceof QuartzScheduler ) {
        String cronString = scheduleRequest.getCronJobTrigger().getCronString();
        String delims = "[ ]+"; //$NON-NLS-1$
        String[] tokens = cronString.split( delims );
        if ( tokens.length < 7 ) {
          cronString += " *";
        }
        ComplexJobTrigger complexJobTrigger = QuartzScheduler.createComplexTrigger( cronString );
        complexJobTrigger.setStartTime( scheduleRequest.getCronJobTrigger().getStartTime() );
        complexJobTrigger.setEndTime( scheduleRequest.getCronJobTrigger().getEndTime() );
        complexJobTrigger.setDuration( scheduleRequest.getCronJobTrigger().getDuration() );
        complexJobTrigger.setUiPassParam( scheduleRequest.getCronJobTrigger().getUiPassParam() );
        jobTrigger = complexJobTrigger;
      }  else {
        throw new IllegalArgumentException();
      }
    }

    return jobTrigger;
  }

  public static void updateStartDateForTimeZone( JobScheduleRequest request ) {
    if ( request.getSimpleJobTrigger() != null ) {
      if ( request.getSimpleJobTrigger().getStartTime() != null ) {
        Date origStartDate = request.getSimpleJobTrigger().getStartTime();
        Date serverTimeZoneStartDate = convertDateToServerTimeZone( origStartDate, request.getTimeZone() );
        request.getSimpleJobTrigger().setStartTime( serverTimeZoneStartDate );
      }
    } else if ( request.getComplexJobTrigger() != null ) {
      if ( request.getComplexJobTrigger().getStartTime() != null ) {
        Date origStartDate = request.getComplexJobTrigger().getStartTime();
        Date serverTimeZoneStartDate = convertDateToServerTimeZone( origStartDate, request.getTimeZone() );
        request.getComplexJobTrigger().setStartTime( serverTimeZoneStartDate );
      }
    } else if ( request.getCronJobTrigger() != null ) {
      if ( request.getCronJobTrigger().getStartTime() != null ) {
        Date origStartDate = request.getCronJobTrigger().getStartTime();
        Date serverTimeZoneStartDate = convertDateToServerTimeZone( origStartDate, request.getTimeZone() );
        request.getCronJobTrigger().setStartTime( serverTimeZoneStartDate );
      }
    }
  }

  public static Date convertDateToServerTimeZone( Date dateTime, String timeZone ) {
    Calendar userDefinedTime = Calendar.getInstance();
    userDefinedTime.setTime( dateTime );
    if ( !TimeZone.getDefault().getID().equalsIgnoreCase( timeZone ) ) {
      logger.warn( "original defined time: " + userDefinedTime.getTime().toString() + " on tz:" + timeZone );
      Calendar quartzStartDate = new GregorianCalendar( TimeZone.getTimeZone( timeZone ) );
      quartzStartDate.set( Calendar.YEAR, userDefinedTime.get( Calendar.YEAR ) );
      quartzStartDate.set( Calendar.MONTH, userDefinedTime.get( Calendar.MONTH ) );
      quartzStartDate.set( Calendar.DAY_OF_MONTH, userDefinedTime.get( Calendar.DAY_OF_MONTH ) );
      quartzStartDate.set( Calendar.HOUR_OF_DAY, userDefinedTime.get( Calendar.HOUR_OF_DAY ) );
      quartzStartDate.set( Calendar.MINUTE, userDefinedTime.get( Calendar.MINUTE ) );
      quartzStartDate.set( Calendar.SECOND, userDefinedTime.get( Calendar.SECOND ) );
      quartzStartDate.set( Calendar.MILLISECOND, userDefinedTime.get( Calendar.MILLISECOND ) );
      logger.warn( "adapted time for " + TimeZone.getDefault().getID() + ": " + quartzStartDate.getTime().toString() );
      return quartzStartDate.getTime();
    } else {
      return dateTime;
    }
  }


  public static HashMap<String, Serializable> handlePDIScheduling( RepositoryFile file,
                                                                   HashMap<String, Serializable> parameterMap,
                                                                   Map<String, String> pdiParameters ) {

    HashMap<String, Serializable> convertedParameterMap = new HashMap<>();
    IPdiContentProvider provider = null;
    Map<String, String> kettleParams = new HashMap<>();
    Map<String, String> kettleVars = new HashMap<>();
    Map<String, String> scheduleKettleVars = new HashMap<>();
    boolean fallbackToOldBehavior = false;
    try {
      provider = getiPdiContentProvider();
      kettleParams = provider.getUserParameters( file.getPath() );
      kettleVars = provider.getVariables( file.getPath() );
    } catch ( PluginBeanException e ) {
      logger.error( e );
      fallbackToOldBehavior = true;
    }

    boolean paramsAdded = false;
    if ( pdiParameters != null ) {
      convertedParameterMap.put( ScheduleExportUtil.RUN_PARAMETERS_KEY, (Serializable) pdiParameters );
      paramsAdded = true;
    } else {
      pdiParameters = new HashMap<>();
    }

    if ( file != null && isPdiFile( file ) ) {

      Iterator<String> it = parameterMap.keySet().iterator();

      while ( it.hasNext() ) {

        String param = it.next();

        if ( !StringUtils.isEmpty( param ) && parameterMap.containsKey( param ) ) {
          convertedParameterMap.put( param, parameterMap.get( param ).toString() );
          if ( !paramsAdded && ( fallbackToOldBehavior || kettleParams.containsKey( param ) ) ) {
            pdiParameters.put( param, parameterMap.get( param ).toString() );
          }
          if ( kettleVars.containsKey( param ) ) {
            scheduleKettleVars.put( param, parameterMap.get( param ).toString() );
          }
        }
      }

      convertedParameterMap.put( "directory", FilenameUtils.getPathNoEndSeparator( file.getPath() ) );
      String type = isTransformation( file ) ? "transformation" : "job";
      convertedParameterMap.put( type, FilenameUtils.getBaseName( file.getPath() ) );

    } else {
      convertedParameterMap.putAll( parameterMap );
    }
    convertedParameterMap.putIfAbsent( ScheduleExportUtil.RUN_PARAMETERS_KEY, (Serializable) pdiParameters );
    convertedParameterMap.putIfAbsent( "variables", (Serializable) scheduleKettleVars );
    return convertedParameterMap;
  }

  public static IPdiContentProvider getiPdiContentProvider() throws PluginBeanException {
    IPdiContentProvider provider;
    provider = (IPdiContentProvider) PentahoSystem.get( IPluginManager.class ).getBean(
      IPdiContentProvider.class.getSimpleName() );
    return provider;
  }

  public static boolean isPdiFile( RepositoryFile file ) {
    return isTransformation( file ) || isJob( file );
  }

  public static boolean isTransformation( RepositoryFile file ) {
    return file != null && "ktr".equalsIgnoreCase( FilenameUtils.getExtension( file.getName() ) );
  }

  public static boolean isJob( RepositoryFile file ) {
    return file != null && "kjb".equalsIgnoreCase( FilenameUtils.getExtension( file.getName() ) );
  }

  public static String resolveActionId( final String inputFile ) {
    // unchanged logic, ported over from its original location ( SchedulerService ) into this SchedulerUtil class
    if ( !StringUtils.isEmpty( inputFile ) && !StringUtils.isEmpty( getExtension( inputFile ) ) ) {
      return getExtension( inputFile ) + RESERVED_BACKGROUND_EXECUTION_ACTION_ID;
    }
    return null;
  }

  public static String getExtension( final String filename ) {
    // unchanged logic, ported over from its original location ( SchedulerService ) into this SchedulerUtil class
    return RepositoryFilenameUtils.getExtension( filename );
  }
  private static String generateCronString(long interval, Date startDate) {
    Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
    calendar.setTime(startDate);
    int hour = calendar.get(Calendar.HOUR_OF_DAY);
    int minute = calendar.get(Calendar.MINUTE);

    Cron cron = CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ))
            .withYear(always())
            .withDoM(every((int)interval))
            .withMonth(always())
            .withDoW(questionMark())
            .withHour(on(hour))
            .withMinute(on(minute))
            .withSecond(on(0)).instance();
    return cron.asString();
  }
}
