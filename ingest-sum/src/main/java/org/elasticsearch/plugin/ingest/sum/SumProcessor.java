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
import org.elasticsearch.ingest.core.AbstractProcessor;
import org.elasticsearch.ingest.core.AbstractProcessorFactory;
import org.elasticsearch.ingest.core.IngestDocument;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.ingest.core.ConfigurationUtils.readOptionalList;
import static org.elasticsearch.ingest.core.ConfigurationUtils.readStringProperty;

public class SumProcessor extends AbstractProcessor {

    public static final String TYPE = "sum";

    private final String field;
    private final List<String> fields;

    public SumProcessor(String tag, String field, List<String> fields) throws IOException {
        super(tag);
        this.field = field;
        this.fields = fields;
    }

    @Override
    public void execute(IngestDocument ingestDocument) throws Exception {
        long count = 0;
        for (String fieldName : fields) {
            try {
                count += ingestDocument.getFieldValue(fieldName, Number.class).longValue();
            } catch (IllegalArgumentException e) {
                throw new ElasticsearchException("[{}] is not a number", fieldName);
            }
        }
        ingestDocument.setFieldValue(field, count);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public static final class Factory extends AbstractProcessorFactory<SumProcessor> {

        @Override
        public SumProcessor doCreate(String processorTag, Map<String, Object> config) throws Exception {
            String field = readStringProperty(TYPE, processorTag, config, "field");
            List<String> fields = readOptionalList(TYPE, processorTag, config, "fields");

            return new SumProcessor(processorTag, field, fields);
        }
    }
}
