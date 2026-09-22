///*
// * Copyright (c) 2026, RTE (http://www.rte-france.com)
// * This Source Code Form is subject to the terms of the Mozilla Public
// * License, v. 2.0. If a copy of the MPL was not distributed with this
// * file, You can obtain one at http://mozilla.org/MPL/2.0/.
// */
//package com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor_logs;
//
//import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
//import com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor_logs.model.MergingSupervisorLogsModel;
//import com.farao_community.farao.ce_merging.xsd.execution_logs.Logs;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.context.TestConfiguration;
//
//import java.io.IOException;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
///**
// * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
// * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
// */
//@SpringBootTest
//class ExecutionLogsServiceTest {
//
//    private String singleJsonRecord;
//    private final Path resourceDirectory = Paths.get("src", "test", "resources", "execution_logs");
//    private final String absolutePath = resourceDirectory.toFile().getAbsolutePath();
//
//    @Autowired
//    ExecutionLogsService executionLogsService;
//
//    @TestConfiguration
//    static class TestContextConfiguration {
//
//    }
//
//    @BeforeEach
//    void setUp() {
//        singleJsonRecord = """
//                {
//                  "agent": {
//                    "id": "fd5584e2-1c48-4707-9356-b257a7eb15ba",
//                    "version": "7.4.0",
//                    "type": "filebeat",
//                    "ephemeral_id": "3bebd8b9-ab79-4084-96ad-c56b699fc551",
//                    "hostname": "084dff5e0dbd"
//                  },
//                  "input": {
//                    "type": "container"
//                  },
//                  "application_name": "moonlight-case-store-server",
//                  "spanExportable": "false",
//                  "docker": {
//                    "container": {
//                      "labels": {
//                        "com_docker_compose_oneoff": "False",
//                        "decode_log_event_to_json_object": "true",
//                        "com_docker_compose_project": "core-merging-platform",
//                        "com_docker_compose_config-hash": "cbb994a5a801bb6d90d5a80058e8bdc520963f9e295f50892abb17de7cc8e048",
//                        "com_docker_compose_version": "1.22.0",
//                        "com_docker_compose_service": "moonlight-case-store-server",
//                        "collect_logs_with_filebeat": "true",
//                        "com_docker_compose_container-number": "1"
//                      }
//                    }
//                  },
//                  "tags": [
//                    "beats_input_codec_plain_applied",
//                    "logstash_filter_applied"
//                  ],
//                  "spanId": "200db1fbb1300304",
//                  "level": "WARN",
//                  "message": "Node XGK_DE11: maximum active power is undefined, set value to -9999.0",
//                  "host": {
//                    "name": "084dff5e0dbd"
//                  },
//                  "thread_name": "http-nio-8080-exec-4",
//                  "container": {
//                    "image": {
//                      "name": "inca.rte-france.com/gridcapa/moonlight-case-store-server:0.0.1-SNAPSHOT"
//                    },
//                    "id": "b30a449dc8b83d8203b2c72d33aefe3197c36c0da632fb8a0845e6ac3ecac7e2",
//                    "name": "core-merging-platform_case-store_1"
//                  },
//                  "X-B3-SpanId": "200db1fbb1300304",
//                  "X-B3-ParentSpanId": "9864d9ec8b84dc1a",
//                  "@timestamp": "2020-05-19T10:28:33.901Z",
//                  "X-Span-Export": "false",
//                  "X-B3-TraceId": "9864d9ec8b84dc1a",
//                  "parentId": "9864d9ec8b84dc1a",
//                  "traceId": "9864d9ec8b84dc1a",
//                  "@version": "1",
//                  "stream": "stdout",
//                  "log": {
//                    "file": {
//                      "path": "/var/lib/docker/containers/b30a449dc8b83d8203b2c72d33aefe3197c36c0da632fb8a0845e6ac3ecac7e2/b30a449dc8b83d8203b2c72d33aefe3197c36c0da632fb8a0845e6ac3ecac7e2-json.log"
//                    },
//                    "offset": 53588
//                  },
//                  "logger_name": "com.powsybl.ucte.network.UcteNode",
//                  "ecs": {
//                    "version": "1.1.0"
//                  },
//                  "level_value": 30000
//                }""";
//    }
//
//    @Test
//    public void testLogsModelConversion() {
//        List<String> jsonRecordsList = new ArrayList<>();
//        List<MergingSupervisorLogsModel> mergingSupervisorLogsModelList = new ArrayList<>();
//        jsonRecordsList.add(singleJsonRecord);
//        jsonRecordsList.forEach(json -> {
//            try {
//                MergingSupervisorLogsModel elkLogRecord = executionLogsService.convertLogsModel(json);
//                mergingSupervisorLogsModelList.add(elkLogRecord);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
//        assertEquals(1, mergingSupervisorLogsModelList.size());
//        assertEquals("WARN", mergingSupervisorLogsModelList.get(0).getLevel());
//        assertEquals("19/05/2020 10:28:33", mergingSupervisorLogsModelList.get(0).getTimestamp());
//        assertEquals("Node XGK_DE11: maximum active power is undefined, set value to -9999.0", mergingSupervisorLogsModelList.get(0).getMessage());
//    }
//
//    @Test
//    public void loadflowLogsUnmarshallingTest() {
//        Logs logs = JaxbUtils.readFromPath(Logs.class, absolutePath.concat("/open_loadflow_logs.xml"));
//        assertEquals(1, logs.getCtxt().size());
//        assertEquals("OpenLoadFlow logs", logs.getCtxt().get(0).getNom());
//        assertEquals(1, logs.getCtxt().get(0).getRecOrCtxt().size());
//    }
//
//    @Test
//    public void timeStampFormatConversionTest() {
//        String timestamp = "2020-05-19T10:28:33.901Z";
//        assertEquals("19/05/2020 10:28:33", executionLogsService.getMergingSupervisorTimestamp(timestamp));
//    }
//
//}
//
