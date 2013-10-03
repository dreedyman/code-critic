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

import java.util.ArrayList;
import java.util.List;

/**
 * Identifies attributes of a source file.
 *
 * @author Dennis Reedy
 */
public class SourceFile implements Comparable<SourceFile> {
    private final String file;
    private final List<ChangeSet> changeSets = new ArrayList<ChangeSet>();

    public SourceFile(String file) {
        if(file==null)
            throw new IllegalArgumentException("file cannot be null");
        this.file = file;
    }

    public void addChangeSet(ChangeSet changeSet) {
        changeSets.add(changeSet);
    }

    public String getFile() {
        return file;
    }

    public List<ChangeSet> getChangeSets() {
        return changeSets;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        SourceFile that = (SourceFile) o;
        return !(file != null ? !file.equals(that.file) : that.file != null);
    }

    @Override
    public int hashCode() {
        return file != null ? file.hashCode() : 0;
    }

    public int compareTo(SourceFile sourceFile) {
        return file.compareTo(sourceFile.getFile());
    }

    @Override
    public String toString() {
        return file;
    }
}
