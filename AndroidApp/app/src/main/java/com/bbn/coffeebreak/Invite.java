/*
 * Copyright 2021 Raytheon BBN Technologies Corp.
 * Copyright 2021 Two Six Labs, LLC DBA Two Six Technologies
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * invite
 * <p>
 * An invitation to a meeting.
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "meetingID",
        "title",
        "organizer",
        "location",
        "notes",
        "duration",
        "granularity",
        "attendees",
        "constraints",
        "technology"
})
public class Invite {

    /**
     * UUID
     * (Required)
     *
     */
    @JsonProperty("meetingID")
    @JsonPropertyDescription("UUID")
    private String meetingID;
    /**
     * Name of the event.
     *
     */
    @JsonProperty("title")
    @JsonPropertyDescription("Name of the event.")
    private String title;
    /**
     * email address
     * (Required)
     *
     */
    @JsonProperty("organizer")
    @JsonPropertyDescription("email address")
    private String organizer;
    /**
     * Location of the event.
     *
     */
    @JsonProperty("location")
    @JsonPropertyDescription("Location of the event.")
    private String location;
    /**
     * Notes about the event.
     *
     */
    @JsonProperty("notes")
    @JsonPropertyDescription("Notes about the event.")
    private String notes;
    /**
     * duration, in encoding
     * (Required)
     *
     */
    @JsonProperty("duration")
    @JsonPropertyDescription("duration, in encoding")
    private String duration;
    /**
     * duration, in encoding
     * (Required)
     *
     */
    @JsonProperty("granularity")
    @JsonPropertyDescription("duration, in encoding")
    private String granularity;
    /**
     * List of people invited to the event.
     * (Required)
     *
     */
    @JsonProperty("attendees")
    @JsonDeserialize(as = java.util.LinkedHashSet.class)
    @JsonPropertyDescription("List of people invited to the event.")
    private Set<String> attendees = null;
    /**
     *
     * (Required)
     *
     */
    @JsonProperty("constraints")
    private Constraints constraints;
    /**
     * The technology to be used.
     * (Required)
     *
     */
    @JsonProperty("technology")
    @JsonPropertyDescription("The technology to be used.")
    private String technology;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * UUID
     * (Required)
     *
     */
    @JsonProperty("meetingID")
    public String getMeetingID() {
        return meetingID;
    }

    /**
     * UUID
     * (Required)
     *
     */
    @JsonProperty("meetingID")
    public void setMeetingID(String meetingID) {
        this.meetingID = meetingID;
    }

    public Invite withMeetingID(String meetingID) {
        this.meetingID = meetingID;
        return this;
    }

    /**
     * Name of the event.
     *
     */
    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    /**
     * Name of the event.
     *
     */
    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }

    public Invite withTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * email address
     * (Required)
     *
     */
    @JsonProperty("organizer")
    public String getOrganizer() {
        return organizer;
    }

    /**
     * email address
     * (Required)
     *
     */
    @JsonProperty("organizer")
    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public Invite withOrganizer(String organizer) {
        this.organizer = organizer;
        return this;
    }

    /**
     * Location of the event.
     *
     */
    @JsonProperty("location")
    public String getLocation() {
        return location;
    }

    /**
     * Location of the event.
     *
     */
    @JsonProperty("location")
    public void setLocation(String location) {
        this.location = location;
    }

    public Invite withLocation(String location) {
        this.location = location;
        return this;
    }

    /**
     * Notes about the event.
     *
     */
    @JsonProperty("notes")
    public String getNotes() {
        return notes;
    }

    /**
     * Notes about the event.
     *
     */
    @JsonProperty("notes")
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Invite withNotes(String notes) {
        this.notes = notes;
        return this;
    }

    /**
     * duration, in encoding
     * (Required)
     *
     */
    @JsonProperty("duration")
    public String getDuration() {
        return duration;
    }

    /**
     * duration, in encoding
     * (Required)
     *
     */
    @JsonProperty("duration")
    public void setDuration(String duration) {
        this.duration = duration;
    }

    public Invite withDuration(String duration) {
        this.duration = duration;
        return this;
    }

    /**
     * duration, in encoding
     * (Required)
     *
     */
    @JsonProperty("granularity")
    public String getGranularity() {
        return granularity;
    }

    /**
     * duration, in encoding
     * (Required)
     *
     */
    @JsonProperty("granularity")
    public void setGranularity(String granularity) {
        this.granularity = granularity;
    }

    public Invite withGranularity(String granularity) {
        this.granularity = granularity;
        return this;
    }

    /**
     * List of people invited to the event.
     * (Required)
     *
     */
    @JsonProperty("attendees")
    public Set<String> getAttendees() {
        return attendees;
    }

    /**
     * List of people invited to the event.
     * (Required)
     *
     */
    @JsonProperty("attendees")
    public void setAttendees(Set<String> attendees) {
        this.attendees = attendees;
    }

    public Invite withAttendees(Set<String> attendees) {
        this.attendees = attendees;
        return this;
    }

    /**
     *
     * (Required)
     *
     */
    @JsonProperty("constraints")
    public Constraints getConstraints() {
        return constraints;
    }

    /**
     *
     * (Required)
     *
     */
    @JsonProperty("constraints")
    public void setConstraints(Constraints constraints) {
        this.constraints = constraints;
    }

    public Invite withConstraints(Constraints constraints) {
        this.constraints = constraints;
        return this;
    }

    /**
     * The technology to be used.
     * (Required)
     *
     */
    @JsonProperty("technology")
    public String getTechnology() {
        return technology;
    }

    /**
     * The technology to be used.
     * (Required)
     *
     */
    @JsonProperty("technology")
    public void setTechnology(String technology) {
        this.technology = technology;
    }

    public Invite withTechnology(String technology) {
        this.technology = technology;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public Invite withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("meetingID", meetingID).append("title", title).append("organizer", organizer).append("location", location).append("notes", notes).append("duration", duration).append("granularity", granularity).append("attendees", attendees).append("constraints", constraints).append("technology", technology).append("additionalProperties", additionalProperties).toString();
    }

}