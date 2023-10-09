/*
 * Copyright 2023 Roberto Leinardi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leinardi.pycharm.pylint.plapi;

import com.squareup.moshi.Json;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Objects;

/**
 * An issue as reported by the Pylint tool.
 */
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
        return "Issue{" +
                "severityLevel=" + severityLevel +
                ", module='" + module + '\'' +
                ", obj='" + obj + '\'' +
                ", line=" + line +
                ", column=" + column +
                ", path='" + path + '\'' +
                ", symbol='" + symbol + '\'' +
                ", message='" + message + '\'' +
                ", messageId='" + messageId + '\'' +
                '}';
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Issue)) {
            return false;
        }
        Issue issue = (Issue) o;
        return line == issue.line &&
                column == issue.column &&
                severityLevel == issue.severityLevel &&
                Objects.equals(module, issue.module) &&
                Objects.equals(obj, issue.obj) &&
                Objects.equals(path, issue.path) &&
                Objects.equals(symbol, issue.symbol) &&
                Objects.equals(message, issue.message) &&
                Objects.equals(messageId, issue.messageId);
    }
}
