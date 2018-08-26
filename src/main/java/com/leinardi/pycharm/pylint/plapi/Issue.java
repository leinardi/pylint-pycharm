/*
 * Copyright 2018 Roberto Leinardi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leinardi.pycharm.pylint.plapi;

/**
 * An issue as reported by the Pylint tool.
 */

import com.squareup.moshi.Json;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class Issue {

    @Json(name = "type")
    private SeverityLevel severityLevel;
    @Json(name = "module")
    private String module;
    @Json(name = "obj")
    private String obj;
    @Json(name = "line")
    private int line;
    @Json(name = "column")
    private int column;
    @Json(name = "path")
    private String path;
    @Json(name = "symbol")
    private String symbol;
    @Json(name = "message")
    private String message;
    @Json(name = "message-id")
    private String messageId;

    public SeverityLevel getSeverityLevel() {
        return severityLevel;
    }

    public void setSeverityLevel(SeverityLevel severityLevel) {
        this.severityLevel = severityLevel;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getObj() {
        return obj;
    }

    public void setObj(String obj) {
        this.obj = obj;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("type", severityLevel)
                .append("module", module)
                .append("obj", obj)
                .append("line", line)
                .append("column", column)
                .append("path", path)
                .append("symbol", symbol)
                .append("message", message)
                .append("messageId", messageId).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(message)
                .append(module)
                .append(symbol)
                .append(path)
                .append(column)
                .append(line)
                .append(obj)
                .append(messageId)
                .append(severityLevel)
                .toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Issue)) {
            return false;
        }
        Issue rhs = ((Issue) other);
        return new EqualsBuilder()
                .append(message, rhs.message)
                .append(module, rhs.module)
                .append(symbol, rhs.symbol)
                .append(path, rhs.path)
                .append(column, rhs.column)
                .append(line, rhs.line)
                .append(obj, rhs.obj)
                .append(messageId, rhs.messageId)
                .append(severityLevel, rhs.severityLevel)
                .isEquals();
    }

}
