/*
 * Copyright (C) 2020-2023 Hedera Hashgraph, LLC
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

package com.hedera.mirror.importer.parser.performance;

import static org.assertj.core.api.Assertions.assertThat;

import com.hedera.mirror.common.domain.transaction.RecordFile;
import com.hedera.mirror.importer.db.DBProperties;
import com.hedera.mirror.importer.domain.StreamFileData;
import com.hedera.mirror.importer.parser.record.RecordFileParser;
import com.hedera.mirror.importer.reader.record.RecordFileReader;
import com.hedera.mirror.importer.repository.AccountBalanceRepository;
import com.hedera.mirror.importer.repository.EntityRepository;
import com.hedera.mirror.importer.repository.RecordFileRepository;
import com.hedera.mirror.importer.repository.TopicMessageRepository;
import com.hedera.mirror.importer.repository.TransactionRepository;
import java.sql.SQLException;
import java.util.function.Consumer;
import javax.sql.DataSource;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.CrudRepository;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.startupcheck.IndefiniteWaitOneShotStartupCheckStrategy;

@CustomLog
@SpringBootTest
public abstract class PerformanceIntegrationTest {

    @Value("classpath:data/recordstreams/performance/v2")
    Resource[] testFiles;

    @Autowired
    private RecordFileReader recordFileReader;

    @Autowired
    private RecordFileParser recordFileParser;

    @Autowired
    DataSource dataSource;

    @Autowired
    private DBProperties dbProperties;

    @Autowired
    private RecordFileRepository recordFileRepository;

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private AccountBalanceRepository accountBalanceRepository;

    @Autowired
    private TopicMessageRepository topicMessageRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private static final String restoreClientImagePrefix =
            "gcr.io/mirrornode/hedera-mirror-node/postgres-restore" + "-client:";

    protected GenericContainer<?> createRestoreContainer(String dockerImageTag) {
        log.debug("Creating restore container to connect to {}", dbProperties);
        Consumer<OutputFrame> logConsumer = o -> log.info("Restore container: {}", o::getUtf8String);
        GenericContainer<?> container = new GenericContainer<>(restoreClientImagePrefix + dockerImageTag)
                .withEnv("DB_NAME", dbProperties.getName())
                .withEnv("DB_USER", dbProperties.getUsername())
                .withEnv("DB_PASS", dbProperties.getPassword())
                .withEnv("DB_PORT", Integer.toString(dbProperties.getPort()))
                .withLogConsumer(logConsumer)
                .withNetworkMode("host")
                .withStartupCheckStrategy(new IndefiniteWaitOneShotStartupCheckStrategy());
        return container;
    }

    void parse() throws Exception {
        for (Resource resource : testFiles) {
            RecordFile recordFile = recordFileReader.read(StreamFileData.from(resource.getFile()));
            recordFileParser.parse(recordFile);
        }
    }

    void checkSeededTablesArePresent() throws SQLException {
        verifyTableSize(entityRepository, "entity");
        verifyTableSize(accountBalanceRepository, "account_balance");
        verifyTableSize(topicMessageRepository, "topicmessages");
        verifyTableSize(transactionRepository, "transaction");
    }

    void verifyTableSize(CrudRepository<?, ?> repository, String label) {
        long count = repository.count();
        log.info("Table {} was populated with {} rows", label, count);
        assertThat(count).isPositive();
    }

    void clearLastProcessedRecordHash() {
        recordFileRepository.findLatest().ifPresent(r -> {
            r.setHash("");
            recordFileRepository.save(r);
        });
    }
}
