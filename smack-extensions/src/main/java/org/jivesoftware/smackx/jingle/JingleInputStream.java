/**
 *
 * Copyright 2017 Paul Schaub
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.jingle;

import java.io.InputStream;

/**
 * Created by vanitas on 06.06.17.
 */
public class JingleInputStream {
    private final InputStream inputStream;
    private final int blockSize;

    public JingleInputStream(InputStream inputStream, int blockSize) {
        this.inputStream = inputStream;
        this.blockSize = blockSize;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public int getBlockSize() {
        return blockSize;
    }
}
