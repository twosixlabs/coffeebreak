/*
 * Copyright 2021 Raytheon BBN Technologies Corp.
 * Copyright 2021 Two Six Technologies
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.bbn.coffeebreak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ICalParse {

    private final static String SUMMARY = "SUMMARY:";
    private final static String LOCATION = "LOCATION:";
    private final static String DESCRIPTION = "DESCRIPTION:";
    private final static String DTEND = "DTEND;VALUE=DATE-TIME:";
    private final static String DTSTART = "DTSTART;VALUE=DATE-TIME:";
    private final static String DTSTAMP = "DTSTAMP;VALUE=DATE-TIME:";
    private final static String UID = "UID:";
    private final static String SEQUENCE = "SEQUENCE:";
    private final static String LAST_MODIFIED = "LAST-MODIFIED:";
    private final static String TRANSP = "TRANSP:";
    private final static String RRULE = "RRULE:";
    private final static String RECURRENCE = "RECURRENCE:";
    private final static String CREATED = "CREATED:";
    private final static String ORGANIZER = "ORGANIZER:";
    private final static String EXDATE = "EXDATE:";
    private final static String AVAILABILITY = "AVAILABILITY:";

    private String summary = "";
    private String location = "";
    private String description = "";
    private String dtend = "";
    private String dtstart = "";
    private String dtstamp = "";
    private String uid = "";
    private String sequence = "";
    private String lastModified = "";
    private String transp = "";
    private String rrule = "";
    private String recurrence = "";
    private String created = "";
    private String organizer = "";
    private String exdate = "";
    private String availability = "";


    List<String> fields = new ArrayList<>();

    public ICalParse(String iCalendar){

        fields = Arrays.asList(iCalendar.split("\n"));

        for(String field : fields){

            String key = field.substring(0, field.indexOf(':') + 1);

            switch(key){
                case SUMMARY:
                    summary = field.substring(field.indexOf(':') + 1);
                    break;
                case LOCATION:
                    location = field.substring(field.indexOf(':') + 1);
                    break;
                case DTEND:
                    dtend = field.substring(field.indexOf(':') + 1);
                    break;
                case DESCRIPTION:
                    description = field.substring(field.indexOf(':') + 1);
                    break;
                case DTSTART:
                    dtstart = field.substring(field.indexOf(':') + 1);
                    break;
                case DTSTAMP:
                    dtstamp = field.substring(field.indexOf(':') + 1);;
                    break;
                case UID:
                    uid = field.substring(field.indexOf(':') + 1);
                    break;
                case SEQUENCE:
                    sequence = field.substring(field.indexOf(':') + 1);
                    break;
                case LAST_MODIFIED:
                    lastModified = field.substring(field.indexOf(':') + 1);
                    break;
                case RECURRENCE:
                    recurrence = field.substring(field.indexOf(':') + 1);
                    break;
                case TRANSP:
                    transp = field.substring(field.indexOf(':') + 1);
                    break;
                case RRULE:
                    rrule = field.substring(field.indexOf(':') + 1);
                    break;
                case CREATED:
                    created = field.substring(field.indexOf(':') + 1);
                    break;
                case ORGANIZER:
                    organizer = field.substring(field.indexOf(':') + 1);
                    break;
                case EXDATE:
                    exdate = field.substring(field.indexOf(':') + 1);
                    break;
                case AVAILABILITY:
                    availability = field.substring(field.indexOf(':') + 1);
                    break;
            }
        }
    }


    public String getSummary() {
        return summary;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public String getDtend() {
        return dtend;
    }

    public String getDtstart() {
        return dtstart;
    }

    public String getDtstamp() {
        return dtstamp;
    }

    public String getUid() {
        return uid;
    }

    public String getSequence() {
        return sequence;
    }

    public String getLastModified() {
        return lastModified;
    }

    public String getTransp() {
        return transp;
    }

    public String getRrule() {
        return rrule;
    }

    public String getRecurrence() {
        return recurrence;
    }

    public String getCreated() {
        return created;
    }

    public String getOrganizer() {
        return organizer;
    }

    public String getExdate() {
        return exdate;
    }

    public String getAvailability(){
        return availability;
    }

}
