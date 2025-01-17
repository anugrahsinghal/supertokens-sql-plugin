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

package io.supertokens.storage.sql.domainobject.session;

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

//CREATE TABLE IF NOT EXISTS session_access_token_signing_keys (created_at_time BIGINT NOT NULL,
//value TEXT,CONSTRAINT session_access_token_signing_keys_pkey PRIMARY KEY(created_at_time) );

@Entity
@Table(name = "session_access_token_signing_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SessionAccessTokenSigningKeysDO extends PrimaryKeyFetchable {

    @Id
    private long created_at_time;

    @Column(columnDefinition = "TEXT")
    private String value;

    @Override
    public boolean equals(Object other) {
        if (other instanceof SessionAccessTokenSigningKeysDO) {
            SessionAccessTokenSigningKeysDO otherDO = (SessionAccessTokenSigningKeysDO) other;
            return otherDO.getCreated_at_time() == this.getCreated_at_time();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(this.getCreated_at_time()).hashCode();
    }

    @Override
    public Serializable getPrimaryKey() {
        return created_at_time;
    }
}
