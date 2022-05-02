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

import io.supertokens.pluginInterface.RowMapper;
import io.supertokens.pluginInterface.emailpassword.PasswordResetTokenInfo;
import io.supertokens.pluginInterface.emailpassword.UserInfo;
import io.supertokens.pluginInterface.exceptions.StorageQueryException;
import io.supertokens.pluginInterface.exceptions.StorageTransactionLogicException;
import io.supertokens.storage.sql.ConnectionPool;
import io.supertokens.storage.sql.Start;
import io.supertokens.storage.sql.config.Config;
import io.supertokens.storage.sql.domainobject.emailpassword.EmailPasswordUsersDO;
import io.supertokens.storage.sql.domainobject.emailpassword.PasswordResetTokensDO;
import io.supertokens.storage.sql.hibernate.CustomQueryWrapper;
import io.supertokens.storage.sql.hibernate.CustomSessionWrapper;
import io.supertokens.storage.sql.utils.Utils;
import org.hibernate.LockMode;

import javax.persistence.LockModeType;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.supertokens.pluginInterface.RECIPE_ID.EMAIL_PASSWORD;
import static io.supertokens.storage.sql.QueryExecutorTemplate.execute;
import static io.supertokens.storage.sql.QueryExecutorTemplate.update;
import static io.supertokens.storage.sql.config.Config.getConfig;
import static java.lang.System.currentTimeMillis;

public class EmailPasswordQueries {

    static String getQueryToCreateUsersTable(Start start) {
        String schema = Config.getConfig(start).getTableSchema();
        String emailPasswordUsersTable = Config.getConfig(start).getEmailPasswordUsersTable();
        // @formatter:off
        return "CREATE TABLE IF NOT EXISTS " + emailPasswordUsersTable + " ("
                + "user_id CHAR(36) NOT NULL,"
                + "email VARCHAR(256) NOT NULL CONSTRAINT " +
                Utils.getConstraintName(schema, emailPasswordUsersTable, "email", "key") + " UNIQUE,"
                + "password_hash VARCHAR(128) NOT NULL," + "time_joined BIGINT NOT NULL,"
                + "CONSTRAINT " + Utils.getConstraintName(schema, emailPasswordUsersTable, null, "pkey") +
                " PRIMARY KEY (user_id));";
        // @formatter:on
    }

    static String getQueryToCreatePasswordResetTokensTable(Start start) {
        String schema = Config.getConfig(start).getTableSchema();
        String passwordResetTokensTable = Config.getConfig(start).getPasswordResetTokensTable();
        // @formatter:off
        return "CREATE TABLE IF NOT EXISTS " + passwordResetTokensTable + " ("
                + "user_id CHAR(36) NOT NULL,"
                + "token VARCHAR(128) NOT NULL CONSTRAINT " +
                Utils.getConstraintName(schema, passwordResetTokensTable, "token", "key") + " UNIQUE,"
                + "token_expiry BIGINT NOT NULL,"
                + "CONSTRAINT " + Utils.getConstraintName(schema, passwordResetTokensTable, null, "pkey") +
                " PRIMARY KEY (user_id, token),"
                + ("CONSTRAINT " + Utils.getConstraintName(schema, passwordResetTokensTable, "user_id", "fkey") +
                " FOREIGN KEY (user_id)"
                + " REFERENCES " + Config.getConfig(start).getEmailPasswordUsersTable() + "(user_id)"
                + " ON DELETE CASCADE ON UPDATE CASCADE);");
        // @formatter:on
    }

    static String getQueryToCreatePasswordResetTokenExpiryIndex(Start start) {
        return "CREATE INDEX emailpassword_password_reset_token_expiry_index ON "
                + Config.getConfig(start).getPasswordResetTokensTable() + "(token_expiry);";
    }

    public static void deleteExpiredPasswordResetTokens(Start start) throws SQLException, StorageQueryException {
        ConnectionPool.withSession(start, (session, con) -> {
            String QUERY = "DELETE FROM PasswordResetTokensDO where token_expiry < :expiry";
            CustomQueryWrapper q = session.createQuery(QUERY);
            q.setParameter("expiry", currentTimeMillis());
            q.executeUpdate();
            return null;
        }, true);
    }

    public static void updateUsersPassword_Transaction(CustomSessionWrapper session, String userId,
            String newPassword) {
        String QUERY = "UPDATE EmailPasswordUsersDO entity SET entity.password_hash = :passwordhash WHERE entity"
                + ".user_id = :userid";

        CustomQueryWrapper q = session.createQuery(QUERY);
        q.setParameter("passwordhash", newPassword);
        q.setParameter("userid", userId);
        q.executeUpdate();
    }

    public static void updateUsersEmail_Transaction(CustomSessionWrapper session, String userId, String newEmail) {
        String QUERY = "UPDATE EmailPasswordUsersDO entity SET entity.email = :email WHERE entity"
                + ".user_id = :userid";

        CustomQueryWrapper q = session.createQuery(QUERY);
        q.setParameter("email", newEmail);
        q.setParameter("userid", userId);
        q.executeUpdate();
    }

    public static void deleteAllPasswordResetTokensForUser_Transaction(CustomSessionWrapper session, String userId) {
        String QUERY = "DELETE FROM PasswordResetTokensDO entity WHERE entity.pk.user.user_id = :userid";
        CustomQueryWrapper q = session.createQuery(QUERY);
        q.setParameter("userid", userId);
        q.executeUpdate();
    }

    public static PasswordResetTokenInfo[] getAllPasswordResetTokenInfoForUser(Start start, String userId)
            throws StorageQueryException, SQLException {
        String QUERY = "SELECT user_id, token, token_expiry FROM " + getConfig(start).getPasswordResetTokensTable()
                + " WHERE user_id = ?";

        return execute(start, QUERY, pst -> pst.setString(1, userId), result -> {
            List<PasswordResetTokenInfo> temp = new ArrayList<>();
            while (result.next()) {
                temp.add(PasswordResetRowMapper.getInstance().mapOrThrow(result));
            }
            PasswordResetTokenInfo[] finalResult = new PasswordResetTokenInfo[temp.size()];
            for (int i = 0; i < temp.size(); i++) {
                finalResult[i] = temp.get(i);
            }
            return finalResult;
        });
    }

    public static PasswordResetTokenInfo[] getAllPasswordResetTokenInfoForUser_Transaction(CustomSessionWrapper session,
            String userId) {
        String QUERY = "SELECT entity FROM PasswordResetTokensDO entity WHERE entity.pk.user.user_id = :userid";

        CustomQueryWrapper<PasswordResetTokensDO> q = session.createQuery(QUERY, PasswordResetTokensDO.class);
        q.setParameter("userid", userId);
        q.setLockMode(LockModeType.PESSIMISTIC_WRITE);

        List<PasswordResetTokensDO> result = q.list(PasswordResetTokensDO::getPk);

        PasswordResetTokenInfo[] finalResult = new PasswordResetTokenInfo[result.size()];
        for (int i = 0; i < result.size(); i++) {
            PasswordResetTokensDO curr = result.get(i);
            finalResult[i] = new PasswordResetTokenInfo(curr.getPk().getUser().getUser_id(), curr.getPk().getToken(),
                    curr.getToken_expiry());
        }
        return finalResult;
    }

    public static UserInfo getUserInfoUsingId_Transaction(CustomSessionWrapper session, String id) {
        EmailPasswordUsersDO result = session.get(EmailPasswordUsersDO.class, id, LockMode.PESSIMISTIC_WRITE);
        if (result == null) {
            return null;
        }
        return new UserInfo(result.getUser_id(), result.getEmail(), result.getPassword_hash(), result.getTime_joined());
    }

    @Deprecated
    public static UserInfo[] getUsersInfo(Start start, Integer limit, String timeJoinedOrder)
            throws SQLException, StorageQueryException {
        return ConnectionPool.withSession(start, (session, con) -> {
            String QUERY = "SELECT entity FROM EmailPasswordUsersDO entity ORDER BY entity.time_joined "
                    + timeJoinedOrder + ", user_id DESC";
            CustomQueryWrapper<EmailPasswordUsersDO> q = session.createQuery(QUERY, EmailPasswordUsersDO.class);
            q.setMaxResults(limit);
            List<EmailPasswordUsersDO> result = q.list(EmailPasswordUsersDO::getUser_id);
            UserInfo[] finalResult = new UserInfo[result.size()];
            for (int i = 0; i < result.size(); i++) {
                EmailPasswordUsersDO curr = result.get(i);
                finalResult[i] = new UserInfo(curr.getUser_id(), curr.getEmail(), curr.getPassword_hash(),
                        curr.getTime_joined());
            }
            return finalResult;
        }, false);
    }

    @Deprecated
    public static UserInfo[] getUsersInfo(Start start, String userId, Long timeJoined, Integer limit,
            String timeJoinedOrder) throws SQLException, StorageQueryException {
        return ConnectionPool.withSession(start, (session, con) -> {
            String timeJoinedOrderSymbol = timeJoinedOrder.equals("ASC") ? ">" : "<";
            String QUERY = "SELECT entity FROM EmailPasswordUsersDO entity WHERE entity.time_joined "
                    + timeJoinedOrderSymbol
                    + " :tj1 OR (entity.time_joined = :tj2 AND entity.user_id <= :userid) ORDER BY entity"
                    + ".time_joined " + timeJoinedOrder + ", user_id DESC";
            CustomQueryWrapper<EmailPasswordUsersDO> q = session.createQuery(QUERY, EmailPasswordUsersDO.class);
            q.setMaxResults(limit);
            q.setParameter("tj1", timeJoined);
            q.setParameter("tj2", timeJoined);
            q.setParameter("userid", userId);
            List<EmailPasswordUsersDO> result = q.list(EmailPasswordUsersDO::getUser_id);
            UserInfo[] finalResult = new UserInfo[result.size()];
            for (int i = 0; i < result.size(); i++) {
                EmailPasswordUsersDO curr = result.get(i);
                finalResult[i] = new UserInfo(curr.getUser_id(), curr.getEmail(), curr.getPassword_hash(),
                        curr.getTime_joined());
            }
            return finalResult;
        }, false);
    }

    @Deprecated
    public static long getUsersCount(Start start) throws SQLException, StorageQueryException {
        return ConnectionPool.withSession(start, (session, con) -> {
            CustomQueryWrapper<Long> q = session.createQuery("SELECT COUNT(*) FROM EmailPasswordUsersDO", Long.class);
            List<Long> result = q.list(item -> null);
            return result.get(0);
        }, false);
    }

    public static PasswordResetTokenInfo getPasswordResetTokenInfo(Start start, String token)
            throws SQLException, StorageQueryException {
        String QUERY = "SELECT user_id, token, token_expiry FROM " + getConfig(start).getPasswordResetTokensTable()
                + " WHERE token = ?";
        return execute(start, QUERY, pst -> pst.setString(1, token), result -> {
            if (result.next()) {
                return PasswordResetRowMapper.getInstance().mapOrThrow(result);
            }
            return null;
        });
    }

    public static void addPasswordResetToken(Start start, String userId, String tokenHash, long expiry)
            throws SQLException, StorageQueryException {
        String QUERY = "INSERT INTO " + getConfig(start).getPasswordResetTokensTable()
                + "(user_id, token, token_expiry)" + " VALUES(?, ?, ?)";

        update(start, QUERY, pst -> {
            pst.setString(1, userId);
            pst.setString(2, tokenHash);
            pst.setLong(3, expiry);
        });
    }

    public static void signUp(Start start, String userId, String email, String passwordHash, long timeJoined)
            throws StorageQueryException, StorageTransactionLogicException {
        start.startTransaction(con -> {
            Connection sqlCon = (Connection) con.getConnection();
            try {
                {
                    String QUERY = "INSERT INTO " + getConfig(start).getUsersTable()
                            + "(user_id, recipe_id, time_joined)" + " VALUES(?, ?, ?)";
                    update(sqlCon, QUERY, pst -> {
                        pst.setString(1, userId);
                        pst.setString(2, EMAIL_PASSWORD.toString());
                        pst.setLong(3, timeJoined);
                    });
                }

                {
                    String QUERY = "INSERT INTO " + getConfig(start).getEmailPasswordUsersTable()
                            + "(user_id, email, password_hash, time_joined)" + " VALUES(?, ?, ?, ?)";

                    update(sqlCon, QUERY, pst -> {
                        pst.setString(1, userId);
                        pst.setString(2, email);
                        pst.setString(3, passwordHash);
                        pst.setLong(4, timeJoined);
                    });
                }

                sqlCon.commit();
            } catch (SQLException throwables) {
                throw new StorageTransactionLogicException(throwables);
            }
            return null;
        });
    }

    public static void deleteUser(Start start, String userId)
            throws StorageQueryException, StorageTransactionLogicException {
        start.startTransaction(con -> {
            Connection sqlCon = (Connection) con.getConnection();
            try {
                {
                    String QUERY = "DELETE FROM " + getConfig(start).getUsersTable()
                            + " WHERE user_id = ? AND recipe_id = ?";

                    update(sqlCon, QUERY, pst -> {
                        pst.setString(1, userId);
                        pst.setString(2, EMAIL_PASSWORD.toString());
                    });
                }

                {
                    String QUERY = "DELETE FROM " + getConfig(start).getEmailPasswordUsersTable()
                            + " WHERE user_id = ?";

                    update(sqlCon, QUERY, pst -> pst.setString(1, userId));
                }
                sqlCon.commit();
            } catch (SQLException throwables) {
                throw new StorageTransactionLogicException(throwables);
            }
            return null;
        });
    }

    public static UserInfo getUserInfoUsingId(Start start, String id) throws SQLException, StorageQueryException {
        List<String> input = new ArrayList<>();
        input.add(id);
        List<UserInfo> result = getUsersInfoUsingIdList(start, input);
        if (result.size() == 1) {
            return result.get(0);
        }
        return null;
    }

    public static List<UserInfo> getUsersInfoUsingIdList(Start start, List<String> ids)
            throws SQLException, StorageQueryException {
        if (ids.size() > 0) {
            StringBuilder QUERY = new StringBuilder("SELECT user_id, email, password_hash, time_joined FROM "
                    + getConfig(start).getEmailPasswordUsersTable());
            QUERY.append(" WHERE user_id IN (");
            for (int i = 0; i < ids.size(); i++) {

                QUERY.append("?");
                if (i != ids.size() - 1) {
                    // not the last element
                    QUERY.append(",");
                }
            }
            QUERY.append(")");

            return execute(start, QUERY.toString(), pst -> {
                for (int i = 0; i < ids.size(); i++) {
                    // i+1 cause this starts with 1 and not 0
                    pst.setString(i + 1, ids.get(i));
                }
            }, result -> {
                List<UserInfo> finalResult = new ArrayList<>();
                while (result.next()) {
                    finalResult.add(UserInfoRowMapper.getInstance().mapOrThrow(result));
                }
                return finalResult;
            });
        }
        return Collections.emptyList();
    }

    public static UserInfo getUserInfoUsingEmail(Start start, String email) throws StorageQueryException, SQLException {
        String QUERY = "SELECT user_id, email, password_hash, time_joined FROM "
                + getConfig(start).getEmailPasswordUsersTable() + " WHERE email = ?";
        return execute(start, QUERY, pst -> pst.setString(1, email), result -> {
            if (result.next()) {
                return UserInfoRowMapper.getInstance().mapOrThrow(result);
            }
            return null;
        });
    }

    private static class PasswordResetRowMapper implements RowMapper<PasswordResetTokenInfo, ResultSet> {
        public static final PasswordResetRowMapper INSTANCE = new PasswordResetRowMapper();

        private PasswordResetRowMapper() {
        }

        private static PasswordResetRowMapper getInstance() {
            return INSTANCE;
        }

        @Override
        public PasswordResetTokenInfo map(ResultSet result) throws StorageQueryException {
            try {
                return new PasswordResetTokenInfo(result.getString("user_id"), result.getString("token"),
                        result.getLong("token_expiry"));
            } catch (Exception e) {
                throw new StorageQueryException(e);
            }
        }
    }

    private static class UserInfoRowMapper implements RowMapper<UserInfo, ResultSet> {
        static final UserInfoRowMapper INSTANCE = new UserInfoRowMapper();

        private UserInfoRowMapper() {
        }

        private static UserInfoRowMapper getInstance() {
            return INSTANCE;
        }

        @Override
        public UserInfo map(ResultSet result) throws Exception {
            return new UserInfo(result.getString("user_id"), result.getString("email"),
                    result.getString("password_hash"), result.getLong("time_joined"));
        }
    }
}
