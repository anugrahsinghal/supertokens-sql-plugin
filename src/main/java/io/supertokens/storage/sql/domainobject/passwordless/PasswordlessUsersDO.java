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

package io.supertokens.storage.sql.domainobject.passwordless;

import io.supertokens.storage.sql.domainobject.PrimaryKeyFetchable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

//CREATE TABLE IF NOT EXISTS passwordless_users (user_id CHAR(36) NOT NULL,email VARCHAR(256) CONSTRAINT
// passwordless_users_email_key UNIQUE,phone_number VARCHAR(256) CONSTRAINT passwordless_users_phone_number_key
// UNIQUE,time_joined BIGINT NOT NULL, CONSTRAINT passwordless_users_pkey PRIMARY KEY (user_id));

@Entity
@Table(name = "passwordless_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordlessUsersDO extends PrimaryKeyFetchable {

    @Id
    @Column(length = 36)
    private String user_id;

    @Column(length = 256, unique = true)
    private String email;

    @Column(length = 256, unique = true)
    private String phone_number;

    @Column(nullable = false)
    private long time_joined;

    @Override
    public boolean equals(Object other) {
        if (other instanceof PasswordlessUsersDO) {
            PasswordlessUsersDO otherDO = (PasswordlessUsersDO) other;
            return otherDO.getUser_id().equals(this.getUser_id());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.getUser_id().hashCode();
    }

    @Override
    public Serializable getPrimaryKey() {
        return user_id;
    }

}
