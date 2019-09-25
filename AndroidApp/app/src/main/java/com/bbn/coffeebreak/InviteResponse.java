package com.bbn.coffeebreak;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "meetingID",
        "response"
})
public class InviteResponse implements Serializable, Parcelable
{

    @JsonProperty("meetingID")
    private String meetingID;
    @JsonProperty("response")
    private Integer response;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    public final static Parcelable.Creator<InviteResponse> CREATOR = new Creator<InviteResponse>() {


        @SuppressWarnings({
                "unchecked"
        })
        public InviteResponse createFromParcel(Parcel in) {
            return new InviteResponse(in);
        }

        public InviteResponse[] newArray(int size) {
            return (new InviteResponse[size]);
        }

    }
            ;
    private final static long serialVersionUID = 8316594793073705147L;

    protected InviteResponse(Parcel in) {
        this.meetingID = ((String) in.readValue((String.class.getClassLoader())));
        this.response = ((Integer) in.readValue((Integer.class.getClassLoader())));
        this.additionalProperties = ((Map<String, Object> ) in.readValue((Map.class.getClassLoader())));
    }

    /**
     * No args constructor for use in serialization
     *
     */
    public InviteResponse() {
    }

    /**
     *
     * @param response
     * @param meetingID
     */
    public InviteResponse(String meetingID, Integer response) {
        super();
        this.meetingID = meetingID;
        this.response = response;
    }

    @JsonProperty("meetingID")
    public String getMeetingID() {
        return meetingID;
    }

    @JsonProperty("meetingID")
    public void setMeetingID(String meetingID) {
        this.meetingID = meetingID;
    }

    public InviteResponse withMeetingID(String meetingID) {
        this.meetingID = meetingID;
        return this;
    }

    @JsonProperty("response")
    public Integer getResponse() {
        return response;
    }

    @JsonProperty("response")
    public void setResponse(Integer response) {
        this.response = response;
    }

    public InviteResponse withResponse(Integer response) {
        this.response = response;
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

    public InviteResponse withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("meetingID", meetingID).append("response", response).append("additionalProperties", additionalProperties).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(response).append(additionalProperties).append(meetingID).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof InviteResponse) == false) {
            return false;
        }
        InviteResponse rhs = ((InviteResponse) other);
        return new EqualsBuilder().append(response, rhs.response).append(additionalProperties, rhs.additionalProperties).append(meetingID, rhs.meetingID).isEquals();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(meetingID);
        dest.writeValue(response);
        dest.writeValue(additionalProperties);
    }

    public int describeContents() {
        return 0;
    }

}