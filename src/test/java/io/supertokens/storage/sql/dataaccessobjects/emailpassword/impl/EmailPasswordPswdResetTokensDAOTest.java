/*
 *    Copyright (c) 2022, VRAI Labs and/or its affiliates. All rights reserved.
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

package io.supertokens.storage.sql.dataaccessobjects.emailpassword.impl;

import io.supertokens.pluginInterface.emailpassword.exceptions.UnknownUserIdException;
import io.supertokens.storage.sql.domainobjects.emailpassword.EmailPasswordPswdResetTokensDO;
import io.supertokens.storage.sql.domainobjects.emailpassword.EmailPasswordPswdResetTokensPKDO;
import io.supertokens.storage.sql.domainobjects.emailpassword.EmailPasswordUsersDO;
import io.supertokens.storage.sql.test.HibernateUtilTest;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.PersistenceException;
import java.io.Serializable;
import java.util.List;

import static io.supertokens.storage.sql.TestConstants.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class EmailPasswordPswdResetTokensDAOTest {

    EmailPasswordPswdResetTokensDAO emailPasswordPswdResetTokensDAO;
    EmailPasswordUsersDAO emailPasswordUsersDAO;
    Session session;

    @Before
    public void setUp() throws Exception {
        session = HibernateUtilTest.getSessionFactory().openSession();
        emailPasswordPswdResetTokensDAO = new EmailPasswordPswdResetTokensDAO(session);
        emailPasswordUsersDAO = new EmailPasswordUsersDAO(session);

        Transaction transaction = session.beginTransaction();
        emailPasswordUsersDAO.removeAll();
        transaction.commit();
    }

    @After
    public void tearDown() throws Exception {

        Transaction transaction = session.beginTransaction();
        emailPasswordUsersDAO.removeAll();
        transaction.commit();
        session.close();

    }

    private EmailPasswordPswdResetTokensDO getEmailPasswordPswdResetTokensDO() {
        return getEmailPasswordPswdResetTokensDO(TOKEN, USER_ID, EMAIL, PASS_HASH, CREATED_AT, TOKEN_EXPIRY);
    }

    private EmailPasswordPswdResetTokensDO getEmailPasswordPswdResetTokensDO(String token, String userId, String email,
            String passHash, long createdAt, long tokenExpiry) {
        EmailPasswordPswdResetTokensDO emailPasswordPswdResetTokensDO = new EmailPasswordPswdResetTokensDO();
        EmailPasswordPswdResetTokensPKDO primaryKey = new EmailPasswordPswdResetTokensPKDO();

        primaryKey.setToken(token);
        primaryKey.setUser_id(getEmailPasswordUsersDO(userId, email, passHash, createdAt));

        emailPasswordPswdResetTokensDO.setPrimaryKey(primaryKey);
        emailPasswordPswdResetTokensDO.setToken_expiry(tokenExpiry);
        return emailPasswordPswdResetTokensDO;
    }

    private EmailPasswordUsersDO getEmailPasswordUsersDO() {
        return new EmailPasswordUsersDO(USER_ID, EMAIL, PASS_HASH, CREATED_AT);
    }

    private EmailPasswordUsersDO getEmailPasswordUsersDO(String userId, String email, String passHash, long createdAt) {
        return new EmailPasswordUsersDO(userId, email, passHash, createdAt);
    }

    @Test
    public void createFailParentKey() {

        Transaction transaction = session.beginTransaction();
        EmailPasswordPswdResetTokensDO emailPasswordPswdResetTokensDO = getEmailPasswordPswdResetTokensDO();

        try {
            Serializable id = emailPasswordPswdResetTokensDAO.create(emailPasswordPswdResetTokensDO);
            transaction.commit();
        } catch (PersistenceException p) {

            if (transaction != null) {
                transaction.rollback();
            }

            assertTrue(p.getCause() instanceof ConstraintViolationException);
            return;
        }

        fail();
    }

    @Test
    public void createSuccess() throws Exception {

        Transaction transaction = session.beginTransaction();

        EmailPasswordPswdResetTokensDO emailPasswordPswdResetTokensDO = getEmailPasswordPswdResetTokensDO();

        // create parent entry
        emailPasswordUsersDAO.create(emailPasswordPswdResetTokensDO.getPrimaryKey().getUser_id());

        Serializable id = emailPasswordPswdResetTokensDAO.create(emailPasswordPswdResetTokensDO);
        transaction.commit();
        assertTrue(id != null);
        assertTrue(((EmailPasswordPswdResetTokensPKDO) id).getToken().equals(TOKEN));
        assertTrue(((EmailPasswordPswdResetTokensPKDO) id).getUser_id().getUser_id().equals(USER_ID));

    }

    @Test
    public void get() throws Exception {

        createSuccess();
        Transaction transaction = session.beginTransaction();

        EmailPasswordUsersDO emailPasswordUsersDO = emailPasswordUsersDAO.get(USER_ID);
        EmailPasswordPswdResetTokensDO emailPasswordPswdResetTokensDO = emailPasswordPswdResetTokensDAO
                .get(new EmailPasswordPswdResetTokensPKDO(emailPasswordUsersDO, TOKEN));
        transaction.commit();

        assertTrue(emailPasswordPswdResetTokensDO != null);
        assertTrue(emailPasswordPswdResetTokensDO.getToken_expiry() == TOKEN_EXPIRY);
        assertTrue(emailPasswordPswdResetTokensDO.getPrimaryKey().getToken() == TOKEN);
        assertTrue(emailPasswordPswdResetTokensDO.getPrimaryKey().getUser_id() == emailPasswordUsersDO);

    }

    @Test
    public void getAll() throws Exception {
        createSuccess();
        List<EmailPasswordPswdResetTokensDO> results = emailPasswordPswdResetTokensDAO.getAll();
        assertTrue(results != null);
        assertTrue(results.size() == 1);
    }

    @Test
    public void remove() throws Exception {
        createSuccess();
        Transaction transaction = session.beginTransaction();
        EmailPasswordUsersDO emailPasswordUsersDO = emailPasswordUsersDAO.get(USER_ID);
        emailPasswordPswdResetTokensDAO
                .removeWhereUserIdEquals(new EmailPasswordPswdResetTokensPKDO(emailPasswordUsersDO, TOKEN));
        transaction.commit();
        List<EmailPasswordPswdResetTokensDO> results = emailPasswordPswdResetTokensDAO.getAll();
        assertTrue(results.size() == 0);

    }

    @Test
    public void removeAll() throws Exception {
        createSuccess();
        Transaction transaction = session.beginTransaction();
        EmailPasswordUsersDO emailPasswordUsersDO = emailPasswordUsersDAO.get(USER_ID);

        emailPasswordPswdResetTokensDAO.removeAll();
        transaction.commit();

        List<EmailPasswordPswdResetTokensDO> results = emailPasswordPswdResetTokensDAO.getAll();
        assertTrue(results.size() == 0);

    }

    @Test
    public void deleteWhereTokenExpiryIsLessThan() throws Exception {
        createSuccess();
        Transaction transaction = session.beginTransaction();
        emailPasswordPswdResetTokensDAO.deleteWhereTokenExpiryIsLessThan(System.currentTimeMillis());
        transaction.commit();
        assertTrue(emailPasswordPswdResetTokensDAO.getAll().size() == 0);
    }

    @Test
    public void deleteAllWhereUserIdEquals() throws Exception {
        // create parent and child first pair
        createSuccess();
        Transaction transaction = session.beginTransaction();

        EmailPasswordPswdResetTokensDO emailPasswordPswdResetTokensDO = getEmailPasswordPswdResetTokensDO(
                TOKEN + "UPDATED", USER_ID + "UPDATED", EMAIL + "UPDATED", PASS_HASH + "UPDATED", CREATED_AT + 20l,
                TOKEN_EXPIRY + 20l);

        // create parent and child second pair
        emailPasswordUsersDAO.create(emailPasswordPswdResetTokensDO.getPrimaryKey().getUser_id());
        emailPasswordPswdResetTokensDAO.create(emailPasswordPswdResetTokensDO);

        // create another child using the same parent
        EmailPasswordPswdResetTokensDO emailPasswordPswdResetTokensDO_two = getEmailPasswordPswdResetTokensDO(
                TOKEN + "UPDATEDTWO", USER_ID + "UPDATED", EMAIL + "UPDATED", PASS_HASH + "UPDATED", CREATED_AT + 20l,
                TOKEN_EXPIRY + 20l);
        emailPasswordPswdResetTokensDAO.create(emailPasswordPswdResetTokensDO_two);

        // test delete for each pair
        assertTrue(emailPasswordPswdResetTokensDAO.getAll().size() == 3);
        // remove first child parent pair
        emailPasswordPswdResetTokensDAO.deleteAllWhereUserIdEquals(USER_ID);
        assertTrue(emailPasswordPswdResetTokensDAO.getAll().size() == 2);

        // remove second pair
        emailPasswordPswdResetTokensDAO.deleteAllWhereUserIdEquals(USER_ID + "UPDATED");
        assertTrue(emailPasswordPswdResetTokensDAO.getAll().size() == 0);
        transaction.commit();
    }

    @Test
    public void getAllPasswordResetTokenInfoForUser() throws Exception {
        createSuccess();
        Transaction transaction = session.beginTransaction();

        EmailPasswordPswdResetTokensDO emailPasswordPswdResetTokensDO_two = getEmailPasswordPswdResetTokensDO(
                TOKEN + "two", USER_ID, EMAIL, PASS_HASH, CREATED_AT, TOKEN_EXPIRY);
        emailPasswordPswdResetTokensDAO.create(emailPasswordPswdResetTokensDO_two);
        transaction.commit();

        List<EmailPasswordPswdResetTokensDO> results = emailPasswordPswdResetTokensDAO
                .getAllPasswordResetTokenInfoForUser(USER_ID);

        assertTrue(results.size() == 2);
    }

    @Test
    public void lockAndgetAllPasswordResetTokenInfoForUser() throws Exception {
        createSuccess();
        Transaction transaction = session.beginTransaction();
        EmailPasswordPswdResetTokensDO emailPasswordPswdResetTokensDO_two = getEmailPasswordPswdResetTokensDO(
                TOKEN + "two", USER_ID, EMAIL, PASS_HASH, CREATED_AT, TOKEN_EXPIRY);
        emailPasswordPswdResetTokensDAO.create(emailPasswordPswdResetTokensDO_two);

        List<EmailPasswordPswdResetTokensDO> results = emailPasswordPswdResetTokensDAO
                .getAllPasswordResetTokenInfoForUser_locked(USER_ID);
        transaction.commit();

        assertTrue(results.size() == 2);
    }

    @Test
    public void getPasswordResetTokenInfo() throws Exception {
        createSuccess();

        Transaction transaction = session.beginTransaction();
        EmailPasswordPswdResetTokensDO emailPasswordPswdResetTokensDO_two = getEmailPasswordPswdResetTokensDO(
                TOKEN + "two", USER_ID, EMAIL, PASS_HASH, CREATED_AT, TOKEN_EXPIRY);
        emailPasswordPswdResetTokensDAO.create(emailPasswordPswdResetTokensDO_two);
        transaction.commit();

        EmailPasswordPswdResetTokensDO emailPasswordPswdResetTokensDO = emailPasswordPswdResetTokensDAO
                .getPasswordResetTokenInfo(TOKEN);

        assertTrue(emailPasswordPswdResetTokensDO != null);
        assertTrue(emailPasswordPswdResetTokensDO.getToken_expiry() == TOKEN_EXPIRY);

        assertTrue(emailPasswordPswdResetTokensDO.getPrimaryKey().getUser_id().getEmail().equals(EMAIL));

    }

    @Test
    public void insertPasswordResetTokenInfoException() {

        Transaction transaction = session.beginTransaction();

        try {

            EmailPasswordPswdResetTokensPKDO key = emailPasswordPswdResetTokensDAO.insertPasswordResetTokenInfo(USER_ID,
                    TOKEN, TOKEN_EXPIRY);
            transaction.commit();

        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            assertTrue(e instanceof UnknownUserIdException);
            return;
        }

        fail();

    }

    @Test
    public void insertPasswordResetTokenInfoSuccess() throws Exception {
        Transaction transaction = session.beginTransaction();
        emailPasswordUsersDAO.create(getEmailPasswordUsersDO());
        EmailPasswordPswdResetTokensPKDO key = emailPasswordPswdResetTokensDAO.insertPasswordResetTokenInfo(USER_ID,
                TOKEN, TOKEN_EXPIRY);
        transaction.commit();

        assertTrue(key != null);
        assertTrue(key.getToken().equals(TOKEN));
        assertTrue(key.getUser_id().getPassword_hash().equals(PASS_HASH));

    }
}