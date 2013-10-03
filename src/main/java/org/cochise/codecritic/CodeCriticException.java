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
 * Indicates an error when working with the code-critic.
 *
 * @author Dennis Reedy
 */
public class CodeCriticException extends Exception {

    /**
     * Constructs a CodeCriticException with the specified
     * detail message and optional exception that was raised
     *
     * @param s     the detail message
     * @param cause the exception that was raised
     */
    public CodeCriticException(String s, Throwable cause) {
        super(s, cause);
    }

    /**
     * Constructs an CodeCriticException with the specified detail
     * message.
     *
     * @param reason The reason for the exception
     */
    public CodeCriticException(String reason) {
        super(reason);
    }

    /**
     * Constructs an CodeCriticException with no detail message.
     */
    public CodeCriticException() {
        super();
    }
}
