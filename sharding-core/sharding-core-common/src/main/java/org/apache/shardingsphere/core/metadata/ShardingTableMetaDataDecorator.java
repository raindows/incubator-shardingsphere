/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.core.metadata;

import org.apache.shardingsphere.core.rule.DataNode;
import org.apache.shardingsphere.core.rule.ShardingRule;
import org.apache.shardingsphere.core.rule.TableRule;
import org.apache.shardingsphere.sql.parser.binder.metadata.column.ColumnMetaData;
import org.apache.shardingsphere.sql.parser.binder.metadata.index.IndexMetaData;
import org.apache.shardingsphere.sql.parser.binder.metadata.table.TableMetaData;
import org.apache.shardingsphere.underlying.common.metadata.decorator.TableMetaDataDecorator;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Table meta data decorator for sharding.
 */
public final class ShardingTableMetaDataDecorator implements TableMetaDataDecorator<ShardingRule> {
    
    @Override
    public TableMetaData decorate(final TableMetaData tableMetaData, final String tableName, final ShardingRule shardingRule) {
        return shardingRule.findTableRule(tableName).map(
            tableRule -> new TableMetaData(getColumnMetaDataList(tableMetaData, tableRule), getIndexMetaDataList(tableMetaData, tableRule))).orElse(tableMetaData);
    }
    
    private Collection<ColumnMetaData> getColumnMetaDataList(final TableMetaData tableMetaData, final TableRule tableRule) {
        Optional<String> generateKeyColumn = tableRule.getGenerateKeyColumn();
        if (!generateKeyColumn.isPresent()) {
            return tableMetaData.getColumns().values();
        }
        Collection<ColumnMetaData> result = new LinkedList<>();
        for (Entry<String, ColumnMetaData> entry : tableMetaData.getColumns().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(generateKeyColumn.get())) {
                result.add(new ColumnMetaData(entry.getValue().getName(), entry.getValue().getDataType(), entry.getValue().isPrimaryKey(), false, true));
            } else {
                result.add(entry.getValue());
            }
        }
        return result;
    }
    
    private Collection<IndexMetaData> getIndexMetaDataList(final TableMetaData tableMetaData, final TableRule tableRule) {
        Collection<IndexMetaData> result = new HashSet<>();
        for (Entry<String, IndexMetaData> entry : tableMetaData.getIndexes().entrySet()) {
            for (DataNode each : tableRule.getActualDataNodes()) {
                getLogicIndex(entry.getKey(), each.getTableName()).ifPresent(logicIndex -> result.add(new IndexMetaData(logicIndex)));
            }
        }
        return result;
    }
    
    private Optional<String> getLogicIndex(final String actualIndexName, final String actualTableName) {
        String indexNameSuffix = "_" + actualTableName;
        return actualIndexName.endsWith(indexNameSuffix) ? Optional.of(actualIndexName.replace(indexNameSuffix, "")) : Optional.empty();
    }
}
