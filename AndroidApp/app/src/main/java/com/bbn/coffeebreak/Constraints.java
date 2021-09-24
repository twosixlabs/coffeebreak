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

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "begin",
        "end"
})
public class Constraints {

    /**
     * time, in encoding
     *
     */
    @JsonProperty("begin")
    @JsonPropertyDescription("time, in encoding")
    private String begin;
    /**
     * time, in encoding
     *
     */
    @JsonProperty("end")
    @JsonPropertyDescription("time, in encoding")
    private String end;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * time, in encoding
     *
     */
    @JsonProperty("begin")
    public String getBegin() {
        return begin;
    }

    /**
     * time, in encoding
     *
     */
    @JsonProperty("begin")
    public void setBegin(String begin) {
        this.begin = begin;
    }

    public Constraints withBegin(String begin) {
        this.begin = begin;
        return this;
    }

    /**
     * time, in encoding
     *
     */
    @JsonProperty("end")
    public String getEnd() {
        return end;
    }

    /**
     * time, in encoding
     *
     */
    @JsonProperty("end")
    public void setEnd(String end) {
        this.end = end;
    }

    public Constraints withEnd(String end) {
        this.end = end;
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

    public Constraints withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("begin", begin).append("end", end).append("additionalProperties", additionalProperties).toString();
    }

}