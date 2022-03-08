/*
 *    Copyright (c) 2021, VRAI Labs and/or its affiliates. All rights reserved.
 *
 *    This software is licensed under the Apache License, Version 2.0 (the
 *    "License") as published by the Apache Software Foundation.
 *
 *    You may not use this file except in compliance with the License. You may
 *    obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 */

package io.supertokens.storage.sql.queries;

import io.supertokens.pluginInterface.RowMapper;
import io.supertokens.pluginInterface.exceptions.StorageQueryException;
import io.supertokens.pluginInterface.jwt.JWTAsymmetricSigningKeyInfo;
import io.supertokens.pluginInterface.jwt.JWTSigningKeyInfo;
import io.supertokens.pluginInterface.jwt.JWTSymmetricSigningKeyInfo;
import io.supertokens.storage.sql.Start;
import io.supertokens.storage.sql.config.Config;
import io.supertokens.storage.sql.utils.Utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static io.supertokens.storage.sql.PreparedStatementValueSetter.NO_OP_SETTER;
import static io.supertokens.storage.sql.QueryExecutorTemplate.execute;
import static io.supertokens.storage.sql.QueryExecutorTemplate.update;
import static io.supertokens.storage.sql.config.Config.getConfig;

public class JWTSigningQueries {
    static String getQueryToCreateJWTSigningTable(Start start) {
        /*
         * created_at should only be used to determine the key that was added to the database last, it should not be
         * used to determine the validity or lifetime of a key. While the assumption that created_at refers to the time
         * the key was generated holds true for keys generated by the core, it is not guaranteed when we allow user
         * defined
         * keys in the future.
         */
        String schema = Config.getConfig(start).getTableSchema();
        String jwtSigningKeysTable = Config.getConfig(start).getJWTSigningKeysTable();
        // @formatter:off
        return "CREATE TABLE IF NOT EXISTS " + jwtSigningKeysTable + " ("
                + "key_id VARCHAR(255) NOT NULL,"
                + "key_string TEXT NOT NULL,"
                + "algorithm VARCHAR(10) NOT NULL,"
                + "created_at BIGINT,"
                + "CONSTRAINT " + Utils.getConstraintName(schema, jwtSigningKeysTable, null, "pkey") +
                " PRIMARY KEY(key_id));";
        // @formatter:on
    }

    public static List<JWTSigningKeyInfo> getJWTSigningKeys_Transaction(Start start, Connection con)
            throws SQLException, StorageQueryException {
        String QUERY = "SELECT * FROM " + getConfig(start).getJWTSigningKeysTable()
                + " ORDER BY created_at DESC FOR UPDATE";

        return execute(con, QUERY, NO_OP_SETTER, result -> {
            List<JWTSigningKeyInfo> keys = new ArrayList<>();

            while (result.next()) {
                keys.add(JWTSigningKeyInfoRowMapper.getInstance().mapOrThrow(result));
            }

            return keys;
        });
    }

    private static class JWTSigningKeyInfoRowMapper implements RowMapper<JWTSigningKeyInfo, ResultSet> {
        private static final JWTSigningKeyInfoRowMapper INSTANCE = new JWTSigningKeyInfoRowMapper();

        private JWTSigningKeyInfoRowMapper() {
        }

        private static JWTSigningKeyInfoRowMapper getInstance() {
            return INSTANCE;
        }

        @Override
        public JWTSigningKeyInfo map(ResultSet result) throws Exception {
            String keyId = result.getString("key_id");
            String keyString = result.getString("key_string");
            long createdAt = result.getLong("created_at");
            String algorithm = result.getString("algorithm");

            if (keyString.contains("|")) {
                return new JWTAsymmetricSigningKeyInfo(keyId, createdAt, algorithm, keyString);
            } else {
                return new JWTSymmetricSigningKeyInfo(keyId, createdAt, algorithm, keyString);
            }
        }
    }

    public static void setJWTSigningKeyInfo_Transaction(Start start, Connection con, JWTSigningKeyInfo info)
            throws SQLException, StorageQueryException {

        String QUERY = "INSERT INTO " + getConfig(start).getJWTSigningKeysTable()
                + "(key_id, key_string, created_at, algorithm) VALUES(?, ?, ?, ?)";

        update(con, QUERY, pst -> {
            pst.setString(1, info.keyId);
            pst.setString(2, info.keyString);
            pst.setLong(3, info.createdAtTime);
            pst.setString(4, info.algorithm);
        });
    }
}
