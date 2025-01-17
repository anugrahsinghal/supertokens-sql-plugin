/*
 *    Copyright (c) 2020, VRAI Labs and/or its affiliates. All rights reserved.
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

import io.supertokens.pluginInterface.KeyValueInfo;
import io.supertokens.pluginInterface.RECIPE_ID;
import io.supertokens.pluginInterface.authRecipe.AuthRecipeUserInfo;
import io.supertokens.pluginInterface.exceptions.StorageQueryException;
import io.supertokens.pluginInterface.useridmapping.UserIdMapping;
import io.supertokens.storage.sql.ConnectionPool;
import io.supertokens.storage.sql.Start;
import io.supertokens.storage.sql.config.Config;
import io.supertokens.storage.sql.domainobject.general.AllAuthRecipeUsersDO;
import io.supertokens.storage.sql.domainobject.general.KeyValueDO;
import io.supertokens.storage.sql.domainobject.useridmapping.UserIdMappingDO;
import io.supertokens.storage.sql.hibernate.CustomQueryWrapper;
import io.supertokens.storage.sql.hibernate.CustomSessionWrapper;
import io.supertokens.storage.sql.utils.Utils;
import org.hibernate.LockMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.supertokens.storage.sql.PreparedStatementValueSetter.NO_OP_SETTER;
import static io.supertokens.storage.sql.ProcessState.PROCESS_STATE.CREATING_NEW_TABLE;
import static io.supertokens.storage.sql.ProcessState.getInstance;
import static io.supertokens.storage.sql.QueryExecutorTemplate.execute;
import static io.supertokens.storage.sql.QueryExecutorTemplate.update;
import static io.supertokens.storage.sql.config.Config.getConfig;
import static io.supertokens.storage.sql.queries.EmailPasswordQueries.getQueryToCreatePasswordResetTokenExpiryIndex;
import static io.supertokens.storage.sql.queries.EmailPasswordQueries.getQueryToCreatePasswordResetTokensTable;
import static io.supertokens.storage.sql.queries.EmailVerificationQueries.*;
import static io.supertokens.storage.sql.queries.JWTSigningQueries.getQueryToCreateJWTSigningTable;
import static io.supertokens.storage.sql.queries.PasswordlessQueries.*;
import static io.supertokens.storage.sql.queries.SessionQueries.getQueryToCreateAccessTokenSigningKeysTable;
import static io.supertokens.storage.sql.queries.SessionQueries.getQueryToCreateSessionInfoTable;
import static io.supertokens.storage.sql.queries.UserMetadataQueries.getQueryToCreateUserMetadataTable;

public class GeneralQueries {

    private static boolean doesTableExists(Start start, String tableName) {
        try {
            String QUERY = "SELECT 1 FROM " + tableName + " LIMIT 1";
            execute(start, QUERY, NO_OP_SETTER, result -> null);
            return true;
        } catch (SQLException | StorageQueryException e) {
            return false;
        }
    }

    static String getQueryToCreateUsersTable(Start start) {
        String schema = Config.getConfig(start).getTableSchema();
        String usersTable = Config.getConfig(start).getUsersTable();
        // @formatter:off
        return "CREATE TABLE IF NOT EXISTS " + usersTable + " ("
                + "user_id CHAR(36) NOT NULL,"
                + "recipe_id VARCHAR(128) NOT NULL,"
                + "time_joined BIGINT NOT NULL,"
                + "CONSTRAINT " + Utils.getConstraintName(schema, usersTable, null, "pkey") +
                " PRIMARY KEY (user_id));";
        // @formatter:on
    }

    static String getQueryToCreateUserPaginationIndex(Start start) {
        return "CREATE INDEX all_auth_recipe_users_pagination_index ON " + Config.getConfig(start).getUsersTable()
                + "(time_joined DESC, user_id " + "DESC);";
    }

    private static String getQueryToCreateKeyValueTable(Start start) {
        String schema = Config.getConfig(start).getTableSchema();
        String keyValueTable = Config.getConfig(start).getKeyValueTable();
        // @formatter:off
        return "CREATE TABLE IF NOT EXISTS " + keyValueTable + " ("
                + "name VARCHAR(128),"
                + "value TEXT,"
                + "created_at_time BIGINT ,"
                + "CONSTRAINT " + Utils.getConstraintName(schema, keyValueTable, null, "pkey") + " PRIMARY KEY(name)" +
                " );";
        // @formatter:on
    }

    public static void createTablesIfNotExists(Start start) throws SQLException, StorageQueryException {
        int numberOfRetries = 0;
        boolean retry = true;
        while (retry) {
            retry = false;
            try {
                if (!doesTableExists(start, Config.getConfig(start).getKeyValueTable())) {
                    getInstance(start).addState(CREATING_NEW_TABLE, null);
                    update(start, getQueryToCreateKeyValueTable(start), NO_OP_SETTER);
                }

                if (!doesTableExists(start, Config.getConfig(start).getUsersTable())) {
                    getInstance(start).addState(CREATING_NEW_TABLE, null);
                    update(start, getQueryToCreateUsersTable(start), NO_OP_SETTER);

                    // index
                    update(start, getQueryToCreateUserPaginationIndex(start), NO_OP_SETTER);
                }

                if (!doesTableExists(start, Config.getConfig(start).getAccessTokenSigningKeysTable())) {
                    getInstance(start).addState(CREATING_NEW_TABLE, null);
                    update(start, getQueryToCreateAccessTokenSigningKeysTable(start), NO_OP_SETTER);
                }

                if (!doesTableExists(start, Config.getConfig(start).getSessionInfoTable())) {
                    getInstance(start).addState(CREATING_NEW_TABLE, null);
                    update(start, getQueryToCreateSessionInfoTable(start), NO_OP_SETTER);
                }

                if (!doesTableExists(start, Config.getConfig(start).getEmailPasswordUsersTable())) {
                    getInstance(start).addState(CREATING_NEW_TABLE, null);
                    update(start, EmailPasswordQueries.getQueryToCreateUsersTable(start), NO_OP_SETTER);
                }

                if (!doesTableExists(start, Config.getConfig(start).getPasswordResetTokensTable())) {
                    getInstance(start).addState(CREATING_NEW_TABLE, null);
                    update(start, getQueryToCreatePasswordResetTokensTable(start), NO_OP_SETTER);
                    // index
                    update(start, getQueryToCreatePasswordResetTokenExpiryIndex(start), NO_OP_SETTER);
                }

                if (!doesTableExists(start, Config.getConfig(start).getEmailVerificationTable())) {
                    getInstance(start).addState(CREATING_NEW_TABLE, null);
                    update(start, getQueryToCreateEmailVerificationTable(start), NO_OP_SETTER);
                }

                if (!doesTableExists(start, Config.getConfig(start).getEmailVerificationTokensTable())) {
                    getInstance(start).addState(CREATING_NEW_TABLE, null);
                    update(start, getQueryToCreateEmailVerificationTokensTable(start), NO_OP_SETTER);
                    // index
                    update(start, getQueryToCreateEmailVerificationTokenExpiryIndex(start), NO_OP_SETTER);
                }

                if (!doesTableExists(start, Config.getConfig(start).getThirdPartyUsersTable())) {
                    getInstance(start).addState(CREATING_NEW_TABLE, null);
                    update(start, ThirdPartyQueries.getQueryToCreateUsersTable(start), NO_OP_SETTER);
                }

                if (!doesTableExists(start, Config.getConfig(start).getJWTSigningKeysTable())) {
                    getInstance(start).addState(CREATING_NEW_TABLE, null);
                    update(start, getQueryToCreateJWTSigningTable(start), NO_OP_SETTER);
                }

                if (!doesTableExists(start, Config.getConfig(start).getPasswordlessUsersTable())) {
                    getInstance(start).addState(CREATING_NEW_TABLE, null);
                    update(start, PasswordlessQueries.getQueryToCreateUsersTable(start), NO_OP_SETTER);
                }

                if (!doesTableExists(start, Config.getConfig(start).getPasswordlessDevicesTable())) {
                    getInstance(start).addState(CREATING_NEW_TABLE, null);
                    update(start, getQueryToCreateDevicesTable(start), NO_OP_SETTER);
                    // index
                    update(start, getQueryToCreateDeviceEmailIndex(start), NO_OP_SETTER);
                    update(start, getQueryToCreateDevicePhoneNumberIndex(start), NO_OP_SETTER);
                }

                if (!doesTableExists(start, Config.getConfig(start).getPasswordlessCodesTable())) {
                    getInstance(start).addState(CREATING_NEW_TABLE, null);
                    update(start, getQueryToCreateCodesTable(start), NO_OP_SETTER);
                    // index
                    update(start, getQueryToCreateCodeCreatedAtIndex(start), NO_OP_SETTER);
                }

                // This PostgreSQL specific, because it's created automatically in MySQL and it doesn't support "create
                // index if not exists"
                // We missed creating this earlier for the codes table, so it may be missing even if the table exists
                update(start, getQueryToCreateCodeDeviceIdHashIndex(start), NO_OP_SETTER);

                if (!doesTableExists(start, Config.getConfig(start).getUserMetadataTable())) {
                    getInstance(start).addState(CREATING_NEW_TABLE, null);
                    update(start, getQueryToCreateUserMetadataTable(start), NO_OP_SETTER);
                }

                if (!doesTableExists(start, Config.getConfig(start).getRolesTable())) {
                    getInstance(start).addState(CREATING_NEW_TABLE, null);
                    update(start, UserRolesQueries.getQueryToCreateRolesTable(start), NO_OP_SETTER);
                }

                if (!doesTableExists(start, Config.getConfig(start).getUserRolesPermissionsTable())) {
                    getInstance(start).addState(CREATING_NEW_TABLE, null);
                    update(start, UserRolesQueries.getQueryToCreateRolePermissionsTable(start), NO_OP_SETTER);
                    // index
                    update(start, UserRolesQueries.getQueryToCreateRolePermissionsPermissionIndex(start), NO_OP_SETTER);
                }

                if (!doesTableExists(start, Config.getConfig(start).getUserRolesTable())) {
                    getInstance(start).addState(CREATING_NEW_TABLE, null);
                    update(start, UserRolesQueries.getQueryToCreateUserRolesTable(start), NO_OP_SETTER);
                    // index
                    update(start, UserRolesQueries.getQueryToCreateUserRolesRoleIndex(start), NO_OP_SETTER);
                }

                if (!doesTableExists(start, Config.getConfig(start).getUserIdMappingTable())) {
                    getInstance(start).addState(CREATING_NEW_TABLE, null);
                    update(start, UserIdMappingQueries.getQueryToCreateUserIdMappingTable(start), NO_OP_SETTER);
                }
            } catch (Exception e) {
                if (e.getMessage().contains("schema") && e.getMessage().contains("does not exist")
                        && numberOfRetries < 1) {
                    // we must create the schema and try again.
                    String schema = Config.getConfig(start).getTableSchema();
                    if (!schema.equals("public")) {
                        String query = "CREATE SCHEMA " + schema;
                        update(start, query, NO_OP_SETTER);
                        numberOfRetries++;
                        retry = true;
                        continue;
                    }
                }
                throw e;
            }
        }
    }

    @TestOnly
    public static void deleteAllTables(Start start) throws SQLException, StorageQueryException {
        {
            String DROP_QUERY = "DROP INDEX IF EXISTS emailpassword_password_reset_token_expiry_index";
            update(start, DROP_QUERY, NO_OP_SETTER);
        }
        {
            String DROP_QUERY = "DROP INDEX IF EXISTS emailverification_tokens_index";
            update(start, DROP_QUERY, NO_OP_SETTER);
        }
        {
            String DROP_QUERY = "DROP INDEX IF EXISTS all_auth_recipe_users_pagination_index";
            update(start, DROP_QUERY, NO_OP_SETTER);
        }

        {
            String DROP_QUERY = "DROP TABLE IF EXISTS " + getConfig(start).getKeyValueTable() + ","
                    + getConfig(start).getUserIdMappingTable() + "," + getConfig(start).getUsersTable() + ","
                    + getConfig(start).getAccessTokenSigningKeysTable() + "," + getConfig(start).getSessionInfoTable()
                    + "," + getConfig(start).getEmailPasswordUsersTable() + ","
                    + getConfig(start).getPasswordResetTokensTable() + ","
                    + getConfig(start).getEmailVerificationTokensTable() + ","
                    + getConfig(start).getEmailVerificationTable() + "," + getConfig(start).getThirdPartyUsersTable()
                    + "," + getConfig(start).getJWTSigningKeysTable() + ","
                    + getConfig(start).getPasswordlessCodesTable() + ","
                    + getConfig(start).getPasswordlessDevicesTable() + ","
                    + getConfig(start).getPasswordlessUsersTable() + "," + getConfig(start).getUserMetadataTable()
                    + ", " + getConfig(start).getRolesTable() + "," + getConfig(start).getUserRolesPermissionsTable()
                    + "," + getConfig(start).getUserRolesTable();
            update(start, DROP_QUERY, NO_OP_SETTER);
        }
    }

    public static void setKeyValue_Transaction(CustomSessionWrapper session, String key, KeyValueInfo info) {
        // we want to do an "insert .. on conflict" style query here. There is no
        // direct way of doing that, we so first get it, and then we save or update.
        // We do not apply a pessimistic write lock here because if this function
        // is called twice in a row with the same key, then it will throw an error.
        // Also we do not call saveOrUpdate since that does an extra select query in case
        // we are inserting a new value.
        KeyValueDO toInsertOrUpdate = session.get(KeyValueDO.class, key);
        if (toInsertOrUpdate == null) {
            toInsertOrUpdate = new KeyValueDO();
            toInsertOrUpdate.setName(key);
            toInsertOrUpdate.setValue(info.value);
            toInsertOrUpdate.setCreated_at_time(info.createdAtTime);
            session.save(KeyValueDO.class, key, toInsertOrUpdate);
        } else {
            toInsertOrUpdate.setValue(info.value);
            toInsertOrUpdate.setCreated_at_time(info.createdAtTime);
            session.update(KeyValueDO.class, key, toInsertOrUpdate);
        }
    }

    public static void setKeyValue(Start start, String key, KeyValueInfo info)
            throws SQLException, StorageQueryException {
        ConnectionPool.withSession(start, (session, con) -> {
            setKeyValue_Transaction(session, key, info);
            return null;
        }, true);
    }

    public static KeyValueInfo getKeyValue(Start start, String key) throws SQLException, StorageQueryException {
        return ConnectionPool.withSession(start, (session, con) -> {
            KeyValueDO result = session.get(KeyValueDO.class, key);
            if (result != null) {
                return new KeyValueInfo(result.getValue(), result.getCreated_at_time());
            }
            return null;
        }, false);
    }

    public static KeyValueInfo getKeyValue_Transaction(CustomSessionWrapper session, String key) {
        KeyValueDO result = session.get(KeyValueDO.class, key, LockMode.PESSIMISTIC_WRITE);
        if (result == null) {
            return null;
        }
        return new KeyValueInfo(result.getValue(), result.getCreated_at_time());
    }

    public static void deleteKeyValue_Transaction(CustomSessionWrapper session, String key) {
        KeyValueDO toDelete = new KeyValueDO();
        toDelete.setName(key);
        session.delete(KeyValueDO.class, key, toDelete);
    }

    public static long getUsersCount(Start start, RECIPE_ID[] includeRecipeIds)
            throws SQLException, StorageQueryException {
        return ConnectionPool.withSession(start, (session, con) -> {
            CustomQueryWrapper<Long> q;
            if (includeRecipeIds == null || includeRecipeIds.length == 0) {
                q = session.createQuery("SELECT COUNT(*) FROM AllAuthRecipeUsersDO", Long.class);
            } else {
                q = session.createQuery("SELECT COUNT(*) FROM AllAuthRecipeUsersDO WHERE recipe_id IN (:recipe_ids)",
                        Long.class);
                String[] includeRecipeIdsStr = new String[includeRecipeIds.length];
                for (int i = 0; i < includeRecipeIds.length; i++) {
                    includeRecipeIdsStr[i] = includeRecipeIds[i].toString();
                }
                q.setParameterList("recipe_ids", includeRecipeIdsStr);
            }
            List<Long> result = q.list();
            return result.get(0);
        }, false);
    }

    public static AuthRecipeUserInfo[] getUsers(Start start, @NotNull Integer limit, @NotNull String timeJoinedOrder,
            @Nullable RECIPE_ID[] includeRecipeIds, @Nullable String userId, @Nullable Long timeJoined)
            throws SQLException, StorageQueryException {

        // This list will be used to keep track of the result's order from the db
        List<AllAuthRecipeUsersDO> usersFromQuery = ConnectionPool.withSession(start, (session, con) -> {
            CustomQueryWrapper<AllAuthRecipeUsersDO> q;
            StringBuilder RECIPE_ID_CONDITION = new StringBuilder();
            if (includeRecipeIds != null && includeRecipeIds.length > 0) {
                RECIPE_ID_CONDITION.append("recipe_id IN (:recipe_ids)");
            }

            if (timeJoined != null && userId != null) {
                String recipeIdCondition = RECIPE_ID_CONDITION.toString();
                if (!recipeIdCondition.equals("")) {
                    recipeIdCondition = recipeIdCondition + " AND";
                }
                String timeJoinedOrderSymbol = timeJoinedOrder.equals("ASC") ? ">" : "<";
                String QUERY = "SELECT entity FROM AllAuthRecipeUsersDO entity WHERE " + recipeIdCondition
                        + " (time_joined " + timeJoinedOrderSymbol
                        + " :time_joined1 OR (time_joined = :time_joined2 AND user_id <= :user_id)) ORDER BY "
                        + "time_joined " + timeJoinedOrder + ", user_id DESC";
                q = session.createQuery(QUERY, AllAuthRecipeUsersDO.class);
                q.setParameter("time_joined1", timeJoined);
                q.setParameter("time_joined2", timeJoined);
                q.setParameter("user_id", userId);
                q.setMaxResults(limit);
            } else {
                String recipeIdCondition = RECIPE_ID_CONDITION.toString();
                if (!recipeIdCondition.equals("")) {
                    recipeIdCondition = " WHERE " + recipeIdCondition;
                }
                String QUERY = "SELECT entity FROM AllAuthRecipeUsersDO entity" + recipeIdCondition
                        + " ORDER BY time_joined " + timeJoinedOrder + ", user_id DESC";
                q = session.createQuery(QUERY, AllAuthRecipeUsersDO.class);
                q.setMaxResults(limit);
            }
            if (includeRecipeIds != null && includeRecipeIds.length > 0) {
                String[] includeRecipeIdsStr = new String[includeRecipeIds.length];
                for (int i = 0; i < includeRecipeIds.length; i++) {
                    includeRecipeIdsStr[i] = includeRecipeIds[i].toString();
                }
                q.setParameterList("recipe_ids", includeRecipeIdsStr);
            }
            return q.list();
        }, false);

        // we create a map from recipe ID -> userId[]
        Map<RECIPE_ID, List<String>> recipeIdToUserIdListMap = new HashMap<>();
        for (AllAuthRecipeUsersDO user : usersFromQuery) {
            RECIPE_ID recipeId = RECIPE_ID.getEnumFromString(user.getRecipe_id());
            if (recipeId == null) {
                throw new SQLException("Unrecognised recipe ID in database: " + user.getRecipe_id());
            }
            List<String> userIdList = recipeIdToUserIdListMap.get(recipeId);
            if (userIdList == null) {
                userIdList = new ArrayList<>();
            }
            userIdList.add(user.getUser_id());
            recipeIdToUserIdListMap.put(recipeId, userIdList);
        }

        AuthRecipeUserInfo[] finalResult = new AuthRecipeUserInfo[usersFromQuery.size()];

        // we give the userId[] for each recipe to fetch all those user's details
        for (RECIPE_ID recipeId : recipeIdToUserIdListMap.keySet()) {
            List<? extends AuthRecipeUserInfo> users = getUserInfoForRecipeIdFromUserIds(start, recipeId,
                    recipeIdToUserIdListMap.get(recipeId));

            // we fill in all the slots in finalResult based on their position in usersFromQuery
            Map<String, AuthRecipeUserInfo> userIdToInfoMap = new HashMap<>();
            for (AuthRecipeUserInfo user : users) {
                userIdToInfoMap.put(user.id, user);
            }
            for (int i = 0; i < usersFromQuery.size(); i++) {
                if (finalResult[i] == null) {
                    finalResult[i] = userIdToInfoMap.get(usersFromQuery.get(i).getUser_id());
                }
            }
        }

        return finalResult;
    }

    public static boolean doesUserIdExist(Start start, String userId) throws SQLException, StorageQueryException {
        return ConnectionPool.withSession(start, (session, con) -> {
            AllAuthRecipeUsersDO user = session.get(AllAuthRecipeUsersDO.class, userId);
            return user != null;

        }, false);

    }

    private static List<? extends AuthRecipeUserInfo> getUserInfoForRecipeIdFromUserIds(Start start, RECIPE_ID recipeId,
            List<String> userIds) throws StorageQueryException, SQLException {
        if (recipeId == RECIPE_ID.EMAIL_PASSWORD) {
            return EmailPasswordQueries.getUsersInfoUsingIdList(start, userIds);
        } else if (recipeId == RECIPE_ID.THIRD_PARTY) {
            return ThirdPartyQueries.getUsersInfoUsingIdList(start, userIds);
        } else if (recipeId == RECIPE_ID.PASSWORDLESS) {
            return PasswordlessQueries.getUsersByIdList(start, userIds);
        } else {
            throw new IllegalArgumentException("No implementation of get users for recipe: " + recipeId.toString());
        }
    }
}
