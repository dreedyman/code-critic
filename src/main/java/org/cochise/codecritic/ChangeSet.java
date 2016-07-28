/*
 * Copyright to the original author or authors.
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
package org.cochise.codecritic;

/**
 * Hold properties for a mercurial change set
 *
 * @author Dennis Reedy
 */
public class ChangeSet implements Comparable<ChangeSet> {
    private final int number;
    private String link;
    private final String changeSet;
    private String message;
    private String developer;
    private String email;
    private String date;
    private String diff;
    private boolean merge;

    public ChangeSet(final int number, final String link, final String changeSet) {
        this.number = number;
        this.changeSet = changeSet;
        this.link = link+changeSet;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public int getNumber() {
        return number;
    }

    public String getLink() {
        return link;
    }

    public void setMerge() {
        merge = true;
    }

    boolean isMerge() {
        return merge;
    }

    void setLink(String link) {
        this.link = link;
    }

    public String getChangeSet() {
        return changeSet;
    }

    public String getMessage() {
        return message==null?"":message;
    }

    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = developer;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int compareTo(final ChangeSet c) {
        return number==c.number?0:(number>c.number?1:-1);
    }

    public String getDiff() {
        return diff;
    }

    public void setDiff(String diff) {
        this.diff = diff;
    }

    @Override
    public String toString() {
        return number+":"+changeSet;
    }
}
