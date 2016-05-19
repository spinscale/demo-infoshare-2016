/*
 * Copyright [2016] [Alexander Reelsen]
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
 *
 */

package org.elasticsearch.plugin.ingest.sum;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ingest.RandomDocumentPicks;
import org.elasticsearch.ingest.core.IngestDocument;
import org.elasticsearch.test.ESTestCase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

public class SumProcessorTests extends ESTestCase {

    public void testThatProcessorWorks() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("bytes_in", 1234);
        document.put("bytes_out", 4321);
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);

        SumProcessor processor = new SumProcessor(randomAsciiOfLength(10), "bytes_total", Arrays.asList("bytes_in", "bytes_out"));
        processor.execute(ingestDocument);
        Map<String, Object> data = ingestDocument.getSourceAndMetadata();

        assertThat(data, hasKey("bytes_total"));
        assertThat(data.get("bytes_total"), is(5555L));
    }

    public void testThatStringsReturnExceptions() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("bytes_out", "foo");
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);

        SumProcessor processor = new SumProcessor(randomAsciiOfLength(10), "bytes_total", Arrays.asList("bytes_in", "bytes_out"));
        ElasticsearchException exception = expectThrows(ElasticsearchException.class, () -> processor.execute(ingestDocument));
        assertThat(exception.getMessage(), containsString("[bytes_in] is not a number"));
    }
}

